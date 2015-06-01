package com.android.slyce.handler;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.slyce.Slyce;
import com.android.slyce.enums.SlyceRequestStage;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.Search2DProgress;
import com.android.slyce.SlyceBarcode;
import com.android.slyce.models.SlyceProgress;
import com.android.slyce.utils.Buzzer;
import com.android.slyce.utils.SlyceLog;
import com.android.slycesdk.R;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by davidsvilem on 4/21/15.
 */
public class CameraSynchronizer extends Handler {

    private final String TAG = CameraSynchronizer.class.getSimpleName();

    private OnSlyceCameraListener mCameraListener;

    public CameraSynchronizer(OnSlyceCameraListener listener){
        super(Looper.getMainLooper());

        mCameraListener = listener;
    }

    public void onBarcodeRecognition(SlyceBarcode barcode){
        obtainMessage(1, barcode).sendToTarget();
    }

    public void on2DRecognition(String irId, String productInfo){
        Search2DProgress search2DProgress = new Search2DProgress(irId, productInfo);
        obtainMessage(2, search2DProgress).sendToTarget();
    }

    public void on2DExtendedRecognition(JSONObject products){
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

    public void on3DRecognition(JSONObject products){
        obtainMessage(8, products).sendToTarget();
    }

    public void onSlyceRequestStage(SlyceRequestStage message){
        obtainMessage(9, message).sendToTarget();
    }

    public void onSnap(Bitmap bitmap){
        obtainMessage(10, bitmap).sendToTarget();
    }

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what){

            case 1:

                mCameraListener.onCameraBarcodeRecognition((SlyceBarcode) msg.obj);

                break;

            case 2:

                Search2DProgress search2DProgress = (Search2DProgress) msg.obj;

                String irid = search2DProgress.irId;
                String productInfo = search2DProgress.productInfo;

                mCameraListener.onCamera2DRecognition(irid, productInfo);

                SlyceLog.i(TAG, "onCamera2DRecognition(" + irid + ", " + productInfo + ")");

                break;

            case 3:

                JSONObject extenedInfo = (JSONObject) msg.obj;

                mCameraListener.onCamera2DExtendedRecognition(extenedInfo);

                SlyceLog.i(TAG, "onCamera2DExtendedRecognition(" + extenedInfo + ")");

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

                mCameraListener.onCamera3DRecognition(products);

                SlyceLog.i(TAG, "onCamera3DRecognition(" + products + ")");
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
        }
    }
}
