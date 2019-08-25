package com.stevedegroof.recipe_wizard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import java.util.ArrayList;

/**
 * The "magic" screen. Lets the user freeze the quantities on certain phrases in the directions
 */
public class DirectionsAdjustment extends CommonActivity
{

    Recipe recipe;
    int index;
    ArrayList<DirectionsPhrase> phrases;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        index = intent.getIntExtra(CommonActivity.EXTRA_RECIPE, -1);
        if (index >= 0)
        {
            recipe = Recipes.getInstance().getList().get(index);
        }
        setContentView(R.layout.activity_directions_adjustment);
    }

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
     * Save the recipe, along with its scaling exclusions
     *
     * @param view
     */
    public void save(View view)
    {
        LinearLayout phraseList = findViewById(R.id.phraseList);
        ArrayList<DirectionsPhrase> exludedPhrases = new ArrayList<DirectionsPhrase>();
        for (int i = 0; i < phraseList.getChildCount(); i++)
        {
            View item = phraseList.getChildAt(i);
            if (item instanceof CheckBox)
            {
                if (((CheckBox) item).isChecked())
                {
                    exludedPhrases.add(phrases.get(i));
                }
            }
        }
        recipe.setExcludedPhrases(exludedPhrases);
        Recipes.getInstance().save(getApplicationContext());
        finish();
    }


    /**
     * Has this phrase already been excluded?
     *
     * @param phrase
     * @return
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
