package com.android.slyce;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.slyce.enums.SlyceRequestStage;
import com.android.slyce.listeners.OnSlyceCameraListener;

import org.json.JSONArray;
import org.json.JSONObject;

public class CameraActivity extends Activity implements OnSlyceCameraListener, View.OnClickListener {

    private static final String TAG = CameraActivity.class.getSimpleName();
    private SlyceCamera slyceCamera;
    private Button snap;
    private Button flash;
    private Button focuseAtPoint;
    private SurfaceView preview;

    private ProgressBar snapProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        initViews();

        // initialize SlyceCamera object
        slyceCamera = new SlyceCamera(this, Slyce.getInstance(this), preview, null, this);

        //// initialize SlyceCamera object with custom barcode format set if needed (the default is detection of all formats)
        //int barcodeFormatSet = Barcode.CODE_39+Barcode.EAN_8+ Barcode.CODE_93;// customize barcode detection, the default is detection of all formats.
        //slyceCamera = new SlyceCamera(this, Slyce.getInstance(this), preview, null, this,barcodeFormatSet);

        slyceCamera.shouldPauseScanner(false);                // the default is true
        //slyceCamera.setShouldPauseScannerDelayTime(5000);     // the default is 3000
        //slyceCamera.setContinuousRecognition(false);          // the default is true
        //slyceCamera.setContinuousRecognition2D(false);        // the default is true
        //slyceCamera.setContinuousRecognitionBarcodes(false);  // the default is true
    }

    @Override
    protected void onResume() {
        super.onResume();
        slyceCamera.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        slyceCamera.stop();
    }

    private void initViews(){

        preview = (SurfaceView) findViewById(R.id.preview);

        snap = (Button) findViewById(R.id.snap);
        snap.setOnClickListener(this);

        flash = (Button) findViewById(R.id.flash);
        flash.setOnClickListener(this);

        focuseAtPoint = (Button) findViewById(R.id.focus_at_point);
        focuseAtPoint.setOnClickListener(this);

        snapProgress = (ProgressBar) findViewById(R.id.snap_progress);
    }

    /* OnSlyceCameraListener */
    @Override
    public void onCameraBarcodeDetected(SlyceBarcode barcode) {

        Toast.makeText(this,
                        "onCameraBarcodeDetected:" + "\n" +
                        "Barcode Type: " + barcode.getType() + "\n" +
                        "Barcode: " + barcode.getBarcode(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraImageDetected(String productInfo) {

        Toast.makeText(this,
                "onCameraImageDetected:" + "\n" +
                        "Product Info: " + productInfo, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraImageInfoReceived(JSONArray products) {

        Toast.makeText(this,
                "onCameraImageInfoReceived:" +
                        "\n" + "Products: " + products, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraSlyceProgress(long progress, String message, String token) {

        Toast.makeText(this,
                "onCameraSlyceProgress: " + progress +
                        "\n" + "Message: " + message +
                        "\n" + "Token: " + token, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraSlyceRequestStage(SlyceRequestStage message) {

        Toast.makeText(this, "onCameraSlyceRequestStage:" + "\n" + "Message: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraResultsReceived(JSONObject products) {

        Toast.makeText(this, "onCameraResultsReceived:" +  "\n" + "Products: " + products, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSlyceCameraError(String message) {

        snapProgress.setVisibility(View.INVISIBLE);

        Toast.makeText(this, "onSlyceCameraError: " + "\n" + "Message: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTap(float x, float y) {

        Toast.makeText(this, "onTap:" + "\n" + "X: " + x + "\n" + "Y:" + y, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraPreviewMode(boolean front) {

    }

    @Override
    public void onSnap(Bitmap bitmap) {

        Toast.makeText(this, "onSnap:" + "\n" + "Width: " + bitmap.getWidth() + "\n" + "Height:" + bitmap.getHeight(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFinished() {

        Toast.makeText(this, "onCameraFinished", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraBarcodeInfoReceived(JSONObject products) {
        Toast.makeText(this, "onCameraBarcodeInfoReceived: " + "\n" + "Products: " + products, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.snap:

                slyceCamera.snap();
                snapProgress.setVisibility(View.VISIBLE);

                break;

            case R.id.flash:

                slyceCamera.turnFlash();

                break;

            case R.id.focus_at_point:

                slyceCamera.focusAtPoint(250, 250);

                break;
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SlyceCamera.MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, no need for action

                } else {

                    // permission denied
                    Log.e(TAG,"Camera permission denied");

                    showMessageOKCancel("You need to allow access to Camera",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[] {Manifest.permission.CAMERA},
                                            SlyceCamera.MY_PERMISSIONS_REQUEST_CAMERA);
                                }
                            });

                }
                return;
            }

            case SlyceCamera.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_DATA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, no need for action

                } else {

                    // permission denied
                    Log.e(TAG,"Camera permission denied");

                    showMessageOKCancel("You need to allow access to Storage",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            SlyceCamera.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_DATA);
                                }
                            });

                }
                return;
            }

        }
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(CameraActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }


}
