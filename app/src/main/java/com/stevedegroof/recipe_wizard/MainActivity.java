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

/**
 * The main activity of the Recipe Wizard application.
 * This activity displays a list of recipes and allows users to add, view, share, import, and export recipes.
 * It also includes a search functionality to filter recipes based on keywords.
 * <p>
 * Key functionalities:
 * <ul>
 *     <li>Displays a list of recipes using a RecyclerView.</li>
 *     <li>Allows adding new recipes through {@link AddRecipeActivity}.</li>
 *     <li>Allows viewing recipe details through {@link ViewRecipeActivity}.</li>
 *     <li>Enables sharing the entire recipe book as plain text.</li>
 *     <li>Supports importing recipes from a text file with options to append, overwrite, or merge.</li>
 *     <li>Supports exporting recipes to a text file.</li>
 *     <li>Provides a search field to filter recipes based on title, ingredients, or notes.</li>
 *     <li>Manages recipe data persistence through the {@link Recipes} singleton class.</li>
 *     <li>Handles file I/O operations asynchronously using an {@link ExecutorService}.</li>
 *     <li>Displays progress during import operations using a {@link ProgressReceiver} and an {@link AlertDialog}.</li>
 * </ul>
 */
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

    private boolean includeNotes = true;


    /**
     * Called when the activity is first created.
     * This method initializes the activity, sets up the UI,
     * and registers necessary listeners and receivers.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
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

        fileIoExecutor = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());


        buttonAdd = findViewById(R.id.button_add);
        buttonShare = findViewById(R.id.button_share);
        buttonImport = findViewById(R.id.button_import);
        buttonExport = findViewById(R.id.button_export);
        searchFieldEditText = findViewById(R.id.search_field_edit_text);


        buttonShare.setOnClickListener(this::checkIncludeNotes);
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

    /**
     * Called when the add recipe button is clicked.
     * Starts the AddRecipeActivity to allow the user to add a new recipe.
     *
     * @param view The view that was clicked (the add recipe button).
     */
    private void addRecipe(View view)
    {
        Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
        startActivity(intent);
    }

    /**
     * Filters the list of recipes based on the provided query.
     * <p>
     * If the query is empty, all recipes are shown, sorted by name.
     * If the query contains a single term, recipes are filtered if their text contains the term,
     * and the results are sorted by name.
     * If the query contains multiple comma-separated terms, recipes are scored based on how many
     * search terms are found in their ingredients. The score is a combination of the ratio of
     * matched terms to total search terms and the ratio of matched terms to total ingredients.
     * Recipes with a score greater than 0 are shown, sorted by score.
     * <p>
     * After filtering and sorting, the RecyclerView adapter is notified of the changes.
     * If a current recipe was selected before filtering, the list attempts to scroll to its
     * new position.
     *
     * @param query The search query string. Can be empty, a single term, or comma-separated terms.
     */
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
                recipeText = recipe.toPlainText(true).toLowerCase();
                ingredients = recipe.getIngredients().toLowerCase();
                termMatchCount = 0;
                if (singleTerm)
                {
                    score = (recipeText.contains(lowerCaseQuery)) ? 1 : 0;
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

            int position = (currentRecipe == null) ? -1 : findRecipePositionByText(currentRecipe.toPlainText(true), filteredRecipes);

            if (position != -1 && filteredRecipes.size() > 0)
            {
                RecyclerView.LayoutManager layoutManager = recyclerViewRecipes.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager)
                {
                    ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset((position > 1) ? (position - 2) : 0, 0);
                } else
                {
                    recyclerViewRecipes.scrollToPosition((position > 1) ? (position - 2) : 0);
                }
                Recipes.getInstance().setCurrentRecipe(position == -1 ? null : filteredRecipes.get(position));
            }
        }
    }


    /**
     * Finds the position of a recipe in a list by its plain text representation.
     *
     * @param recipeText  The plain text of the recipe to find.
     * @param recipesList The list of recipes to search within.
     * @return The index of the recipe in the list if found, otherwise -1.
     * Returns -1 if either recipeText or recipesList is null.
     */
    private int findRecipePositionByText(String recipeText, List<Recipe> recipesList)
    {
        if (recipeText == null || recipesList == null)
        {
            return -1;
        }
        for (int i = 0; i < recipesList.size(); i++)
        {
            Recipe recipe = recipesList.get(i);
            if (recipe != null && recipe.toPlainText(true) != null && recipe.toPlainText(true).equals(recipeText))
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * Called when the activity will start interacting with the user.
     * At this point your activity is at the top of the activity stack,
     * with user input going to it.
     * <p>
     * This method is called after {@link #onRestart} or {@link #onPause}.
     * It is where you should initialize any resources that were released
     * in {@link #onPause}. In this case, it re-initializes the
     * {@link RecipeAdapter} and repopulates the list of recipes.
     */
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

    /**
     * Loads recipes from storage, sorts them, and populates the local list of all recipes.
     * Then, it filters the recipes based on the current text in the search field.
     */
    void fillList()
    {
        Recipes.getInstance().load(getApplicationContext());
        Recipes.getInstance().sort();
        allRecipes = new ArrayList<>();
        allRecipes.addAll(Recipes.getInstance().getList());
        filterRecipes(searchFieldEditText.getText().toString());
    }


    /**
     * Load recipes from the selected file.
     * This method reads the content of the file specified by {@code importUri},
     * parses each recipe, and adds them to the current list of recipes.
     * The import behavior (append, overwrite, or merge) is determined by the
     * {@code importMode} variable.
     * Progress updates are sent via a BroadcastReceiver.
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
     * Parses the provided text of a single recipe and adds it to the specified {@link Recipes} object.
     * This method utilizes {@link RecipeParser} to extract details like title, servings,
     * ingredients, directions, and notes from the recipe text.
     *
     * @param recipes    The {@link Recipes} object to which the parsed recipe will be added.
     * @param recipeText The string containing the raw text of the recipe to be parsed.
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
        recipe.setNotes(parser.getNotes());
        recipes.getList().add(recipe);
    }


    /**
     * Checks if any recipes have notes and prompts the user whether to include them when sharing.
     * If no recipes have notes, it proceeds to share recipes without notes.
     * If recipes have notes, it displays a dialog asking the user if they want to include notes.
     * Based on the user's choice, it either includes or excludes notes and then proceeds to share the recipes.
     *
     * @param view The view that triggered this method, typically a button.
     */
    public void checkIncludeNotes(View view)
    {
        setIncludeNotes(true);
        boolean hasNotes = false;
        for (Recipe recipe : Recipes.getInstance().getList())
        {
            if (recipe.getNotes() != null && !recipe.getNotes().replaceAll("\n", "").trim().isEmpty())
            {
                hasNotes = true;
                break;
            }
        }
        if (hasNotes)
        {
            new AlertDialog.Builder(view.getContext())
                    .setTitle(R.string.share)
                    .setMessage(R.string.include_notes_prompt)
                    .setPositiveButton("Yes", (dialog, which) ->
                    {
                        setIncludeNotes(true);
                        shareRecipes(view);
                    })
                    .setNegativeButton("No", (dialog, which) ->
                    {
                        setIncludeNotes(false);
                        shareRecipes(view);
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        } else
        {
            setIncludeNotes(false);
            shareRecipes(view);
        }
    }

    /**
     * Share entire recipe book as plain text.
     * Writes the recipes to a temporary file, then creates an email intent
     * to share the file.
     *
     * @param view The view that triggered this method, used for context.
     */
    public void shareRecipes(View view)
    {
        try
        {

            FileOutputStream out = openFileOutput(BOOK_FILE_NAME, Context.MODE_PRIVATE);
            out.write(Recipes.getInstance().toPlainText(includeNotes).getBytes());
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
     * Called when the export button is clicked. Opens a dialog to save the recipes to a file.
     *
     * @param view The view that was clicked (the export button).
     */
    public void exportRecipes(View view)
    {
        openSaveDialog();
    }

    /**
     * Called when the import button is clicked. Opens the import dialog.
     *
     * @param view The view that was clicked (the import button).
     */
    public void importRecipes(View view)
    {
        openImportDialog();
    }


    /**
     * Presents a dialog for the user to choose a location and filename for exporting recipes.
     * This method launches an activity that allows the user to select a destination for the
     * recipe export file. The default filename for the export is specified by
     * {@link #DEFAULT_EXPORT_FILENAME}.
     */
    private void openSaveDialog()
    {
        exportFileLauncher.launch(DEFAULT_EXPORT_FILENAME);
    }

    /**
     * Opens a dialog for the user to select a text file to import recipes from.
     * It launches an activity that allows the user to pick a document with the MIME type "text/plain".
     * The result of this operation (the URI of the selected file) is handled by the {@code importFileLauncher}.
     */
    private void openImportDialog()
    {
        importFileLauncher.launch(new String[]{"text/plain"});
    }

    /**
     * Checks the import type and displays a dialog to the user to select the import mode.
     * The available import modes are:
     * - Append: Adds the imported recipes to the existing ones.
     * - Overwrite: Replaces all existing recipes with the imported ones.
     * - Merge: Merges the imported recipes with the existing ones, removing duplicates.
     *
     * @param view The view that triggered the import action.
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

    /**
     * Creates and displays an AlertDialog to prompt the user for the import type.
     * The dialog offers three options: "Add" (append), "Replace" (overwrite), and "Merge".
     * Based on the user's choice, it sets the {@code importMode} and then calls
     * {@link #importRecipes(View)} to proceed with the import.
     *
     * @param view The current view, used to get the context for the AlertDialog.
     * @return The created and shown AlertDialog.
     */
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
     * Writes the content of the recipe book to the specified URI.
     * This method is used for exporting the recipe book to a file.
     * It retrieves the recipes, converts them to plain text (optionally including notes),
     * and writes the text to the output stream associated with the given URI.
     * Toasts are displayed to indicate success or failure of the export operation.
     *
     * @param uri The URI of the file where the recipe book content will be written.
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
            outputStream.write(recipes.toPlainText(includeNotes).getBytes());
            outputStream.close();
            Toast.makeText(getApplicationContext(), "Recipes exported successfully.", Toast.LENGTH_SHORT).show();
        } catch (Exception e)
        {
            Toast.makeText(getApplicationContext(), "Unable to export. " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Reads the content of a recipe book file for import.
     * Displays a progress dialog while reading and processing the file.
     * Loads recipes from the file in a background thread.
     * Handles potential errors during file loading and displays a toast message.
     *
     * @param uri The URI of the recipe book file to import.
     */
    private void readFileContent(Uri uri)
    {
        importUri = uri;

        if (progressDialog != null && !progressDialog.isShowing())
        {
            progressDialog.show();
        } else if (progressDialog == null)
        {
            AlertDialog.Builder progBuilder = new AlertDialog.Builder(this);
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
     * Sets the progress of a file import operation.
     * Updates a TextView with the given message and a ProgressBar with the given progress value.
     * The ProgressBar's maximum value is set to 1000.
     *
     * @param message  The message to display, indicating the current stage of the import.
     * @param progress The current progress value, ranging from 0 to 1000.
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

    /**
     * Called when the activity is being destroyed.
     * This is the final call the activity receives.
     * It performs cleanup operations such as unregistering the broadcast receiver
     * and shutting down the executor service.
     */
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

        if (fileIoExecutor != null && !fileIoExecutor.isShutdown())
        {
            fileIoExecutor.shutdown();
        }
    }

    /**
     * Sets whether to include notes in shared or exported recipes.
     *
     * @param includeNotes true to include notes, false otherwise.
     */
    private void setIncludeNotes(boolean includeNotes)
    {
        this.includeNotes = includeNotes;
    }

    /**
     * Receives broadcast intents indicating the progress of file loading operations.
     * This class updates the UI to reflect the current stage of the file loading process,
     * such as reading the file, parsing recipes, merging recipes, or completion.
     * It uses constants to define the actions and extra keys for the intents it handles.
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
