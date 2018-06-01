package it.slyce.slycesdk_headless_demo_java;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class CameraActivity extends AppCompatActivity implements CameraResultDisplayListener, CameraResultListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String KEY_LENS_ID = "KEY_LENS_ID";
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private CameraResultFragment fragment;
    private CameraSurfaceView cameraSurfaceView;

    /**
     * Starts the Activity.
     *
     * @param context
     * @param lensId
     */
    public static void startActivity(Context context, String lensId) {
        Intent intent = new Intent(context, CameraActivity.class);
        intent.putExtra(KEY_LENS_ID, lensId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        cameraSurfaceView = findViewById(R.id.camera_surface_view);
        cameraSurfaceView.init(getLensId(), this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isCameraPermissionGranted()) {

            // If camera permission has not been granted, request it.
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        } else {

            // If camera permission has been granted, start the camera preview.
            cameraSurfaceView.startCameraPreview();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraSurfaceView.stopCameraPreview();
    }

    @Override
    public void onBackPressed() {
        if (fragment != null && fragment.isAdded()) {
            cameraSurfaceView.startCameraPreview();
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCameraResultDisplayClosed() {
        cameraSurfaceView.startCameraPreview();
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void onSearchCompleted() {
        if (fragment != null) {
            fragment.notifySearchComplete();
        }
    }

    @Override
    public void onSearchStarted() {
        fragment = CameraResultFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.camera_result_fragment_container, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
        cameraSurfaceView.stopCameraPreview();
    }

    @Override
    public void onSearchReceivedUpdate(String header, String update) {
        if (fragment != null) {
            fragment.addSearchUpdate(header, update);
        }
    }

    private String getLensId() {
        return getIntent() != null ? getIntent().getStringExtra(KEY_LENS_ID) : null;
    }

    private boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED;
    }
}
