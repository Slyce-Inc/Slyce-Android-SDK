package com.android.slyce.handler;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.MoodStocksProgress;
import com.android.slyce.models.SlyceProgress;
import com.android.slyce.utils.SlyceLog;
import org.json.JSONArray;

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

    public void onBarcodeRecognition(String barcode){
        obtainMessage(1, barcode).sendToTarget();
    }

    public void on2DRecognition(String irId, String productInfo){
        MoodStocksProgress moodStocksProgress = new MoodStocksProgress(irId, productInfo);
        obtainMessage(2, moodStocksProgress).sendToTarget();
    }

    public void on2DExtendedRecognition(JSONArray products){
        obtainMessage(3, products).sendToTarget();
    }

    public void onError(String message){
        obtainMessage(4, message).sendToTarget();
    }

    public void onSnap(Bitmap bitmap){
        obtainMessage(5, bitmap).sendToTarget();
    }

    public void onTap(){
        obtainMessage(6).sendToTarget();
    }

    public void onSlyceProgress(long progress, String message, String token){
        SlyceProgress slyceProgress = new SlyceProgress(progress, message, token);
        obtainMessage(7, slyceProgress).sendToTarget();
    }

    public void on3DRecognition(JSONArray products){
        obtainMessage(8, products).sendToTarget();
    }

    public void onStageLevelFinish(OnSlyceRequestListener.StageMessage message){
        obtainMessage(9, message).sendToTarget();
    }

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what){

            case 1:

                mCameraListener.onCameraBarcodeRecognition((String) msg.obj);

                break;

            case 2:

                MoodStocksProgress moodStocksProgress = (MoodStocksProgress) msg.obj;

                String irid = moodStocksProgress.irId;
                String productInfo = moodStocksProgress.productInfo;

                mCameraListener.onCamera2DRecognition(irid, productInfo);

                SlyceLog.i(TAG, "onCamera2DRecognition(" + irid + ", " + productInfo + ")");

                break;

            case 3:

                JSONArray extenedInfo = (JSONArray) msg.obj;

                mCameraListener.onCamera2DExtendedRecognition(extenedInfo);

                SlyceLog.i(TAG, "onCamera2DExtendedRecognition(" + extenedInfo + ")");

                break;

            case 4:

                String message = (String) msg.obj;

                mCameraListener.onSlyceCameraError(message);

                SlyceLog.i(TAG, "onSlyceCameraError(" + message + ")");

                break;

            case 5:

                mCameraListener.onSnap((Bitmap) msg.obj);

                SlyceLog.i(TAG, "onSnap()");

                break;

            case 6:

                mCameraListener.onTap();

                SlyceLog.i(TAG, "onTap()");

            case 7:

                SlyceProgress slyceProgress = (SlyceProgress) msg.obj;

                long progress = slyceProgress.progress;
                String progressMsg = slyceProgress.message;
                String token = slyceProgress.token;

                mCameraListener.onCameraSlyceProgress(progress, progressMsg, token);

                SlyceLog.i(TAG, "onCameraSlyceProgress(" + progress + ", " + progressMsg + ", " + token + ")");

                break;

            case 8:

                JSONArray products = (JSONArray) msg.obj;

                mCameraListener.onCamera3DRecognition(products);

                SlyceLog.i(TAG, "onCamera3DRecognition(" + products + ")");
                break;

            case 9:

                OnSlyceRequestListener.StageMessage stageMsg = (OnSlyceRequestListener.StageMessage) msg.obj;

                mCameraListener.onCameraStageLevelFinish(stageMsg);

                SlyceLog.i(TAG, "onCameraStageLevelFinish(" + stageMsg + ")");

                break;
        }
    }
}
