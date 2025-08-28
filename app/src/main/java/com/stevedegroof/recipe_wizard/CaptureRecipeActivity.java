package com.stevedegroof.recipe_wizard;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.util.UnstableApi;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;


/**
 * Activity for capturing recipe text.
 * <p>
 * This activity allows users to input recipe text manually, capture text from the device camera,
 * or select an image from the gallery for text extraction using ML Kit Text Recognition.
 * The captured or input text can then be processed and used to create a new recipe.
 * <p>
 * Key functionalities include:
 * <ul>
 *     <li>Manual text input via a {@link TextInputEditText}.</li>
 *     <li>Image capture using the device camera.</li>
 *     <li>Image selection from the device gallery.</li>
 *     <li>Text recognition from images using {@link com.google.mlkit.vision.text.TextRecognizer}.</li>
 *     <li>Navigation to {@link EditTextBlocksActivity} to allow users to edit recognized text blocks.</li>
 *     <li>Parsing the final text to create a {@link Recipe} object.</li>
 *     <li>Saving the created recipe and returning to the previous activity.</li>
 * </ul>
 * <p>
 * This activity handles camera permission requests and uses {@link ActivityResultLauncher}
 * for handling results from image picking, picture taking, and permission requests.
 * It also manages the lifecycle of these operations and updates the UI accordingly.
 */
public class CaptureRecipeActivity extends AppCompatActivity
{
    private MaterialToolbar toolbarCaptureRecipe;
    private TextInputEditText captureEditText;
    private ImageButton cameraButton, imageButton, doneButton;
    private com.google.mlkit.vision.text.TextRecognizer textRecognizer;

    private ActivityResultLauncher<Intent> pickImageLauncher;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private Uri cameraImageUri;

    private ActivityResultLauncher<String> requestCameraPermissionLauncher;


