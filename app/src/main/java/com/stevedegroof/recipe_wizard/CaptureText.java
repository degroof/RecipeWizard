package com.stevedegroof.recipe_wizard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * get raw recipe text
 */
public class CaptureText extends StandardActivity
{

    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_text);
    }

    /**
     * Send raw text back to edit screen
     *
     * @param view
     */
    public void done(View view)
    {
        TextView textView = findViewById(R.id.captureText);
        String rawText = textView.getText().toString();
        Intent intent = new Intent();
        intent.putExtra(AddEditRecipe.EXTRA_RAW_TEXT, rawText);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * grab text from camera
     *
     * @param view
     */
    public void grabFromCamera(View view)
    {
        Intent i = new Intent(this, OcrCapture.class);
        startActivityForResult(i, 1);
    }

    /**
     * grab text from image
     *
     * @param view
     */
    public void grabFromImage(View view)
    {
        Intent i = new Intent(this, ImageCapture.class);
        startActivityForResult(i, 1);
    }


    /**
     * returning from capture activity...
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1)
        {
            if (resultCode == RESULT_OK)
            {
                String rawText = data.getStringExtra(AddEditRecipe.EXTRA_RAW_TEXT);
                TextView targetView = findViewById(R.id.captureText);
                String existingText = targetView.getText().toString() + "\n" + rawText;
                targetView.setText(existingText);
            }
        }
    }

}
