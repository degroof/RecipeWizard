package com.stevedegroof.recipe_wizard;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.vision.text.Text;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A singleton class for managing a collection of {@link Recipe} objects.
 * This class provides functionalities to load, save, sort, and manipulate recipes.
 * It also handles the conversion of recipes to plain text and manages the current recipe being viewed or edited.
 * The class uses a singleton pattern to ensure only one instance manages the recipes throughout the application.
 *
 * <p>Key functionalities include:
 * <ul>
 *     <li>Loading recipes from and saving recipes to private storage using JSON format.</li>
 *     <li>Sorting recipes based on predefined criteria (e.g., name, score).</li>
 *     <li>Converting the entire recipe collection or individual recipes to plain text.</li>
 *     <li>Removing duplicate recipes from the collection.</li>
 *     <li>Managing the currently selected recipe.</li>
 *     <li>Storing raw text and vision text from image recognition for recipe creation.</li>
 * </ul>
 * </p>
 *
 * <p><b>Constants:</b></p>
 * <ul>
 *     <li>{@link #NAME}: Constant representing sorting by recipe name.</li>
 *     <li>{@link #SCORE}: Constant representing sorting by recipe score.</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * // Get the singleton instance
 * Recipes recipesManager = Recipes.getInstance();
 *
 * // Load recipes
 * recipesManager.load(context);
 *
 * // Get the list of recipes
 * ArrayList&lt;Recipe&gt; allRecipes = recipesManager.getList();
 *
 * // Save recipes
 * recipesManager.save(context);
 * </pre>
 */
public class Recipes
{
    public static final int NAME = 0;
    public static final int SCORE = 1;
    private static final Recipes theInstance = new Recipes();
    private final Type recipesType = new TypeToken<ArrayList<Recipe>>()
    {
    }.getType();
    private ArrayList<Recipe> list = new ArrayList<>();
    private Recipe currentRecipe;

    private int sortOn = NAME;

    private String rawText = "";
    private Text visionText;

    private Recipes()
    {
    }

    public static Recipes getInstance()
    {
        return theInstance;
    }

    public ArrayList<Recipe> getList()
    {
        return list;
    }

    public void sort()
    {
        Collections.sort(list);
    }

    /**
     * Loads recipes from private storage.
     * <p>
     * This method reads a JSON file containing recipe data, deserializes it,
     * and populates the internal list of recipes. If the file is not found or
     * an error occurs during reading or parsing, the list remains empty or
     * in its previous state. After successfully loading, the recipes are sorted.
     *
     * @param ctx The application context, used to access private file storage.
     */
    public void load(Context ctx)
    {
        Gson gson = new Gson();
        String json = "";
        list.clear();
        StringBuilder sb;
        try
        {
            FileInputStream fis = ctx.openFileInput(MainActivity.RECIPE_FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                sb.append(line);
            }
            json = sb.toString();
            bufferedReader.close();
            if (!json.isEmpty())
            {
                list = gson.fromJson(json, recipesType);
                sort();
            }
        } catch (Exception e)
        {
        }

    }

    /**
     * Saves the list of recipes to private storage.
     * The recipes are serialized to JSON format before being written to a file.
     * After saving, the list of recipes is sorted.
     *
     * @param ctx The context used to access private storage.
     */
    public void save(Context ctx)
    {
        FileOutputStream outputStream;

        final Gson gson = new Gson();
        String serializedRecipes = gson.toJson(list, recipesType);

        try
        {
            outputStream = ctx.openFileOutput(MainActivity.RECIPE_FILE_NAME, Context.MODE_PRIVATE);
            outputStream.write(serializedRecipes.getBytes());
            outputStream.close();
        } catch (Exception e)
        {
        }
        sort();
    }

    /**
     * Extracts the entire recipe book into plain text.
     * Each recipe is separated by {@link MainActivity#RECIPE_BREAK}.
     *
     * @param includeNotes If true, notes for each recipe will be included in the output.
     * @return A string containing all recipes in plain text format.
     */
    public String toPlainText(boolean includeNotes)
    {
        StringBuilder textContent = new StringBuilder();
        Recipe recipe;
        for (int i = 0; i < getList().size(); i++)
        {
            recipe = getList().get(i);
            if (i != 0) textContent.append(MainActivity.RECIPE_BREAK + "\n");
            textContent.append(recipe.toPlainText(includeNotes));
        }
        return textContent.toString();
    }

    public int getSortOn()
    {
        return sortOn;
    }

    public void setSortOn(int sortOn)
    {
        this.sortOn = sortOn;
    }

    /**
     * Removes duplicate recipes from the list.
     * <p>
     * This method iterates through the existing list of recipes and identifies duplicates
     * based on a case-insensitive comparison of their titles and plain text content
     * (with all whitespace removed). It broadcasts progress updates during the deduplication
     * process.
     *
     * @param context The application context, used to send broadcast intents for progress updates.
     */
    public void dedupe(Context context)
    {
        Intent intent = new Intent();
        intent.setAction(MainActivity.ProgressReceiver.ACTION);
        boolean found = false;
        ArrayList<Recipe> newRecipes = new ArrayList<>();
        int recipeNumber = 0;
        int recipeCount = theInstance.getList().size();
        for (Recipe recipe : theInstance.getList())
        {
            recipeNumber++;
            intent.putExtra(MainActivity.ProgressReceiver.PROGRESS, MainActivity.ProgressReceiver.MERGE);
            intent.putExtra(MainActivity.ProgressReceiver.VALUE, (long) recipeNumber);
            intent.putExtra(MainActivity.ProgressReceiver.TOTAL, (long) recipeCount);
            context.sendBroadcast(intent);
            found = false;
            for (int i = 0; i < newRecipes.size() && !found; i++)
            {
                Recipe newRecipe = newRecipes.get(i);
                found = newRecipe.getTitle().replaceAll("\\s", "").equalsIgnoreCase(recipe.getTitle().replaceAll("\\s", ""))
                        && newRecipe.toPlainText(true).replaceAll("\\s", "").equalsIgnoreCase(recipe.toPlainText(true).replaceAll("\\s", ""));
            }
            if (!found) newRecipes.add(recipe);
        }
        theInstance.list = newRecipes;
    }


    public Recipe getCurrentRecipe()
    {
        return currentRecipe;
    }

    public void setCurrentRecipe(Recipe currentRecipe)
    {
        this.currentRecipe = currentRecipe;
    }

    public String getRawText()
    {
        return rawText;
    }

    public void setRawText(String rawText)
    {
        this.rawText = rawText;
    }

    public Text getVisionText()
    {
        return visionText;
    }

    public void setVisionText(Text visionText)
    {
        this.visionText = visionText;

    }
}
