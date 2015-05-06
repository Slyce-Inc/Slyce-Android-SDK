package com.android.slyce.listeners;

import com.android.slyce.models.SlyceBarcode;

import org.json.JSONArray;

public interface OnSlyceRequestListener {

    void onBarcodeRecognition(SlyceBarcode barcode);

    void onSlyceProgress(long progress, String message, String id);

    void on2DRecognition(String irid, String productInfo);
    void on2DExtendedRecognition(JSONArray products);

    void on3DRecognition(JSONArray products);
    void onStageLevelFinish(StageMessage message);

    void onError(String message);

    enum StageMessage{
        BitmapUploaded,
    }
}
