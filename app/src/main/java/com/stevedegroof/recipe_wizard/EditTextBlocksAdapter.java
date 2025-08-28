package com.stevedegroof.recipe_wizard;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.mlkit.vision.text.Text;

import java.util.Collections;
import java.util.List;

/**
 * RecyclerView Adapter for displaying and managing a list of Text.TextBlock items.
 * This adapter allows users to remove items and reorder them via drag-and-drop.
 * It uses {@link ItemTouchHelperAdapter} to handle drag-and-drop functionality and
 * provides interfaces for item removal and drag start events.
 */
public class EditTextBlocksAdapter extends RecyclerView.Adapter<EditTextBlocksAdapter.TextBlockViewHolder>
        implements ItemTouchHelperAdapter
{

    private final OnItemRemoveListener removeListener;
    private final OnStartDragListener dragStartListener;
    private List<Text.TextBlock> textBlockList;

    /**
     * Constructor for EditTextBlocksAdapter.
     *
     * @param textBlockList     The list of TextBlock items to display.
     * @param removeListener    Listener for item removal events.
     * @param dragStartListener Listener for item drag start events.
     */
    public EditTextBlocksAdapter(List<Text.TextBlock> textBlockList, OnItemRemoveListener removeListener,
                                 OnStartDragListener dragStartListener)
    {
        this.textBlockList = textBlockList;
        this.removeListener = removeListener;
        this.dragStartListener = dragStartListener;
    }

    /**
     * Called when RecyclerView needs a new {@link TextBlockViewHolder} of the given type to represent
     * an item.
     * <p>
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     * <p>
     * The new ViewHolder will be used to display items of the adapter using
     * {@link #onBindViewHolder(TextBlockViewHolder, int)}. Since it will be re-used to display
     * different items in the data set, it is a good idea to cache references to sub views of
     * the View to avoid unnecessary {@link View#findViewById(int)} calls.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new TextBlockViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public TextBlockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_block, parent, false);
        return new TextBlockViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the {@link TextBlockViewHolder#itemView} to reflect the item at the given position.
     * It sets the text of the text block, and sets up listeners for the remove button and drag handle.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull TextBlockViewHolder holder, int position)
    {
        Text.TextBlock textBlock = textBlockList.get(position);
        holder.textViewBlockContent.setText(textBlock.getText().replace("\n", " "));

        holder.buttonRemoveTextBlock.setOnClickListener(v ->
        {
            if (removeListener != null)
            {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION)
                {
                    removeListener.onItemRemoved(currentPosition);
                }
            }
        });
        holder.dragHandle.setOnTouchListener((v, event) ->
        {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
            {
                if (dragStartListener != null)
                {
                    dragStartListener.onStartDrag(holder);
                }
            }
            return true;
        });

        holder.dragHandle.setClickable(true);
        holder.dragHandle.setFocusable(true);


        holder.dragHandle.setOnClickListener(v ->
        {
            if (dragStartListener != null)
            {
                dragStartListener.onStartDrag(holder);
            }
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount()
    {
        return textBlockList == null ? 0 : textBlockList.size();
    }

    /**
     * Called when an item has been moved.
     *
     * @param fromPosition The starting position of the item.
     * @param toPosition   The ending position of the item.
     */
    @Override
    public void onItemMove(int fromPosition, int toPosition)
    {
        if (fromPosition < toPosition)
        {
            for (int i = fromPosition; i < toPosition; i++)
            {
                Collections.swap(textBlockList, i, i + 1);
            }
        } else
        {
            for (int i = fromPosition; i > toPosition; i--)
            {
                Collections.swap(textBlockList, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position)
    {
    }

    public void updateList(List<Text.TextBlock> newList)
    {
        this.textBlockList = newList;
        notifyDataSetChanged();
    }

    public interface OnItemRemoveListener
    {
        void onItemRemoved(int position);
    }

    /**
     * Interface for listening to when an item drag is initiated.
     */
    public interface OnStartDragListener
    {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    /**
     * ViewHolder class for individual text block items in the RecyclerView.
     * It holds references to the UI elements within each item's layout
     * and implements {@link ItemTouchHelperViewHolder} to respond to drag-and-drop events.
     */
    static class TextBlockViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder
    {
        TextView textViewBlockContent;
        ImageButton buttonRemoveTextBlock;
        ImageView dragHandle;

        /**
         * Constructor for the TextBlockViewHolder.
         *
         * @param itemView The view of the item.
         */
        public TextBlockViewHolder(@NonNull View itemView)
        {
            super(itemView);
            textViewBlockContent = itemView.findViewById(R.id.text_view_block_content);
            buttonRemoveTextBlock = itemView.findViewById(R.id.button_remove_text_block);
            dragHandle = itemView.findViewById(R.id.drag_handle);
        }


        @Override
        public void onItemSelected()
        {
            itemView.setAlpha(0.7f);
        }

        @Override
        public void onItemClear()
        {
            itemView.setAlpha(1.0f);
        }

    }
}
