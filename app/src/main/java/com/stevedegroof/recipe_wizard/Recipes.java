package com.stevedegroof.recipe_wizard;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

    public int getSortOn()
    {
        return sortOn;
    }

    public void setSortOn(int sortOn)
    {
        this.sortOn = sortOn;
    }

    private int sortOn = NAME;

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
        } catch (IOException e)
        {
        }
        if (!json.isEmpty())
        {
            list = gson.fromJson(json, recipesType);
            sort();
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
        String serializedObject = gson.toJson(list, recipesType);

        try
        {
            outputStream = ctx.openFileOutput(CommonActivity.RECIPE_FILE_NAME, Context.MODE_PRIVATE);
            outputStream.write(serializedObject.getBytes());
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

}
