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

public class EditTextBlocksAdapter extends RecyclerView.Adapter<EditTextBlocksAdapter.TextBlockViewHolder>
        implements ItemTouchHelperAdapter
{

    private List<Text.TextBlock> textBlockList;
    private final OnItemRemoveListener removeListener;
    private final OnStartDragListener dragStartListener;

    public EditTextBlocksAdapter(List<Text.TextBlock> textBlockList, OnItemRemoveListener removeListener,
                                 OnStartDragListener dragStartListener)
    {
        this.textBlockList = textBlockList;
        this.removeListener = removeListener;
        this.dragStartListener = dragStartListener;
    }

    @NonNull
    @Override
    public TextBlockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_block, parent, false);
        return new TextBlockViewHolder(view);
    }

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

    @Override
    public int getItemCount()
    {
        return textBlockList == null ? 0 : textBlockList.size();
    }

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

    public interface OnStartDragListener
    {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    static class TextBlockViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder
    {
        TextView textViewBlockContent;
        ImageButton buttonRemoveTextBlock;
        ImageView dragHandle;

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
