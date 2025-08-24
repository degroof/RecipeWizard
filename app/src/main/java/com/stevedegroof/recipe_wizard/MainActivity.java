package com.stevedegroof.recipe_wizard;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
{
    public static final int IMPORT_APPEND = 10002;
    public static final int IMPORT_OVERWRITE = 10003;
    public static final int IMPORT_MERGE = 10006;
    public static final String DEFAULT_EXPORT_FILENAME = "RecipesExport.txt";
    public static final String RECIPE_BREAK_DETECT = "------";
    public static final String BOOK_FILE_NAME = "RecipeBook.txt";
    public static final String RECIPE_FILE_NAME = "Recipes.json";
    public static final String RECIPE_BREAK = "-----------------";
    Uri importUri = null;
    View progressDialogView;
    private int importMode = IMPORT_APPEND;
    private RecyclerView recyclerViewRecipes;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> allRecipes;
    private List<Recipe> filteredRecipes;
    private EditText searchFieldEditText;
    private ImageButton buttonAdd, buttonShare, buttonImport, buttonExport;
    private ProgressBar progressBar;
    private ProgressReceiver progressReceiver;
    private AlertDialog progressDialog;
    private ActivityResultLauncher<String> exportFileLauncher;
    private ActivityResultLauncher<String[]> importFileLauncher;
    private ExecutorService fileIoExecutor;
    private Handler mainThreadHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        insetsController.setAppearanceLightStatusBars(true);


        progressBar = findViewById(R.id.progressBar);


        if (progressBar != null)
        {
            progressBar.setVisibility(View.GONE);
        }
        progressReceiver = new ProgressReceiver();
        IntentFilter filter = new IntentFilter(ProgressReceiver.ACTION);
        ContextCompat.registerReceiver(this, progressReceiver, filter, ContextCompat.RECEIVER_EXPORTED);


        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fileIoExecutor = Executors.newSingleThreadExecutor(); // Creates a single thread for sequential file operations
        mainThreadHandler = new Handler(Looper.getMainLooper()); // Handler to post results to the main thread


        buttonAdd = findViewById(R.id.button_add);
        buttonShare = findViewById(R.id.button_share);
        buttonImport = findViewById(R.id.button_import);
        buttonExport = findViewById(R.id.button_export);
        searchFieldEditText = findViewById(R.id.search_field_edit_text);


        buttonShare.setOnClickListener(this::shareRecipes);
        buttonImport.setOnClickListener(this::checkImportType);
        buttonExport.setOnClickListener(this::exportRecipes);
        buttonAdd.setOnClickListener(this::addRecipe);


        searchFieldEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                Recipes.getInstance().setCurrentRecipe(null);
                filterRecipes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        recyclerViewRecipes = findViewById(R.id.recycler_view_recipes);
        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerViewRecipes.getLayoutManager();
        if (layoutManager != null)
        {
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                    recyclerViewRecipes.getContext(),
                    layoutManager.getOrientation()
            );
            recyclerViewRecipes.addItemDecoration(dividerItemDecoration);
        }

        exportFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/plain"),
                uri ->
                {
                    if (uri != null)
                    {
                        writeFileContent(uri);
                    } else
                    {
                        Toast.makeText(MainActivity.this, "Export cancelled or failed.", Toast.LENGTH_SHORT).show();
                    }
                });


        importFileLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri ->
                {
                    if (uri != null)
                    {
                        readFileContent(uri);
                    } else
                    {

                        Toast.makeText(MainActivity.this, "Import cancelled or failed.", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void addRecipe(View view)
    {
        Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
        startActivity(intent);
    }

    private void filterRecipes(String query)
    {
        String recipeText;
        String ingredients;
        int termMatchCount;
        if (filteredRecipes == null) filteredRecipes = new ArrayList<>();
        if (allRecipes == null) allRecipes = Recipes.getInstance().getList();
        filteredRecipes.clear();
        if (query.isEmpty())
        {
            Recipes.getInstance().setSortOn(Recipes.NAME);
            Recipes.getInstance().sort();
            filteredRecipes.addAll(allRecipes);
        } else
        {
            String lowerCaseQuery = query.toLowerCase().trim();
            String[] searchTerms = lowerCaseQuery.split(",");
            boolean singleTerm = !(query.trim().contains(","));
            if (singleTerm)
            {
                Recipes.getInstance().setSortOn(Recipes.NAME);
            } else
            {
                Recipes.getInstance().setSortOn(Recipes.SCORE);
            }

            for (Recipe recipe : allRecipes)
            {
                double termScore = 0;
                double ingredientScore = 0;
                double score = 1;
                recipeText = recipe.toPlainText().toLowerCase();
                ingredients = recipe.getIngredients().toLowerCase();
                termMatchCount = 0;
                if (singleTerm)
                {
                    score = (recipeText.contains(lowerCaseQuery))?1:0;
                } else
                {
                    String[] ingredientList = ingredients.split("\n");
                    double termCount = searchTerms.length;
                    for (String term : searchTerms)
                    {
                        if (!term.trim().isEmpty() && ingredients.contains(term.trim()))
                        {
                            termMatchCount++;
                        }
                    }
                    if (termCount > 0d) termScore = ((double) termMatchCount) / termCount;
                    double ingredientCount = ingredientList.length;
                    if (ingredientCount > 0d)
                        ingredientScore = ((double) termMatchCount) / ingredientCount;
                    score = (termScore + ingredientScore) / 2.0;
                }
                recipe.setSortScore(score);
            }
            Recipes.getInstance().sort();
            for (Recipe recipe : Recipes.getInstance().getList())
            {
                if (recipe.getSortScore() > 0) filteredRecipes.add(recipe);
            }
        }
        if (recipeAdapter != null)
        {
            recipeAdapter.notifyDataSetChanged();
            Recipe currentRecipe = Recipes.getInstance().getCurrentRecipe();

            int position = (currentRecipe == null) ? -1 : findRecipePositionByText(currentRecipe.toPlainText(), filteredRecipes);

            if (position != -1 && filteredRecipes.size()>0)
            {
                RecyclerView.LayoutManager layoutManager = recyclerViewRecipes.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager)
                {
                    ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset((position>1)?(position-2):0, 0);
                } else
                {
                    recyclerViewRecipes.scrollToPosition((position>1)?(position-2):0);
                }
                Recipes.getInstance().setCurrentRecipe(position==-1?null:filteredRecipes.get(position));
            }
        }
    }


    private int findRecipePositionByTitle(String recipeTitle, List<Recipe> recipesList)
    {
        if (recipeTitle == null || recipesList == null)
        {
            return -1;
        }
        for (int i = 0; i < recipesList.size(); i++)
        {
            Recipe recipe = recipesList.get(i);
            if (recipe != null && recipe.getTitle() != null && recipe.getTitle().equals(recipeTitle))
            {
                return i;
            }
        }
        return -1;
    }

    private int findRecipePositionByText(String recipeText, List<Recipe> recipesList)
    {
        if (recipeText == null || recipesList == null)
        {
            return -1;
        }
        for (int i = 0; i < recipesList.size(); i++)
        {
            Recipe recipe = recipesList.get(i);
            if (recipe != null && recipe.toPlainText() != null && recipe.toPlainText().equals(recipeText))
            {
                return i;
            }
        }
        return -1;
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        filteredRecipes = new ArrayList<>();
        recipeAdapter = new RecipeAdapter(filteredRecipes, item ->
        {
            Intent intent = new Intent(MainActivity.this, ViewRecipeActivity.class);
            Recipes.getInstance().setCurrentRecipe(item);
            startActivity(intent);
        });
        recyclerViewRecipes.setAdapter(recipeAdapter);
        fillList();
    }

    void fillList()
    {
        Recipes.getInstance().load(getApplicationContext());
        Recipes.getInstance().sort();
        allRecipes = new ArrayList<>();
        allRecipes.addAll(Recipes.getInstance().getList());
        filterRecipes(searchFieldEditText.getText().toString());
    }


    /**
     * Load recipes
     */
    private void loadRecipes()
    {
        Intent intent = new Intent();
        intent.setAction(MainActivity.ProgressReceiver.ACTION);
        Recipes recipes = Recipes.getInstance();
        String recipeText = "";
        StringBuilder fileContents = new StringBuilder();
        String line = "";
        long fileSize = 0;
        try
        {
            ContentResolver cr = getContentResolver();
            AssetFileDescriptor afd = cr.openAssetFileDescriptor(importUri, "r");
            fileSize = afd.getLength();
            afd.close();
            InputStream inputStream = cr.openInputStream(importUri);
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);
            while (line != null)
            {
                line = br.readLine();
                if (line != null) fileContents.append(line).append("\n");
                intent.putExtra(ProgressReceiver.PROGRESS, ProgressReceiver.READING);
                intent.putExtra(ProgressReceiver.VALUE, (long) fileContents.length());
                intent.putExtra(ProgressReceiver.TOTAL, fileSize);
                getApplicationContext().sendBroadcast(intent);
            }
            br.close();
        } catch (Exception e)
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Unable to import. " + e.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }
        if (importMode == IMPORT_OVERWRITE)
        {
            recipes.getList().clear();
        }
        String[] recipesText = fileContents.toString().split("\n");
        for (String s : recipesText)
        {
            line = s;
            if (line.startsWith(RECIPE_BREAK_DETECT))
            {
                try
                {
                    parseAndAddRecipe(recipes, recipeText);
                } catch (Throwable e)
                {
                }
                recipeText = "";
            } else
            {
                recipeText += line + "\n";
            }
        }
        if (!recipeText.isEmpty() && !recipeText.equals("null"))
        {
            try
            {
                parseAndAddRecipe(recipes, recipeText);
            } catch (Throwable t)
            {
            }
        }
        if (importMode == IMPORT_MERGE)
        {
            recipes.dedupe(getApplicationContext());
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
        parser.setRawText(recipeText, true);
        recipe.setTitle(parser.getTitle());
        recipe.setServings(Integer.toString(parser.getServings()));
        recipe.setMetric(parser.isMetric());
        recipe.setIngredients(parser.getIngredients());
        recipe.setDirections(parser.getDirections());
        recipes.getList().add(recipe);
    }

    /**
     * Share entire recipe book as plain text
     *
     * @param view
     */
    public void shareRecipes(View view)
    {
        try
        {

            FileOutputStream out = openFileOutput(BOOK_FILE_NAME, Context.MODE_PRIVATE);
            out.write(Recipes.getInstance().toPlainText().getBytes());
            out.close();

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
        } catch (Exception e)
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Unable to share recipe book. " + e.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /**
     * @param view
     */
    public void exportRecipes(View view)
    {
        openSaveDialog();
    }

    /**
     * @param view
     */
    public void importRecipes(View view)
    {
        openImportDialog();
    }


    /**
     * present dialog for export
     */
    private void openSaveDialog()
    {
        exportFileLauncher.launch(DEFAULT_EXPORT_FILENAME);
    }

    /**
     * open dialog for import
     */
    private void openImportDialog()
    {
        importFileLauncher.launch(new String[]{"text/plain"});
    }

    /**
     * @param view
     */
    public void checkImportType(View view)
    {
        AlertDialog.Builder progBuilder = new AlertDialog.Builder(view.getContext());
        progBuilder.setCancelable(false);
        progBuilder.setView(R.layout.layout_loading_dialog);
        progressDialog = progBuilder.create();
        LayoutInflater inflater = this.getLayoutInflater();
        progressDialogView = inflater.inflate(R.layout.layout_loading_dialog, null);
        progressDialog.setView(progressDialogView);

        AlertDialog alert = getAlertDialog(view);


        Button btnNeutral = alert.getButton(AlertDialog.BUTTON_POSITIVE);
        Button btnPositive = alert.getButton(AlertDialog.BUTTON_POSITIVE);
        Button btnNegative = alert.getButton(AlertDialog.BUTTON_NEGATIVE);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();
        layoutParams.weight = -10;
        btnPositive.setLayoutParams(layoutParams);
        btnNegative.setLayoutParams(layoutParams);
        btnNeutral.setLayoutParams(layoutParams);
    }

    @NonNull
    private AlertDialog getAlertDialog(View view)
    {
        final View v = view;
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle(R.string.import_title);
        builder.setMessage(R.string.import_prompt);
        builder.setNeutralButton(R.string.add, (dialog, which) ->
        {
            importMode = IMPORT_APPEND;
            importRecipes(v);
        });
        builder.setNegativeButton(R.string.replace, (dialog, which) ->
        {
            importMode = IMPORT_OVERWRITE;
            importRecipes(v);
        });
        builder.setPositiveButton(R.string.merge, (dialog, which) ->
        {
            importMode = IMPORT_MERGE;
            importRecipes(v);
        });

        builder.setIcon(android.R.drawable.ic_dialog_info);
        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }

    /**
     * Write exported recipe book
     */
    private void writeFileContent(Uri uri)
    {
        try
        {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream == null)
            {
                Toast.makeText(getApplicationContext(), "Unable to open output stream for export.", Toast.LENGTH_LONG).show();
                return;
            }
            Recipes recipes = Recipes.getInstance();
            outputStream.write(recipes.toPlainText().getBytes());
            outputStream.close();
            Toast.makeText(getApplicationContext(), "Recipes exported successfully.", Toast.LENGTH_SHORT).show();
        } catch (Exception e)
        {
            Toast.makeText(getApplicationContext(), "Unable to export. " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Read recipe book file for import
     *
     * @param uri
     */
    private void readFileContent(Uri uri)
    {
        importUri = uri;

        // Show progress dialog on the UI thread
        if (progressDialog != null && !progressDialog.isShowing())
        {
            progressDialog.show();
        } else if (progressDialog == null)
        {
            // Re-initialize progressDialog if it's null (e.g., after screen rotation if not handled)
            // This part might need adjustment based on how your progressDialog is managed across config changes
            AlertDialog.Builder progBuilder = new AlertDialog.Builder(this); // Use 'this' for context
            progBuilder.setCancelable(false);
            LayoutInflater inflater = this.getLayoutInflater();
            progressDialogView = inflater.inflate(R.layout.layout_loading_dialog, null);
            progBuilder.setView(progressDialogView);
            progressDialog = progBuilder.create();
            progressDialog.show();
        }

        fileIoExecutor.execute(() ->
        {
            try
            {
                loadRecipes();
            } catch (Exception e)
            {
                mainThreadHandler.post(() ->
                {
                    if (progressDialog != null && progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(MainActivity.this, "Error loading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Set the file import progress
     *
     * @param message
     * @param progress
     */
    private void setProgress(String message, int progress)
    {
        TextView messageView = progressDialogView.findViewById(R.id.loadingProgressText);
        if (messageView != null)
        {
            messageView.setText(message);
        }
        ProgressBar progressBar = progressDialogView.findViewById(R.id.progressBar);
        if (progressBar != null)
        {
            progressBar.setMax(1000);
            progressBar.setProgress(progress);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (progressReceiver != null)
        {
            try
            {
                unregisterReceiver(progressReceiver);
            } catch (IllegalArgumentException e)
            {
            }
        }

        // Shut down the ExecutorService
        if (fileIoExecutor != null && !fileIoExecutor.isShutdown())
        {
            fileIoExecutor.shutdown(); // Initiates an orderly shutdown
        }
    }

    /**
     * receives notifications of file load progress
     */
    protected class ProgressReceiver extends BroadcastReceiver
    {
        public static final String ACTION = "com.stevedegroof.recipe_wizard.ACTION_PROGRESS";
        public static final String DONE = "done";
        public static final String MERGE = "merge";
        public static final String LOAD = "load";
        public static final String READING = "reading";
        public static final String PROGRESS = "progress";
        public static final String TOTAL = "total";
        public static final String VALUE = "value";

        @Override
        public void onReceive(Context context, Intent intent)
        {
            String progress = intent.getStringExtra(PROGRESS);
            if (progress != null)
            {
                long value = intent.getLongExtra(VALUE, 0);
                long total = intent.getLongExtra(TOTAL, 10L);
                int permil = 1000;
                if (value < total)
                {
                    permil = (int) (value * 1000L / total);
                }
                switch (progress)
                {
                    case READING:
                        setProgress("Loading file", permil);
                        break;
                    case LOAD:
                        setProgress("Getting recipes", permil);
                        break;
                    case MERGE:
                        setProgress("Merging recipes", permil);
                        break;
                    case DONE:
                        if (progressDialog != null) progressDialog.dismiss();
                        Recipes.getInstance().save(getApplicationContext());
                        fillList();
                        break;
                }
            }
        }
    }

}
