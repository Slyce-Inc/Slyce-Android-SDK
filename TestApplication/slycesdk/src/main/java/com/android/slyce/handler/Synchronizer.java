package com.android.slyce.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.slyce.SlyceRequest;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.SlyceProgress;
import org.json.JSONArray;

/**
 * Created by davidsvilem on 3/26/15.
 */
public class Synchronizer extends Handler {

    private OnSlyceRequestListener mRequestListener;

    public Synchronizer(OnSlyceRequestListener listener){
        super(Looper.getMainLooper());
        mRequestListener = listener;
    }

    public void onSlyceProgress(long progress, String message, String token){
        SlyceProgress slyceProgress = new SlyceProgress(progress, message, token);
        obtainMessage(1, slyceProgress).sendToTarget();
    }

    public void on2DRecognition(){
        obtainMessage(2).sendToTarget();
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

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what){

            case 1:

                SlyceProgress slyceProgress = (SlyceProgress) msg.obj;
                mRequestListener.onSlyceProgress(slyceProgress.progress, slyceProgress.message, slyceProgress.token);

                break;

            case 2:

                mRequestListener.on2DRecognition();

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

        }

        super.handleMessage(msg);
    }
}
