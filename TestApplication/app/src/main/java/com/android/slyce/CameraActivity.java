package com.android.slyce;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Toast;

import com.android.slyce.camera.SlyceCamera;
import com.android.slyce.listeners.OnSlyceCameraListener;
import org.json.JSONArray;

public class CameraActivity extends Activity implements OnSlyceCameraListener{

    private SlyceCamera slyceCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        SurfaceView preview = (SurfaceView) findViewById(R.id.preview);

        Slyce slyce = Slyce.getInstance(this, "YOUR CLIENT ID");

        slyceCamera = new SlyceCamera(this, slyce, preview, this);
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

    /* OnSlyceCameraListener */
    @Override
    public void onBarcodeRecognition(String barcode) {



    }

    @Override
    public void on2DRecognition(String irId, String productInfo) {

        Toast.makeText(this,
                "MoodStocks Progress: " + irId +
                        "\n" + "Message: " + productInfo, Toast.LENGTH_LONG).show();

    }

    @Override
    public void on2DExtendedRecognition(JSONArray products) {

        Toast.makeText(this,
                "MoodStocks Extended: " + products, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(String message) {

        Toast.makeText(CameraActivity.this, "onError: " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSnap(Bitmap bitmap) {

    }

    @Override
    public void onTap() {

    }
}
