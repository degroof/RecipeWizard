package com.stevedegroof.recipe_wizard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback
{

    private final ItemTouchHelperAdapter mAdapter;
    private boolean mDragEnabled = true;

    public SimpleItemTouchHelperCallback(ItemTouchHelperAdapter adapter)
    {
        mAdapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled()
    {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled()
    {
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder)
    {
        if (!mDragEnabled)
        {
            return 0;
        }
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target)
    {
        if (source.getItemViewType() != target.getItemViewType())
        {
            return false;
        }
        mAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction)
    {
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState)
    {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE)
        {
            if (viewHolder instanceof ItemTouchHelperViewHolder)
            {
                ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
                itemViewHolder.onItemSelected();
            }
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder)
    {
        super.clearView(recyclerView, viewHolder);
        if (viewHolder instanceof ItemTouchHelperViewHolder)
        {
            ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
            itemViewHolder.onItemClear();
        }
    }

    public void setDragEnabled(boolean enabled)
    {
        mDragEnabled = enabled;
    }
}
