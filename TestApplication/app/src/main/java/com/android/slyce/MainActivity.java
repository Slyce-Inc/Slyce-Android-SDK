package com.android.slyce;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.slyce.listeners.OnSlyceOpenListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.requests.SlyceProductsRequest;
import com.android.slyce.utils.SlyceLog;

import org.json.JSONArray;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends ActionBarActivity implements View.OnClickListener, OnSlyceRequestListener, TextView.OnEditorActionListener {

    private final String TAG = MainActivity.class.getSimpleName();

    private Button enterUrl;
    private Button uploadImage;
    private Button cancelRequests;

    private TextView acceptTextView;
    private TextView premium;
    private TextView enabled2D;
    private TextView results;

    private EditText clientIdEditText;

    private static final int SELECT_PICTURE = 1;

    private Slyce slyce;

    private SlyceProductsRequest slyceProductsRequestImageUrl;
    private SlyceProductsRequest slyceProductsRequestImage;

    private ProgressBar progressBar;

    private String imageUrl;

    private boolean isSlyceSDKOpened = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initViews();
    }

    private void initViews(){

        uploadImage = (Button) findViewById(R.id.upload_image);
        enterUrl = (Button) findViewById(R.id.enter_url);
        cancelRequests = (Button) findViewById(R.id.cancel_requests);
        clientIdEditText = (EditText) findViewById(R.id.client_id);
        acceptTextView = (TextView) findViewById(R.id.accept_client_id);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        enabled2D = (TextView) findViewById(R.id.enabled_2d);
        premium = (TextView) findViewById(R.id.premium);
        results = (TextView) findViewById(R.id.results);

        results.setTextIsSelectable(true);

        uploadImage.setOnClickListener(this);
        enterUrl.setOnClickListener(this);
        acceptTextView.setOnClickListener(this);
        cancelRequests.setOnClickListener(this);

        clientIdEditText.setOnEditorActionListener(this);
    }

    // OnSlyceRequestListener callbacks
    @Override
    public void onSlyceProgress(final long progress, final String message, String token) {

        Toast.makeText(this,
                "Progress: " + progress +
                        "\n" + "Message: " + message +
                        "\n" + "Token: " + token, Toast.LENGTH_LONG).show();
    }

    @Override
    public void on2DRecognition() {
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void on3DRecognition(final JSONArray products) {

        Toast.makeText(MainActivity.this, "Found " +  products.length() + " products", Toast.LENGTH_LONG).show();

        SlyceLog.i(TAG, "Products: " + products.toString());

        if(products.length() > 0){
            results.setText(products.toString());
        }

        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onError(final String message) {

        Toast.makeText(MainActivity.this, "onError: " + message, Toast.LENGTH_LONG).show();

        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onStageLevelFinish(StageMessage message) {

        Toast.makeText(MainActivity.this, "Stage Message: " + message, Toast.LENGTH_LONG).show();
    }
    // OnSlyceRequestListener callbacks

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.accept_client_id:

                String clientId = clientIdEditText.getText().toString();

                if(TextUtils.isEmpty(clientId)){
                    showDialogError("Please insert Client ID");
                    return;
                }

                hideKeyboard();

                // Show progress bar
                progressBar.setVisibility(View.VISIBLE);

                // Assigning it to null for re initiation (Do Not do this in real app)
                if(slyce  != null){
                    slyce.release();
                }

                // Reset boolean
                isSlyceSDKOpened = false;

                slyce = Slyce.getInstance(this, clientId);
                slyce.open(new OnSlyceOpenListener() {

                    @Override
                    public void onOpenSuccess() {

                        Toast.makeText(MainActivity.this, "Slyce SDK opened", Toast.LENGTH_LONG).show();

                        // Hide progress
                        progressBar.setVisibility(View.INVISIBLE);

                        // Set boolean
                        isSlyceSDKOpened = true;

                        // Set Premium and 2D Enabled properties
                        premium.setText(getString(R.string.premium, String.valueOf(slyce.isPremiumUser()).toUpperCase()));
                        enabled2D.setText(getString(R.string.enabled_2d, String.valueOf(slyce.is2DSearchEnabled()).toUpperCase()));
                    }

                    @Override
                    public void onOpenFail(String message) {

                        // Hide progress
                        progressBar.setVisibility(View.INVISIBLE);

                        Toast.makeText(MainActivity.this, "Slyce SDK open failed: " + message, Toast.LENGTH_LONG).show();
                    }
                });

                break;

            case R.id.upload_image:

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), SELECT_PICTURE);

                break;

            case R.id.enter_url:

                showDialog();

                break;

            case R.id.cancel_requests:

                progressBar.setVisibility(View.INVISIBLE);

                if(slyceProductsRequestImage != null){
                    slyceProductsRequestImage.cancel();
                }

                if(slyceProductsRequestImageUrl != null){
                    slyceProductsRequestImageUrl.cancel();
                }

                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {

            if (requestCode == SELECT_PICTURE) {

                Bitmap selectedBitmap = null;

                Uri selectedImageUri = data.getData();
                if (Build.VERSION.SDK_INT < 19) {

                    String selectedImagePath = getPath(selectedImageUri);
                    selectedBitmap = BitmapFactory.decodeFile(selectedImagePath);

                } else {

                    ParcelFileDescriptor parcelFileDescriptor;
                    try {
                        parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedImageUri, "r");
                        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        selectedBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                        parcelFileDescriptor.close();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(selectedBitmap == null){
                    showDialogError("Please pick image first");
                    return;
                }

                if(slyce == null){
                    showDialogError("Please init Slyce object");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                slyceProductsRequestImage = new SlyceProductsRequest(slyce, this, selectedBitmap);
                slyceProductsRequestImage.execute();
            }
        }
    }

    /**
     * helper to retrieve the path of an image URI
     */
    public String getPath(Uri uri) {
        if( uri == null ) {
            return null;
        }
        String[] projection = { MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    private void showDialogError(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error").setMessage(message).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.show();
    }

    private void showDialog(){

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        final EditText edittext = new EditText(this);
        edittext.setLines(1);
        alert.setMessage("Enter Image URL");
        alert.setTitle("Upload Image URL");

        alert.setView(edittext);

        alert.setPositiveButton("Go", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                imageUrl = edittext.getText().toString();

                if(TextUtils.isEmpty(imageUrl)){
                    showDialogError("Please insert Image Url");
                    return;
                }

                if(slyce == null){
                    showDialogError("Please init Slyce object");
                    return;
                }

                slyceProductsRequestImageUrl = new SlyceProductsRequest(slyce, MainActivity.this, imageUrl);
                slyceProductsRequestImageUrl.execute();

                progressBar.setVisibility(View.VISIBLE);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

        if (actionId == EditorInfo.IME_ACTION_GO) {

            hideKeyboard();

            acceptTextView.performClick();

            return true;
        }
        return false;
    }

    private void hideKeyboard(){
        InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }
}
