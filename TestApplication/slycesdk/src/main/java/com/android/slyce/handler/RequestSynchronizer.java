package com.android.slyce.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.slyce.enums.SlyceRequestStage;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.SlyceBarcode;
import com.android.slyce.models.SlyceProgress;
import com.android.slyce.utils.SlyceLog;

import org.json.JSONArray;
import org.json.JSONObject;

public class RequestSynchronizer extends Handler {

    private final String TAG = RequestSynchronizer.class.getSimpleName();

    private OnSlyceRequestListener mRequestListener;

    public RequestSynchronizer(OnSlyceRequestListener listener){
        super(Looper.getMainLooper());
        mRequestListener = listener;
    }

    public void onBarcodeDetected(SlyceBarcode barcode){
        obtainMessage(0, barcode).sendToTarget();
    }

    public void onSlyceProgress(long progress, String message, String token){
        SlyceProgress slyceProgress = new SlyceProgress(progress, message, token);
        obtainMessage(1, slyceProgress).sendToTarget();
    }

    public void onImageDetected(String productInfo){
        obtainMessage(2, productInfo).sendToTarget();
    }

    public void onResultsReceived(JSONObject products){
        obtainMessage(3, products).sendToTarget();
    }

    public void onError(String message){
        obtainMessage(4, message).sendToTarget();
    }

    public void onSlyceRequestStage(SlyceRequestStage message){
        obtainMessage(5, message).sendToTarget();
    }

    public void onImageInfoReceived(JSONArray products){
        obtainMessage(6, products).sendToTarget();
    }

    public void onFinished(){
        obtainMessage(7).sendToTarget();
    }

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what){

            case 0:

                SlyceBarcode barcode = (SlyceBarcode) msg.obj;
                mRequestListener.onBarcodeDetected(barcode);

                break;

            case 1:

                SlyceProgress slyceProgress = (SlyceProgress) msg.obj;

                long progress = slyceProgress.progress;
                String progressMsg = slyceProgress.message;
                String token = slyceProgress.token;

                mRequestListener.onSlyceProgress(progress, progressMsg, token);

                SlyceLog.i(TAG, "onSlyceProgress(" + progress + ", " + progressMsg + ", " + token + ")");

                break;

            case 2:

                String productInfo = (String) msg.obj;

                mRequestListener.onImageDetected(productInfo);

                SlyceLog.i(TAG, "onImageDetected(" + productInfo + ")");

                break;

            case 3:

                JSONObject products = (JSONObject) msg.obj;

                mRequestListener.onResultsReceived(products);

                SlyceLog.i(TAG, "onResultsReceived(" + products + ")");

                break;

            case 4:

                String message = (String) msg.obj;

                mRequestListener.onError(message);

                SlyceLog.i(TAG, "onError(" + message + ")");

                break;

            case 5:

                SlyceRequestStage stageMsg = (SlyceRequestStage) msg.obj;

                mRequestListener.onSlyceRequestStage(stageMsg);

                SlyceLog.i(TAG, "onSlyceRequestStage(" + stageMsg + ")");

                break;

            case 6:

                JSONArray extenedInfo = (JSONArray) msg.obj;

                mRequestListener.onImageInfoReceived(extenedInfo);

                SlyceLog.i(TAG, "onImageInfoReceived(" + extenedInfo + ")");

                break;

            case 7:

                mRequestListener.onFinished();

                SlyceLog.i(TAG, "onFinished()");

                break;
        }
    }
}
