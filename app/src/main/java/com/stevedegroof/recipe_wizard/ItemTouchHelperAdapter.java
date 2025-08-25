package com.stevedegroof.recipe_wizard;

public interface ItemTouchHelperAdapter
{
    /**
     * Called when an item has been dragged far enough to trigger a move.
     * Implementations should call RecyclerView.Adapter#notifyItemMoved(int, int) after
     * adjusting the underlying data to reflect the move.
     *
     * @param fromPosition The start position of the moved item.
     * @param toPosition   The end position of the moved item.
     */
    void onItemMove(int fromPosition, int toPosition);

    /**
     * Called when an item has been dismissed by a swipe.
     * Implementations should call RecyclerView.Adapter#notifyItemRemoved(int) after
     * adjusting the underlying data to reflect the dismissal.
     *
     * @param position The position of the item dismissed.
     */
    void onItemDismiss(int position);
}
