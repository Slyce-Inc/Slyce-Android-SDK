package com.android.slyce.listeners;

import android.graphics.Bitmap;

import com.android.slyce.SlyceRequest;

import org.json.JSONArray;

/**
 * Created by davidsvilem on 3/23/15.
 */
public interface OnSlyceRequestListener {

    public void onSlyceProgress(long progress, String message, String id);
    public void on2DRecognition();
    public void on3DRecognition(JSONArray products);
    public void onStageLevelFinish(StageMessage message);
    public void onError(String message);

    public enum StageMessage{

        BitmapUploaded,

    }
}
