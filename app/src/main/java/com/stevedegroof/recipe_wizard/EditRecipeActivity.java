package com.stevedegroof.recipe_wizard;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;


/**
 * Activity for editing an existing recipe.
 * Allows users to modify recipe details such as name, servings, ingredients, directions, notes, and unit system.
 * Provides options to save changes, delete the recipe, or use a wizard for adjusting directions.
 */
public class EditRecipeActivity extends AppCompatActivity
{
    private Recipe recipeToEdit;
    private MaterialToolbar toolbarEditRecipe;
    private TextInputEditText recipeNameEditText, servingsEditText, ingredientsEditText, directionsEditText, notesEditText;
    private MaterialButton unitsToggleEditButton;
    private ImageButton saveButton, wizardButton, deleteButton;
    private UnitSystem currentUnitSystem = UnitSystem.IMPERIAL;

    /**
     * Initializes the activity.
     * Sets up the layout, toolbar, and views for editing a recipe.
     * Populates the fields with the current recipe's data.
     * Sets up listeners for the save, wizard, delete, and unit toggle buttons.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipe);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        insetsController.setAppearanceLightStatusBars(true);

        toolbarEditRecipe = findViewById(R.id.toolbarEditRecipe);
        setSupportActionBar(toolbarEditRecipe);

        recipeNameEditText = findViewById(R.id.edittext_recipe_name);
        servingsEditText = findViewById(R.id.edittext_servings);
        unitsToggleEditButton = findViewById(R.id.button_units_toggle_edit);
        ingredientsEditText = findViewById(R.id.edittext_ingredients);
        directionsEditText = findViewById(R.id.edittext_directions);
        notesEditText = findViewById(R.id.edittext_notes);

        saveButton = findViewById(R.id.button_save_recipe);
        wizardButton = findViewById(R.id.button_wizard_recipe);
        deleteButton = findViewById(R.id.button_delete_edited_recipe);
        recipeToEdit = Recipes.getInstance().getCurrentRecipe();

        populateFieldsWithRecipeData();
        setupButtonListeners();

    }

    /**
     * Called when the activity will start interacting with the user.
     * At this point your activity is at the top of the activity stack, with user input going to it.
     * It checks if there are any convertible units in the directions and hides the wizard button if not.
     */
    public void onResume()
    {
        super.onResume();
        if (recipeToEdit != null)
        {

            String directions = recipeToEdit.getDirections();
            ArrayList<DirectionsPhrase> phrases = new UnitsConverter().getPhrases(directions);
            if (phrases.isEmpty())
            {
                wizardButton.setVisibility(View.GONE);
            }
        }
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
        inflater.inflate(R.menu.edit_recipe_menu, menu);
        return true;
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

        if (id == R.id.action_home_edit)
        {
            navigateToMainActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Navigates back to the MainActivity.
     * <p>
     * This method creates an Intent to start the MainActivity. It also adds flags to clear the
     * activity stack above MainActivity and ensure that if MainActivity is already running,
     * it is brought to the front without creating a new instance. After starting MainActivity,
     * it finishes the current EditRecipeActivity.
     */
    private void navigateToMainActivity()
    {
        Intent intent = new Intent(EditRecipeActivity.this, MainActivity.class);


        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Populates the input fields with the data from the recipe being edited.
     * If the recipe object is null, an error message is displayed.
     * Sets the recipe name, servings, ingredients, directions, and notes fields.
     * Also sets the current unit system based on the recipe's stored preference
     * and updates the units toggle button display accordingly.
     */
    private void populateFieldsWithRecipeData()
    {
        if (recipeToEdit == null)
        {
            Toast.makeText(this, "Internal error: Recipe object is null in populateFields.", Toast.LENGTH_SHORT).show();
            return;
        }


        if (recipeToEdit.getTitle() != null)
        {
            recipeNameEditText.setText(recipeToEdit.getTitle());
        } else
        {
            recipeNameEditText.setText("");
        }


        if (recipeToEdit.getServings() != null)
        {
            servingsEditText.setText(recipeToEdit.getServings());
        } else
        {
            servingsEditText.setText("");
        }


        ingredientsEditText.setText(recipeToEdit.getIngredients());


        directionsEditText.setText(recipeToEdit.getDirections());

        notesEditText.setText(recipeToEdit.getNotes());


        currentUnitSystem = recipeToEdit.isMetric() ? UnitSystem.METRIC : UnitSystem.IMPERIAL;
        updateUnitsToggleDisplay();
    }

    /**
     * Updates the text of the units toggle button based on the current unit system.
     * If the current unit system is METRIC, the button text is set to "Metric".
     * Otherwise (if IMPERIAL), the button text is set to "Imperial".
     */
    private void updateUnitsToggleDisplay()
    {
        if (currentUnitSystem == UnitSystem.METRIC)
        {
            unitsToggleEditButton.setText(getString(R.string.units_metric));
        } else
        {
            unitsToggleEditButton.setText(getString(R.string.units_imperial));
        }
    }

    /**
     * Sets up listeners for the various buttons in the activity.
     * This includes:
     * - Save button: Saves the current recipe.
     * - Units toggle button: Toggles the unit system (imperial/metric) and updates the display.
     * - Delete button: Shows a confirmation dialog and deletes the recipe if confirmed.
     * - Wizard button: Navigates to the DirectionsAdjustmentActivity.
     */
    private void setupButtonListeners()
    {
        saveButton.setOnClickListener(v -> saveRecipe());

        unitsToggleEditButton.setOnClickListener(v ->
        {
            toggleUnitSystem();
            updateUnitsToggleDisplay();
        });

        deleteButton.setOnClickListener(v -> new AlertDialog.Builder(v.getContext())
                .setTitle(R.string.delete_tc)
                .setMessage(R.string.delete_prompt)
                .setPositiveButton(android.R.string.yes, (dialog, which) ->
                {
                    Recipes recipes = Recipes.getInstance();
                    recipes.getList().remove(recipeToEdit);
                    recipes.save(getApplicationContext());
                    navigateToMainActivity();
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show());

        wizardButton.setOnClickListener(v ->
        {
            Intent intent = new Intent(EditRecipeActivity.this, DirectionsAdjustmentActivity.class);
            startActivity(intent);
        });


    }

    /**
     * Toggles the current unit system between imperial and metric.
     * If the current system is imperial, it's changed to metric, and vice-versa.
     */
    private void toggleUnitSystem()
    {
        currentUnitSystem = (currentUnitSystem == UnitSystem.IMPERIAL) ? UnitSystem.METRIC : UnitSystem.IMPERIAL;
    }

    /**
     * Saves the current recipe data.
     * If {@code recipeToEdit} is null, a new Recipe object is created.
     * The method retrieves data from the input fields (recipe name, servings, ingredients, directions, notes),
     * sets the unit system (metric or imperial), and updates the {@code recipeToEdit} object.
     * Finally, it saves the updated recipe list to persistent storage using {@link Recipes#save(android.content.Context)}
     * and finishes the activity.
     */
    private void saveRecipe()
    {


        if (recipeToEdit == null)
            recipeToEdit = new Recipe();

        recipeToEdit.setTitle(recipeNameEditText.getText().toString().trim());
        recipeToEdit.setServings(servingsEditText.getText().toString().trim());
        recipeToEdit.setMetric(currentUnitSystem == UnitSystem.METRIC);


        recipeToEdit.setIngredients(String.valueOf(ingredientsEditText.getText()));
        recipeToEdit.setDirections(String.valueOf(directionsEditText.getText()));
        recipeToEdit.setNotes(String.valueOf(notesEditText.getText()));

        Recipes.getInstance().save(this.getApplicationContext());
        finish();

    }


    private enum UnitSystem
    {IMPERIAL, METRIC}


}
