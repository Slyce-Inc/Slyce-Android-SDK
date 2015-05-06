package com.android.slyce.listeners;

import android.graphics.Bitmap;

import com.android.slyce.models.SlyceBarcode;

import org.json.JSONArray;

/**
 * Created by davidsvilem on 4/21/15.
 */
public interface OnSlyceCameraListener {

    /* Standart/Premium */
    void onCameraBarcodeRecognition(SlyceBarcode barcode);

    /* Premium */
    void onCamera2DRecognition(String irId, String productInfo);
    void onCamera2DExtendedRecognition(JSONArray products);

    void onCameraSlyceProgress(long progress, String message, String id);
    void onCamera3DRecognition(JSONArray products);
    void onCameraStageLevelFinish(OnSlyceRequestListener.StageMessage message);

    /* Error */
    void onSlyceCameraError(String message);

    /* Pass the Bitmap to host application */
    void onSnap(Bitmap bitmap);

    void onTap();
}
