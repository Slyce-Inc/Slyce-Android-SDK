package com.android.slyce.listeners;

import android.graphics.Bitmap;

import org.json.JSONArray;

/**
 * Created by davidsvilem on 4/21/15.
 */
public interface OnSlyceCameraListener {

    /* Standart/Premium */
    void onBarcodeRecognition(String barcode);

    /* Premium */
    void on2DRecognition(String irId, String productInfo);
    void on2DExtendedRecognition(JSONArray products);

    /* Error */
    void onError(String message);

    void onSnap(Bitmap bitmap);

    void onTap();
}
