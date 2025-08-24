package com.stevedegroof.recipe_wizard;

import android.os.Parcel;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class Recipe implements Comparable<Recipe>
{
    private ArrayList<DirectionsPhrase> excludedPhrases = new ArrayList<>();
    private String title;
    private String servings;
    private boolean isMetric;
    private String ingredients;
    private String directions;

    private transient double sortScore = 1d;


    public Recipe(String title, String servings, boolean isMetric, String ingredients, String directions)
    {
        this.title = title;
        this.servings = servings;
        this.isMetric = isMetric;
        this.ingredients = ingredients;
        this.directions = directions;
    }


    public Recipe()
    {

    }

    protected Recipe(Parcel in)
    {
        title = in.readString();
        servings = in.readString();
        isMetric = in.readByte() != 0;
        ingredients = in.readString();
        directions = in.readString();

    }


    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getServings()
    {
        return servings;
    }

    public void setServings(String servings)
    {
        this.servings = servings;
    }

    public void setServings(int servings)
    {
        this.servings = Integer.toString(servings);
    }

    public boolean isMetric()
    {
        return isMetric;
    }

    public void setMetric(boolean metric)
    {
        isMetric = metric;
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


    public String toPlainText()
    {
        String textContent = getTitle() + "\n";
        textContent += "Ingredients\n" + getIngredients();
        textContent += "Directions\n" + getDirections();
        if (!textContent.endsWith("\n")) textContent += "\n";
        textContent += "Serves " + getServings() + "\n";
        return textContent;
    }


    @NonNull
    @Override
    public String toString()
    {
        return title != null ? title : "Untitled Recipe";
    }

    /**
     * Used for sorting
     *
     * @param recipe
     * @return
     */
    @Override
    public int compareTo(Recipe recipe)
    {
        if (Recipes.getInstance().getSortOn() == Recipes.NAME)
            return title.compareTo(recipe.getTitle());
        else
            return Double.compare(recipe.sortScore, sortScore);
    }


    public ArrayList<DirectionsPhrase> getExcludedPhrases()
    {
        return excludedPhrases;
    }

    public void setExcludedPhrases(ArrayList<DirectionsPhrase> excludedPhrases)
    {
        this.excludedPhrases = excludedPhrases;
    }

    public double getSortScore()
    {
        return sortScore;
    }

    public void setSortScore(double sortScore)
    {
        this.sortScore = sortScore;
    }


    public int getTotalIngredients()
    {
        return ingredients.split("\n").length;
    }

}
