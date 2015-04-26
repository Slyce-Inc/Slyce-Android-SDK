package com.android.slyce.camera;

import android.app.Activity;
import android.util.Log;
import android.view.SurfaceView;
import com.android.slyce.Slyce;
import com.android.slyce.communication.ComManager;
import com.android.slyce.handler.CameraSynchronizer;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.utils.Utils;
import com.moodstocks.android.AutoScannerSession;
import com.moodstocks.android.AutoScannerSession.Listener;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Result;
import com.moodstocks.android.Scanner;

import org.json.JSONArray;

public class SlyceCamera implements Listener{

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

    public SlyceCamera(Activity activity, Slyce slyce, SurfaceView preview, OnSlyceCameraListener listener){

        mActivity = activity;

        mClientId = slyce.getClientID();

        mCameraSynchronizer = new CameraSynchronizer(listener);

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

        // TODO: ask Nathan why this value is not at the C'tor, otherwise the app developer can change it at runtime

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

    @Override
    public void onWarning(String s) {
        Log.i(TAG,"onWarning");
    }

}
