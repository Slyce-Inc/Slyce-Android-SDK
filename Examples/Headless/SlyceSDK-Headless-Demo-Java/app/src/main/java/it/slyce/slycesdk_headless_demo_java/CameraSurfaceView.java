package it.slyce.slycesdk_headless_demo_java;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.gson.GsonBuilder;

import java.util.List;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.SlyceScanner;
import it.slyce.sdk.SlyceSearchResponse;
import it.slyce.sdk.SlyceSearchResponseUpdate;
import it.slyce.sdk.SlyceSearchTask;
import it.slyce.sdk.SlyceSearchTaskListenerAdapter;
import it.slyce.sdk.SlyceSession;
import it.slyce.sdk.SlyceSessionListenerAdapter;
import it.slyce.sdk.exception.SlyceError;
import it.slyce.sdk.exception.initialization.SlyceMissingGDPRComplianceException;
import it.slyce.sdk.exception.initialization.SlyceNotOpenedException;

import static android.content.Context.WINDOW_SERVICE;
import static android.content.pm.PackageManager.FEATURE_CAMERA_AUTOFOCUS;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = CameraSurfaceView.class.getSimpleName();

    private Camera camera;
    private CameraResultListener listener;
    private SlyceScanner slyceScanner;
    private SlyceSession slyceSession;
    private String lensId;
    private SurfaceHolder surfaceHolder;

    // These methods are used to handle the results and updates from Slyce after starting a search.
    // Here, we only display the raw data in the text view, however, in production,  a view would
    // be rendered to showcase the output.

    private SlyceSearchTaskListenerAdapter slyceSearchTaskListenerAdapter = new SlyceSearchTaskListenerAdapter() {

        @Override
        public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask task, SlyceSearchResponseUpdate.JobCreated update) {
            if (listener != null) {
                listener.onSearchReceivedUpdate("Job Created", (update != null ? new GsonBuilder().setPrettyPrinting().create().toJson(update) : "null"));
            }
        }

        @Override
        public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask task, SlyceSearchResponseUpdate.TagFound update) {
            if (listener != null) {
                listener.onSearchReceivedUpdate("Tag Found", (update != null ? new GsonBuilder().setPrettyPrinting().create().toJson(update) : "null"));
            }
        }

        @Override
        public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask task, SlyceSearchResponseUpdate.ResultsReceived update) {
            if (listener != null) {
                listener.onSearchReceivedUpdate("Results Received", (update != null ? new GsonBuilder().setPrettyPrinting().create().toJson(update) : "null"));
            }
        }

        @Override
        public void onSlyceSearchTaskFinished(SlyceSearchTask task, SlyceSearchResponse response) {
            if (listener != null) {
                listener.onSearchReceivedUpdate("Finished", (response != null ? new GsonBuilder().setPrettyPrinting().create().toJson(response) : "null"));
                listener.onSearchCompleted();
            }
        }

        @Override
        public void onSlyceSearchTaskFailed(SlyceSearchTask task, List<SlyceError> errors) {
            if (listener != null) {
                listener.onSearchReceivedUpdate("Failed", (errors != null ? new GsonBuilder().setPrettyPrinting().create().toJson(errors) : "null"));
                listener.onSearchCompleted();
            }
        }
    };

    /**
     * Constructor.
     *
     * @param context
     */
    public CameraSurfaceView(Context context) {
        super(context);
        setupView();
    }

    /**
     * Constructor.
     *
     * @param context
     * @param attrs
     */
    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupView();
    }

    /**
     * Constructor.
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupView();
    }

    /**
     * Constructor.
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     * @param defStyleRes
     */
    @TargetApi(LOLLIPOP)
    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setupView();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startCameraPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCameraPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        stopCameraPreview();
        startCameraPreview();
    }

    private void setupView() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        // In order to receive updates from the SlyceScanner after attaching it to an active
        // camera, we must add a listener to the session which was used to create the SlyceScanner.
        // In this example, the SlyceScanner is created when the camera preview is started.
        // However, we can attach a SlyceSessionListener (or SlyceSessionListenerAdapter) to the
        // session at any point after getting a valid session to receive notifications of any
        // SlyceSearchTasks that are created.

        try {
            slyceSession = Slyce.getInstance(getContext()).createSession();
        } catch (SlyceMissingGDPRComplianceException e) {
            e.printStackTrace();
        } catch (SlyceNotOpenedException e) {
            e.printStackTrace();
        }
        slyceSession.addListener(new SlyceSessionListenerAdapter() {

            @Override
            public void onSessionWillStartSearchTask(@NonNull SlyceSession session, @NonNull SlyceSearchTask searchTask) {
                searchTask.addListener(slyceSearchTaskListenerAdapter);
                listener.onSearchStarted();
            }
        });
    }

    public void init(String lensId, CameraResultListener listener) {
        this.lensId = lensId;
        this.listener = listener;
    }

    public void startCameraPreview() {
        try {
            camera = Camera.open(CAMERA_FACING_BACK);
            camera.setDisplayOrientation(getDisplayOrientationForCamera(getContext()));
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

            // Once the device camera is completely set up, AND Slyce has been opened (which was
            // completed on the MainActivity for this example) we can create a SlyceScanner using
            // the camera instance, a session and the lens ID.

            slyceScanner = slyceSession.createScanner(getContext(), camera, lensId);

            // After a SlyceScanner is created, AND the camera preview has been started, we can resume
            // the SlyceScanner.

            slyceScanner.resume();

            enableAutoFocus();

        } catch (Exception e) {
            Log.d(TAG, "startCameraPreview: " + e);
        }
    }

    public void stopCameraPreview() {
        try {

            // Before the camera is stopped, we should suspend any SlyceScanner instance to prevent
            // it from trying to access an inactive instance of the camera it has bound to.

            slyceScanner.suspend();

            if (camera != null) {
                camera.stopPreview();
                camera = null;
            }
        } catch (Exception e) {
            Log.d(TAG, "stopCameraPreview: " + e.getMessage());
        }
    }

    // CAMERA HELPER METHODS

    private void enableAutoFocus() {
        if (camera != null && getContext().getPackageManager().hasSystemFeature(FEATURE_CAMERA_AUTOFOCUS)) {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters != null) {
                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes != null) {
                    for (int i = 0; i < focusModes.size(); i++) {
                        String focusMode = focusModes.get(i);
                        if ((focusMode != null) && focusMode.equals(FOCUS_MODE_AUTO)) {
                            parameters.setFocusMode(focusMode);
                            camera.setParameters(parameters);
                            camera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    try {
                                        camera.cancelAutoFocus();
                                        camera.autoFocus(this);
                                    } catch (Exception e) {
                                        Log.d(TAG, "onAutoFocus: " + e.getMessage());
                                    }
                                }
                            });
                            return;
                        }
                    }
                }
            }
        }
    }

    private int getDeviceOrientationDegrees(Context context) {
        int degrees = 0;

        WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        return degrees;
    }

    private int getDisplayOrientationForCamera(Context context) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(CAMERA_FACING_BACK, cameraInfo);
        return (cameraInfo.orientation - getDeviceOrientationDegrees(context) + 360) % 360;
    }
}
