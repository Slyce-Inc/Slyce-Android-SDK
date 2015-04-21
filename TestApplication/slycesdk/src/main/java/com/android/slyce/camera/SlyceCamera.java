package com.android.slyce.camera;

import android.app.Activity;
import android.util.Log;
import android.view.SurfaceView;

import com.android.slyce.Slyce;
import com.moodstocks.android.AutoScannerSession;
import com.moodstocks.android.AutoScannerSession.Listener;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Result;
import com.moodstocks.android.Scanner;

/**
 * Created by davidsvilem on 4/20/15.
 */
public class SlyceCamera implements Listener{

    private AutoScannerSession session;
    private static final int TYPES = Result.Type.IMAGE | Result.Type.QRCODE | Result.Type.EAN13;

    public SlyceCamera(Activity activity, Slyce slyce, SurfaceView preview){

        try {
            session = new AutoScannerSession(activity, Scanner.get(), this, preview);
            session.setResultTypes(TYPES);
        } catch (MoodstocksError moodstocksError) {
            moodstocksError.printStackTrace();
        }
    }

    public void start() {
        session.start();
    }

    public void stop() {
        session.stop();
    }

    public void snap(){
        //
    }

    @Override
    public void onCameraOpenFailed(Exception e) {
        Log.i("","");
    }

    @Override
    public void onResult(Result result) {
        Log.i("","");
    }

    @Override
    public void onWarning(String s) {
        Log.i("","");
    }
}
