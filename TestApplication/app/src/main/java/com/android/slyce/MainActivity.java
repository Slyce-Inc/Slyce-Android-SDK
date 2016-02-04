package com.android.slyce;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.slyce.enums.SlyceRequestStage;
import com.android.slyce.fragments.BaseDialogFragment;
import com.android.slyce.listeners.OnSlyceCameraFragmentListener;
import com.android.slyce.listeners.OnSlyceOpenListener;
import com.android.slyce.listeners.OnSlyceRequestListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends Activity implements OnSlyceRequestListener, OnSlyceCameraFragmentListener,TextView.OnEditorActionListener {

    private final String TAG = MainActivity.class.getSimpleName();

    private TextView acceptTextView;
    private TextView premium;
    private TextView enabled2D;
    private TextView results;

    private EditText clientIdEditText;

    private static final int SELECT_PICTURE = 1;

    private Slyce slyce;

    private SlyceRequest slyceRequestImageUrl;
    private SlyceRequest slyceRequestImage;
    private SlyceRequest slycePublicKeywordsImageUrl;

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

        clientIdEditText = (EditText) findViewById(R.id.client_id);
        acceptTextView = (TextView) findViewById(R.id.accept_client_id);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        enabled2D = (TextView) findViewById(R.id.enabled_2d);
        premium = (TextView) findViewById(R.id.premium);
        results = (TextView) findViewById(R.id.results);

        results.setTextIsSelectable(true);

        clientIdEditText.setOnEditorActionListener(this);
    }

    // OnSlyceRequestListener callbacks
    @Override
    public void onSlyceProgress(final long progress, final String message, String token) {

        Toast.makeText(this,
                "onSlyceProgress:" +
                        "\n" + "Progress: " + progress +
                        "\n" + "Message: " + message +
                        "\n" + "Token: " + token, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onImageDetected(String productInfo) {

        Toast.makeText(this,
                "onImageDetected:" +  "\n" +
                        "Product Info: " + productInfo, Toast.LENGTH_SHORT).show();

        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onImageInfoReceived(JSONArray products) {
        Toast.makeText(this,
                "onImageInfoReceived:" +
                        "\n" + "Products: " + products, Toast.LENGTH_SHORT).show();

        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onBarcodeDetected(SlyceBarcode barcode) {

        Toast.makeText(this,
                "onBarcodeDetected:" + "\n" +
                "Barcode Type: " + barcode.getType() + "\n" +
                "Barcode: " + barcode.getBarcode(), Toast.LENGTH_SHORT).show();

        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onResultsReceived(final JSONObject products) {

        Toast.makeText(MainActivity.this, "onResultsReceived:" +  "\n" + "Products: " + products, Toast.LENGTH_SHORT).show();

        results.setText(products.toString());

        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onItemDescriptionReceived(JSONObject keywords) {

        Toast.makeText(MainActivity.this, "onItemDescriptionReceived:" +  "\n" + "Keywords: " + keywords, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(final String message) {

        Toast.makeText(MainActivity.this, "onError: " + "\n" + "Message: " + message, Toast.LENGTH_SHORT).show();

        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onSlyceRequestStage(SlyceRequestStage message) {

        Toast.makeText(MainActivity.this, "onSlyceRequestStage:" + "\n" + "Message: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFinished() {

        Toast.makeText(MainActivity.this, "onFinished", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBarcodeInfoReceived(JSONObject products){

        Toast.makeText(MainActivity.this, "onBarcodeInfoReceived: " + "\n" + "Products: " + products, Toast.LENGTH_SHORT).show();
    }
    // OnSlyceRequestListener callbacks END

    // OnSlyceCameraFragmentListener callbacks (Full UI Mode)
    @Override
    public void onCameraFragmentResultsReceived(JSONObject results) {

        // Display products in a grid
        JSONArray products = results.optJSONArray("products");

        Intent intent = new Intent(this, ProductsGridActivity.class);
        intent.putExtra(ProductsGridActivity.PRODUCTS_KEY, products.toString());
        intent.putExtra(ProductsGridActivity.PRODUCTS_TYPE, ProductsGridActivity.PRODUCTS_3D_KEY);
        startActivity(intent);
    }

    @Override
    public void onCameraFragmentBarcodeDetected(SlyceBarcode barcode) {
        Toast.makeText(MainActivity.this, "onCameraFragmentBarcodeDetected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragmentImageDetected(String productInfo) {
        Toast.makeText(MainActivity.this, "onCameraFragmentImageDetected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragmentImageInfoReceived(JSONArray results) {

        // Display products in a grid
        Intent intent = new Intent(this, ProductsGridActivity.class);
        intent.putExtra(ProductsGridActivity.PRODUCTS_KEY, results.toString());
        intent.putExtra(ProductsGridActivity.PRODUCTS_TYPE, ProductsGridActivity.PRODUCTS_2D_KEY);
        startActivity(intent);
    }

    @Override
    public void onCameraFragmentError(String message) {
        Toast.makeText(MainActivity.this, "onCameraFragmentError", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragmentFinished() {
        Toast.makeText(MainActivity.this, "onCameraFragmentFinished", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFragmentBarcodeInfoReceived(JSONObject products){
        Toast.makeText(MainActivity.this, "onCameraFragmentBarcodeInfoReceived", Toast.LENGTH_SHORT).show();
    }
    // OnSlyceCameraFragmentListener callbacks END

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

                slyceRequestImage = new SlyceRequest(slyce, this, new JSONObject());
                slyceRequestImage.getProducts(selectedBitmap);
            }
        }
    }

    private void startSlyceCameraFragment(){

        // Add SlyceCameraFragment
        SlyceCameraFragment slyceFragment = SlyceCameraFragment.newInstance(null, true, false);

        // initialize SlyceCameraFragment object with custom barcode format set, if needed (the default is detection of all formats)
        //int barcodeFormatSet = Barcode.EAN_8 + Barcode.CODE_39+ Barcode.CODE_93;// customize barcode detection, the default is detection of all formats.
        //SlyceCameraFragment slyceFragment = SlyceCameraFragment.newInstance(null, true, false,true,true,3000, barcodeFormatSet);

        //customize the color of the circular progress
        //slyceFragment.setCircularProgressColor(Color.GREEN);


        //all custom fragments should extend the BaseDialogFragment and implement the onDismiss method, see example of CustomHelpScreen class

        //replace the default "Scanning Tips" screen
        //slyceFragment.setCustomHelpScreen(getCustomDialogScreen(CustomHelpScreen.SCAN_TIPS_DIALOG));

        //replace the default "not found" screen
        //slyceFragment.setCustomNotFoundScreen(getCustomDialogScreen(CustomHelpScreen.NOT_FOUND_DIALOG));

        //add custom buttons with custom fragments dialogs, the positioning is taken in percent of the screen when 100% is the width/height of the screen for x/y respectively
        //slyceFragment.addCustomScreenWithButton(getCustomDialogScreen(CustomHelpScreen.GENERAL_DIALOG),50.9f,50f,R.drawable.slyce_flash,this);
        //slyceFragment.addCustomScreenWithButton(getCustomDialogScreen(CustomHelpScreen.NOT_FOUND_DIALOG),15f,88f,R.drawable.slyce_flash,this);


        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.slyce_camera_fragment_container, slyceFragment);
        transaction.addToBackStack(null);
        // Commit the transaction
        transaction.commit();
    }


    /**
     * helper to create a BaseDialogFragment
     * all custom fragments must extend the BaseDialogFragment and implement the onDismiss method as shown in the example
     */
    private BaseDialogFragment getCustomDialogScreen(String dialogType){
        return CustomHelpScreen.newInstance(dialogType);
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

                slyceRequestImageUrl = new SlyceRequest(slyce, MainActivity.this, new JSONObject());
                slyceRequestImageUrl.getProducts(imageUrl);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(slyce != null){
            slyce.close();
        }
    }

    public void onButtonClick(View v){

        switch(v.getId()){

            case R.id.upload_image:

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), SELECT_PICTURE);

                break;

            case R.id.accept_client_id:

                String clientId = clientIdEditText.getText().toString();

                if(TextUtils.isEmpty(clientId)){
                    showDialogError("Please insert Client ID");
                    return;
                }

                hideKeyboard();

                // Show progress bar
                progressBar.setVisibility(View.VISIBLE);

                // Reset boolean
                isSlyceSDKOpened = false;

                slyce = Slyce.getInstance(this);

                slyce.open(clientId, new OnSlyceOpenListener(){

                    @Override
                    public void onOpenSuccess() {

                        Toast.makeText(MainActivity.this, "Slyce SDK opened", Toast.LENGTH_SHORT).show();

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

                        Toast.makeText(MainActivity.this, "Slyce SDK open failed: " + message, Toast.LENGTH_SHORT).show();
                    }
                });

                break;

            case R.id.enter_url:

                showDialog();

                break;

            case R.id.cancel_requests:

                progressBar.setVisibility(View.INVISIBLE);

                if(slyceRequestImage != null){
                    slyceRequestImage.cancel();
                }

                if(slyceRequestImageUrl != null){
                    slyceRequestImageUrl.cancel();
                }

                if(slycePublicKeywordsImageUrl != null){
                    slycePublicKeywordsImageUrl.cancel();
                }

                break;

            case R.id.camera_activity:

                if(slyce == null){
                    showDialogError("Please init Slyce object");
                    return;
                }

                startActivity(new Intent(this, CameraActivity.class));

                break;

            case R.id.full_ui_mode:

                if(slyce == null){
                    showDialogError("Please init Slyce object");
                    return;
                }

                startSlyceCameraFragment();

                break;

            case R.id.public_keywords:

                if(slyce == null){
                    showDialogError("Please init Slyce object");
                    return;
                }

                slycePublicKeywordsImageUrl = new SlyceRequest(slyce, this, new JSONObject());
                slycePublicKeywordsImageUrl.getItemDescription("https://cdn.lulus.com/images/product/medium/127386.jpg");

                break;
        }
    }
}
