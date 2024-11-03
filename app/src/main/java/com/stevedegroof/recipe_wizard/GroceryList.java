package com.stevedegroof.recipe_wizard;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * screen to display / edit grocery list
 */
public class GroceryList extends StandardActivity
{

    GroceryListViewAdapter adapter;
    ArrayList<GroceryListItem> items = new ArrayList<GroceryListItem>();

    ItemTouchHelper.Callback itemMoveCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_list);
        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.groceryListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GroceryListViewAdapter(this, items);
        recyclerView.setAdapter(adapter);
        //set up drag-and-drop
        itemMoveCallback =
                new ItemMoveCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(itemMoveCallback);
        touchHelper.attachToRecyclerView(recyclerView);

    }

    /**
     * consolidate any edited items
     *
     * @param view
     */
    public void recalc(View view)
    {
        items = adapter.getGroceryListItems();
        items = new UnitsConverter().consolidateGroceryList(items);
        if (items.isEmpty() || !items.get(0).getText().isEmpty())
        {
            items.add(0, new GroceryListItem("", false));
        }
        adapter.setGroceryListItems(items);
        Recipes.getInstance().setGroceryList(items);
        Recipes.getInstance().save(getApplicationContext());
        adapter.notifyDataSetChanged();
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
        Recipes.getInstance().getGroceryList().clear();
        Recipes.getInstance().save(getApplicationContext());
        recalc(v);
        adapter.notifyDataSetChanged();
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
        updateGroceryList();
        ArrayList<GroceryListItem> glItems = Recipes.getInstance().getGroceryList();
        String ingredientsText = "";
        for (GroceryListItem item : glItems) ingredientsText += item.getText() + "\n";
        intent.putExtra(Intent.EXTRA_TEXT, ingredientsText);

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
    public void onResume()
    {
        super.onResume();
        items = Recipes.getInstance().getGroceryList();
        items = new UnitsConverter().consolidateGroceryList(items);
        Recipes.getInstance().setGroceryList(items);
        if (items.isEmpty() || !items.get(0).getText().isEmpty())
        {
            items.add(0, new GroceryListItem("", false));
        }
        adapter.setGroceryListItems(items);
        adapter.notifyDataSetChanged();
    }


    /**
     * Update grocery list to reflect screen text
     */
    private void updateGroceryList()
    {
        ArrayList<GroceryListItem> ingrList = new ArrayList<>();
        for (int i = 0; i < adapter.getItemCount(); i++)
        {
            if (!adapter.getItem(i).getText().isEmpty())
                ingrList.add(adapter.getItem(i));
        }
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

    public ItemMoveCallback getItemMoveCallback()
    {
        return (ItemMoveCallback) itemMoveCallback;
    }
}
