package com.stevedegroof.recipe_wizard;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Adapter for grocery list recycler view
 */
public class GroceryListViewAdapter extends RecyclerView.Adapter<GroceryListViewAdapter.GroceryListViewHolder>
        implements ItemMoveCallback.ItemTouchHelperContract
{

    private ArrayList<GroceryListItem> groceryListItems;
    private LayoutInflater inflater;
    private Context context;

    /**
     * @param context
     * @param data
     */
    GroceryListViewAdapter(Context context, ArrayList<GroceryListItem> data)
    {
        this.inflater = LayoutInflater.from(context);
        this.groceryListItems = data;
        this.context = context;
    }


    /**
     * inflates the row layout from xml when needed
     *
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public GroceryListViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = inflater.inflate(R.layout.draggable_checklist_item, parent, false);
        return new GroceryListViewHolder(view);
    }


    /**
     * binds the data to the EditText in each ro
     *
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(GroceryListViewHolder holder, int position)
    {
        String item = groceryListItems.get(position).getText();
        holder.editText.setText(item);
        boolean checked = groceryListItems.get(position).isChecked();
        holder.checkBox.setChecked(checked);
    }


    /**
     * total number of rows
     *
     * @return
     */
    @Override
    public int getItemCount()
    {
        return groceryListItems.size();
    }


    /**
     * stores and recycles views as they are scrolled off screen
     */
    public class GroceryListViewHolder extends RecyclerView.ViewHolder
    {
        EditText editText;
        CheckBox checkBox;

        GroceryListViewHolder(final View itemView)
        {
            super(itemView);

            editText = itemView.findViewById(R.id.editTextItem);
            checkBox = itemView.findViewById(R.id.checkBox);

            editText.addTextChangedListener(
                    new TextWatcher()
                    {
                        public void afterTextChanged(Editable s)
                        {
                        }

                        public void beforeTextChanged(CharSequence s, int start, int count, int after)
                        {
                        }

                        public void onTextChanged(CharSequence s, int start,
                                                  int before, int count)
                        {
                            if (s.toString().contains("\n")) //text contains newline; create new item
                            {
                                if (count > 1) //probably a paste
                                {
                                    int offset = 0;
                                    if (start > 0)
                                    {
                                        groceryListItems.get(getAdapterPosition()).setText(s.toString().substring(0, start) + s.toString().substring(start + count));
                                        offset = 1;
                                        groceryListItems.add(getAdapterPosition() + 1, new GroceryListItem("", false));
                                    }
                                    String pastedText = s.toString();
                                    pastedText = pastedText.substring(start, start + count);
                                    String items[] = pastedText.split("\n");
                                    groceryListItems.get(getAdapterPosition() + offset).setText(items[0]);
                                    if (items.length > 1) //more than one item
                                    {
                                        for (int i = 1; i < items.length; i++)
                                        {
                                            groceryListItems.add(getAdapterPosition() + i + offset, new GroceryListItem(items[i], false));
                                        }
                                        notifyDataSetChanged();
                                    }
                                } else //user hit enter on existing item
                                {
                                    groceryListItems.get(getAdapterPosition()).setText(s.toString().replaceAll("\n", ""));
                                    notifyItemChanged(getAdapterPosition());
                                    groceryListItems.add(getAdapterPosition() + 1, new GroceryListItem("", false));
                                    notifyItemInserted(getAdapterPosition() + 1);
                                }
                            } else //update list with text change
                            {
                                groceryListItems.get(getAdapterPosition()).setText(s.toString());
                            }
                        }
                    }
            );

            //update list with checkbox change
            checkBox.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener()
                    {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean b)
                        {
                            groceryListItems.get(getAdapterPosition()).setChecked(b);
                        }
                    }
            );

            //delete item
            ImageView clearIcon = itemView.findViewById(R.id.imageViewX);
            clearIcon.setOnClickListener(
                    new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            groceryListItems.remove(getAdapterPosition());
                            notifyDataSetChanged();
                        }
                    }
            );

            //allow drag only if drag icon was touched
            ImageView dragIcon = itemView.findViewById(R.id.imageViewDrag);
            dragIcon.setOnTouchListener(
                    new View.OnTouchListener()
                    {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent)
                        {
                            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                            {
                                ((GroceryList) context).getItemMoveCallback().setMoveEnabled(true);
                            } else
                            {
                                ((GroceryList) context).getItemMoveCallback().setMoveEnabled(false);
                            }
                            return false;
                        }
                    }
            );

        }

    }


    /**
     * convenience method for getting data at click position
     *
     * @param id
     * @return
     */
    GroceryListItem getItem(int id)
    {
        return groceryListItems.get(id);
    }

    /**
     * Row moved; update list to match
     *
     * @param fromPosition
     * @param toPosition
     */
    @Override
    public void onRowMoved(int fromPosition, int toPosition)
    {
        if (fromPosition < toPosition)
        {
            for (int i = fromPosition; i < toPosition; i++)
            {
                Collections.swap(groceryListItems, i, i + 1);
            }
        } else
        {
            for (int i = fromPosition; i > toPosition; i--)
            {
                Collections.swap(groceryListItems, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        ((GroceryList) context).getItemMoveCallback().setMoveEnabled(false);
    }

    /**
     * Dragging item; highlight row
     *
     * @param myViewHolder
     */
    @Override
    public void onRowSelected(GroceryListViewHolder myViewHolder)
    {
        myViewHolder.itemView.setBackground(context.getResources().getDrawable(R.drawable.custom_border));
        ((GroceryList) context).getItemMoveCallback().setMoveEnabled(false);
    }

    /**
     * Item dropped; un-highlight row
     *
     * @param myViewHolder
     */
    @Override
    public void onRowClear(GroceryListViewHolder myViewHolder)
    {
        myViewHolder.itemView.setBackground(context.getResources().getDrawable(R.drawable.no_border));
        ((GroceryList) context).getItemMoveCallback().setMoveEnabled(false);
    }

    /**
     * If a newly-added item is empty, set focus on it
     *
     * @param holder
     */
    @Override
    public void onViewAttachedToWindow(@NonNull GroceryListViewHolder holder)
    {
        super.onViewAttachedToWindow(holder);
        if (holder.editText.getText().toString().isEmpty())
            holder.editText.requestFocus();
        holder.editText.setEnabled(false);
        holder.editText.setEnabled(true);
    }

    /**
     * @param groceryListItems
     */
    public void setGroceryListItems(ArrayList<GroceryListItem> groceryListItems)
    {
        this.groceryListItems = groceryListItems;
    }

    /**
     * @return
     */
    public ArrayList<GroceryListItem> getGroceryListItems()
    {
        return groceryListItems;
    }
}
