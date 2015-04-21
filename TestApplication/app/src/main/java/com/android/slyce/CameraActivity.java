package com.android.slyce;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
import com.android.slyce.camera.SlyceCamera;

/**
 * Created by davidsvilem on 4/20/15.
 */
public class CameraActivity extends Activity {

    private SlyceCamera slyceCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        SurfaceView preview = (SurfaceView) findViewById(R.id.preview);


        Slyce slyce = Slyce.getInstance(this, "YOUR CLIENT ID");

        slyceCamera = new SlyceCamera(this, slyce, preview);

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
}
