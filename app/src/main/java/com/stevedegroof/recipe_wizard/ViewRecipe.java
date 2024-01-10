package com.stevedegroof.recipe_wizard;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

/**
 * show recipe in view-only mode
 */
public class ViewRecipe extends StandardActivity
{
    boolean isMetric = false;
    Recipe recipe = null;
    Recipe recalculatedRecipe = null;
    private int index = -1;
    private int servings = -1;
    private int shareMode = SHARE_ORIGINAL;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = getIntent();
        index = intent.getIntExtra(CommonActivity.EXTRA_RECIPE, -1);
        setContentView(R.layout.activity_view_recipe);
    }

    /**
     * display the recipe
     *
     * @param recipeIndex
     */
    private void loadRecipe(int recipeIndex) {
        int numberOfRecipes = Recipes.getInstance().getList().size();
        if (index >= numberOfRecipes || index < 0) goHome();
        recipe = Recipes.getInstance().getList().get(recipeIndex);
        if (servings < 0) servings = recipe.getServings();
        TextView view = findViewById(R.id.viewRecipeNameText);
        view.setText(recipe.getTitle());
        view = findViewById(R.id.servingsText);
        view.setText("" + servings + " " + getResources().getString(R.string.servings));
        view = findViewById(R.id.viewRecipeIngredientsText);
        view.setText(recipe.getIngredients());
        view = findViewById(R.id.viewRecipeDirectionsText);
        String directions = recipe.getDirections();
        String[] lines = directions.split("\n");
        directions = "";
        for (int i = 0; i < lines.length; i++)
        {
            if (!lines[i].isEmpty())
            {
                directions += "" + (i + 1) + ". " + lines[i] + "\n\n";
            }
        }

        view.setText(directions);
        ToggleButton metricSwitch = findViewById(R.id.measurementSwitch);
        metricSwitch.setChecked(recipe.isMetric());
        servings = recipe.getServings();
        isMetric = recipe.isMetric();
    }

    /**
     * set imperial or metric
     *
     * @param v
     */
    public void setMeasurementSystem(View v)
    {
        ToggleButton metricSwitch = findViewById(R.id.measurementSwitch);
        isMetric = metricSwitch.isChecked();
        recalculateRecipe();
    }

    /**
     * increase the number of servings by 1
     *
     * @param v
     */
    public void increaseServings(View v)
    {
        servings++;
        recalculateRecipe();
    }

    /**
     * decrease the number of servings by 1
     *
     * @param v
     */
    public void decreaseServings(View v)
    {
        servings--;
        if (servings == 0)
        {
            servings = 1;
        } else
        {
            recalculateRecipe();
        }
    }


    /**
     * Recalculate ingredients and directions based on measurement system and servings
     */
    private void recalculateRecipe()
    {
        recalculatedRecipe = new Recipe();
        String ingredients = recipe.getIngredients();
        String directions = recipe.getDirections();
        if (servings != recipe.getServings() || isMetric != recipe.isMetric())
        {
            String ingredientArray[] = ingredients.split("\n");
            ingredients = "";
            for (int i = 0; i < ingredientArray.length; i++)
            {
                ingredients += new UnitsConverter().convert(ingredientArray[i], recipe.getServings(), servings,
                        recipe.isMetric() ? UnitsConverter.METRIC : UnitsConverter.IMPERIAL,
                        isMetric ? UnitsConverter.METRIC : UnitsConverter.IMPERIAL, true) + "\n";
            }
            recalculatedRecipe.setIngredients(ingredients);
            recalculatedRecipe.setMetric(isMetric);
            recalculatedRecipe.setServings(servings);
            recalculatedRecipe.setTitle(recipe.getTitle());
            directions = new UnitsConverter().convertDirections(directions, recipe.getServings(), servings,
                    recipe.isMetric() ? UnitsConverter.METRIC : UnitsConverter.IMPERIAL,
                    isMetric ? UnitsConverter.METRIC : UnitsConverter.IMPERIAL, recipe.getExcludedPhrases()) + "\n";
            recalculatedRecipe.setDirections(directions);
        }
        String[] lines = directions.split("\n");
        directions = "";
        for (int i = 0; i < lines.length; i++)
        {
            if (!lines[i].isEmpty())
            {
                directions += "" + (i + 1) + ". " + lines[i] + "\n\n";
            }
        }
        TextView view = findViewById(R.id.servingsText);
        view.setText("" + servings + " " + getResources().getString(R.string.servings));
        view = findViewById(R.id.viewRecipeIngredientsText);
        view.setText(ingredients);
        view = findViewById(R.id.viewRecipeDirectionsText);
        view.setText(directions);
    }

    public void onResume()
    {
        super.onResume();
        int currentServings = -1;
        boolean currentIsMetric;
        if (index >= 0)
        {
            currentServings = servings;
            currentIsMetric = isMetric;
            loadRecipe(index);
            if (currentServings > 0)
            {
                servings = currentServings;
                isMetric = currentIsMetric;
            }
        }

        TextView view = findViewById(R.id.servingsText);
        view.setText("" + servings + " " + getResources().getString(R.string.servings));
        ToggleButton metricSwitch = findViewById(R.id.measurementSwitch);
        metricSwitch.setChecked(isMetric);
        recalculateRecipe();

    }

    /**
     * user clicked Edit
     *
     * @param view
     */
    public void editRecipe(View view)
    {
        Intent openViewActivity = new Intent(this, AddEditRecipe.class);
        openViewActivity.putExtra(CommonActivity.EXTRA_RECIPE, index);
        startActivityForResult(openViewActivity, 1);
    }

    /**
     * user clicked share. share current recipe
     *
     * @param view
     */
    public void shareRecipe(View view)
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.recipe_prefix) + " " + recipe.getTitle());
        if (shareMode == SHARE_ORIGINAL)
        {
            intent.putExtra(Intent.EXTRA_TEXT, recipe.toPlainText());
        } else
        {
            intent.putExtra(Intent.EXTRA_TEXT, recalculatedRecipe.toPlainText());
        }

        startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_recipe_prefix) + " " + recipe.getTitle()));
    }

    /**
     * before sharing recipe, ask user if they want to share the original or converted one
     *
     * @param view
     */
    public void checkShareType(View view)
    {
        if (recipe.getServings() == servings && recipe.isMetric() == isMetric)
        {
            shareRecipe(view);
        } else
        {
            final View v = view;
            new AlertDialog.Builder(view.getContext())
                    .setTitle(R.string.share)
                    .setMessage(R.string.share_recipe_prompt)
                    .setPositiveButton(R.string.original, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            shareMode = SHARE_ORIGINAL;
                            shareRecipe(v);
                        }
                    })
                    .setNegativeButton(R.string.converted, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            shareMode = SHARE_CONVERTED;
                            shareRecipe(v);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        }
    }


    /**
     * before deleting recipe, ask user
     *
     * @param view
     */
    public void checkDelete(View view)
    {
        final View v = view;
        new AlertDialog.Builder(view.getContext())
                .setTitle(R.string.delete_tc)
                .setMessage(R.string.delete_recipe_prompt)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        deleteRecipe(v);
                    }
                })
                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    /**
     * delete the current recipe
     *
     * @param v
     */
    private void deleteRecipe(View v)
    {
        Recipes.getInstance().getList().remove(recipe);
        Recipes.getInstance().save(getApplicationContext());
        finish();
    }


    /**
     * returning from add or edit
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1)
        {
            if (resultCode == RESULT_DELETE)
            {
                finish();
            } else if (resultCode == RESULT_OK)
            {
                try
                {
                    index = data.getIntExtra(CommonActivity.EXTRA_RECIPE, -1);
                } catch (Throwable t)
                {
                }
            }
        }
    }

    /**
     * Add the ingredients of the current recipe to the grocery list
     *
     * @param view
     */
    public void addIngredientsToList(View view)
    {
        String ingredients = recalculatedRecipe.getIngredients();
        if (recalculatedRecipe.getIngredients().isEmpty())
        {
            ingredients = recipe.getIngredients();
        }
        String ingredientsArray[] = ingredients.split("\n");
        ArrayList<String> groceryListArray = new ArrayList<String>();
        for (String ingredient : ingredientsArray)
        {
            groceryListArray.add(ingredient);
        }
        ArrayList<GroceryListItem> items = new UnitsConverter().convertIngredientsToGroceryList(groceryListArray);
        Recipes.getInstance().getGroceryList().addAll(items);
        Recipes.getInstance().save(getApplicationContext());
        Toast toast = Toast.makeText(view.getContext(), getResources().getString(R.string.added_to_grocery_list), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

}
