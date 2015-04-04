package com.example.davidsvilem.testapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.slyce.Slyce;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.requests.GetProductsRequest;

import org.json.JSONArray;

import java.lang.reflect.Method;


public class MainActivity extends ActionBarActivity implements OnSlyceRequestListener, View.OnClickListener {

    private final String TAG = MainActivity.class.getSimpleName();

    private Button pickImage;
    private Button uploadImage;
    private Button uploadImageUrl;

    private Bitmap selectedBitmap;

    private static final int SELECT_PICTURE = 1;

    private Slyce slyce;

    private GetProductsRequest mGetProductsRequest_1;
    private GetProductsRequest mGetProductsRequest_2;

    private EditText clientIdEditText;

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        String name = Devices.getDeviceName();
//
//        String name2 = Devices.getDeviceName(Build.DEVICE);

        initViews();
    }

    private void initViews(){

        pickImage = (Button) findViewById(R.id.pick_image);
        uploadImage = (Button) findViewById(R.id.upload_image);
        uploadImageUrl = (Button) findViewById(R.id.upload_image_url);
        clientIdEditText = (EditText) findViewById(R.id.client_id);

        // jcpenney852 homedepot623
        clientIdEditText.setText("homedepot623");

        pickImage.setOnClickListener(this);
        uploadImage.setOnClickListener(this);
        uploadImageUrl.setOnClickListener(this);
    }

    // OnSlyceRequestListener callbacks
    @Override
    public void onSlyceProgress(final long progress, final String message, String token) {

        Toast.makeText(MainActivity.this,
                "token=" + token +
                " message= " + message +
                " progress= " + progress,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void on2DRecognition() {

    }

    @Override
    public void on3DRecognition(final JSONArray products) {

        Toast.makeText(MainActivity.this, "Found " +  products.length() + " products", Toast.LENGTH_LONG).show();

        Log.i(TAG, "Products: " + products.toString());
    }

    @Override
    public void onError(final String message) {

        Toast.makeText(MainActivity.this, "onError: " + message, Toast.LENGTH_LONG).show();
    }
    // OnSlyceRequestListener callbacks

    @Override
    public void onClick(View v) {

        String clientId = clientIdEditText.getText().toString();

        if(TextUtils.isEmpty(clientId)){
            showDialogError("Please insert Client ID");
            return;
        }

        slyce = Slyce.getInstance(this, clientId);

        switch(v.getId()){

            case R.id.pick_image:

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), SELECT_PICTURE);

                break;

            case R.id.upload_image:

                if(selectedBitmap == null){
                    showDialogError("Please pick image first");
                    return;
                }

                mGetProductsRequest_1 = new GetProductsRequest(slyce, this, selectedBitmap);
                mGetProductsRequest_1.execute();

                break;

            case R.id.upload_image_url:

                String hearPhones = "http://static.trustedreviews.com/94/00002891c/3862/studio-1.jpg";
//                String macbook = "http://www.mini-laptops-and-notebooks.com/images/Apple_MacBook.jpg";

                mGetProductsRequest_1 = new GetProductsRequest(slyce, this, hearPhones);
                mGetProductsRequest_1.execute();

//                mGetProductsRequest_2 = new GetProductsRequest(slyce, this, hearPhones);
//                mGetProductsRequest_2.execute();

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

                Uri selectedImage = data.getData();

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;//returning null for below statement
                selectedBitmap = BitmapFactory.decodeFile( getPath(selectedImage), options);
            }
        }
    }

    public String getPath(Uri uri) {

        if( uri == null ) {
            return null;
        }

        // this will only work for images selected from gallery
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
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
