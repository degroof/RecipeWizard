package com.stevedegroof.recipe_wizard;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;

enum UnitSystem
{
    IMPERIAL, METRIC
}

/**
 * Activity for viewing a recipe.
 * Allows users to view recipe details, edit, share, delete,
 * and adjust servings and units.
 */
public class ViewRecipeActivity extends AppCompatActivity
{
    public static final int SHARE_CONVERTED = 10004;
    public static final int SHARE_ORIGINAL = 10005;
    Recipe recalculatedRecipe = null;
    private ImageButton editButton;
    private ImageButton shareButton;
    private ImageButton deleteButton;
    private TextView servingsValueText;
    private ImageButton servingsDecrementButton;
    private ImageButton servingsIncrementButton;
    private Button unitsToggleButton;
    private TextView recipeNameTextDetail;
    private TextView ingredientsTextList;
    private TextView directionsTextSteps;
    private TextView notesText;
    private MaterialToolbar toolbarViewRecipe;
    private Recipe currentRecipe;
    private int recalcServings = 4;
    private UnitSystem recalcUnitSystem = UnitSystem.IMPERIAL;
    private int shareMode = SHARE_ORIGINAL;

    private boolean includeNotes = true;

    /**
     * Called when the activity is first created.
     * This method initializes the activity, sets up the UI elements,
     * and registers listeners for user interactions.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.
     *                           <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        toolbarViewRecipe = findViewById(R.id.toolbarViewRecipe);
        setSupportActionBar(toolbarViewRecipe);


        editButton = findViewById(R.id.button_edit_recipe);
        shareButton = findViewById(R.id.button_share_recipe);
        deleteButton = findViewById(R.id.button_delete_recipe);
        servingsValueText = findViewById(R.id.textview_servings_value);
        servingsDecrementButton = findViewById(R.id.button_servings_decrement);
        servingsIncrementButton = findViewById(R.id.button_servings_increment);
        unitsToggleButton = findViewById(R.id.button_units_toggle);


        recipeNameTextDetail = findViewById(R.id.textview_recipe_name_detail);
        ingredientsTextList = findViewById(R.id.textview_ingredients_list);
        directionsTextSteps = findViewById(R.id.textview_directions_steps);
        servingsValueText = findViewById(R.id.textview_servings_value);
        unitsToggleButton = findViewById(R.id.button_units_toggle);
        notesText = findViewById(R.id.textview_notes);

        editButton.setOnClickListener(this::checkEditType);

        shareButton.setOnClickListener(this::checkIncludeNotes);

        deleteButton.setOnClickListener(v -> new AlertDialog.Builder(v.getContext())
                .setTitle(R.string.delete_tc)
                .setMessage(R.string.delete_prompt)
                .setPositiveButton(android.R.string.yes, (dialog, which) ->
                {
                    Recipes recipes = Recipes.getInstance();
                    recipes.getList().remove(currentRecipe);
                    recipes.save(getApplicationContext());
                    navigateToMainActivity();
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show());

        servingsDecrementButton.setOnClickListener(v ->
        {
            if (recalcServings > 1)
            {
                recalcServings--;
                updateServingsDisplay();
                recalculateRecipe();
            }
        });

        servingsIncrementButton.setOnClickListener(v ->
        {
            recalcServings++;
            updateServingsDisplay();
            recalculateRecipe();
        });

        unitsToggleButton.setOnClickListener(v ->
        {
            if (recalcUnitSystem == UnitSystem.IMPERIAL)
            {
                recalcUnitSystem = UnitSystem.METRIC;
            } else
            {
                recalcUnitSystem = UnitSystem.IMPERIAL;
            }
            updateUnitsDisplay();
            recalculateRecipe();
        });
    }

    /**
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.
     *
     * <p>This is only called once, the first time the options menu is
     * displayed.  To update the menu every time it is displayed, see
     * {@link #onPrepareOptionsMenu}.
     *
     * <p>The default implementation populates the menu with standard system
     * menu items.  These are placed in the {@link Menu#CATEGORY_SYSTEM} group so that
     * they will be correctly ordered with application-defined menu items.
     * Deriving classes should always call through to the base implementation.
     *
     * <p>You can safely hold on to <var>menu</var> (and any items created
     * from it), making modifications to it as desired, until the next
     * time onCreateOptionsMenu() is called.
     *
     * <p>When you add items to the menu, you can implement the Activity's
     * {@link #onOptionsItemSelected} method to handle them there.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_recipe_menu, menu);
        return true;
    }


    /**
     * Navigates to the MainActivity.
     * Clears the activity stack and ensures that only one instance of MainActivity is running.
     */
    private void navigateToMainActivity()
    {
        Intent intent = new Intent(ViewRecipeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }


    /**
     * Updates the text view that displays the number of servings.
     * Sets the text of the servingsValueText TextView to the current value of recalcServings.
     */
    private void updateServingsDisplay()
    {
        servingsValueText.setText(String.valueOf(recalcServings));
    }

    /**
     * Updates the text of the units toggle button based on the current unit system.
     * If the unit system is Imperial, the button text is set to "Imperial".
     * If the unit system is Metric, the button text is set to "Metric".
     */
    private void updateUnitsDisplay()
    {
        if (recalcUnitSystem == UnitSystem.IMPERIAL)
        {
            unitsToggleButton.setText(getString(R.string.units_imperial));
        } else
        {
            unitsToggleButton.setText(getString(R.string.units_metric));
        }
    }


    /**
     * Handle action bar item clicks here. The action bar will
     * automatically handle clicks on the Home/Up button, so long
     * as you specify a parent activity in AndroidManifest.xml.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_home)
        {
            navigateToMainActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Loads the recipe data into the UI elements.
     * Sets the recipe title.
     * Initializes servings and unit system based on the current recipe or a previously recalculated recipe.
     * If a recalculated recipe exists and has servings, those values are used.
     * Otherwise, it defaults to the current recipe's servings (or 4 if parsing fails) and unit system.
     * Finally, triggers a recalculation of the recipe based on the determined servings and unit system.
     */
    private void loadRecipeData()
    {

        recipeNameTextDetail.setText(currentRecipe.getTitle());
        if (recalculatedRecipe == null || recalculatedRecipe.getServings() == null)
        {
            try
            {
                recalcServings = Integer.parseInt(currentRecipe.getServings().trim());
            } catch (NumberFormatException e)
            {
                recalcServings = 4;
            }

            recalcUnitSystem = currentRecipe.isMetric() ? UnitSystem.METRIC : UnitSystem.IMPERIAL;
        } else
        {
            recalcServings = Integer.parseInt(recalculatedRecipe.getServings().trim());
            recalcUnitSystem = recalculatedRecipe.isMetric() ? UnitSystem.METRIC : UnitSystem.IMPERIAL;
        }
        recalculateRecipe();
    }


    /**
     * Recalculates the recipe's ingredients and directions based on the selected measurement system (metric or imperial) and the desired number of servings.
     * <p>
     * This method performs the following steps:
     * 1. Initializes a new `Recipe` object to store the recalculated values.
     * 2. Retrieves the ingredients and directions from the `currentRecipe`.
     * 3. If the recipe has been marked for recalculation (i.e., serving size or unit system has changed):
     * a. Splits the ingredients string into an array of individual ingredients.
     * b. Iterates through each ingredient and converts its units using the `UnitsConverter` class.
     * The conversion takes into account the original servings, recalculated servings, original unit system, and recalculated unit system.
     * c. Updates the `recalculatedRecipe` with the converted ingredients, new unit system, and new servings.
     * d. Converts the directions using the `UnitsConverter` class, adjusting quantities based on serving changes and unit system.
     * e. Updates the `recalculatedRecipe` with the converted directions and original notes.
     * 4. Formats the directions by splitting them into lines and prepending line numbers.
     * 5. Updates the UI elements (`ingredientsTextList`, `directionsTextSteps`, `notesText`) to display the (potentially) recalculated recipe details.
     */
    private void recalculateRecipe()
    {
        boolean recalcIsMetric = recalcUnitSystem == UnitSystem.METRIC;
        recalculatedRecipe = new Recipe();
        StringBuilder ingredients = new StringBuilder(currentRecipe.getIngredients());
        StringBuilder directions = new StringBuilder(currentRecipe.getDirections());
        int recipeServings = Integer.parseInt(currentRecipe.getServings());
        if (isRecalced())
        {
            String[] ingredientArray = ingredients.toString().split("\n");
            ingredients = new StringBuilder();
            for (String s : ingredientArray)
            {
                ingredients.append(new UnitsConverter().convert(s, recipeServings, recalcServings,
                        currentRecipe.isMetric() ? UnitsConverter.METRIC : UnitsConverter.IMPERIAL,
                        recalcIsMetric ? UnitsConverter.METRIC : UnitsConverter.IMPERIAL, true)).append("\n");
            }
            recalculatedRecipe.setIngredients(ingredients.toString());
            recalculatedRecipe.setMetric(recalcIsMetric);
            recalculatedRecipe.setServings(recalcServings);
            recalculatedRecipe.setTitle(currentRecipe.getTitle());
            directions = new StringBuilder(new UnitsConverter().convertDirections(directions.toString(), recipeServings, recalcServings,
                    currentRecipe.isMetric() ? UnitsConverter.METRIC : UnitsConverter.IMPERIAL,
                    recalcIsMetric ? UnitsConverter.METRIC : UnitsConverter.IMPERIAL, currentRecipe.getExcludedPhrases()) + "\n");
            recalculatedRecipe.setDirections(directions.toString());
            recalculatedRecipe.setNotes(currentRecipe.getNotes());
        }
        String[] lines = directions.toString().split("\n");
        directions = new StringBuilder();
        for (int i = 0; i < lines.length; i++)
        {
            if (!lines[i].isEmpty())
            {
                directions.append((i + 1)).append(". ").append(lines[i]).append("\n\n");
            }
        }
        ingredientsTextList.setText(ingredients.toString());
        directionsTextSteps.setText(directions.toString());
        notesText.setText(currentRecipe.getNotes());
    }


    /**
     * Called when the activity will start interacting with the user.
     * At this point your activity is at the top of the activity stack, with user input going to it.
     * <p>
     * This method is called after {@link #onRestart} or {@link #onPause}.
     * It reloads the current recipe data, updates the servings and units display,
     * and adjusts the visibility of the notes section based on whether the recipe has notes.
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        currentRecipe = Recipes.getInstance().getCurrentRecipe();
        loadRecipeData();

        updateServingsDisplay();
        updateUnitsDisplay();
        if (currentRecipe.getNotes() == null || currentRecipe.getNotes().replaceAll("\n", "").trim().isEmpty())
        {
            notesText.setVisibility(View.GONE);
            findViewById(R.id.label_notes).setVisibility(View.GONE);
        } else
        {
            notesText.setVisibility(View.VISIBLE);
            findViewById(R.id.label_notes).setVisibility(View.VISIBLE);
        }
    }

    /**
     * Handles the share action when the user clicks the share button.
     * This method creates an Intent to share the current recipe as plain text.
     * It uses the `shareMode` to determine whether to share the original recipe or the recalculated version.
     * The recipe title is included as the subject of the share Intent.
     *
     * @param view The View that was clicked to trigger this method (the share button).
     */
    public void shareRecipe(View view)
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.recipe_prefix) + " " + currentRecipe.getTitle());
        if (shareMode == SHARE_ORIGINAL)
        {
            intent.putExtra(Intent.EXTRA_TEXT, currentRecipe.toPlainText(includeNotes));
        } else
        {
            intent.putExtra(Intent.EXTRA_TEXT, recalculatedRecipe.toPlainText(includeNotes));
        }

        startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_recipe_prefix) + " " + currentRecipe.getTitle()));
    }


    /**
     * Checks if the recipe has notes and prompts the user whether to include them when sharing.
     * If the recipe has no notes, it proceeds to check the share type directly.
     *
     * @param view The view that triggered this method (e.g., a button click).
     */
    public void checkIncludeNotes(View view)
    {
        setIncludeNotes(true);
        if (!currentRecipe.getNotes().replaceAll("\n", "").trim().isEmpty())
        {
            new AlertDialog.Builder(view.getContext())
                    .setTitle(R.string.share)
                    .setMessage(R.string.include_notes_prompt)
                    .setPositiveButton("Yes", (dialog, which) ->
                    {
                        setIncludeNotes(true);
                        checkShareType(view);
                    })
                    .setNegativeButton("No", (dialog, which) ->
                    {
                        setIncludeNotes(false);
                        checkShareType(view);
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        } else
        {
            setIncludeNotes(false);
            checkShareType(view);
        }
    }

    /**
     * Checks if the recipe has been recalculated.
     * If it has, it prompts the user to choose whether to share the original or converted recipe.
     * If not, it directly shares the original recipe.
     *
     * @param view The current view.
     */
    public void checkShareType(View view)
    {
        if (!isRecalced())
        {
            shareRecipe(view);
        } else
        {
            final View v = view;
            new AlertDialog.Builder(view.getContext())
                    .setTitle(R.string.share)
                    .setMessage(R.string.share_recipe_prompt)
                    .setPositiveButton(R.string.original, (dialog, which) ->
                    {
                        shareMode = SHARE_ORIGINAL;
                        shareRecipe(v);
                    })
                    .setNegativeButton(R.string.converted, (dialog, which) ->
                    {
                        shareMode = SHARE_CONVERTED;
                        shareRecipe(v);
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        }
    }

    /**
     * Checks if the recipe has been recalculated.
     * If not, it opens the EditRecipeActivity.
     * If it has been recalculated, it prompts the user to either edit the original recipe
     * or convert the current recipe to the recalculated values and then edit.
     *
     * @param view The view that triggered this method, used to get context for the dialog.
     */
    private void checkEditType(View view)
    {
        if (!isRecalced())
        {
            Intent editIntent = new Intent(ViewRecipeActivity.this, EditRecipeActivity.class);
            startActivity(editIntent);
        } else
        {
            new AlertDialog.Builder(view.getContext())
                    .setTitle(R.string.edit_check)
                    .setMessage(R.string.edit_recipe_prompt)
                    .setPositiveButton(R.string.original, (dialog, which) ->
                    {
                        Intent editIntent = new Intent(ViewRecipeActivity.this, EditRecipeActivity.class);
                        startActivity(editIntent);
                    })
                    .setNegativeButton(R.string.convert_first, (dialog, which) ->
                    {
                        currentRecipe.setServings(recalcServings);
                        currentRecipe.setMetric(recalcUnitSystem == UnitSystem.METRIC);
                        currentRecipe.setIngredients(recalculatedRecipe.getIngredients());
                        currentRecipe.setDirections(recalculatedRecipe.getDirections());
                        Intent editIntent = new Intent(ViewRecipeActivity.this, EditRecipeActivity.class);
                        startActivity(editIntent);
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        }
    }

    /**
     * Checks if the recipe has been recalculated.
     * A recipe is considered recalculated if the number of servings or the unit system
     * (metric/imperial) has been changed from the original recipe.
     *
     * @return True if the recipe has been recalculated, false otherwise.
     */
    private boolean isRecalced()
    {
        boolean recalcIsMetric = recalcUnitSystem == UnitSystem.METRIC;
        int servings = Integer.parseInt(currentRecipe.getServings());
        return (servings != recalcServings || currentRecipe.isMetric() != recalcIsMetric);
    }


    /**
     * Called when the activity is being destroyed.
     * This method clears the FLAG_KEEP_SCREEN_ON flag to allow the screen to turn off.
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void setIncludeNotes(boolean includeNotes)
    {
        this.includeNotes = includeNotes;
    }
}
