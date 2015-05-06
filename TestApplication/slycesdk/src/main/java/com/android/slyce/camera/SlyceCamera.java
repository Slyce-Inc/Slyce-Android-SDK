package com.android.slyce.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import com.android.slyce.Slyce;
import com.android.slyce.communication.ComManager;
import com.android.slyce.enums.SlyceBarcodeType;
import com.android.slyce.handler.CameraSynchronizer;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.SlyceBarcode;
import com.android.slyce.requests.SlyceProductsRequest;
import com.android.slyce.utils.Constants;
import com.android.slyce.utils.Utils;
import com.android.slyce.zbar.BarcodeManager;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Result;
import com.moodstocks.android.Scanner;
import com.android.slyce.moodstocks.AutoScannerSession;
import com.android.slyce.moodstocks.AutoScannerSession.Listener;
import org.json.JSONArray;

public class SlyceCamera extends Handler implements Listener, BarcodeManager.OnBarcodeListener{

    private final String TAG = SlyceCamera.class.getSimpleName();

    /* responsible for sending messages to host application */
    private CameraSynchronizer mCameraSynchronizer;

    /* MS */
    private AutoScannerSession session;

    private static final int TYPES = Result.Type.IMAGE | Result.Type.QRCODE | Result.Type.EAN13;

    /* Hosting Activity */
    private Activity mActivity;

    /* Client ID*/
    private String mClientId;

    private boolean isContinuousRecognition = true;

    private Slyce mSlyce;

    /* Barcode/QR scanner engine */
    private BarcodeManager barcodeManager;

    private static final class SlyceCameraMessage{

        private static final int SEARCH = 0;
    }

    public SlyceCamera(Activity activity, Slyce slyce, SurfaceView preview, OnSlyceCameraListener listener){

        mActivity = activity;

        mClientId = slyce.getClientID();

        mCameraSynchronizer = new CameraSynchronizer(listener);

        mSlyce = slyce;

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
            barcodeManager = new BarcodeManager(activity, preview, this);
        }
    }

    public void setContinuousRecognition(boolean value){
        isContinuousRecognition = value;
    }

    public void start() {

        if(session != null){
            session.start();
        }

        if(barcodeManager != null){
            barcodeManager.start();
        }
    }

    public void stop() {

        if(session != null){
            session.stop();
        }

        if(barcodeManager != null){
//            barcodeManager.pause();
            barcodeManager.stop();
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
            barcodeManager.snap();
        }
    }

    public void turnFlash(){

        if(barcodeManager != null){
            barcodeManager.turnFlash();
        }

        if(session != null){
            session.turnFlash();
        }
    }

    public void focuseAtPoint(){

        if(session != null){

        }

        if(barcodeManager != null){

        }
    }

    /* Barcode engine listener */
    @Override
    public void onBarcodeResult(String result) {
        Log.i(TAG, "onBarcodeResult");

        // Resume the automatic scan after 2 seconds
        new  Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                barcodeManager.resume();

            }
        }, Constants.AUTO_SCAN_DELAY);

        if(isContinuousRecognition){
            // TODO: Create SlyceBarcode based on barcode scanner type and result
            mCameraSynchronizer.onBarcodeRecognition(new SlyceBarcode(SlyceBarcodeType.EAN_13, result));
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

        // result.getType() == Result.Type.IMAGE ? "Image:" : "Barcode:"

        String irId = result.getValue();

        // Resume the automatic scan after 2 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                session.resume();
            }
        }, Constants.AUTO_SCAN_DELAY);

        if(isContinuousRecognition){

            // Notify the host application for basic result
            mCameraSynchronizer.on2DRecognition(irId, Utils.decodeBase64(irId));

            // Get extended products results
            ComManager.getInstance().getIRIDInfo(mClientId, irId, new ComManager.OnExtendedInfoListener() {
                @Override
                public void onExtendedInfo(JSONArray products) {

                    // Notify the host application for extended result
                    mCameraSynchronizer.on2DExtendedRecognition(products);
                }
            });
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

        // Start search Slyce + MoodStock (if 2D enabled)
        obtainMessage(SlyceCameraMessage.SEARCH, bitmap).sendToTarget();
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

                }, (Bitmap) msg.obj);

                request.execute();

                break;

            default:
                break;
        }
    }
}
