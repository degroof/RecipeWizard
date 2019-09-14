package com.stevedegroof.recipe_wizard;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * superclass of any activity with appbar menu
 */
public class StandardActivity extends CommonActivity
{

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {

        // Get menu inflater object.
        MenuInflater menuInflater = getMenuInflater();

        // Inflate the custom overflow menu
        menuInflater.inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        int id = item.getItemId();

        if (id == R.id.action_help)
        {
            help();
        } else if (id == R.id.action_home)
        {
            goHome();
        } else if (id == R.id.action_cart)
        {
            groceryList();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * open grocery list screen
     */
    private void groceryList()
    {
        Intent intent = new Intent(this, GroceryList.class);
        startActivityForResult(intent, 1);
    }


    /**
     * open help screen
     */
    public void help()
    {
        Intent intent = new Intent(this, Help.class);
        startActivity(intent);
    }


}
