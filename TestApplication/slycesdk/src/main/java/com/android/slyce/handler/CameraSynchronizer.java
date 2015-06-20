package com.android.slyce.handler;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.slyce.enums.SlyceRequestStage;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.SlyceBarcode;
import com.android.slyce.models.SlyceProgress;
import com.android.slyce.utils.SlyceLog;

import org.json.JSONArray;
import org.json.JSONObject;

public class CameraSynchronizer extends Handler {

    private final String TAG = CameraSynchronizer.class.getSimpleName();

    private OnSlyceCameraListener mCameraListener;

    public CameraSynchronizer(OnSlyceCameraListener listener){
        super(Looper.getMainLooper());

        mCameraListener = listener;
    }

    public void onCameraBarcodeDetected(SlyceBarcode barcode){
        obtainMessage(1, barcode).sendToTarget();
    }

    public void onCameraImageDetected(String productInfo){
        obtainMessage(2, productInfo).sendToTarget();
    }

    public void onCameraImageInfoReceived(JSONArray products){
        obtainMessage(3, products).sendToTarget();
    }

    public void onError(String message){
        obtainMessage(4, message).sendToTarget();
    }

    public void onTap(float x, float y){
        Object[] point = new Object[2];
        point[0] = x;
        point[1] = y;
        obtainMessage(6, point).sendToTarget();
    }

    public void onSlyceProgress(long progress, String message, String token){
        SlyceProgress slyceProgress = new SlyceProgress(progress, message, token);
        obtainMessage(7, slyceProgress).sendToTarget();
    }

    public void onCameraResultsReceived(JSONObject products){
        obtainMessage(8, products).sendToTarget();
    }

    public void onSlyceRequestStage(SlyceRequestStage message){
        obtainMessage(9, message).sendToTarget();
    }

    public void onSnap(Bitmap bitmap){
        obtainMessage(10, bitmap).sendToTarget();
    }

    public void onFinished(){
        obtainMessage(11).sendToTarget();
    }

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what){

            case 1:

                mCameraListener.onCameraBarcodeDetected((SlyceBarcode) msg.obj);

                break;

            case 2:

                String productInfo = (String) msg.obj;

                mCameraListener.onCameraImageDetected(productInfo);

                SlyceLog.i(TAG, "onCameraImageDetected(" + productInfo + ")");

                break;

            case 3:

                JSONArray extenedInfo = (JSONArray) msg.obj;

                mCameraListener.onCameraImageInfoReceived(extenedInfo);

                SlyceLog.i(TAG, "onCameraImageInfoReceived(" + extenedInfo + ")");

                break;

            case 4:

                String message = (String) msg.obj;

                mCameraListener.onSlyceCameraError(message);

                SlyceLog.i(TAG, "onSlyceCameraError(" + message + ")");

                break;

            case 6:

                Object[] point = (Object[]) msg.obj;
                mCameraListener.onTap((float) point[0], (float) point[1]);

                SlyceLog.i(TAG, "onTap("+(float)point[0]+","+(float)point[1]+")");

                break;

            case 7:

                SlyceProgress slyceProgress = (SlyceProgress) msg.obj;

                long progress = slyceProgress.progress;
                String progressMsg = slyceProgress.message;
                String token = slyceProgress.token;

                mCameraListener.onCameraSlyceProgress(progress, progressMsg, token);

                SlyceLog.i(TAG, "onCameraSlyceProgress(" + progress + ", " + progressMsg + ", " + token + ")");

                break;

            case 8:

                JSONObject products = (JSONObject) msg.obj;

                mCameraListener.onCameraResultsReceived(products);

                SlyceLog.i(TAG, "onCameraResultsReceived(" + products + ")");
                break;

            case 9:

                SlyceRequestStage stageMsg = (SlyceRequestStage) msg.obj;

                mCameraListener.onCameraSlyceRequestStage(stageMsg);

                SlyceLog.i(TAG, "onCameraSlyceRequestStage(" + stageMsg + ")");

                break;

            case 10:

                mCameraListener.onSnap((Bitmap) msg.obj);

                SlyceLog.i(TAG, "onSnap()");

                break;

            case 11:

                mCameraListener.onCameraFinished();

                SlyceLog.i(TAG, "onCameraFinished()");

                break;
        }
    }
}
