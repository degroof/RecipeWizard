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


public class EditTextBlocksActivity extends AppCompatActivity implements EditTextBlocksAdapter.OnStartDragListener
{

    public static final String EXTRA_TEXT_BLOCKS_IN = "EXTRA_TEXT_BLOCKS_IN";
    public static final String EXTRA_EDITED_TEXT_OUT = "EXTRA_EDITED_TEXT_OUT";

    private RecyclerView recyclerViewTextBlocks;
    private EditTextBlocksAdapter adapter;
    private ArrayList<Text.TextBlock> currentTextBlocks;
    private ItemTouchHelper mItemTouchHelper;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.edit_text_blocks_menu, menu);
        return true;
    }

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

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder)
    {
        if (mItemTouchHelper != null)
        {
            mItemTouchHelper.startDrag(viewHolder);
        }
    }
}
