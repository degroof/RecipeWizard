package com.stevedegroof.recipe_wizard;

import android.os.Parcel;

import androidx.annotation.NonNull;

import java.util.ArrayList;

/**
 * Represents a single recipe with its title, servings, ingredients, directions, and notes.
 * This class implements {@link Comparable} to allow sorting of recipes.
 * It also supports parceling for Android.
 */
public class Recipe implements Comparable<Recipe>
{
    private ArrayList<DirectionsPhrase> excludedPhrases = new ArrayList<>();
    private String title;
    private String servings;
    private boolean isMetric;
    private String ingredients;
    private String directions;
    private String notes;

    private transient double sortScore = 1d;


    /**
     * Constructor for a Recipe object
     *
     * @param title       Title of the recipe
     * @param servings    Number of servings the recipe makes
     * @param isMetric    True if the recipe uses metric units, false otherwise
     * @param ingredients List of ingredients for the recipe
     * @param directions  Directions for preparing the recipe
     */
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


    /**
     * Converts the recipe to a plain text string.
     *
     * @param includeNotes Whether to include the notes in the output.
     * @return The plain text representation of the recipe.
     */
    public String toPlainText(boolean includeNotes)
    {
        String textContent = getTitle() + "\n";
        textContent += "Ingredients\n" + getIngredients();
        textContent += "Directions\n" + getDirections();
        if (!textContent.endsWith("\n")) textContent += "\n";
        textContent += "Serves " + getServings() + "\n";
        if (includeNotes && notes != null && !notes.isEmpty())
            textContent += "Notes\n" + notes + ((notes.endsWith("\n")) ? "" : "\n");
        return textContent;
    }


    @NonNull
    @Override
    public String toString()
    {
        return title != null ? title : "Untitled Recipe";
    }

    /**
     * Compares this recipe with the specified recipe for order.
     * Returns a negative integer, zero, or a positive integer as this recipe
     * is less than, equal to, or greater than the specified recipe.
     * <p>
     * The comparison is based on the current sort setting in {@link Recipes}.
     * If sorting by name, it compares the titles of the recipes.
     * Otherwise, it compares the sort scores, with a higher score indicating
     * a "greater" recipe (resulting in descending order for sort scores).
     *
     * @param recipe the recipe to be compared.
     * @return a negative integer, zero, or a positive integer as this recipe
     * is less than, equal to, or greater than the specified recipe.
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


    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }
}
