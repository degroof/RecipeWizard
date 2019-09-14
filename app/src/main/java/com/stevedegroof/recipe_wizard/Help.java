package com.stevedegroof.recipe_wizard;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class Help extends StandardActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
    }

    /**
     * Hide help item
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.action_help);
        if (item != null)
            item.setVisible(false);
        return result;
    }
}
