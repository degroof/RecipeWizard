package com.stevedegroof.recipe_wizard;

import java.util.ArrayList;

/**
 * A single recipe
 */
public class Recipe implements Comparable<Recipe>
{
    private String title = "";
    private String ingredients = "";
    private String directions = "";
    private int servings = 4;
    private boolean isMetric = false;
    private ArrayList<DirectionsPhrase> excludedPhrases = new ArrayList<DirectionsPhrase>();

    /**
     * Used for sorting
     *
     * @param recipe
     * @return
     */
    @Override
    public int compareTo(Recipe recipe)
    {
        return title.compareTo(recipe.getTitle());
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getIngredients()
    {
        return ingredients;
    }

    public void setIngredients(String ingredients)
    {
        this.ingredients = ingredients;
    }

    public String getDirections()
    {
        return directions;
    }

    public void setDirections(String directions)
    {
        this.directions = directions;
    }

    public int getServings()
    {
        return servings;
    }

    public void setServings(int servings)
    {
        this.servings = servings;
    }

    public boolean isMetric()
    {
        return isMetric;
    }

    public void setMetric(boolean metric)
    {
        isMetric = metric;
    }


    public ArrayList<DirectionsPhrase> getExcludedPhrases()
    {
        return excludedPhrases;
    }

    public void setExcludedPhrases(ArrayList<DirectionsPhrase> excludedPhrases)
    {
        this.excludedPhrases = excludedPhrases;
    }

    /**
     * Convert the recipe to plain text (human-readable and parsable)
     *
     * @return
     */
    public String toPlainText()
    {
        String textContent = getTitle() + "\n";
        textContent += "Ingredients\n" + getIngredients();
        textContent += "Directions\n" + getDirections();
        if (!textContent.endsWith("\n")) textContent += "\n";
        textContent += "Serves " + getServings() + "\n";
        return textContent;
    }
}
