package com.stevedegroof.recipe_wizard;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.*;


/**
 * Main activity of app
 */
public class MainActivity extends StandardActivity {

    private int importMode = IMPORT_APPEND;
    private String searchString = "";
    ProgressDialog pd;
    Uri importUri = null;
    boolean loadingFile = false;
    private ProgressReceiver progressReceiver;
    private AlertDialog progressDialog;
    View progressDialogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        progressReceiver = new ProgressReceiver();
        IntentFilter filter = new IntentFilter(ProgressReceiver.ACTION);
        ContextCompat.registerReceiver(this, progressReceiver, filter, ContextCompat.RECEIVER_EXPORTED);

        setContentView(R.layout.activity_main);
        SearchView searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                callSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                callSearch(newText);
                return true;
            }

        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(progressReceiver);
    }

    /**
     * user typed or deleted search text
     *
     * @param query
     */
    public void callSearch(String query) {
        searchString = query;
        fillList();
    }

    /**
     * Show keyboard if user taps anywhere in search field
     *
     * @param v
     */
    public void searchTapped(View v)
    {
        ((SearchView) findViewById(R.id.searchView)).onActionViewExpanded();
    }

    /**
     * get recipes and display the list
     */
    public void onResume()
    {
        super.onResume();
        Recipes.getInstance().load(getApplicationContext());
        fillList();
    }


    /**
     * Hide home item
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.action_home);
        if (item != null)
            item.setVisible(false);
        return result;
    }

    /**
     * Set the match score for the recipe.
     *
     * @param recipe
     * @param query
     */
    private void filterRecipe(Recipe recipe, String query)
    {
        String recipeText = recipe.toPlainText().toLowerCase();
        String ingredients = recipe.getIngredients();
        double score = 1d;
        if (query.contains(","))
        {
            Recipes.getInstance().setSortOn(Recipes.SCORE);
            int matchCount = 0;
            for (String searchTerm : query.split(","))
            {
                if (!searchTerm.trim().isEmpty() && ingredients.contains(searchTerm.toLowerCase().trim()))
                {
                    matchCount++;
                }
            }
            recipe.setMatchingIngredients(matchCount);
            recipe.setTotalIngredients(ingredients.split("\n").length);
            double ingredientCount = (double) (recipe.getTotalIngredients());
            score = 0d;
            if (ingredientCount > 0d) score = ((double) matchCount) / ingredientCount;
        } else
        {
            Recipes.getInstance().setSortOn(Recipes.NAME);
            score = recipeText.contains(query.toLowerCase().trim()) ? 1d : 0d;
        }
        recipe.setSortScore(score);
    }

    /**
     * Display the list of recipe names
     */
    private void fillList()
    {
        LinearLayout recipeList = findViewById(R.id.recipeListLayout);
        recipeList.removeAllViews(); //delete any existing
        Recipes recipes = Recipes.getInstance();
        if (searchString.isEmpty()) Recipes.getInstance().setSortOn(Recipes.NAME);
        for (int i = 0; i < recipes.getList().size(); i++)
        {
            filterRecipe(recipes.getList().get(i), searchString);
        }
        recipes.sort();
        for (int i = 0; i < recipes.getList().size(); i++)
        {
            if (recipes.getList().get(i).getSortScore() > 0d)
            {
                final TextView recipeNameText = new TextView(getApplicationContext());
                recipeNameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                recipeNameText.setBackgroundColor(getResources().getColor(R.color.colorTextBackground));
                recipeNameText.setTextColor(getResources().getColor(R.color.colorText));
                recipeNameText.setPadding(16, 0, 0, 0);
                String recipeTitle = recipes.getList().get(i).getTitle();
                if (recipes.getSortOn() == Recipes.SCORE)
                {
                    recipeTitle += " (" + recipes.getList().get(i).getMatchingIngredients() + " of " + recipes.getList().get(i).getTotalIngredients() + ")";
                }
                recipeNameText.setText(recipeTitle);
                recipeNameText.setHint(Integer.toString(i));
                recipeNameText.setOnClickListener(
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                view.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                                int index = Integer.parseInt(((TextView) view).getHint().toString());
                                viewRecipe(index);
                            }
                        }
                );
                recipeList.addView(recipeNameText);
            }
        }
    }

    /**
     * User clicked a recipe. Bring up view-only activity
     *
     * @param index
     */
    private void viewRecipe(int index)
    {
        Intent openViewActivity = new Intent(this, ViewRecipe.class);
        openViewActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        openViewActivity.putExtra(EXTRA_RECIPE, index);
        startActivity(openViewActivity);
    }

    /**
     * User clicked Add. Open add/edit activity
     *
     * @param view
     */
    public void addRecipe(View view)
    {
        Intent intent = new Intent(this, AddEditRecipe.class);
        startActivity(intent);
    }


    /**
     * User clicked export. Check permissions, then open export activity.
     *
     * @param view
     */
    public void exportRecipes(View view)
    {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    RequestFileWritePermissionID);
        } else
        {
            openSaveDialog();
        }
    }


    /**
     * User clicked Import. Check permissions, then open import activity.
     *
     * @param view
     */
    public void importRecipes(View view)
    {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    RequestFileReadPermissionID);
        } else
        {
            openImportDialog();
        }
    }


    /**
     * Returning from an activity.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EXPORT)
        {
            if (resultCode == RESULT_OK)
            {
                writeFileContent(data.getData());
            }
        } else if (requestCode == REQUEST_IMPORT)
        {
            if (resultCode == RESULT_OK)
            {
                readFileContent(data.getData());
            }
        }
    }

    /**
     * Write exported recipe book
     *
     * @param uri
     */
    private void writeFileContent(Uri uri)
    {
        try
        {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);

            Recipes recipes = Recipes.getInstance();

            outputStream.write(recipes.toPlainText().getBytes());

            outputStream.close();

        } catch (Exception e)
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Unable to export. " + e.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /**
     * Read recipe book file for import
     *
     * @param uri
     */
    private void readFileContent(Uri uri) {
        loadingFile = true;
        importUri = uri;
        //pd = ProgressDialog.show(this, "Loading recipes", "Please wait...");
        progressDialog.show();
        new FileLoadTask().execute();
    }

    /**
     * Load recipes
     * Load recipes
     */
    private void loadRecipes() {
        Intent intent = new Intent();
        intent.setAction(MainActivity.ProgressReceiver.ACTION);
        Recipes recipes = Recipes.getInstance();
        String recipeText = "";
        String fileContents = "";
        String line = "";
        long fileSize = 0;
        try {
            ContentResolver cr = getContentResolver();
            AssetFileDescriptor afd = cr.openAssetFileDescriptor(importUri, "r");
            fileSize = afd.getLength();
            afd.close();
            InputStream inputStream = cr.openInputStream(importUri);
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);
            while (line != null) {
                line = br.readLine();
                if (line != null) fileContents += line + "\n";
                intent.putExtra(ProgressReceiver.PROGRESS, ProgressReceiver.READING);
                intent.putExtra(ProgressReceiver.VALUE, (long) fileContents.length());
                intent.putExtra(ProgressReceiver.TOTAL, fileSize);
                getApplicationContext().sendBroadcast(intent);
            }
            br.close();
        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(), "Unable to import. " + e.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }
        if (importMode == IMPORT_OVERWRITE) {
            recipes.getList().clear(); //clear out all recipes
        }
        String[] recipesText = fileContents.split("\n");
        long currentlyLoaded = 0;
        for (int i = 0; i < recipesText.length; i++) {
            line = recipesText[i];
            currentlyLoaded += line.length() + 1;
            if (line.startsWith(RECIPE_BREAK_DETECT)) {
                intent.putExtra(ProgressReceiver.PROGRESS, ProgressReceiver.LOAD);
                intent.putExtra(ProgressReceiver.VALUE, (long) currentlyLoaded);
                intent.putExtra(ProgressReceiver.TOTAL, fileSize);
                getApplicationContext().sendBroadcast(intent);
                try {
                    parseAndAddRecipe(recipes, recipeText);
                } catch (Throwable e) {
                }
                recipeText = "";
            } else {
                recipeText += line + "\n";
            }
        }
        if (recipeText != null && !recipeText.isEmpty() && !recipeText.equals("null")) {
            try {
                parseAndAddRecipe(recipes, recipeText);
            } catch (Throwable t) {
            }
        }
        if (importMode == IMPORT_MERGE) {
            recipes.dedupe(getApplicationContext()); //merge recipes
        }
        intent.putExtra(ProgressReceiver.PROGRESS, ProgressReceiver.DONE);
        getApplicationContext().sendBroadcast(intent);
    }

    /**
     * Parse the text of one recipe, and add to the list
     *
     * @param recipes
     * @param recipeText
     */
    private void parseAndAddRecipe(Recipes recipes, String recipeText)
    {
        Recipe recipe = new Recipe();
        RecipeParser parser = new RecipeParser();
        parser.setRawText(recipeText, true); //imported text is assumed to be formatted correctly
        recipe.setTitle(parser.getTitle());
        recipe.setServings(parser.getServings());
        recipe.setMetric(parser.isMetric());
        recipe.setIngredients(parser.getIngredients());
        recipe.setDirections(parser.getDirections());
        recipes.getList().add(recipe);
    }

    /**
     * Respond to permissions request
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case RequestFileWritePermissionID: //exporting
            {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    openSaveDialog();
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    openSaveDialog();
                }
            }
            break;
            case RequestFileReadPermissionID: //importing
            {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    openImportDialog();
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    openImportDialog();
                }
            }
            break;
        }
    }

    /**
     * present dialog for export
     */
    private void openSaveDialog()
    {


        Intent intent = new Intent()
                .setAction(Intent.ACTION_CREATE_DOCUMENT)
                .putExtra(Intent.EXTRA_TITLE, DEFAULT_EXPORT_FILENAME)
                .setType("text/plain");
        startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.select_location_prompt)), REQUEST_EXPORT);
    }

    /**
     * open dialog for import
     */
    private void openImportDialog() {
        Intent intent = new Intent()
                .setAction(Intent.ACTION_OPEN_DOCUMENT)
                .putExtra(Intent.EXTRA_TITLE, DEFAULT_EXPORT_FILENAME)
                .setType("text/plain");
        startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.select_file_prompt)), REQUEST_IMPORT);
    }

    /**
     * @param view
     */
    public void checkImportType(View view) {
        AlertDialog.Builder progBuilder = new AlertDialog.Builder(view.getContext());
        progBuilder.setCancelable(false); // if you want user to wait for some process to finish,
        progBuilder.setView(R.layout.layout_loading_dialog);
        progressDialog = progBuilder.create();
        LayoutInflater inflater = this.getLayoutInflater();
        progressDialogView = inflater.inflate(R.layout.layout_loading_dialog, null);
        progressDialog.setView(progressDialogView);

        final View v = view;
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle(R.string.import_title);
        builder.setMessage(R.string.import_prompt);
        builder.setNeutralButton(R.string.add, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                importMode = IMPORT_APPEND;
                importRecipes(v);
            }
        });
        builder.setNegativeButton(R.string.replace, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                importMode = IMPORT_OVERWRITE;
                importRecipes(v);
            }
        });
        builder.setPositiveButton(R.string.merge, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                importMode = IMPORT_MERGE;
                importRecipes(v);
            }
        });

        builder.setIcon(android.R.drawable.ic_dialog_info);
        AlertDialog alert = builder.create();
        alert.show();


        Button btnNeutral = alert.getButton(AlertDialog.BUTTON_POSITIVE);
        Button btnPositive = alert.getButton(AlertDialog.BUTTON_POSITIVE);
        Button btnNegative = alert.getButton(AlertDialog.BUTTON_NEGATIVE);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();
        layoutParams.weight = -10;
        btnPositive.setLayoutParams(layoutParams);
        btnNegative.setLayoutParams(layoutParams);
        btnNeutral.setLayoutParams(layoutParams);
    }


    /**
     * Share entire recipe book as plain text
     *
     * @param view
     */
    public void shareRecipes(View view)
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        try
        {
            //write recipe book to private storage
            FileOutputStream out = openFileOutput(BOOK_FILE_NAME, Context.MODE_PRIVATE);
            out.write(Recipes.getInstance().toPlainText().getBytes());
            out.close();
            //share saved file
            Context context = getApplicationContext();
            File filelocation = new File(context.getFilesDir() + "/" + BOOK_FILE_NAME);
            Uri path = FileProvider.getUriForFile(context, getResources().getString(R.string.provider_name), filelocation);
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("vnd.android.cursor.dir/email");
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.recipe_book));
            emailIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.email_body));
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            emailIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.share_book)));
        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(), "Unable to share recipe book. " + e.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }
    }


    /**
     * Loads import file asychronously
     */
    private class FileLoadTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {

                loadRecipes();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    /**
     * receives notifications of file load progress
     */
    protected class ProgressReceiver extends BroadcastReceiver {
        public static final String ACTION = "com.stevedegroof.recipe_wizard.ACTION_PROGRESS";
        public static final String DONE = "done";
        public static final String MERGE = "merge";
        public static final String LOAD = "load";
        public static final String READING = "reading";
        public static final String PROGRESS = "progress";
        public static final String TOTAL = "total";
        public static final String VALUE = "value";

        @Override
        public void onReceive(Context context, Intent intent) {
            String progress = intent.getStringExtra(PROGRESS);
            if (progress != null) {
                long value = intent.getLongExtra(VALUE, 0);
                long total = intent.getLongExtra(TOTAL, 10L);
                int permil = 1000;
                if (value < total) {
                    permil = (int) (value * 1000L / total);
                }
                if (progress.equals(READING)) {
                    setProgress("Loading file", permil);
                } else if (progress.equals(LOAD)) {
                    setProgress("Getting recipes", permil);
                } else if (progress.equals(MERGE)) {
                    setProgress("Merging recipes", permil);
                } else if (progress.equals(DONE)) {
                    if (progressDialog != null) progressDialog.dismiss();
                    Recipes.getInstance().save(getApplicationContext());
                    fillList();
                }
            }
        }
    }

    /**
     * Set the file import progress
     *
     * @param message
     * @param progress
     */
    private void setProgress(String message, int progress) {
        TextView messageView = progressDialogView.findViewById(R.id.loadingProgressText);
        if (messageView != null) {
            messageView.setText(message);
        }
        ProgressBar progressBar = progressDialogView.findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setMax(1000);
            progressBar.setProgress(progress);
        }
    }

}
