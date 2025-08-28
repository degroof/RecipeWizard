package com.stevedegroof.recipe_wizard;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying a list of recipes in a RecyclerView.
 * This adapter handles the creation of ViewHolder objects and binding data to them.
 * It also provides an interface for handling item click events.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>
{

    private final OnItemClickListener listener;
    private final List<Recipe> recipeList;


    /**
     * Constructor for RecipeAdapter.
     *
     * @param recipeList The list of recipes to display.
     * @param listener   The listener for item click events.
     */
    public RecipeAdapter(List<Recipe> recipeList, OnItemClickListener listener)
    {
        this.recipeList = recipeList;
        this.listener = listener;
    }

    /**
     * Called when RecyclerView needs a new {@link RecipeViewHolder} of the given type to represent
     * an item.
     * <p>
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     * <p>
     * The new ViewHolder will be used to display items of the adapter using
     * {@link #onBindViewHolder(RecipeViewHolder, int)}. Since it will be re-used to display
     * different items in the data set, it is a good idea to cache references to sub views of
     * the View to avoid unnecessary {@link View#findViewById(int)} calls.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new RecipeViewHolder that holds a View of the given view type.
     * @see #getItemViewType(int)
     * @see #onBindViewHolder(RecipeViewHolder, int)
     */
    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_recipe, parent, false);
        return new RecipeViewHolder(itemView);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the itemView to reflect the item at the
     * given position.
     *
     * @param holder   The ViewHolder which should be updated to represent the
     *                 contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position)
    {

        Recipe currentRecipe = recipeList.get(position);

        holder.bind(currentRecipe, listener);
    }

    @Override
    public int getItemCount()
    {
        return recipeList != null ? recipeList.size() : 0;
    }


    public interface OnItemClickListener
    {
        void onItemClick(Recipe recipe);
    }


    /**
     * ViewHolder class for recipe items in the RecyclerView.
     * This class holds the views for a single recipe item and binds data to these views.
     */
    static class RecipeViewHolder extends RecyclerView.ViewHolder
    {
        TextView recipeNameTextView;

        RecipeViewHolder(View itemView)
        {
            super(itemView);
            recipeNameTextView = itemView.findViewById(R.id.text_view_recipe_name);
        }


        /**
         * Binds a recipe to the ViewHolder.
         * Sets the recipe name in the TextView and applies bold typeface if it's the current recipe.
         * Sets an OnClickListener to handle item clicks.
         *
         * @param recipe   The recipe to bind.
         * @param listener The listener for item click events.
         */
        void bind(final Recipe recipe, final OnItemClickListener listener)
        {
            recipeNameTextView.setText(recipe.getTitle());
            recipeNameTextView.setTypeface(null, recipe == Recipes.getInstance().getCurrentRecipe() ? Typeface.BOLD : Typeface.NORMAL);
            recipeNameTextView.setPadding(16, 0, 0, 0);
            itemView.setOnClickListener(v ->
            {
                if (listener != null)
                {
                    listener.onItemClick(recipe);
                }
            });
        }
    }
}
