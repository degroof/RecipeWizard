package com.stevedegroof.recipe_wizard;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

/**
 * superclass of all activities in app
 */
public class CommonActivity extends AppCompatActivity
{
    public static final String EXTRA_RECIPE = "com.stevedegroof.recipe_wizard.RECIPE";
    public static final int RequestCameraPermissionID = 1001;
    public static final int RequestFileWritePermissionID = 1002;
    public static final int RequestFileReadPermissionID = 1003;
    public static final int RESULT_DELETE = 10001;
    public static final int RESULT_HOME = 10002;
    public static final String DEFAULT_EXPORT_FILENAME = "RecipesExport.txt";
    public static final String RECIPE_BREAK_DETECT = "------";
    public static final String BOOK_FILE_NAME = "RecipeBook.txt";
    public static final String RECIPE_FILE_NAME = "Recipes.json";
    public static final String GROCERY_FILE_NAME = "Groceries.json";
    public static final String RECIPE_BREAK = "-----------------";
    public static final String EXTRA_RAW_TEXT = "rawText";
    public static final int REQUEST_CAPTURE_TEXT = 1;
    public static final int REQUEST_IMPORT = 2;
    public static final int REQUEST_EXPORT = 3;
    public static final int REQUEST_MAGIC = 4;
    public static final int IMPORT_APPEND = 10002;
    public static final int IMPORT_OVERWRITE = 10003;
    public static final int IMPORT_MERGE = 10006;
    public static final int SHARE_CONVERTED = 10004;
    public static final int SHARE_ORIGINAL = 10005;

    /**
     * returning from activity...
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_HOME && !(this instanceof MainActivity))
        {
            Intent intent = new Intent();
            setResult(RESULT_HOME, intent);
            finish();
        }
    }


    /**
     * Regardless of how deep you are, go to the home screen
     */
    public void goHome()
    {
        Intent intent = new Intent();
        setResult(RESULT_HOME, intent);
        finish();
    }

}
