package com.android.slyce.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.MoodStocksProgress;
import com.android.slyce.models.SlyceProgress;
import org.json.JSONArray;

public class RequestSynchronizer extends Handler {

    private OnSlyceRequestListener mRequestListener;

    public RequestSynchronizer(OnSlyceRequestListener listener){
        super(Looper.getMainLooper());
        mRequestListener = listener;
    }

    public void onSlyceProgress(long progress, String message, String token){
        SlyceProgress slyceProgress = new SlyceProgress(progress, message, token);
        obtainMessage(1, slyceProgress).sendToTarget();
    }

    public void on2DRecognition(String irId, String productInfo){
        MoodStocksProgress moodStocksProgress = new MoodStocksProgress(irId, productInfo);
        obtainMessage(2, moodStocksProgress).sendToTarget();
    }

    public void on3DRecognition(JSONArray products){
        obtainMessage(3, products).sendToTarget();
    }

    public void onError(String message){
        obtainMessage(4, message).sendToTarget();
    }

    public void onStageLevelFinish(OnSlyceRequestListener.StageMessage message){
        obtainMessage(5, message).sendToTarget();
    }

    public void on2DExtendedRecognition(JSONArray products){
        obtainMessage(6, products).sendToTarget();
    }

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what){

            case 1:

                SlyceProgress slyceProgress = (SlyceProgress) msg.obj;
                mRequestListener.onSlyceProgress(slyceProgress.progress, slyceProgress.message, slyceProgress.token);

                break;

            case 2:

                MoodStocksProgress moodStocksProgress = (MoodStocksProgress) msg.obj;
                mRequestListener.on2DRecognition(moodStocksProgress.irId, moodStocksProgress.productInfo);

                break;

            case 3:

                mRequestListener.on3DRecognition((JSONArray) msg.obj);

                break;

            case 4:

                mRequestListener.onError((String) msg.obj);

                break;

            case 5:

                mRequestListener.onStageLevelFinish((OnSlyceRequestListener.StageMessage) msg.obj);

                break;

            case 6:

                mRequestListener.on2DExtendedRecognition((JSONArray) msg.obj);

                break;
        }
    }
}
