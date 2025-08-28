package com.stevedegroof.recipe_wizard;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;


public class AddRecipeActivity extends AppCompatActivity
{
    private Recipe recipeToAdd;
    private MaterialToolbar toolbarAddRecipe;
    private TextInputEditText recipeNameEditText, servingsEditText, ingredientsEditText, directionsEditText, notesEditText;
    private MaterialButton unitsToggleEditButton;
    private ImageButton saveButton, captureButton;
    private UnitSystem currentUnitSystem = UnitSystem.IMPERIAL;

    /**
     * Called when the activity is first created.
     * Initializes the UI elements, sets up the toolbar, and prepares a new recipe object.
     * It also populates the fields with default or existing recipe data and sets up button listeners.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        insetsController.setAppearanceLightStatusBars(true);

        Recipes.getInstance().setCurrentRecipe(null);
        toolbarAddRecipe = findViewById(R.id.toolbarAddRecipe);
        setSupportActionBar(toolbarAddRecipe);

        recipeNameEditText = findViewById(R.id.edittext_recipe_name);
        servingsEditText = findViewById(R.id.edittext_servings);
        unitsToggleEditButton = findViewById(R.id.button_units_toggle_edit);
        ingredientsEditText = findViewById(R.id.edittext_ingredients);
        directionsEditText = findViewById(R.id.edittext_directions);
        notesEditText = findViewById(R.id.edittext_notes);

        saveButton = findViewById(R.id.button_save_recipe);
        captureButton = findViewById(R.id.button_capture_recipe);
        recipeToAdd = new Recipe();
        recipeToAdd.setIngredients("");
        recipeToAdd.setDirections("");
        recipeToAdd.setMetric(false);
        recipeToAdd.setServings(4);
        recipeToAdd.setTitle("");

        populateFields();
        setupButtonListeners();

    }

    /**
     * Called when the activity will start interacting with the user.
     * At this point your activity is at the top of the activity stack, with user input going to it.
     * Always call through to the superclass's implementation of this method.
     * If there is a current recipe, it populates the fields with the recipe's data.
     * Finally, it requests focus on the recipe name edit text.
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        Recipe recipe = Recipes.getInstance().getCurrentRecipe();
        if (recipe != null)
        {
            recipeToAdd = recipe;
            populateFields();
            recipeNameEditText.requestFocus();
        } else
        {
            recipeNameEditText.requestFocus();
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
     * Navigates to the MainActivity.
     * This method creates an Intent to start MainActivity and sets flags
     * to clear the activity stack above MainActivity if it's already running,
     * or bring it to the front. It then starts the MainActivity and finishes
     * the current AddRecipeActivity.
     */
    private void navigateToMainActivity()
    {
        Intent intent = new Intent(AddRecipeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Populates the input fields with the data from the current recipe being added or edited.
     * This includes setting the text for recipe name, servings, ingredients, directions, and notes.
     * It also sets the current unit system based on the recipe's metric status and updates the
     * display of the units toggle button.
     */
    private void populateFields()
    {
        recipeNameEditText.setText(recipeToAdd.getTitle());
        servingsEditText.setText(recipeToAdd.getServings());
        ingredientsEditText.setText(recipeToAdd.getIngredients());
        directionsEditText.setText(recipeToAdd.getDirections());
        notesEditText.setText(recipeToAdd.getNotes());
        currentUnitSystem = recipeToAdd.isMetric() ? UnitSystem.METRIC : UnitSystem.IMPERIAL;
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
     * Sets up listeners for the save, units toggle, and capture buttons.
     * The save button saves the current recipe details.
     * The units toggle button switches between imperial and metric units and updates the display.
     * The capture button navigates to the CaptureRecipeActivity.
     */
    private void setupButtonListeners()
    {
        saveButton.setOnClickListener(v -> saveRecipe());

        unitsToggleEditButton.setOnClickListener(v ->
        {
            toggleUnitSystem();
            updateUnitsToggleDisplay();
        });


        captureButton.setOnClickListener(v ->
        {
            Intent intent = new Intent(AddRecipeActivity.this, CaptureRecipeActivity.class);
            startActivity(intent);
        });

    }

    /**
     * Toggles the current unit system between imperial and metric.
     * If the current system is imperial, it switches to metric, and vice-versa.
     */
    private void toggleUnitSystem()
    {
        currentUnitSystem = (currentUnitSystem == UnitSystem.IMPERIAL) ? UnitSystem.METRIC : UnitSystem.IMPERIAL;
    }

    /**
     * Saves the recipe data entered by the user.
     * <p>
     * This method retrieves the recipe details from the input fields,
     * creates a new {@link Recipe} object, and populates it with the entered data.
     * It then adds the new recipe to the global list of recipes, saves the list
     * to persistent storage, sets the current recipe to the newly added one,
     * and navigates to the {@link ViewRecipeActivity} to display the saved recipe.
     * The current activity is then finished.
     */
    private void saveRecipe()
    {
        recipeToAdd = new Recipe();

        recipeToAdd.setTitle(recipeNameEditText.getText().toString().trim());
        recipeToAdd.setServings(servingsEditText.getText().toString().trim());
        recipeToAdd.setMetric(currentUnitSystem == UnitSystem.METRIC);


        recipeToAdd.setIngredients(String.valueOf(ingredientsEditText.getText()));
        recipeToAdd.setDirections(String.valueOf(directionsEditText.getText()));
        recipeToAdd.setNotes(String.valueOf(notesEditText.getText()));

        Recipes.getInstance().getList().add(recipeToAdd);
        Recipes.getInstance().save(this.getApplicationContext());
        Recipes.getInstance().setCurrentRecipe(recipeToAdd);

        Intent intent = new Intent(this, ViewRecipeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }


    private enum UnitSystem
    {IMPERIAL, METRIC}


}
