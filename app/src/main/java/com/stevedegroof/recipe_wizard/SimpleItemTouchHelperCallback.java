package com.stevedegroof.recipe_wizard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * An implementation of {@link ItemTouchHelper.Callback} that enables basic drag & drop and
 * swipe-to-dismiss. Drag events are automatically started by an item long-press.<br/>
 * </br/>
 * Expects the {@link RecyclerView.Adapter} to listen for {@link
 * ItemTouchHelperAdapter} callbacks and the {@link RecyclerView.ViewHolder} to implement
 * {@link ItemTouchHelperViewHolder}.
 */
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

    /**
     * Defines the movement flags for drag and swipe gestures.
     * Dragging is enabled only if mDragEnabled is true.
     * Swiping is disabled.
     *
     * @param recyclerView The RecyclerView to which ItemTouchHelper is attached.
     * @param viewHolder   The ViewHolder for which the movement flags are being queried.
     * @return A bitmask of movement flags.
     */
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

    /**
     * Called when an item has been dragged far enough to trigger a move. This is called every time
     * an item is shifted, and not at the end of a "drop" event.
     *
     * @param recyclerView The RecyclerView to which ItemTouchHelper is attached.
     * @param source       The ViewHolder which is being dragged by the user.
     * @param target       The ViewHolder over which the currently active item is being dragged.
     * @return True if the {@code viewHolder} has been moved to the adapter position of
     * {@code target}.
     */
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

    /**
     * Called when the ViewHolder is changed (highlighted or not)
     *
     * @param viewHolder  The new ViewHolder that is selected by the user.
     * @param actionState The new action state.
     */
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

    /**
     * Called by ItemTouchHelper when the user interaction with an element is over and it
     * also completed its animation.
     * <p>
     * This is a good place to clear item selection.
     * <p>
     * This type of interaction includes simply clicking on an item or dragging it.
     *
     * @param recyclerView The RecyclerView which is hosting the ViewHolder
     * @param viewHolder   The ViewHolder which has been interacted by the user.
     */
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
