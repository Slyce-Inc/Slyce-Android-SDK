package it.slyce.slycesdk_lensview_demo_java;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.gson.Gson;

import java.util.List;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.SlyceCompletionHandler;
import it.slyce.sdk.SlyceLensView;
import it.slyce.sdk.SlyceSearchResponse;
import it.slyce.sdk.SlyceSearchResponseUpdate;
import it.slyce.sdk.SlyceSearchTask;
import it.slyce.sdk.SlyceSearchTaskListenerAdapter;
import it.slyce.sdk.SlyceSession;
import it.slyce.sdk.SlyceSessionListenerAdapter;
import it.slyce.sdk.exception.SlyceError;
import it.slyce.sdk.exception.lens.SlyceInvalidLensException;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] REQUIRED_PERMISSIONS = { Manifest.permission.CAMERA };

    // Add your Slyce account credentials here.
    private static final String SLYCE_ACCOUNT_ID = "";
    private static final String SLYCE_API_KEY = "";
    private static final String SLYCE_SPACE_ID = "";
    private static final String SLYCE_LENS_ID = "slyce.3D";

    private SlyceLensView slyceLensView;
    private SlyceSession slyceSession;

    // These methods are used to handle the results and updates from Slyce after starting a search.
    private SlyceSearchTaskListenerAdapter slyceSearchTaskListenerAdapter = new SlyceSearchTaskListenerAdapter() {

        @Override
        public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask task, SlyceSearchResponseUpdate.JobCreated update) {
            Log.d(TAG, "onSlyceSearchTaskReceivedUpdate: " + new Gson().toJson(update));
        }

        @Override
        public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask task, SlyceSearchResponseUpdate.TagFound update) {
            Log.d(TAG, "onSlyceSearchTaskReceivedUpdate: " + new Gson().toJson(update));
        }

        @Override
        public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask task, SlyceSearchResponseUpdate.ResultsReceived update) {
            Log.d(TAG, "onSlyceSearchTaskReceivedUpdate: " + new Gson().toJson(update));
        }

        @Override
        public void onSlyceSearchTaskFinished(SlyceSearchTask task, SlyceSearchResponse response) {
            Log.d(TAG, "onSlyceSearchTaskFinished: " + new Gson().toJson(response));
        }

        @Override
        public void onSlyceSearchTaskFailed(SlyceSearchTask task, List<SlyceError> errors) {
            Log.d(TAG, "onSlyceSearchTaskFailed: " + new Gson().toJson(errors));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // A LensView can be added to any layout and is a fully contained Slyce
        // camera experience, managed through the session and configuration given
        // at init-time.
        setContentView(R.layout.activity_main);
        slyceLensView = findViewById(R.id.slyce_lens_view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Slyce.getInstance(this).isOpen() && isCameraPermissionGranted()) {

            // If Slyce is open and camera permission has been granted, start the LensView camera.
            initSlyceSession();
            initSlyceLensView();

        } else if (!Slyce.getInstance(this).isOpen()) {

            // If Slyce is not open, attempt to open it.
            openSlyce();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (Slyce.getInstance(this).isOpen() && isCameraPermissionGranted()) {

            // If Slyce is open and camera permission has been granted, stop the LensView camera.
            slyceLensView.getCameraControls().stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (isCameraPermissionGranted()) {
            initSlyceSession();
            initSlyceLensView();
        }
    }

    private void initSlyceLensView() {

        // In order to create a LensView, we must provide an active session
        // (available once opening Slyce), and a lens ID, which is specific
        // to the lens type.
        //
        // In this case, we are creating a LensView which can
        // be used to detect and receive Visual Search results.
        try {
            slyceLensView.init(slyceSession, SLYCE_LENS_ID);
        } catch (SlyceInvalidLensException e) {
            e.printStackTrace();
        }

        // Starts the lens view.
        slyceLensView.getCameraControls().start();
    }

    private void initSlyceSession() {

        slyceSession = Slyce.getInstance(this).getDefaultSession();

        // In order to receive results, we must add ourselves as a listener on the session.
        slyceSession.addListener(new SlyceSessionListenerAdapter() {
            @Override
            public void onSessionWillStartSearchTask(@NonNull SlyceSession slyceSession, @NonNull SlyceSearchTask slyceSearchTask) {

                // In order to receive updates to the task after it is created, we must
                // assign a listener, which will funnel information through the below
                // SlyceSearchTaskListener (or SlyceSearchTaskListenerAdapter) methods.
                slyceSearchTask.addListener(slyceSearchTaskListenerAdapter);
            }

            @Override
            public void onSessionDidFinishSearchTask(@NonNull SlyceSession slyceSession, @NonNull SlyceSearchTask slyceSearchTask) {
                slyceSearchTask.removeListener(slyceSearchTaskListenerAdapter);
            }
        });
    }

    private boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED;
    }

    private void openSlyce() {
        Slyce.getInstance(this).open(SLYCE_ACCOUNT_ID, SLYCE_API_KEY, SLYCE_SPACE_ID, new SlyceCompletionHandler() {

            @Override
            public void onCompletion(@Nullable SlyceError slyceError) {
                if (slyceError == null) {

                    if (!isCameraPermissionGranted()) {

                        // If camera permission has not been granted, request it.
                        ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
                    } else {

                        // If camera permission has been granted, initialize the session & LensView.
                        initSlyceSession();
                        initSlyceLensView();
                    }

                } else {

                    // If there was an error opening the Slyce SDK, log the error.
                    Log.e(TAG, "onSlyceOpenFailed", slyceError);
                }
            }
        });
    }
}
