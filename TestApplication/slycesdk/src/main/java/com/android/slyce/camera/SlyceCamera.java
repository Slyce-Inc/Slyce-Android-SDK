package com.android.slyce.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import com.android.slyce.Slyce;
import com.android.slyce.communication.ComManager;
import com.android.slyce.handler.CameraSynchronizer;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.SlyceBarcode;
import com.android.slyce.report.mpmetrics.MixpanelAPI;
import com.android.slyce.requests.SlyceProductsRequest;
import com.android.slyce.utils.BarcodeHelper;
import com.android.slyce.utils.BarcodeHelper.ScannerType;
import com.android.slyce.utils.Constants;
import com.android.slyce.utils.Utils;
import com.android.slyce.zbar.BarcodeSession;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Result;
import com.moodstocks.android.Scanner;
import com.android.slyce.moodstocks.AutoScannerSession;
import com.android.slyce.moodstocks.AutoScannerSession.Listener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SlyceCamera extends Handler implements Listener, BarcodeSession.OnBarcodeListener{

    private final String TAG = SlyceCamera.class.getSimpleName();

    /* responsible for sending messages to host application */
    private CameraSynchronizer mCameraSynchronizer;

    /* MS */
    private AutoScannerSession session;

    private static final int TYPES = Result.Type.IMAGE | Result.Type.QRCODE | Result.Type.EAN13 | Result.Type.DATAMATRIX | Result.Type.EAN8 | Result.Type.QRCODE | Result.Type.NONE;

    /* Hosting Activity */
    private Activity mActivity;

    /* Client ID*/
    private String mClientId;

    private boolean isContinuousRecognition = true;

    private Slyce mSlyce;

    /* Barcode/QR scanner engine */
    private BarcodeSession barcodeSession;

    /* Options Json from SlyceProductRequest */
    private JSONObject mOptions;

    private MixpanelAPI mixpanel;

    private static final class SlyceCameraMessage{

        private static final int SEARCH = 0;
    }

    public SlyceCamera(Activity activity, Slyce slyce, SurfaceView preview, JSONObject options, OnSlyceCameraListener listener){

        mActivity = activity;

        mClientId = slyce.getClientID();

        mCameraSynchronizer = new CameraSynchronizer(listener);

        mSlyce = slyce;

        mOptions = options;

        mixpanel = MixpanelAPI.getInstance(mActivity, Constants.MIXPANEL_TOKEN);

        // If 2D Enabled -> MS automatic scanner
        // Else -> Barcode/QR engine scanner
        if(slyce.is2DSearchEnabled()){

            // Start MS detection
            try {
                session = new AutoScannerSession(activity, Scanner.get(), this, preview);
                session.setResultTypes(TYPES);
            } catch (MoodstocksError moodstocksError) {
                moodstocksError.printStackTrace();
            }

        }else{

            // Start Barcode/QR scanner
            barcodeSession = new BarcodeSession(activity, preview, this);
        }

        // Detect touch point on camera preview
        if(preview != null){
            preview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {

                        float x = event.getX();
                        float y =  event.getY();

                        mCameraSynchronizer.onTap(x, y);

                        focusAreas(true, x, y);

                        return true;
                    }
                    return false;
                }
            });
        }
    }

    public void setContinuousRecognition(boolean value){
        isContinuousRecognition = value;
    }

    public void start() {

        if(session != null){
            session.start();
        }

        if(barcodeSession != null){
            barcodeSession.start();
        }
    }

    public void stop() {

        if(session != null){
            session.stop();
        }

        if(barcodeSession != null){
            barcodeSession.stop();
        }
    }

    public void snap(){

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

    public void turnFlash(){

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

    public void focusAtPoint(float x, float y){
        focusAreas(true, x, y);
    }

    /* Barcode engine listener */
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
    /* */

    /* Listener */
    @Override
    public void onCameraOpenFailed(Exception e) {
        Log.i(TAG, "onCameraOpenFailed");
    }

    @Override
    public void onResult(Result result) {

        String value = result.getValue();

        // Resume the automatic scan after 2 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                session.resume();
            }
        }, Constants.AUTO_SCAN_DELAY);

        if(isContinuousRecognition){

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
                ComManager.getInstance().getIRIDInfo(mClientId, value, new ComManager.OnExtendedInfoListener() {
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
        }
    }

    @Override
    public void onWarning(String s) {
        Log.i(TAG, "onWarning");
    }

    @Override
    public void onSnap(Bitmap bitmap) {
        Log.i(TAG, "onSnap");

        handleSnap(bitmap);
    }
    /* */

    private void handleSnap(Bitmap bitmap){

        // Notify the host application on the taken bitmap
        mCameraSynchronizer.onSnap(bitmap);

        // Report to MP on image snapped
        mixpanel.track(Constants.IMAGE_SNAPPED, null);

        // Start search Slyce + MoodStock (if 2D enabled)
        obtainMessage(SlyceCameraMessage.SEARCH, bitmap).sendToTarget();
    }

    private void handleBarcodeResult(int type, String result, ScannerType scannerType){

        // Create a SlyceBarcode object
        SlyceBarcode slyceBarcode = BarcodeHelper.createSlyceBarcode(type, scannerType, result);

        // Notify the host applicatin for barcode detection
        mCameraSynchronizer.onBarcodeRecognition(slyceBarcode);

        try {
            JSONObject imageDetectReport = new JSONObject();
            imageDetectReport.put(Constants.DETECTION_TYPE, slyceBarcode.getTypeString());
            imageDetectReport.put(Constants.DATA_BARCODE, slyceBarcode.getBarcode());
            mixpanel = MixpanelAPI.getInstance(mActivity, Constants.MIXPANEL_TOKEN);
            mixpanel.track(Constants.BARCODE_DETECTED, imageDetectReport);
        } catch (JSONException e){}
    }

    @Override
    public void handleMessage(Message msg) {

        switch(msg.what){

            case SlyceCameraMessage.SEARCH:

                // Slyce + 2D search
                SlyceProductsRequest request = new SlyceProductsRequest(mSlyce, new OnSlyceRequestListener() {
                    @Override
                    public void onBarcodeRecognition(SlyceBarcode barcode) {
                        mCameraSynchronizer.onBarcodeRecognition(barcode);
                    }

                    @Override
                    public void onSlyceProgress(long progress, String message, String id) {
                        mCameraSynchronizer.onSlyceProgress(progress, message, id);
                    }

                    @Override
                    public void on2DRecognition(String irid, String productInfo) {
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
                    public void onStageLevelFinish(StageMessage message) {
                        mCameraSynchronizer.onStageLevelFinish(message);
                    }

                    @Override
                    public void onError(String message) {
                        mCameraSynchronizer.onError(message);
                    }

                }, (Bitmap) msg.obj, mOptions);

                request.execute();

                break;

            default:
                break;
        }
    }
}
