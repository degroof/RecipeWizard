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
        }

        return super.onOptionsItemSelected(item);
    }


    public void help()
    {
        Intent intent = new Intent(this, Help.class);
        startActivity(intent);
    }


}
