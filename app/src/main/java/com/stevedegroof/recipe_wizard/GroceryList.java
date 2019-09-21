package com.stevedegroof.recipe_wizard;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Collections;

/**
 * screen to display / edit grocery list
 */
public class GroceryList extends StandardActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_list);

    }

    /**
     * Ask user to confirm delete
     *
     * @param view
     */
    public void checkDelete(View view)
    {
        final View v = view;
        new AlertDialog.Builder(view.getContext())
                .setTitle(R.string.delete_tc)
                .setMessage(R.string.delete_sl_prompt)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        deleteList(v);
                    }
                })
                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();

    }


    /**
     * Delete grocery list
     *
     * @param v
     */
    private void deleteList(View v)
    {
        EditText list = findViewById(R.id.groceryList);
        list.setText("");
        Recipes.getInstance().getGroceryList().clear();
        Recipes.getInstance().save(getApplicationContext());
    }


    /**
     * Share grocery list
     *
     * @param view
     */
    public void share(View view)
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.grocery_list));
        EditText list = findViewById(R.id.groceryList);
        intent.putExtra(Intent.EXTRA_TEXT, list.getText().toString());

        startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_grocery_list)));
    }

    /**
     * Hide grocery cart item
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.action_cart);
        if (item != null)
            item.setVisible(false);
        return result;
    }

    /**
     * load grocery cart items, consolidating similar ingredients
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        ArrayList<String> ingredients = Recipes.getInstance().getGroceryList();
        ingredients = new UnitsConverter().consolidateGroceryList(ingredients);
        Collections.sort(ingredients, String.CASE_INSENSITIVE_ORDER);
        Recipes.getInstance().setGroceryList(ingredients);
        fillList(ingredients);
    }

    private void fillList(ArrayList<String> ingredients)
    {
        String ingr = "";
        for (String ingredient : ingredients)
        {
            if (!ingredient.trim().isEmpty()) ingr += ingredient + "\n";
        }
        EditText list = findViewById(R.id.groceryList);
        list.setText(ingr);
    }

    /**
     * Update grocery list to reflect screen text
     */
    private void updateGroceryList()
    {
        EditText list = findViewById(R.id.groceryList);
        String ingredients = list.getText().toString();
        String ingrArray[] = ingredients.split("\n");
        ArrayList<String> ingrList = new ArrayList<>();
        for (String ingr : ingrArray) ingrList.add(ingr);
        Recipes.getInstance().setGroceryList(ingrList);
        Recipes.getInstance().save(getApplicationContext());
    }

    /**
     * save list on back
     */
    @Override
    public void onBackPressed()
    {
        updateGroceryList();
        super.onBackPressed();
    }

    /**
     * save list on home
     */
    @Override
    public void goHome()
    {
        updateGroceryList();
        super.goHome();
    }
}
