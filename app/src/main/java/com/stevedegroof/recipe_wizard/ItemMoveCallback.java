package com.stevedegroof.recipe_wizard;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ItemMoveCallback extends ItemTouchHelper.Callback
{

    private final ItemTouchHelperContract viewAdapter;
    private boolean moveEnabled = false;

    /**
     * @param adapter
     */
    public ItemMoveCallback(ItemTouchHelperContract adapter)
    {
        viewAdapter = adapter;
    }


    @Override
    public boolean isLongPressDragEnabled()
    {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled()
    {
        return false;
    }


    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i)
    {
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder)
    {
        int dragFlags = 0;
        if (moveEnabled) dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target)
    {
        viewAdapter.onRowMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder,
                                  int actionState)
    {


        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE)
        {
            if (viewHolder instanceof GroceryListViewAdapter.GroceryListViewHolder)
            {
                GroceryListViewAdapter.GroceryListViewHolder groceryListViewHolder =
                        (GroceryListViewAdapter.GroceryListViewHolder) viewHolder;
                viewAdapter.onRowSelected(groceryListViewHolder);
            }

        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder)
    {
        super.clearView(recyclerView, viewHolder);

        if (viewHolder instanceof GroceryListViewAdapter.GroceryListViewHolder)
        {
            GroceryListViewAdapter.GroceryListViewHolder glViewHolder =
                    (GroceryListViewAdapter.GroceryListViewHolder) viewHolder;
            viewAdapter.onRowClear(glViewHolder);
        }
    }

    public interface ItemTouchHelperContract
    {

        void onRowMoved(int fromPosition, int toPosition);

        void onRowSelected(GroceryListViewAdapter.GroceryListViewHolder ViewHolder);

        void onRowClear(GroceryListViewAdapter.GroceryListViewHolder ViewHolder);

    }

    public void setMoveEnabled(boolean moveEnabled)
    {
        this.moveEnabled = moveEnabled;
    }
}


