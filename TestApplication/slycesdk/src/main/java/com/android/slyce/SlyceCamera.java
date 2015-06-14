package com.android.slyce;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
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
import com.android.slyce.utils.SlyceLog;
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

    private Slyce mSlyce;

    /* Barcode/QR scanner engine */
    private BarcodeSession barcodeSession;

    /* Options Json from SlyceProductRequest */
    private JSONObject mOptions;

    private MixpanelAPI mixpanel;

    private List<SlyceProductsRequest> mSlyceProductsRequestMap;

    private SlyceProductsRequest mSlyceProductsRequest;

    private boolean mShouldPauseScanner = true;

    private boolean mContinuousRecognition = true;

    private static final class SlyceCameraMessage{
        private static final int SNAP_SEARCH = 001;
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

        if(slyce == null){
            SlyceLog.e(TAG, Constants.SLYCE_OBJECT_IS_NULL);
            return;
        }

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
            session.disableDetection();
            session.snap();
        }else{
            // Snap via Barcode/QR engine
            barcodeSession.disableDetection();
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
        mContinuousRecognition = value;

        if(session != null){
            if(value){
                session.enableDetection();
            }else{
                session.disableDetection();
            }
        }

        if(barcodeSession != null){
            if(value){
                barcodeSession.enableDetection();
            }else{
                barcodeSession.disableDetection();
            }
        }
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

    /**
     * If true then after 2D automatic detection the scanner will resume scanning after 2 seconds
     */
    @Override
    public void shouldPauseScanner(boolean value) {
        mShouldPauseScanner = value;
    }
    /* */

    /** {@link BarcodeSession.OnBarcodeListener} */
    private class BarcodeSessionListener implements BarcodeSession.OnBarcodeListener{

        @Override
        public void onBarcodeResult(int type, String result) {
            SlyceLog.i(TAG, "onBarcodeResult");

            if(!mShouldPauseScanner && mContinuousRecognition){

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        barcodeSession.enableDetection();
                    }
                },Constants.AUTOMATIC_SCANNER_DELAY);
            }

            // Handle barcode detection
            handleBarcodeResult(type, result, ScannerType._3D);
        }

        @Override
        public void onBarcodeSnap(Bitmap bitmap) {
            SlyceLog.i(TAG, "onBarcodeSnap");

            handleSnap(bitmap);
        }
    }
    /* */

    /** {@link AutoScannerSession.Listener} */
    private class AutoScannerSessionListener implements Listener{

        @Override
        public void onCameraOpenFailed(Exception e) {
            SlyceLog.i(TAG, "2D onCameraOpenFailed");
        }

        @Override
        public void onResult(Result result) {
            SlyceLog.i(TAG, "onResult");

            String value = result.getValue();
            int type = result.getType();

            if(!mShouldPauseScanner && mContinuousRecognition){

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        session.enableDetection();
                    }
                },Constants.AUTOMATIC_SCANNER_DELAY);
            }

            if(type == Result.Type.IMAGE){
                // Image detection

                handleImageResult(value);

            }else{ // Barcode detection

                // Handle barcode
                handleBarcodeResult(type, value, ScannerType._2D);
            }
        }

        @Override
        public void onWarning(String s) {
            SlyceLog.i(TAG, "2D onWarning: " + s);
        }

        @Override
        public void onSnap(Bitmap bitmap) {
            SlyceLog.i(TAG, "2D onSnap");

            handleSnap(bitmap);
        }
    }
    /* */

    /* Private methods */
    private void handleSnap(Bitmap bitmap){

        // Notify the host application on snapped image
        mCameraSynchronizer.onSnap(bitmap);

        // Start SlyceProductsRequest (Slyce + MoodStock)
        obtainMessage(SlyceCameraMessage.SNAP_SEARCH, bitmap).sendToTarget();

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

    private void handleImageResult(String value){

        // Check whether we need to abort current detection
        if(shouldAbortDetection(value)){
            return;
        }

        try {
            JSONObject imageDetectReport = new JSONObject();
            imageDetectReport.put(Constants.DETECTION_TYPE, Constants._2D);
            imageDetectReport.put(Constants.DATA_IRID, value);
            mixpanel = MixpanelAPI.getInstance(mActivity, Constants.MIXPANEL_TOKEN);
            mixpanel.track(Constants.IMAGE_DETECTED, imageDetectReport);
        } catch (JSONException e) {}

        if(!TextUtils.isEmpty(value)){
            // Play sound/vibrate only on detection
            Buzzer.getInstance().buzz(mActivity, R.raw.slyce_detection_sound, mSlyce.isSoundOn(), mSlyce.isVibrateOn());
        }

        // Notify the host application for basic result
        mCameraSynchronizer.onCameraImageDetected(Utils.decodeBase64(value));

        // Get extended products results
        ComManager.getInstance().getProductsFromIRID(value, new ComManager.OnExtendedInfoListener() {
            @Override
            public void onExtendedInfo(JSONArray products) {

                // Notify the host application for extended result
                mCameraSynchronizer.onCameraImageInfoReceived(products);
            }

            @Override
            public void onExtenedInfoError() {
                mCameraSynchronizer.onError(Constants.NO_PRODUCTS_FOUND);
            }
        });

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
        mCameraSynchronizer.onCameraBarcodeDetected(slyceBarcode);
    }
    /* */

    @Override
    public void handleMessage(final Message msg) {

        switch(msg.what){

            case SlyceCameraMessage.SNAP_SEARCH:

                mSlyceProductsRequest = new SlyceProductsRequest(mSlyce, new OnSlyceRequestListener() {
                    @Override
                    public void onBarcodeDetected(SlyceBarcode barcode) {
                        notifyOnBarcodeRecognition(barcode);
                    }

                    @Override
                    public void onSlyceProgress(long progress, String message, String id) {
                        mCameraSynchronizer.onSlyceProgress(progress, message, id);
                    }

                    @Override
                    public void onImageDetected(String productInfo) {
                        if(!TextUtils.isEmpty(productInfo)){
                            // Play sound/vibrate only on detection
                            Buzzer.getInstance().buzz(mActivity, R.raw.slyce_detection_sound, mSlyce.isSoundOn(), mSlyce.isVibrateOn());
                        }

                        mCameraSynchronizer.onCameraImageDetected(productInfo);
                    }

                    @Override
                    public void onImageInfoReceived(JSONArray products) {
                        mCameraSynchronizer.onCameraImageInfoReceived(products);
                    }

                    @Override
                    public void onResultsReceived(JSONObject products) {
                        mCameraSynchronizer.onCameraResultsReceived(products);
                    }

                    @Override
                    public void onSlyceRequestStage(SlyceRequestStage message) {
                        mCameraSynchronizer.onSlyceRequestStage(message);
                    }

                    @Override
                    public void onError(String message) {
                        mCameraSynchronizer.onError(message);
                    }

                    @Override
                    public void onFinished(){

                    }

                }, (Bitmap) msg.obj, mOptions);

                mSlyceProductsRequestMap.add(mSlyceProductsRequest);

                mSlyceProductsRequest.execute();

                break;

            default:
                break;
        }
    }

    private String lastDetectedIrId = null;
    private long lastDetectedTimeStamp = 0;

    /**
     * Abort image detection under two conditions:
     * 1. the same image detected
     * 2. time from last detection is less then {@link Constants#BYPASS_IDENTICAL_DETECTION_DELAY}
     */
    private boolean shouldAbortDetection(String irid){

        if(!TextUtils.equals(irid, lastDetectedIrId)){

            lastDetectedTimeStamp = System.currentTimeMillis();
            lastDetectedIrId = irid;

            // Should Send result
            return false;

        }else{

            long timeDif = System.currentTimeMillis() - lastDetectedTimeStamp;

            if(timeDif < Constants.BYPASS_IDENTICAL_DETECTION_DELAY){

                // Abort detection
                SlyceLog.i(TAG, "Abort 2D detection for two identical images, time difference = " + timeDif);
                return true;

            }else{

                lastDetectedTimeStamp = System.currentTimeMillis();
                lastDetectedIrId = irid;

                // Should Send result
                return false;
            }
        }
    }
}
