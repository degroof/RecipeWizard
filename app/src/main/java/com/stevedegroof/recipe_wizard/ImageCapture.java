package com.stevedegroof.recipe_wizard;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

public class ImageCapture extends CommonActivity
{

    private static final int RESULT_LOAD_IMAGE = 1;

    private String capturedText = "";

    /**
     * Immediately launch the image open dialog
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_capture);

        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }


    /**
     * On return from file dialog
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null)
        {
            Uri uri = data.getData();
            try
            {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

                ImageView imageView = findViewById(R.id.imageCaptureImageView);

                imageView.setImageBitmap(bitmap);

                // process the result
                if (bitmap != null)
                {

                    TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();

                    if (!textRecognizer.isOperational())
                    {
                        IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                        boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

                        if (hasLowStorage)
                        {
                            Toast.makeText(this, "Low Storage", Toast.LENGTH_LONG).show();
                        }
                    }


                    Frame imageFrame = new Frame.Builder()
                            .setBitmap(bitmap)
                            .build();

                    SparseArray<TextBlock> items = textRecognizer.detect(imageFrame);


                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < items.size(); i++)
                    {
                        TextBlock item = items.get(i);
                        stringBuilder.append(item.getValue());
                        stringBuilder.append("\n");
                    }
                    capturedText = stringBuilder.toString();
                    save(capturedText);
                }
            } catch (Exception e)
            {
                Toast toast = Toast.makeText(getApplicationContext(), "Unable to read image. " + e.getMessage(), Toast.LENGTH_LONG);
                toast.setMargin(TOAST_MARGIN, TOAST_MARGIN);
                toast.show();
            }

        } else
        {
            finish();
        }
    }

    /**
     * Send text back to caller
     *
     * @param rawText
     */
    private void save(String rawText)
    {
        Intent intent = new Intent();
        intent.putExtra(AddEditRecipe.EXTRA_RAW_TEXT, rawText);
        setResult(RESULT_OK, intent);
        finish();
    }

}
