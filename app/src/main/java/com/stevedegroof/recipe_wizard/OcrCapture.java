package com.stevedegroof.recipe_wizard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

/**
 * Get text from camera
 */
public class OcrCapture extends CommonActivity
{
    private SurfaceView cameraView;
    private CameraSource cameraSource;
    private String capturedText = "";
    private TextRecognizer textRecognizer = null;

    /**
     * Immediately open camera and wait for response
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);
        cameraView = findViewById(R.id.surface_view);
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational())
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Unable to get camera.", Toast.LENGTH_LONG);
            toast.setMargin(TOAST_MARGIN, TOAST_MARGIN);
            toast.show();
        } else
        {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.ocr_prompt, Toast.LENGTH_LONG);
            toast.setMargin(TOAST_MARGIN, TOAST_MARGIN);
            toast.show();
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();

            cameraView.getHolder().addCallback(new SurfaceHolder.Callback()
            {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder)
                {
                    try
                    {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                        {
                            ActivityCompat.requestPermissions(OcrCapture.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    RequestCameraPermissionID);
                            return;
                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e)
                    {
                        Toast toast = Toast.makeText(getApplicationContext(), "Unable to capture. " + e.getMessage(), Toast.LENGTH_LONG);
                        toast.setMargin(TOAST_MARGIN, TOAST_MARGIN);
                        toast.show();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2)
                {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder)
                {
                    cameraSource.stop();
                }
            });

        }
    }


    /**
     * User tapped screen. Grab text.
     *
     * @param v
     */
    public void screenTapped(View v)
    {
        textRecognizer.setProcessor(new Detector.Processor<TextBlock>()
        {
            @Override
            public void release()
            {

            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections)
            {
                final SparseArray<TextBlock> items = detections.getDetectedItems();
                if (items.size() > 0)
                {
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
            }
        });
    }

    /**
     * send captured text back to caller
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

    /**
     * Request permissions and start camera.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case RequestCameraPermissionID:
            {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                    {
                        return;
                    }
                    try
                    {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e)
                    {
                        Toast toast = Toast.makeText(getApplicationContext(), "Unable to capture. " + e.getMessage(), Toast.LENGTH_LONG);
                        toast.setMargin(TOAST_MARGIN, TOAST_MARGIN);
                        toast.show();
                    }
                }
            }
            break;
        }
    }


}
