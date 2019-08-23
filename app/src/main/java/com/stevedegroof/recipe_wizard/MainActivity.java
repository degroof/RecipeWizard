package com.stevedegroof.recipe_wizard;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;


/**
 * Main activity of app
 */
public class MainActivity extends StandardActivity
{
    private int importMode = IMPORT_APPEND;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
     * Display the list of recipe names
     */
    private void fillList()
    {
        LinearLayout recipeList = findViewById(R.id.recipeListLayout);
        recipeList.removeAllViews(); //delete any existing
        Recipes recipes = Recipes.getInstance();
        for (int i = 0; i < recipes.getList().size(); i++)
        {
            final TextView recipeNameText = new TextView(getApplicationContext());
            recipeNameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            recipeNameText.setPadding(16, 0, 0, 0);
            recipeNameText.setText(recipes.getList().get(i).getTitle());
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
            toast.setMargin(TOAST_MARGIN, TOAST_MARGIN);
            toast.show();
        }
    }

    /**
     * Read recipe book file for import
     *
     * @param uri
     */
    private void readFileContent(Uri uri)
    {
        String fileContents = "";
        String line = "";
        try
        {
            InputStream inputStream = getContentResolver().openInputStream(uri);

            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);

            while (line != null)
            {
                line = br.readLine();
                if (line != null) fileContents += line + "\n";
            }
            br.close();
        } catch (Exception e)
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Unable to import. " + e.getMessage(), Toast.LENGTH_LONG);
            toast.setMargin(TOAST_MARGIN, TOAST_MARGIN);
            toast.show();
        }
        String recipeText = "";
        Recipes recipes = Recipes.getInstance();
        if (importMode == IMPORT_OVERWRITE)
        {
            recipes.getList().clear(); //clear out all recipes
        }
        String[] recipesText = fileContents.split("\n");
        for (int i = 0; i < recipesText.length; i++)
        {
            line = recipesText[i];
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
        if (recipeText != null && !recipeText.isEmpty() && !recipeText.equals("null"))
        {
            try
            {
                parseAndAddRecipe(recipes, recipeText);
            } catch (Throwable t)
            {
            }
        }
        recipes.save(getApplicationContext());
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
        parser.setRawText(recipeText,true); //imported text is assumed to be formatted correctly
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
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    {
                        return;
                    }
                    openSaveDialog();
                }
            }
            break;
            case RequestFileReadPermissionID: //importing
            {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    {
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
        startActivityForResult(Intent.createChooser(intent, "Select a location to save file"), REQUEST_EXPORT);
    }

    /**
     * open dialog for import
     */
    private void openImportDialog()
    {
        Intent intent = new Intent()
                .setAction(Intent.ACTION_OPEN_DOCUMENT)
                .putExtra(Intent.EXTRA_TITLE, DEFAULT_EXPORT_FILENAME)
                .setType("text/plain");
        startActivityForResult(Intent.createChooser(intent, "Select a file to load"), REQUEST_IMPORT);
    }

    public void checkImportType(View view)
    {
        final View v = view;
        new AlertDialog.Builder(view.getContext())
                .setTitle("Import")
                .setMessage("You can either add to your existing recipes, or replace them. Which would you prefer?")
                .setPositiveButton("Add", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        importMode = IMPORT_APPEND;
                        importRecipes(v);
                    }
                })
                .setNegativeButton("Replace", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        importMode = IMPORT_OVERWRITE;
                        importRecipes(v);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
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
            Uri path = FileProvider.getUriForFile(context, "com.stevedegroof.recipe_wizard.provider", filelocation);
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("vnd.android.cursor.dir/email");
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Recipe Book");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Save attachment and import into Recipe Wizard");
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            emailIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(emailIntent, "Share Recipe Book"));
        } catch (Exception e)
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Unable to share recipe book. " + e.getMessage(), Toast.LENGTH_LONG);
            toast.setMargin(TOAST_MARGIN, TOAST_MARGIN);
            toast.show();
        }
    }
}
