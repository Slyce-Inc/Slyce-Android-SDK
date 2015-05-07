package com.android.slyce.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import com.android.slyce.Slyce;
import com.android.slyce.communication.ComManager;
import com.android.slyce.handler.CameraSynchronizer;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.SlyceBarcode;
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
            barcodeSession = new BarcodeSession(activity, preview, this);
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
//            barcodeManager.pause();
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

        if(barcodeSession != null){
            barcodeSession.turnFlash();
        }

        if(session != null){
            session.turnFlash();
        }
    }

    public void focuseAtPoint(){

        if(session != null){

        }

        if(barcodeSession != null){

        }
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

            // Create SlyceBarcode object based on type {SlyceBarcodeType} nad {ScannerType}
            SlyceBarcode slyceBarcode = BarcodeHelper.createSlyceBarcode(type, ScannerType._3D, result);

            mCameraSynchronizer.onBarcodeRecognition(slyceBarcode);
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

        String irId = result.getValue();

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

            }else{
                // Barcode detection

                // Create a SlyceBarcode object
                SlyceBarcode barcode = BarcodeHelper.createSlyceBarcode(type, ScannerType._2D, irId);

                // Notify the host applicatin for barcode detection
                mCameraSynchronizer.onBarcodeRecognition(barcode);
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
