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
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.report.mpmetrics.MPConfig;
import com.android.slyce.requests.SlyceProductsRequest;
import com.crashlytics.android.Crashlytics;
import org.json.JSONArray;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends ActionBarActivity implements View.OnClickListener, OnSlyceRequestListener {

    private final String TAG = MainActivity.class.getSimpleName();

    private Button uploadImage;
    private Button uploadImageUrl;
    private Button initSlyceObject;

    private EditText clientIdEditText;
    private EditText imageUrlEditText;

    private Bitmap selectedBitmap;

    private static final int SELECT_PICTURE = 1;

    private Slyce slyce;

    private SlyceProductsRequest slyceProductsRequestImageUrl;
    private SlyceProductsRequest slyceProductsRequestImage;

    private ProgressBar progressBar;

    /* Examples client id's */
    // jcpenney852
    // homedepot623
    // jcpenney852

    /* Examples url's */
    // "http://static.trustedreviews.com/94/00002891c/3862/studio-1.jpg";
    // "http://www.mini-laptops-and-notebooks.com/images/Apple_MacBook.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);
        setContentView(R.layout.activity_main);

        initViews();
    }

    private void initViews(){

        uploadImage = (Button) findViewById(R.id.upload_image);
        uploadImageUrl = (Button) findViewById(R.id.upload_image_url);
        initSlyceObject = (Button) findViewById(R.id.init_sdk);
        clientIdEditText = (EditText) findViewById(R.id.client_id);
        imageUrlEditText = (EditText) findViewById(R.id.image_url);
        progressBar = (ProgressBar) findViewById(R.id.progress);

        uploadImage.setOnClickListener(this);
        uploadImageUrl.setOnClickListener(this);
        initSlyceObject.setOnClickListener(this);
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

        Log.i(TAG, "Products: " + products.toString());

        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onError(final String message) {

        Toast.makeText(MainActivity.this, "onError: " + message, Toast.LENGTH_LONG).show();

        progressBar.setVisibility(View.INVISIBLE);
    }
    // OnSlyceRequestListener callbacks

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.init_sdk:

                String clientId = clientIdEditText.getText().toString();

                if(TextUtils.isEmpty(clientId)){
                    showDialogError("Please insert Client ID");
                    return;
                }

                // Assigning it to null for re initiation (Do Not do this in real app)
                slyce = null;

                slyce = Slyce.getInstance(this, clientId);

                break;

            case R.id.upload_image:

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), SELECT_PICTURE);

                progressBar.setVisibility(View.VISIBLE);

                break;

            case R.id.upload_image_url:

                String imageUrl = imageUrlEditText.getText().toString();

                if(TextUtils.isEmpty(imageUrl)){
                    showDialogError("Please insert Image Url");
                    return;
                }

                if(slyce == null){
                    showDialogError("Please init Slyce object");
                    return;
                }

                slyceProductsRequestImageUrl = new SlyceProductsRequest(slyce, this, imageUrl);
                slyceProductsRequestImageUrl.execute();

                progressBar.setVisibility(View.VISIBLE);

                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {

            if (requestCode == SELECT_PICTURE) {

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
}
