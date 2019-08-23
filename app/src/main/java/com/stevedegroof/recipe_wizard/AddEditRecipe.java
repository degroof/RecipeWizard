package com.stevedegroof.recipe_wizard;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;


/**
 * Adds a recipe to the book
 */
public class AddEditRecipe extends StandardActivity
{

    private int index = -1;
    private String rawText = "";

    /**
     * Called on create
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        rawText = "";
        setContentView(R.layout.activity_add_edit_recipe);
    }

    /**
     * If user hits back arrow
     */
    @Override
    public void onBackPressed()
    {
        boolean changed = false;
        TextView view = findViewById(R.id.addEditRecipeNameText);
        String name = view.getText().toString();
        view = findViewById(R.id.addEditRecipeIngredientsText);
        String ingedients = view.getText().toString();
        view = findViewById(R.id.addEditRecipeDirectionsText);
        String directions = view.getText().toString();
        view = findViewById(R.id.addEditRecipeServingsText);
        String servings = view.getText().toString();
        if (index != -1) //if recipe was edited and user didn't hit save
        {
            Recipe recipe = Recipes.getInstance().getList().get(index);
            if (!recipe.getIngredients().equals(ingedients) || !recipe.getDirections().equals(directions) || !recipe.getTitle().equals(name) || !Integer.toString(recipe.getServings()).equals(servings))
            {
                changed = true;
            }
        } else // recipe was added and user didn't hit save
        {
            if (!ingedients.equals("") || !directions.equals("") || !name.equals(""))
                changed = true;
        }

        if (changed) //display "are you sure"
        {
            new AlertDialog.Builder(view.getContext())
                    .setTitle("Exit")
                    .setMessage("Are you sure you want to continue without saving?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            AddEditRecipe.super.onBackPressed();
                        }
                    })

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else //go back
        {
            AddEditRecipe.super.onBackPressed();
        }
    }

    /**
     * load a recipe for viewing
     *
     * @param recipeIndex
     */
    private void loadRecipe(int recipeIndex)
    {
        Recipe recipe = Recipes.getInstance().getList().get(recipeIndex);
        TextView view = findViewById(R.id.addEditRecipeNameText);
        view.setText(recipe.getTitle());
        view = findViewById(R.id.addEditRecipeIngredientsText);
        view.setText(recipe.getIngredients());
        view = findViewById(R.id.addEditRecipeDirectionsText);
        view.setText(recipe.getDirections());
        view = findViewById(R.id.addEditRecipeServingsText);
        view.setText(Integer.toString(recipe.getServings()));
        Switch metricSwitch = findViewById(R.id.addEditRecipeUnitsSwitch);
        metricSwitch.setChecked(recipe.isMetric());
    }

    /**
     * save the recipe
     *
     * @param v
     */
    public void saveRecipe(View v)
    {
        Recipes recipes = Recipes.getInstance();
        Recipe recipe;
        if (index == -1) //new recipe
        {
            recipe = new Recipe();
            recipes.getList().add(recipe);
        } else //existing recipe
        {
            recipe = recipes.getList().get(index);
        }
        TextView view = findViewById(R.id.addEditRecipeNameText);
        recipe.setTitle(view.getText().toString());
        view = findViewById(R.id.addEditRecipeIngredientsText);
        recipe.setIngredients(view.getText().toString());
        view = findViewById(R.id.addEditRecipeDirectionsText);
        recipe.setDirections(view.getText().toString());
        view = findViewById(R.id.addEditRecipeServingsText);
        try
        {
            recipe.setServings(Integer.parseInt(view.getText().toString()));
        } catch (NumberFormatException e)
        {
            recipe.setServings(4);
        }
        Switch metricSwitch = findViewById(R.id.addEditRecipeUnitsSwitch);
        recipe.setMetric(metricSwitch.isChecked());
        recipes.sort();
        index = recipes.getList().indexOf(recipe);
        recipes.save(getApplicationContext());
        Intent intent = new Intent();
        intent.putExtra(CommonActivity.EXTRA_RECIPE, index);
        setResult(ViewRecipe.RESULT_OK, intent);
        finish();
    }

    /**
     * the delete or capture button was pressed (edit vs add)
     *
     * @param view
     */
    public void deleteOrCapture(View view)
    {
        if (index == -1) //capture on add
        {
            Intent i = new Intent(this, CaptureText.class);
            startActivityForResult(i, REQUEST_CAPTURE_TEXT);
        } else //delete on edit
        {
            new AlertDialog.Builder(view.getContext())
                    .setTitle("Delete")
                    .setMessage("Are you sure you want to delete?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            Recipes recipes = Recipes.getInstance();
                            recipes.getList().remove(index);
                            recipes.save(getApplicationContext());
                            Intent intent = new Intent();
                            setResult(CommonActivity.RESULT_DELETE, intent);
                            finish();
                        }
                    })
                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        }
    }

    /**
     * called on resume or create
     */
    public void onResume()
    {
        super.onResume();
        Intent intent = getIntent();
        index = intent.getIntExtra(CommonActivity.EXTRA_RECIPE, -1);
        if (index >= 0) //load recipe for edit
        {
            loadRecipe(index);
        } else //change delete button to capture
        {
            ImageButton deleteButton = findViewById(R.id.addEditRecipeDeleteCaptureButton);
            deleteButton.setImageResource(R.drawable.baseline_capture_black_18dp);
        }
    }

    /**
     * called when capture is complete
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE_TEXT)
        {
            if (resultCode == RESULT_OK) //fill in recipe fields from captured text
            {
                rawText += data.getStringExtra(EXTRA_RAW_TEXT);
                RecipeParser recipeParser = new RecipeParser();
                recipeParser.setRawText(rawText);
                TextView targetView = findViewById(R.id.addEditRecipeNameText);
                targetView.setText(recipeParser.getTitle());
                targetView = findViewById(R.id.addEditRecipeIngredientsText);
                targetView.setText(recipeParser.getIngredients());
                targetView = findViewById(R.id.addEditRecipeDirectionsText);
                targetView.setText(recipeParser.getDirections());
                targetView = findViewById(R.id.addEditRecipeServingsText);
                targetView.setText(Integer.toString(recipeParser.getServings()));
                Switch metricSwitch = findViewById(R.id.addEditRecipeUnitsSwitch);
                metricSwitch.setChecked(recipeParser.isMetric());
                Toast toast = Toast.makeText(getApplicationContext(), R.string.capturedPrompt, Toast.LENGTH_LONG);
                toast.setMargin(TOAST_MARGIN, TOAST_MARGIN);
                toast.show();
            }
        }
    }

}