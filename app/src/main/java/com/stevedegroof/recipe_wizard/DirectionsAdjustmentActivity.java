package com.stevedegroof.recipe_wizard;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;

/**
 * The "magic" screen. Lets the user freeze the quantities on certain phrases in the directions.
 * This activity allows users to select specific phrases within the recipe directions
 * and prevent their quantities from being scaled when the overall recipe is adjusted.
 * It displays a list of phrases containing scalable ingredients, allowing the user to
 * check boxes next to phrases they wish to exclude from scaling.
 */
public class DirectionsAdjustmentActivity extends AppCompatActivity
{

    Recipe recipe;
    ArrayList<DirectionsPhrase> phrases;


    /**
     * Initializes the activity.
     * Sets up the window flags for the status bar, sets the status bar color,
     * and sets the appearance of the status bar to light.
     * Retrieves the current recipe and sets the content view.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        recipe = Recipes.getInstance().getCurrentRecipe();
        setContentView(R.layout.activity_directions_adjustment);
    }

    /**
     * Called when the activity will start interacting with the user.
     * At this point your activity is at the top of the activity stack, with user input going to it.
     * Reloads the list of phrases when the activity resumes.
     */
    public void onResume()
    {
        super.onResume();

        loadList();
    }


    /**
     * Sift through the directions, looking for scalable ingredients.
     * Check the boxes of any phrases already excluded.
     */
    private void loadList()
    {
        LinearLayout phraseList = findViewById(R.id.phraseList);
        String directions = recipe.getDirections();
        UnitsConverter unitsConverter = new UnitsConverter();
        phrases = unitsConverter.getPhrases(directions);
        for (DirectionsPhrase p : phrases)
        {
            CheckBox phraseCheckBox = new CheckBox(getApplicationContext());
            if (isExcluded(p.getPhraseText())) phraseCheckBox.setChecked(true);
            phraseCheckBox.setText(p.getPhraseContext());
            phraseList.addView(phraseCheckBox);
        }
    }

    /**
     * Save the recipe, along with its scaling exclusions.
     * This method iterates through the checkboxes in the UI, identifies which phrases
     * are selected for exclusion, updates the current recipe with these exclusions,
     * saves the recipe collection, and then closes the activity.
     *
     * @param view The view that triggered this method, typically a button.
     */
    public void save(View view)
    {
        LinearLayout phraseList = findViewById(R.id.phraseList);
        ArrayList<DirectionsPhrase> excludedPhrases = new ArrayList<>();
        for (int i = 0; i < phraseList.getChildCount(); i++)
        {
            View item = phraseList.getChildAt(i);
            if (item instanceof CheckBox)
            {
                if (((CheckBox) item).isChecked())
                {
                    excludedPhrases.add(phrases.get(i));
                }
            }
        }
        recipe.setExcludedPhrases(excludedPhrases);
        Recipes.getInstance().save(getApplicationContext());
        finish();
    }


    /**
     * Checks if a given phrase has already been excluded from scaling.
     *
     * @param phrase The phrase to check.
     * @return True if the phrase is excluded, false otherwise.
     */
    boolean isExcluded(String phrase)
    {
        if (recipe.getExcludedPhrases() != null)
        {
            for (DirectionsPhrase excludedPhrase : recipe.getExcludedPhrases())
            {
                if (phrase.equals(excludedPhrase.getPhraseText()))
                {
                    return true;
                }
            }
        }
        return false;
    }

}
