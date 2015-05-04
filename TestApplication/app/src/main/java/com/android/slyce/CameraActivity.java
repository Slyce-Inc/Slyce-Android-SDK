package com.android.slyce;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.android.slyce.camera.SlyceCamera;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

public class CameraActivity extends Activity implements OnSlyceCameraListener, View.OnClickListener {

    private SlyceCamera slyceCamera;
    private Button snap;
    private Button flash;

    private ProgressBar snapProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        SurfaceView preview = (SurfaceView) findViewById(R.id.preview);
        snap = (Button) findViewById(R.id.snap);
        snap.setOnClickListener(this);

        flash = (Button) findViewById(R.id.flash);
        flash.setOnClickListener(this);

        snapProgress = (ProgressBar) findViewById(R.id.snap_progress);

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
    public void onCameraBarcodeRecognition(String barcode) {

        Toast.makeText(this,
                "onCameraBarcodeRecognition: " + barcode, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCamera2DRecognition(String irId, String productInfo) {

        Toast.makeText(this,
                "onCamera2DRecognition: " + irId +
                        "\n" + "Message: " + productInfo, Toast.LENGTH_LONG).show();

    }

    @Override
    public void onCamera2DExtendedRecognition(JSONArray products) {

        Toast.makeText(this,
                "onCamera2DExtendedRecognition: " + products, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCameraSlyceProgress(long progress, String message, String token) {

        Toast.makeText(this,
                "onCameraSlyceProgress: " + progress +
                        "\n" + "Message: " + message +
                        "\n" + "Token: " + token, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCamera3DRecognition(JSONArray products) {

        Toast.makeText(CameraActivity.this, "onCamera3DRecognition: " +  products.length() + " products", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCameraStageLevelFinish(OnSlyceRequestListener.StageMessage message) {

        Toast.makeText(CameraActivity.this, "onCameraStageLevelFinish: " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSlyceCameraError(String message) {

        Toast.makeText(CameraActivity.this, "onSlyceCameraError: " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSnap(Bitmap bitmap) {

        snapProgress.setVisibility(View.INVISIBLE);

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
                snapProgress.setVisibility(View.VISIBLE);

                break;

            case R.id.flash:

                slyceCamera.turnFlash();

                break;
        }
    }

    public static String[] saveImage(Bitmap finalBitmap) {

        String[] data = new String[2];

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        data[0] = fname;
        data[1] = file.getPath();

        return data;
    }
}