    /**
     * Initializes the activity.
     * Sets up the layout, toolbar, UI elements, text recognizer, and activity result launchers
     * for picking images, taking pictures, and requesting camera permissions.
     * Also sets up listeners for the buttons.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        Recipes.getInstance().setRawText(null);
        setContentView(R.layout.activity_capture_recipe);

        toolbarCaptureRecipe = findViewById(R.id.toolbarCaptureRecipe);
        setSupportActionBar(toolbarCaptureRecipe);

        captureEditText = findViewById(R.id.edittext_capture);
        cameraButton = findViewById(R.id.button_take_photo);
        imageButton = findViewById(R.id.button_get_image);
        doneButton = findViewById(R.id.button_done);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result ->
                {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
                    {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null)
                        {
                            processImageUriForText(imageUri);
                        }
                    }
                });

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success ->
                {
                    if (success && cameraImageUri != null)
                    {
                        processImageUriForText(cameraImageUri);
                    } else
                    {
                        Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                    }
                });

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted ->
                {
                    if (isGranted)
                    {
                        launchCamera();
                    } else
                    {
                        Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        setupButtonListeners();
    }

    /**
     * Called when the activity will start interacting with the user.
     * At this point your activity is at the top of the activity stack, with user input going to it.
     * This method is always called after {@link #onRestart} and {@link #onStart}.
     *
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * <p>This implementation retrieves the raw text from {@link Recipes#getInstance()} and sets it
     * to the {@code captureEditText} if it's not null. It also requests focus for the
     * {@code captureEditText}.</p>
     *
     * @see #onRestart
     * @see #onStart
     * @see #onPause
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        String rawText = Recipes.getInstance().getRawText();
        if (rawText != null)
        {
            captureEditText.setText(rawText);
        }
        captureEditText.requestFocus();
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_recipe_menu, menu);
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
        int id = item.getItemId();

        if (id == R.id.action_home_edit)
        {
            navigateToMainActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Navigates to the MainActivity.
     * This method creates an Intent to start the MainActivity.
     * It also adds flags to clear the activity stack above MainActivity and ensure it's a single top instance.
     * After starting the MainActivity, it finishes the current CaptureRecipeActivity.
     */
    private void navigateToMainActivity()
    {
        Intent intent = new Intent(CaptureRecipeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Sets up the on-click listeners for the done, camera, and image buttons.
     * - The doneButton parses the text from captureEditText, creates a Recipe object,
     * populates it with the parsed data, sets it as the current recipe in the Recipes singleton,
     * and finishes the activity.
     * - The cameraButton calls the getCameraText() method to capture text from the camera.
     * - The imageButton calls the getImageText() method to get text from an image.
     */
    private void setupButtonListeners()
    {
        doneButton.setOnClickListener(v ->
        {
            String rawText = captureEditText.getText().toString();
            Recipe recipe = new Recipe();
            RecipeParser parser = new RecipeParser();
            parser.setRawText(rawText, false);
            recipe.setTitle(parser.getTitle());
            recipe.setServings(Integer.toString(parser.getServings()));
            recipe.setMetric(parser.isMetric());
            recipe.setIngredients(parser.getIngredients());
            recipe.setDirections(parser.getDirections());
            recipe.setNotes(parser.getNotes());
            Recipes.getInstance().setCurrentRecipe(recipe);
            finish();
        });

        cameraButton.setOnClickListener(v ->
                getCameraText());

        imageButton.setOnClickListener(v ->
                getImageText());


    }

    /**
     * Initiates the process of selecting an image from the device's gallery
     * to extract text. It saves the current text from the input field
     * and then launches an intent to pick an image.
     */
    private void getImageText()
    {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Recipes.getInstance().setRawText(captureEditText.getText().toString());
        pickImageLauncher.launch(intent);
    }


    /**
     * Gets text from the camera.
     * If permission to use the camera has been granted, the camera is launched.
     * Otherwise, a request for permission to use the camera is launched.
     */
    private void getCameraText()
    {
        Recipes.getInstance().setRawText(captureEditText.getText().toString());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        {
            launchCamera();
        } else
        {

            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Processes the image URI for text recognition.
     * <p>
     * This method takes an image URI, creates an InputImage from it, and then uses the
     * ML Kit TextRecognizer to extract text from the image.
     * <p>
     * On successful text recognition:
     * 1. The recognized VisionText is stored in the Recipes singleton.
     * 2. The EditTextBlocksActivity is started to allow the user to edit the recognized text blocks.
     * 3. The raw text (potentially edited in EditTextBlocksActivity) is retrieved from the Recipes singleton.
     * 4. The raw text is appended to the existing text in the {@code captureEditText}.
     * 5. The cursor is moved to the end of the newly added text, and the EditText gains focus.
     * <p>
     * On failure to recognize text, a Toast message "Text recognition failed" is displayed.
     * On failure to load the image (IOException), a Toast message "Failed to load image" is displayed.
     *
     * @param imageUri The URI of the image to process for text.
     */
    private void processImageUriForText(Uri imageUri)
    {
        try
        {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            textRecognizer.process(image)
                    .addOnSuccessListener(visionText ->
                    {
                        Recipes.getInstance().setVisionText(visionText);
                        Intent intent = new Intent(CaptureRecipeActivity.this, EditTextBlocksActivity.class);
                        startActivity(intent);
                        String capturedText = Recipes.getInstance().getRawText();
                        runOnUiThread(() ->
                        {
                            if (captureEditText != null)
                            {
                                int loc = 0;
                                String existingText = captureEditText.getText().toString();
                                if (existingText.isEmpty())
                                {
                                    captureEditText.setText(capturedText);
                                } else
                                {
                                    loc = existingText.length() + 1;
                                    captureEditText.setText(String.format("%s\n%s", existingText, capturedText));
                                }
                                captureEditText.setSelection(loc);
                                captureEditText.requestFocus();
                            }
                        });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(CaptureRecipeActivity.this, "Text recognition failed", Toast.LENGTH_SHORT).show());
        } catch (IOException e)
        {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Launches the device camera to capture an image.
     * This method creates a new image file in the external storage,
     * then starts the camera activity using an ActivityResultLauncher.
     * If the image URI cannot be created, a toast message is displayed.
     */
    private void launchCamera()
    {

        String fileName = "recipe_image_" + System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image captured for recipe text recognition");


        cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (cameraImageUri != null)
        {
            takePictureLauncher.launch(cameraImageUri);
        } else
        {
            Toast.makeText(this, "Failed to create image URI", Toast.LENGTH_SHORT).show();
        }
    }


}
