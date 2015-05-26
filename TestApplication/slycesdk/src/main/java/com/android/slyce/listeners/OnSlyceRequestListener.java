package com.android.slyce.listeners;

import com.android.slyce.SlyceBarcode;
import com.android.slyce.enums.SlyceRequestStage;

import org.json.JSONArray;

public interface OnSlyceRequestListener {

    void onBarcodeRecognition(SlyceBarcode barcode);

    void on2DRecognition(String irid, String productInfo);
    void on2DExtendedRecognition(JSONArray products);

    void on3DRecognition(JSONArray products);

    void onSlyceRequestStage(SlyceRequestStage message);
    void onSlyceProgress(long progress, String message, String id);

    void onError(String message);
}
