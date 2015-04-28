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
import com.android.slyce.requests.SlyceProductsRequest;
import com.android.slyce.utils.Utils;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Result;
import com.moodstocks.android.Scanner;
import com.android.slyce.moodstocks.AutoScannerSession;
import com.android.slyce.moodstocks.AutoScannerSession.Listener;

import org.json.JSONArray;

public class SlyceCamera extends Handler implements Listener{

    private final String TAG = SlyceCamera.class.getSimpleName();

    /* responsible for sending messages to host application */
    private CameraSynchronizer mCameraSynchronizer;

    /* MoodStocks */
    private AutoScannerSession session;

    private static final int TYPES = Result.Type.IMAGE | Result.Type.QRCODE | Result.Type.EAN13;

    /* Hosting Activity */
    private Activity mActivity;

    /* Client ID*/
    private String mClientId;

    private boolean isContinuousRecognition = true;

    private Slyce mSlyce;

    private static final class SlyceCameraMessage{

        private static final int SEARCH_2D  = 0;
    }

    public SlyceCamera(Activity activity, Slyce slyce, SurfaceView preview, OnSlyceCameraListener listener){

        mActivity = activity;

        mClientId = slyce.getClientID();

        mCameraSynchronizer = new CameraSynchronizer(listener);

        mSlyce = slyce;

        // If 2D Enabled -> MoodStocks automatic scanner
        // Else -> Barcode/QR engine scanner
        if(slyce.is2DSearchEnabled()){

            try {
                session = new AutoScannerSession(activity, Scanner.get(), this, preview);
                session.setResultTypes(TYPES);
            } catch (MoodstocksError moodstocksError) {
                moodstocksError.printStackTrace();
            }

        }else{
            // TOOD: start Barcode/QR scanner
        }
    }

    public void setContinuousRecognition(boolean value){
        isContinuousRecognition = value;
    }

    public void start() {

        if(session != null){
            session.start();
        }
    }

    public void stop() {

        if(session != null){
            session.stop();
        }
    }

    public void snap(){

        // If 2D Enabled snap via Moodstocks
        // Else snap     via Barcode/QR engine
        if(session != null){
            // Snap via Moodstocks
            session.snap();
        }else{
            // Snap via Barcode/QR engine

        }
    }

    /* Listener */
    @Override
    public void onCameraOpenFailed(Exception e) {
        Log.i(TAG,"onCameraOpenFailed");
    }

    @Override
    public void onResult(Result result) {

        String irId = result.getValue();

        session.resume();

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

        // Notify the host app the taken bitmap
        mCameraSynchronizer.onSnap(bitmap);

        // Start search 2D
        obtainMessage(SlyceCameraMessage.SEARCH_2D, bitmap).sendToTarget();
    }

    @Override
    public void handleMessage(Message msg) {

        switch(msg.what){

            case SlyceCameraMessage.SEARCH_2D:

                // Slyce + Moodstocks search
                SlyceProductsRequest request = new SlyceProductsRequest(mSlyce, new OnSlyceRequestListener() {
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
