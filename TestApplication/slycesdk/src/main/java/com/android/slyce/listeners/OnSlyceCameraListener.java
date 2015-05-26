package com.android.slyce.listeners;

import android.graphics.Bitmap;

import com.android.slyce.SlyceBarcode;
import com.android.slyce.enums.SlyceRequestStage;

import org.json.JSONArray;

/**
 * Created by davidsvilem on 4/21/15.
 */
public interface OnSlyceCameraListener {

    /* Standart/Premium */
    void onCamera3DRecognition(JSONArray products);
    void onCameraBarcodeRecognition(SlyceBarcode barcode);

    /* Premium */
    void onCamera2DRecognition(String irId, String productInfo);
    void onCamera2DExtendedRecognition(JSONArray products);

    /* Progress Reporting */
    void onCameraSlyceProgress(long progress, String message, String id);
    void onCameraSlyceRequestStage(SlyceRequestStage message);

    /* Error */
    void onSlyceCameraError(String message);

    /* Miscellaneous */
    void onSnap(Bitmap bitmap);
    void onTap(float x, float y);
}
