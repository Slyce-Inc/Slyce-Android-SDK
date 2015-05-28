package com.android.slyce;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import com.android.slyce.communication.ComManager;
import com.android.slyce.enums.SlyceRequestStage;
import com.android.slyce.handler.CameraSynchronizer;
import com.android.slyce.interfaces.SlyceCameraInterface;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.report.mpmetrics.MixpanelAPI;
import com.android.slyce.utils.BarcodeHelper;
import com.android.slyce.utils.BarcodeHelper.ScannerType;
import com.android.slyce.utils.Buzzer;
import com.android.slyce.utils.Constants;
import com.android.slyce.utils.Utils;
import com.android.slyce.barcode.BarcodeSession;
import com.android.slycesdk.R;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Result;
import com.moodstocks.android.Scanner;
import com.android.slyce.scanner.AutoScannerSession;
import com.android.slyce.scanner.AutoScannerSession.Listener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * Provides an integral camera functionality
 *
 * Set of methods that allow developers to perform continuous barcode detection, continuous 2D visual recognition (Premium feature), taking a snap, turn flash on/off, performing focus etc.
 */
public class SlyceCamera extends Handler implements SlyceCameraInterface {

    private final String TAG = SlyceCamera.class.getSimpleName();

    /* responsible for sending messages to host application */
    private CameraSynchronizer mCameraSynchronizer;

    /* MS */
    private AutoScannerSession session;

    private static final int TYPES = Result.Type.IMAGE | Result.Type.QRCODE | Result.Type.EAN13 | Result.Type.DATAMATRIX | Result.Type.EAN8 | Result.Type.QRCODE | Result.Type.NONE;

    /* Hosting Activity */
    private Activity mActivity;

    private boolean isContinuousRecognition = true;

    private Slyce mSlyce;

    /* Barcode/QR scanner engine */
    private BarcodeSession barcodeSession;

    /* Options Json from SlyceProductRequest */
    private JSONObject mOptions;

    private MixpanelAPI mixpanel;

    private List<SlyceProductsRequest> mSlyceProductsRequestMap;

    private SlyceProductsRequest mSlyceProductsRequest;

    private static final class SlyceCameraMessage{

        private static final int SEARCH = 0;
    }

