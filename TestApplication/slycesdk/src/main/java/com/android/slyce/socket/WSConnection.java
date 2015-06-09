package com.android.slyce.socket;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.android.slyce.communication.ComManager;
import com.android.slyce.enums.SlyceRequestStage;
import com.android.slyce.handler.RequestSynchronizer;
import com.android.slyce.listeners.OnImageUploadListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.SlyceBarcode;
import com.android.slyce.utils.BarcodeHelper;
import com.android.slyce.utils.Constants;
import com.android.slyce.models.Ticket;
import com.android.slyce.utils.SlyceLog;
import com.android.slyce.utils.Utils;
import com.android.slyce.async.ByteBufferList;
import com.android.slyce.async.DataEmitter;
import com.android.slyce.async.callback.CompletedCallback;
import com.android.slyce.async.callback.DataCallback;
import com.android.slyce.async.http.AsyncHttpClient;
import com.android.slyce.async.http.WebSocket;
import com.android.slyce.report.mpmetrics.MixpanelAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * Created by davidsvilem on 3/22/15.
 */
public class WSConnection implements
        WebSocket.StringCallback,
        DataCallback,
        CompletedCallback{

    private final String TAG = WSConnection.class.getSimpleName();

    private Context mContext;

    private WebSocket mWebSocket;

    private Bitmap mBitmap;

    private RequestSynchronizer mRequestSynchronizer;

    private String mClientId;
    private String mImageUrl;
    private String mRequestUrl;
    private String imageURL;
    private String token;
    private String keywords;
    private String category;
    private String color;
    private String gender;
    private String brand;
    private String pattern;

    private MethodType mMethodType;

    private OnTokenListener mTokenListener;

    private MixpanelAPI mixpanel;

    private long startDetectionTime = 0;

    private boolean isRequestCancelled = false;
    private boolean mIs2D;
    private boolean is2DSearchNotFound = false;
    private boolean is3DSearchNotFound = false;

    private JSONObject mOptions;

    private WSConnection.MethodType mRequestType;

    public WSConnection(Context context, String clientID, boolean is2D, OnSlyceRequestListener listener, WSConnection.MethodType type){

        mContext = context.getApplicationContext();

        mixpanel = MixpanelAPI.getInstance(context, Constants.MIXPANEL_TOKEN);

        mRequestSynchronizer = new RequestSynchronizer(listener);

        mRequestUrl = createRequestUrl(clientID, Utils.getAndroidID(context));

        mIs2D = is2D;

        mClientId = clientID;

        mRequestType = type;
    }

    public void setOnTokenListener(OnTokenListener listener){
        mTokenListener = listener;
    }

    public void connect(){

        // Slyce
        AsyncHttpClient client = AsyncHttpClient.getDefaultInstance();

        client.websocket(mRequestUrl, null, new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {

                if (ex != null) {

                    StringBuilder error = new StringBuilder();
                    error.append("Message: ").append(ex.getMessage()).append(" Cause: ").append(ex.getCause());

                    // Report to MixPanel
                    reportError(error.toString());

                    mRequestSynchronizer.onError(error.toString());

                }else{

                    mRequestSynchronizer.onSlyceRequestStage(SlyceRequestStage.StageStartingRequest);

                    mWebSocket = webSocket;

                    // Set listeners
                    setCallbacks();

                    // Set request type
                    setMethodType(mRequestType);

                    // Check if the request has been cancelled before connection has been established
                    if(isRequestCancelled){
                        mWebSocket.close();
                        return;
                    }

                    // Ready to perform a request
                    callSlyceMethod(mRequestType);
                }
            }
        });

        // MS
        if(mIs2D){
            perform2DSearchMethod(mRequestType);
        }
    }

    private void callSlyceMethod(MethodType type){

        switch (type){

            case SEND_IMAGE:

                sendRequestImage(mBitmap);

                break;

            case SEND_IMAGE_URL:

                sendRequestImageUrl(mImageUrl);

                break;
        }
    }

    private void perform2DSearchMethod(MethodType type){

        switch (type){

            case SEND_IMAGE:

                ComManager.getInstance().search2DImageFile(mContext, mBitmap,
                        new ComManager.On2DSearchListener() {

                            @Override
                            public void onResponse(String irId, String error) {
                                handle2DSearchResponse(irId, null, error);
                            }
                        });

                break;

            case SEND_IMAGE_URL:

                ComManager.getInstance().seach2DImageURL(mContext, mImageUrl,
                        new ComManager.On2DSearchListener() {
                            @Override
                            public void onResponse(String irId, String error) {
                                handle2DSearchResponse(irId, mImageUrl, error);
                            }
                        });

                break;
        }
    }

    private void handle2DSearchResponse(String irId, String imageUrl, String error){

        if(!error.isEmpty()){
            // Error with 2D search

            // 1. Report to MixPanel
            JSONObject searchError = new JSONObject();
            try {
                searchError.put(Constants.DETECTION_TYPE, Constants._2D);
                searchError.put(Constants.ERROR_MESSAGE, error);
                mixpanel.track(Constants.SEARCH_ERROR, searchError);
            } catch (JSONException e){}

            // 2. Notify the host application for a 2D search error
            mRequestSynchronizer.onError(error);

            return;
        }

        if(!irId.isEmpty()){
            // 2D search found

            // 1. Report to MixPanel
            try {
                JSONObject imageDetectReport = new JSONObject();

                if(imageUrl != null){
                    imageDetectReport.put(Constants.IMAGE_URL, imageUrl);
                }

                imageDetectReport.put(Constants.DETECTION_TYPE, Constants._2D);
                imageDetectReport.put(Constants.DATA_IRID, irId);

                mixpanel.track(Constants.IMAGE_DETECTED, imageDetectReport);

            }catch (JSONException e){}

            // 2. Notify the host application for 2D search found
            mRequestSynchronizer.on2DRecognition(irId, Utils.decodeBase64(irId));

            // Get extended products results
            ComManager.getInstance().getProductsFromIRID(irId, new ComManager.OnExtendedInfoListener()
            {
                @Override
                public void onExtendedInfo(JSONArray products) {

                    // 3. Notify the host application for 2D extended result
                    mRequestSynchronizer.on2DExtendedRecognition(products);
                }

                @Override
                public void onExtenedInfoError() {
                    mRequestSynchronizer.onError(Constants.NO_PRODUCTS_FOUND);
                }
            });

        }else{
            // 2D search not found
            is2DSearchNotFound = true;

            // If 3D search also not found then report to MixPanel
            if(is3DSearchNotFound){
                try {
                    JSONObject searchNotFound = new JSONObject();
                    searchNotFound.put(Constants.DETECTION_TYPE, Constants._2D);
                    mixpanel.track(Constants.SEARCH_NOT_FOUND, searchNotFound);
                } catch (JSONException e) {}
            }

            // Notify the host application for a 2D search not found (empty data)
            mRequestSynchronizer.on2DRecognition("","");
        }
    }

    public void sendRequestImageUrl(String imageUrl){

        String ticket = Ticket.createTicket(Constants.CREATE_TICKET, Constants.IMAGE_URL, imageUrl, mOptions);

        mWebSocket.send(ticket);

        startDetectionTime = System.currentTimeMillis();
    }

    public void sendRequestImage(Bitmap bitmap){

        mBitmap = bitmap;

        String ticket = Ticket.createTicket(Constants.CREATE_TICKET, Constants.TICKET_TYPE, Constants.PRODUCT_SEARCH, mOptions);

        mWebSocket.send(ticket);

        startDetectionTime = System.currentTimeMillis();
    }

    public void setBitmap(Bitmap bitmap){
        mBitmap = bitmap;
    }

    public void setOptions(JSONObject options){
        mOptions = options;
    }

    public void setImageUrl(String imageUrl){
        mImageUrl = imageUrl;
    }

    public void setMethodType(MethodType methodType){
        mMethodType = methodType;
    }

    private void setCallbacks(){

        mWebSocket.setStringCallback(WSConnection.this);

        mWebSocket.setDataCallback(WSConnection.this);

        mWebSocket.setEndCallback(WSConnection.this);

        mWebSocket.setClosedCallback(WSConnection.this);
    }

    private String createRequestUrl(String clientID, String installID){

        // Create the request URL
        StringBuilder url = new StringBuilder();

        url.append(Constants.WS_URL).
                append(Constants.CLIENT_ID).append("=").
                append(clientID).
                append("&").
                append(Constants.INSTALL_ID).append("=").
                append(installID);

        return url.toString();
    }

    // StringCallback
    @Override
    public void onStringAvailable(String s) {
        SlyceLog.i(TAG, "onStringAvailable");

        handleResult(s);
    }

    // DataCallback
    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        SlyceLog.i(TAG, "onDataAvailable");
    }

    // CompletedCallback
    @Override
    public void onCompleted(Exception ex) {
        SlyceLog.i(TAG, "onCompleted");
        if(ex != null){
            StringBuilder error = new StringBuilder();
            error.append("Message: ").append(ex.getMessage()).append(" Cause: ").append(ex.getCause());

            // 1. Report to MixPanel
            JSONObject searchError = new JSONObject();
            try {
                searchError.put(Constants.DETECTION_TYPE, Constants._3D);
//                searchError.put(Constants.ERROR_MESSAGE, error);
                mixpanel.track(Constants.SEARCH_ERROR, searchError);
            } catch (JSONException e){}

            mRequestSynchronizer.onError(error.toString());
        }
    }

    // Handle response
    private void handleResult(String result){

        try {

            final String ticket;

            JSONObject response = new JSONObject(result);

            // Get the event
            String event = response.optString(Constants.EVENT);

            // Get the token
            JSONObject data = response.optJSONObject(Constants.DATA);

            switch(event){

                case Constants.TICKET_CREATED:

                    token = data.optString(Constants.TOKEN);

                    mTokenListener.onTokenReceived(token);

                    // Notifying the host application for sending the image
                    mRequestSynchronizer.onSlyceRequestStage(SlyceRequestStage.StageSendingImage);

                    // Report Foundation ticket (super property)
                    JSONObject props = new JSONObject();
                    props.put(Constants.FOUNDATION_TOKEN, token);
                    mixpanel.registerSuperProperties(props);

                    // Create ticket
                    ticket = Ticket.createTicket(Constants.IMAGE_UPLOADED, Constants.TOKEN, token, null);

                    // Image destination URL
                    String uploadUrl = data.optString(Constants.UPLOAD_URL);

                    // Image Url for MixPanel report
                    imageURL = data.optString(Constants.IMAGE_URL);

                    // Json For MP
                    final JSONObject imageSentReport = new JSONObject();
                    imageSentReport.put(Constants.IMAGE_URL, imageURL);

                    if(mMethodType == MethodType.SEND_IMAGE){

                        uploadBitmapToServer(uploadUrl, mBitmap, new OnImageUploadListener() {

                            @Override
                            public void onImageUploaded(int responseCode) {

                                if(responseCode == 200){

                                    // Report to MP
                                    mixpanel.track(Constants.IMAGE_SENT, imageSentReport);

                                    if(mWebSocket.isOpen()){
                                        // Notify hosting application that bitmap was uploaded and starting analyze stage
                                        mRequestSynchronizer.onSlyceRequestStage(SlyceRequestStage.StageAnalyzingImage);
                                    }

                                    mWebSocket.send(ticket);
                                    mWebSocket.send(new byte[10]);

                                }else{
                                    // Image was not uploaded
                                    mRequestSynchronizer.onError("Error on uploading bitmap");
                                }
                            }
                        });

                    }else{ // MethodType.SEND_IMAGE_URL

                        // Report to MP
                        mixpanel.track(Constants.IMAGE_SENT, imageSentReport);

                        mWebSocket.send(ticket);
                        mWebSocket.send(new byte[10]);

                        mRequestSynchronizer.onSlyceRequestStage(SlyceRequestStage.StageAnalyzingImage);
                    }

                    break;

                case Constants.PROGRESS:

                    // Get message
                    String message = data.optString(Constants.MESSAGE);

                    // Get details
                    JSONObject details = data.optJSONObject(Constants.DETAILS);

                    if(details != null){
                        // Saving details fields to report to MP at "result" event
                        keywords = details.optString(Constants.SEARCH_KEYWORDS);
                        category = details.optString(Constants.CATEGORY_LABEL);
                        color = details.optString(Constants.COLOR_NAME);
                        gender = details.optString(Constants.GENDER_NAME);
                        brand = details.optString(Constants.BRAND_NAME);
                        pattern = details.optString(Constants.PATTERN_NAME);
                    }

                    // Get progress
                    long progress = data.optLong(Constants.PROGRESS);

                    if(progress != -1){

                        // Report to MP
                        JSONObject searchProfressReport = new JSONObject();
                        searchProfressReport.put(Constants.PROGRESS_MESSAGE_CONTENT, message);
                        searchProfressReport.put(Constants.PROGRESS_VALUE, progress);
                        mixpanel.track(Constants.SEARCH_PROGRESS, searchProfressReport);

                        mRequestSynchronizer.onSlyceProgress(progress, message, token);

                        // Keep sending tickets as long progress != -1
                        ticket = Ticket.createTicket(Constants.RESULTS, Constants.TOKEN, token, null);

                        mWebSocket.send(ticket);
                        mWebSocket.send(new byte[10]);

                    }else{

                        // Calculate detection time
                        long totalDetectionTime = System.currentTimeMillis() - startDetectionTime;
                        long time = TimeUnit.MILLISECONDS.toSeconds(totalDetectionTime);

                        // If progress = -1 received then no products found
                        is3DSearchNotFound = true;

                        // If 2D search also not found then report to MixPanel
                        if(is2DSearchNotFound || !mIs2D){
                            JSONObject searchNotFound = new JSONObject();
                            searchNotFound.put(Constants.DETECTION_TYPE, Constants._3D);
                            searchNotFound.put(Constants.TOTAL_DETECTION_TIME, time);
                            mixpanel.track(Constants.SEARCH_NOT_FOUND, searchNotFound);
                        }

                        // Send an error message to host application
                        mRequestSynchronizer.onError(Constants.NO_PRODUCTS_FOUND);
                    }

                    break;

                case Constants.RESULTS:

                    // Notify the app developer for the results
                    JSONObject barcode = data.optJSONObject(Constants.BARCODE);
                    String errorReason = data.optString(Constants.ERROR_REASON);

                    // Calculate detection time
                    long totalDetectionTime = System.currentTimeMillis() - startDetectionTime;
                    long time = TimeUnit.MILLISECONDS.toSeconds(totalDetectionTime);

                    if(!TextUtils.isEmpty(errorReason)){
                        // Not found
                        is3DSearchNotFound = true;

                        // If 2D search also not found then report to MixPanel
                        if(is2DSearchNotFound || !mIs2D){
                            JSONObject searchNotFound = new JSONObject();
                            searchNotFound.put(Constants.DETECTION_TYPE, Constants._3D);
                            searchNotFound.put(Constants.ERROR_MESSAGE, errorReason);
                            mixpanel.track(Constants.SEARCH_NOT_FOUND, searchNotFound);
                        }

                        // Send an error message to host application
                        mRequestSynchronizer.onError(errorReason);

                        return;
                    }

                    if(barcode != null){

                        String format = barcode.optString(Constants.BARCODE_FORMAT);
                        String parsedResult = barcode.optString(Constants.PARSED_RESULT);

                        // Create SlyceBarcode object
                        SlyceBarcode slyceBarcode = BarcodeHelper.createSlyceBarcode(format, BarcodeHelper.ScannerType._Slyce, parsedResult);

                        // Notify the host application on barcode recognition
                        mRequestSynchronizer.onBarcodeRecognition(slyceBarcode);

                        JSONObject imageDetectReport = new JSONObject();
                        imageDetectReport.put(Constants.DETECTION_TYPE, slyceBarcode.getTypeString());
                        imageDetectReport.put(Constants.DATA_BARCODE, slyceBarcode.getBarcode());
                        mixpanel.track(Constants.BARCODE_DETECTED, imageDetectReport);

                        return;
                    }

                    if(data != null){

                        // Report to MixPanel
                        JSONObject imageDetectReport = new JSONObject();
                        imageDetectReport.put(Constants.IMAGE_URL, imageURL);
                        imageDetectReport.put(Constants.DETECTION_TYPE, Constants._3D);

                        if(!TextUtils.isEmpty(keywords)){
                            imageDetectReport.put(Constants.KEYWORDS, keywords);
                        }
                        if(!TextUtils.isEmpty(category)){
                            imageDetectReport.put(Constants.CATEGORY, category);
                        }
                        if(!TextUtils.isEmpty(color)){
                            imageDetectReport.put(Constants.COLOR, color);
                        }
                        if(!TextUtils.isEmpty(gender)){
                            imageDetectReport.put(Constants.GENDER, gender);
                        }
                        if(!TextUtils.isEmpty(brand)){
                            imageDetectReport.put(Constants.BRAND, brand);
                        }
                        if(!TextUtils.isEmpty(pattern)){
                            imageDetectReport.put(Constants.PATTERN, pattern);
                        }

                        imageDetectReport.put(Constants.TOTAL_DETECTION_TIME, time);

                        mixpanel.track(Constants.IMAGE_DETECTED, imageDetectReport);

                        // Check if "products" array exist
                        JSONArray products = data.optJSONArray(Constants.PRODUCTS);
                        if(products != null && products.length()>0){

                            mRequestSynchronizer.on3DRecognition(data);
                        }else{

                            mRequestSynchronizer.onError(Constants.NO_PRODUCTS_FOUND);
                        }

                        return;
                    }

                    break;

                case Constants.WORK_FLOW_ENDED:

                    mWebSocket.close();

                    break;

                case Constants.TICKET_CREATION_FAILED:

                    String error = data.optString(Constants.ERROR);

                    mRequestSynchronizer.onError(error);

                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void uploadBitmapToServer(final String uploadUrl, final Bitmap bitmap, final OnImageUploadListener listener){

        new Thread(new Runnable() {
            @Override
            public void run() {

                if(!TextUtils.isEmpty(uploadUrl)){

                    Bitmap scaledBitmap = Utils.scaleDown(bitmap);

                    int responseCode = Utils.uploadBitmapToSlyce(scaledBitmap, uploadUrl);

                    listener.onImageUploaded(responseCode);
                }
            }
        }).start();
    }

    private void reportError(String error){
        JSONObject searchError = new JSONObject();
        try {
            searchError.put(Constants.DETECTION_TYPE, Constants._3D);
//            searchError.put(Constants.ERROR_MESSAGE, error);
            mixpanel.track(Constants.SEARCH_ERROR, searchError);
        } catch (JSONException e){}
    }

    public void close(){
        if(mWebSocket != null){
            mWebSocket.close();
        }else{
            isRequestCancelled = true;
        }
    }

    public enum MethodType{
        SEND_IMAGE,
        SEND_IMAGE_URL
    }

    public interface OnTokenListener{
        void onTokenReceived(String token);
    }
}
