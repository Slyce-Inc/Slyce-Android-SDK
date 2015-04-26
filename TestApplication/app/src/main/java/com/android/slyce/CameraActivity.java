package com.android.slyce;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.slyce.camera.SlyceCamera;
import com.android.slyce.listeners.OnSlyceCameraListener;
import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

public class CameraActivity extends Activity implements OnSlyceCameraListener, View.OnClickListener {

    private SlyceCamera slyceCamera;
    private Button snap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        SurfaceView preview = (SurfaceView) findViewById(R.id.preview);
        snap = (Button) findViewById(R.id.snap);
        snap.setOnClickListener(this);

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

        Toast.makeText(this,
                "onSnap: " + bitmap.getWidth() + " X " + bitmap.getHeight(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTap() {

    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.snap:

                slyceCamera.snap();

                break;
        }
    }
}