    /**
     *
     * @param activity current Activity
     * @param slyce Slyce SDK instance. It must be opened.
     * @param preview the {@link SurfaceView} into which the camera preview will be displayed.
     * @param options use this JSONObject to pass properties to Slyce servers. Can be null.
     * @param listener the {@link OnSlyceCameraListener} to motify on results and errors.
     */
    public SlyceCamera(Activity activity, Slyce slyce, SurfaceView preview, JSONObject options, OnSlyceCameraListener listener){

        mSlyceProductsRequestMap = new ArrayList<SlyceProductsRequest>();

        mActivity = activity;

        mSlyce = slyce;

        mCameraSynchronizer = new CameraSynchronizer(listener);

        mOptions = options;

        mixpanel = MixpanelAPI.getInstance(mActivity, Constants.MIXPANEL_TOKEN);

        // If 2D Enabled -> MS automatic scanner
        // Else -> Barcode/QR engine scanner
        if(slyce.is2DSearchEnabled()){

            // Start MS detection
            try {
                session = new AutoScannerSession(activity, Scanner.get(), new AutoScannerSessionListener(), preview);
                session.setResultTypes(TYPES);
            } catch (MoodstocksError moodstocksError) {
                moodstocksError.printStackTrace();
            }

        }else{

            // Start Barcode/QR scanner
            barcodeSession = new BarcodeSession(activity, preview, new BarcodeSessionListener());
        }

        // Detect touch point on camera preview
        if (preview != null) {
            preview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {

                        float x = event.getX();
                        float y = event.getY();

                        mCameraSynchronizer.onTap(x, y);

                        focusAreas(true, x, y);

                        return true;
                    }
                    return false;
                }
            });
        }
    }

    /* Interface methods for host application */
    /**
     * Perform an image recognition for the current frame.
     */
    @Override
    public void snap() {
        // If 2D Enabled snap via MS
        // Else snap  via Barcode/QR engine
        if(session != null){
            // Snap via MS
            session.snap();
        }else{
            // Snap via Barcode/QR engine
            barcodeSession.snap();
        }
    }

    /**
     * Starts the camera preview and starts processing the frames.
     */
    @Override
    public void start() {
        if(session != null){
            session.start();
        }

        if (barcodeSession != null){
            barcodeSession.start();
        }
    }

    /**
     * Stops processing the frames and stops camera preview.
     */
    @Override
    public void stop() {
        if(session != null){
            session.stop();
        }

        if(barcodeSession != null){
            barcodeSession.stop();
        }
    }

    /**
     * Stops processing the frames, keeping the camera preview alive.
     */
    @Override
    public void pause(){
        if(session != null){
            session.pause();
        }

        if(barcodeSession != null){
            barcodeSession.pause();
        }
    }

    /**
     * Resumes processing the frames after a call to pause().
     */
    @Override
    public void resume(){
        if(session != null){
            session.resume();
        }

        if(barcodeSession != null){
            barcodeSession.resume();
        }
    }

    /**
     * Canceling all requests triggered by calling to {@link SlyceCameraInterface#snap()}
     */
    @Override
    public void cancel(){
        for(SlyceProductsRequest request : mSlyceProductsRequestMap){
            request.cancel();
        }
    }

    /**
     * Asks the scanner to focus the underlying camera to a specific point.
     *
     * @param x
     * @param y
     */
    @Override
    public void focusAtPoint(float x, float y) {
        focusAreas(true, x, y);
    }

    /**
     * Enable/Disable continuous recognition functionality.
     * Setting this value to false will stop recognizing barcodes in a Regular mode and will stop recognizing 2D products and barcodes in Premium mode. Default is true.
     *
     * @param value boolean
     */
    @Override
    public void setContinuousRecognition(boolean value) {
        isContinuousRecognition = value;
    }

    /**
     * Turns on/off the device flash.
     */
    @Override
    public void turnFlash() {
        boolean flashNewState = false;

        if(barcodeSession != null){
            flashNewState = barcodeSession.turnFlash();
        }

        if(session != null){
            flashNewState = session.turnFlash();
        }

        if(flashNewState){
            mixpanel.track(Constants.FLASH_TURNED_ON, null);
        }else{
            mixpanel.track(Constants.FLASH_TURNED_OFF, null);
        }
    }
    /* */

    /** {@link BarcodeSession.OnBarcodeListener} */
    private class BarcodeSessionListener implements BarcodeSession.OnBarcodeListener{

        @Override
        public void onBarcodeResult(int type, String result) {
            Log.i(TAG, "onBarcodeResult");

            // Resume the automatic scan after 2 seconds
            new  Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    barcodeSession.resume();
                }
            }, Constants.AUTO_SCAN_DELAY);

            if(isContinuousRecognition){
                // Handle barcode detection
                handleBarcodeResult(type, result, ScannerType._3D);
            }
        }

        @Override
        public void onBarcodeSnap(Bitmap bitmap) {
            Log.i(TAG, "onBarcodeSnap");

            handleSnap(bitmap);
        }
    }
    /* */

    /** {@link AutoScannerSession.Listener} */
    private class AutoScannerSessionListener implements Listener{

        @Override
        public void onCameraOpenFailed(Exception e) {
            Log.i(TAG, "onCameraOpenFailed");
        }

        @Override
        public void onResult(Result result) {

            // Resume the automatic scan after 3 seconds
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    session.resume();
                }
            }, Constants.AUTO_SCAN_DELAY);

            if(isContinuousRecognition){

                String value = result.getValue();

                int type = result.getType();

                if(type == Result.Type.IMAGE){
                    // Image detection

                    try {
                        JSONObject imageDetectReport = new JSONObject();
                        imageDetectReport.put(Constants.DETECTION_TYPE, Constants._2D);
                        imageDetectReport.put(Constants.DATA_IRID, value);
                        mixpanel = MixpanelAPI.getInstance(mActivity, Constants.MIXPANEL_TOKEN);
                        mixpanel.track(Constants.IMAGE_DETECTED, imageDetectReport);
                    } catch (JSONException e) {}

                    // Notify the host application for basic result
                    mCameraSynchronizer.on2DRecognition(value, Utils.decodeBase64(value));

                    // Get extended products results
                    ComManager.getInstance().getIRIDInfo(mSlyce.getClientID(), value, new ComManager.OnExtendedInfoListener() {
                        @Override
                        public void onExtendedInfo(JSONArray products) {

                            // Notify the host application for extended result
                            mCameraSynchronizer.on2DExtendedRecognition(products);
                        }
                    });

                }else{
                    // Handle barcode detection
                    handleBarcodeResult(type, value, ScannerType._2D);
                }

            }else{
                // Do Nothing
            }
        }

        @Override
        public void onWarning(String s) {
            Log.i(TAG, "onWarning: " + s);
        }

        @Override
        public void onSnap(Bitmap bitmap) {
            Log.i(TAG, "onSnap");

            handleSnap(bitmap);
        }

    }
    /* */

    /* Private methods */
    private void handleSnap(Bitmap bitmap){

        // Notify the host application on snaped image
        mCameraSynchronizer.onSnap(bitmap);

        // Start search Slyce + MoodStock (if 2D enabled)
        obtainMessage(SlyceCameraMessage.SEARCH, bitmap).sendToTarget();

        // Report to MP on image snapped
        mixpanel.track(Constants.IMAGE_SNAPPED, null);
    }

    private void handleBarcodeResult(int type, String result, ScannerType scannerType){

        // Create a SlyceBarcode object
        SlyceBarcode slyceBarcode = BarcodeHelper.createSlyceBarcode(type, scannerType, result);

        // Notify the host applicatin for barcode detection
        notifyOnBarcodeRecognition(slyceBarcode);

        try {
            JSONObject imageDetectReport = new JSONObject();
            imageDetectReport.put(Constants.DETECTION_TYPE, slyceBarcode.getTypeString());
            imageDetectReport.put(Constants.DATA_BARCODE, slyceBarcode.getBarcode());
            mixpanel = MixpanelAPI.getInstance(mActivity, Constants.MIXPANEL_TOKEN);
            mixpanel.track(Constants.BARCODE_DETECTED, imageDetectReport);
        } catch (JSONException e){}
    }

    private void focusAreas(boolean focusAtPoint, float x, float y){

        // Create the rect of focus area 100px around the center (x,y)
        Rect touchRect = new Rect(
                (int)(x - Constants.FOCUS_SQUARE_AREA),
                (int)(y - Constants.FOCUS_SQUARE_AREA),
                (int)(x + Constants.FOCUS_SQUARE_AREA),
                (int)(y + Constants.FOCUS_SQUARE_AREA));

        if(session != null){
            session.requestFocus(focusAtPoint, touchRect);
        }

        if(barcodeSession != null){
            barcodeSession.requestFocus(focusAtPoint, touchRect);
        }
    }

    private void notifyOnBarcodeRecognition(SlyceBarcode slyceBarcode){

        // Play sound/vibrate
        Buzzer.getInstance().buzz(mActivity, R.raw.slyce_detection_sound, mSlyce.isSoundOn(), mSlyce.isVibrateOn());

        // Notify the host application
        mCameraSynchronizer.onBarcodeRecognition(slyceBarcode);
    }
    /* */

    @Override
    public void handleMessage(final Message msg) {

        switch(msg.what){

            case SlyceCameraMessage.SEARCH:

                final Bitmap bitmap = (Bitmap) msg.obj;

                // Slyce + 2D search
                mSlyceProductsRequest = new SlyceProductsRequest(mSlyce, new OnSlyceRequestListener() {
                    @Override
                    public void onBarcodeRecognition(SlyceBarcode barcode) {
                        notifyOnBarcodeRecognition(barcode);
                    }

                    @Override
                    public void onSlyceProgress(long progress, String message, String id) {
                        mCameraSynchronizer.onSlyceProgress(progress, message, id);
                    }

                    @Override
                    public void on2DRecognition(String irid, String productInfo) {
                        if(!TextUtils.isEmpty(irid)){
                            // Play sound/vibrate only on detection
                            Buzzer.getInstance().buzz(mActivity, R.raw.slyce_detection_sound, mSlyce.isSoundOn(), mSlyce.isVibrateOn());
                        }

                        mCameraSynchronizer.on2DRecognition(irid, productInfo);
                    }

                    @Override
                    public void on2DExtendedRecognition(JSONArray products) {
                        mCameraSynchronizer.on2DExtendedRecognition(products);
                    }

                    @Override
                    public void on3DRecognition(JSONArray products) {
                        mCameraSynchronizer.on3DRecognition(products);
                    }

                    @Override
                    public void onSlyceRequestStage(SlyceRequestStage message) {
                        mCameraSynchronizer.onSlyceRequestStage(message);
                    }

                    @Override
                    public void onError(String message) {
                        mCameraSynchronizer.onError(message);
                    }

                }, (Bitmap) msg.obj, mOptions);

                mSlyceProductsRequestMap.add(mSlyceProductsRequest);

                mSlyceProductsRequest.execute();

                break;

            default:
                break;
        }
    }
}
