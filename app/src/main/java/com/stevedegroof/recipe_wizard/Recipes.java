package com.stevedegroof.recipe_wizard;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A singleton for holding recipes
 */
public class Recipes
{
    private static Recipes theInstance = new Recipes();
    private Type recipesType = new TypeToken<ArrayList<Recipe>>()
    {
    }.getType();
    private ArrayList<Recipe> list = new ArrayList<Recipe>();
    public static final int NAME = 0;
    public static final int SCORE = 1;

    private int sortOn = NAME;

    //grocery list
    private ArrayList<GroceryListItem> groceryList = new ArrayList<GroceryListItem>();
    private Type groceriesType = new TypeToken<ArrayList<GroceryListItem>>()
    {
    }.getType();

    private Type oldGroceriesType = new TypeToken<ArrayList<String>>()
    {
    }.getType();

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
     * load recipes from private storage
     *
     * @param ctx
     */
    public void load(Context ctx)
    {
        Gson gson = new Gson();
        String json = "";
        list.clear();
        StringBuilder sb;
        try
        {
            FileInputStream fis = ctx.openFileInput(CommonActivity.RECIPE_FILE_NAME);
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
        json = "";
        try
        {
            FileInputStream fis = ctx.openFileInput(CommonActivity.GROCERY_FILE_NAME);
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
                groceryList = gson.fromJson(json, groceriesType);
            }
        } catch (Exception e)
        {
            try
            {
                ArrayList<String> ingredients = gson.fromJson(json, oldGroceriesType);
                ArrayList<GroceryListItem> items = new ArrayList<GroceryListItem>();
                for (String ingredient : ingredients)
                    items.add(new GroceryListItem(ingredient, false));
                groceryList = items;
            } catch (Exception ex)
            {
            }
        }
    }

    /**
     * save recipes to private storage
     *
     * @param ctx
     */
    public void save(Context ctx)
    {
        FileOutputStream outputStream;

        final Gson gson = new Gson();
        String serializedRecipes = gson.toJson(list, recipesType);
        String serializedGroceries = gson.toJson(groceryList, groceriesType);

        try
        {
            outputStream = ctx.openFileOutput(CommonActivity.RECIPE_FILE_NAME, Context.MODE_PRIVATE);
            outputStream.write(serializedRecipes.getBytes());
            outputStream.close();
        } catch (Exception e)
        {
        }
        try
        {
            outputStream = ctx.openFileOutput(CommonActivity.GROCERY_FILE_NAME, Context.MODE_PRIVATE);
            outputStream.write(serializedGroceries.getBytes());
            outputStream.close();
        } catch (Exception e)
        {
        }
        sort();
    }

    /**
     * extract the entire recipe book into plain text
     *
     * @return
     */
    public String toPlainText()
    {
        String textContent = "";
        Recipe recipe;
        for (int i = 0; i < getList().size(); i++)
        {
            recipe = getList().get(i);
            if (i != 0) textContent += CommonActivity.RECIPE_BREAK + "\n";
            textContent += recipe.toPlainText();
        }
        return textContent;
    }

    public int getSortOn()
    {
        return sortOn;
    }

    public void setSortOn(int sortOn)
    {
        this.sortOn = sortOn;
    }

    public ArrayList<GroceryListItem> getGroceryList() {
        return groceryList;
    }

    public void setGroceryList(ArrayList<GroceryListItem> groceryList) {
        this.groceryList = groceryList;
    }

    /**
     * remove duplicates
     *
     * @param context
     */
    public void dedupe(Context context) {
        Intent intent = new Intent();
        intent.setAction(MainActivity.ProgressReceiver.ACTION);
        boolean found = false;
        ArrayList<Recipe> newRecipes = new ArrayList<Recipe>();
        int recipeNumber = 0;
        int recipeCount = theInstance.getList().size();
        for (Recipe recipe : theInstance.getList()) {
            recipeNumber++;
            intent.putExtra(MainActivity.ProgressReceiver.PROGRESS, MainActivity.ProgressReceiver.MERGE);
            intent.putExtra(MainActivity.ProgressReceiver.VALUE, (long) recipeNumber);
            intent.putExtra(MainActivity.ProgressReceiver.TOTAL, (long) recipeCount);
            context.sendBroadcast(intent);
            found = false;
            for (int i = 0; i < newRecipes.size() && !found; i++) {
                Recipe newRecipe = newRecipes.get(i);
                if (!found)
                    found = newRecipe.getTitle().replaceAll("\\s", "").equalsIgnoreCase(recipe.getTitle().replaceAll("\\s", ""))
                            && newRecipe.toPlainText().replaceAll("\\s", "").equalsIgnoreCase(recipe.toPlainText().replaceAll("\\s", ""));
            }
            if (!found) newRecipes.add(recipe);
        }
        theInstance.list = newRecipes;
    }
}
