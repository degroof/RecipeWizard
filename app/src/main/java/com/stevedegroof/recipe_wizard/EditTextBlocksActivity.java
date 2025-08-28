package com.stevedegroof.recipe_wizard;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.stream.Collectors;


/**
 * Activity for editing text blocks captured from an image.
 * <p>
 * This activity allows users to:
 * <ul>
 *     <li>View a list of text blocks.</li>
 *     <li>Reorder text blocks by dragging and dropping.</li>
 *     <li>Delete text blocks.</li>
 *     <li>Save the edited text.</li>
 * </ul>
 * <p>
 * It uses a {@link RecyclerView} to display the text blocks and an {@link EditTextBlocksAdapter}
 * to manage the data and interactions. The {@link ItemTouchHelper} is used to enable drag-and-drop
 * functionality.
 * <p>
 * The activity receives the initial text blocks through the {@link Recipes#getVisionText()} method
 * and saves the edited text using {@link Recipes#setRawText(String)}.
 * <p>
 * Implements {@link EditTextBlocksAdapter.OnStartDragListener} to handle the start of a drag operation.
 */
public class EditTextBlocksActivity extends AppCompatActivity implements EditTextBlocksAdapter.OnStartDragListener
{

    private RecyclerView recyclerViewTextBlocks;
    private EditTextBlocksAdapter adapter;
    private ArrayList<Text.TextBlock> currentTextBlocks;
    private ItemTouchHelper mItemTouchHelper;

    /**
     * Called when the activity is first created.
     * Initializes the activity, sets up the toolbar, RecyclerView for text blocks,
     * and a button to save the edited text.
     * Retrieves text blocks from ML Kit's VisionText, filters out empty blocks,
     * and displays them. If no text is captured, it shows a toast message and finishes the activity.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_text_blocks);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_edit_text);
        setSupportActionBar(toolbar);

        recyclerViewTextBlocks = findViewById(R.id.recycler_view_text_blocks);
        ImageButton buttonSave = findViewById(R.id.button_save_edited_text);

        Text visionText = Recipes.getInstance().getVisionText();

        if (visionText != null)
        {
            currentTextBlocks = visionText.getTextBlocks().stream()
                    .filter(block -> !block.getText().trim().isEmpty())
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        if (currentTextBlocks == null || currentTextBlocks.isEmpty())
        {
            currentTextBlocks = new ArrayList<>();
            Toast.makeText(this, "No text captured.", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupRecyclerView();

        buttonSave.setOnClickListener(v -> saveEditedText());
    }

    /**
     * Sets up the RecyclerView to display the text blocks.
     * This includes:
     * - Initializing the {@link EditTextBlocksAdapter} with the current text blocks and a callback for item removal.
     * - Setting a {@link LinearLayoutManager} for the RecyclerView.
     * - Attaching the adapter to the RecyclerView.
     * - Setting up an {@link ItemTouchHelper} to enable drag-and-drop functionality for reordering items.
     * - Adding a {@link DividerItemDecoration} to display dividers between items in the RecyclerView.
     */
    private void setupRecyclerView()
    {
        adapter = new EditTextBlocksAdapter(currentTextBlocks, position ->
        {
            if (position >= 0 && position < currentTextBlocks.size())
            {
                currentTextBlocks.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, currentTextBlocks.size());
            }
        }, this);
        recyclerViewTextBlocks.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTextBlocks.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerViewTextBlocks);

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerViewTextBlocks.getLayoutManager();
        if (layoutManager != null)
        {
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                    recyclerViewTextBlocks.getContext(),
                    layoutManager.getOrientation()
            );
            recyclerViewTextBlocks.addItemDecoration(dividerItemDecoration);
        }
    }

    /**
     * Saves the edited text blocks.
     * It iterates through the currentTextBlocks, trims each block's text,
     * appends it to a StringBuilder with a newline character.
     * The resulting string is then set as the raw text in the Recipes singleton.
     * Finally, it finishes the current activity.
     */
    private void saveEditedText()
    {
        StringBuilder resultText = new StringBuilder();
        for (Text.TextBlock block : currentTextBlocks)
        {
            resultText.append(block.getText().trim()).append("\n");
        }

        Recipes.getInstance().setRawText(resultText.toString());
        finish();
    }

    /**
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.
     *
     * <p>This is only called once, the first time the options menu is
     * displayed.  To update the menu every time it is displayed, see
     * {@link #onPrepareOptionsMenu}.
     *
     * <p>The default implementation populates the menu with standard system
     * menu items.  These are placed in the {@link Menu#CATEGORY_SYSTEM} group so that
     * they will be correctly ordered with application-defined menu items.
     * Deriving classes should always call through to the base implementation.
     *
     * <p>You can safely hold on to <var>menu</var> (and any items created
     * from it), making modifications to it as desired, until the next
     * time onCreateOptionsMenu() is called.
     *
     * <p>When you add items to the menu, you can implement the Activity's
     * {@link #onOptionsItemSelected} method to handle them there.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.edit_text_blocks_menu, menu);
        return true;
    }

    /**
     * Handle action bar item clicks here. The action bar will
     * automatically handle clicks on the Home/Up button, so long
     * as you specify a parent activity in AndroidManifest.xml.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        if (item.getItemId() == R.id.action_home_edit_text)
        {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when a drag gesture is started on a RecyclerView item.
     * This method is part of the {@link EditTextBlocksAdapter.OnStartDragListener} interface
     * and is triggered by the {@link EditTextBlocksAdapter} when a drag handle is touched.
     * It initiates the drag operation using the {@link ItemTouchHelper}.
     *
     * @param viewHolder The ViewHolder of the item that is being dragged.
     */
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder)
    {
        if (mItemTouchHelper != null)
        {
            mItemTouchHelper.startDrag(viewHolder);
        }
    }
}
