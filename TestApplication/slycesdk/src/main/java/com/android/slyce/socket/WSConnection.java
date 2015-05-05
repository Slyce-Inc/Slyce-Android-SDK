package com.android.slyce.socket;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import com.android.slyce.communication.ComManager;
import com.android.slyce.handler.RequestSynchronizer;
import com.android.slyce.listeners.OnImageUploadListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
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

    private boolean isCancelled = false;
    private boolean mIs2D;

    private JSONObject mOptions;

    public WSConnection(Context context, String clientID, boolean is2D, OnSlyceRequestListener listener){

        mContext = context.getApplicationContext();

        mixpanel = MixpanelAPI.getInstance(context, Constants.MIXPANEL_TOKEN);

        mRequestSynchronizer = new RequestSynchronizer(listener);

        mRequestUrl = createRequestUrl(clientID, Utils.getAndroidID(context));

        mIs2D = is2D;

        mClientId = clientID;
    }

    public void setOnTokenListener(OnTokenListener listener){
        mTokenListener = listener;
    }

    public void connect(final MethodType methodType){

        // Slyce
        AsyncHttpClient client = AsyncHttpClient.getDefaultInstance();

        client.websocket(mRequestUrl, null, new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {

                if (ex != null) {

                    StringBuilder error = new StringBuilder();
                    error.append("Message: ").append(ex.getMessage()).append(" Cause: ").append(ex.getCause());

                    mRequestSynchronizer.onError(error.toString());

                }else{

                    mWebSocket = webSocket;

                    // Set listeners
                    setCallbacks();

                    // Set request type
                    setMethodType(methodType);

                    // Check if the request has been cancelled before connection has been established
                    if(isCancelled){
                        mWebSocket.close();
                        return;
                    }

                    // Ready to perform a request
                    callSlyceMethod(methodType);
                }
            }
        });

        // MS
        if(mIs2D){
            perform2DSearchMethod(methodType);
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

                ComManager.getInstance().searchMSImageFile(mContext, mBitmap,
                        new ComManager.On2DSearchListener() {

                            @Override
                            public void onResponse(String irId, String error) {
                                handle2DSearchResponse(irId, null, error);
                            }
                        });

                break;

            case SEND_IMAGE_URL:

                ComManager.getInstance().seachMSImageURL(mContext, mImageUrl,
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

        // Return if irId got back empty or null
        if(TextUtils.isEmpty(irId)){

            // Notify the host application for a 2D search error
            mRequestSynchronizer.onError(error);

            // Report to MixPanel
            JSONObject searchError = new JSONObject();
            try {
                searchError.put(Constants.DETECTION_TYPE, Constants._2D);
                searchError.put(Constants.ERROR_MESSAGE, error);
                mixpanel.track(Constants.SEARCH_ERROR, searchError);
            } catch (JSONException e){}

            return;
        }

        // Report to MixPanel
        try {
            JSONObject imageDetectReport = new JSONObject();

            if(imageUrl != null){
                imageDetectReport.put(Constants.IMAGE_URL, imageUrl);
            }

            imageDetectReport.put(Constants.DETECTION_TYPE, Constants._2D);
            imageDetectReport.put(Constants.DATA_IRID, irId);

            mixpanel.track(Constants.IMAGE_DETECTED, imageDetectReport);

        }catch (JSONException e){}

        // Notify the host application for basic result
        mRequestSynchronizer.on2DRecognition(irId, Utils.decodeBase64(irId));

        // Get extended products results
        ComManager.getInstance().getIRIDInfo(mClientId, irId, new ComManager.OnExtendedInfoListener() {
            @Override
            public void onExtendedInfo(JSONArray products) {

                // Notify the host application for extended result
                mRequestSynchronizer.on2DExtendedRecognition(products);
            }
        });
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

                                    // Notify hosting application that bitmap was uploaded
                                    mRequestSynchronizer.onStageLevelFinish(OnSlyceRequestListener.StageMessage.BitmapUploaded);

                                    mWebSocket.send(ticket);
                                    mWebSocket.send(new byte[10]);

                                }else{
                                    // Image was not uploaded
                                    mRequestSynchronizer.onError("Error on uploading bitmap");
                                }
                            }
                        });

                    }else{

                        // Report to MP
                        mixpanel.track(Constants.IMAGE_SENT, imageSentReport);

                        mWebSocket.send(ticket);
                        mWebSocket.send(new byte[10]);
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
                        JSONObject searchNotFound = new JSONObject();
                        searchNotFound.put(Constants.DETECTION_TYPE, Constants._3D);
                        searchNotFound.put(Constants.TOTAL_DETECTION_TIME, time);
                        mixpanel.track(Constants.SEARCH_NOT_FOUND, searchNotFound);

                        // Send an empty products array
                        mRequestSynchronizer.on3DRecognition(new JSONArray());
                    }

                    break;

                case Constants.RESULTS:

                    // Notify the app developer for the results
                    JSONArray products = data.optJSONArray(Constants.PRODUCTS);

                    // Calculate detection time
                    long totalDetectionTime = System.currentTimeMillis() - startDetectionTime;
                    long time = TimeUnit.MILLISECONDS.toSeconds(totalDetectionTime);

                    if(products == null){

                        JSONObject searchNotFound = new JSONObject();
                        searchNotFound.put(Constants.DETECTION_TYPE, Constants._3D);
                        mixpanel.track(Constants.SEARCH_NOT_FOUND, searchNotFound);

                        // Send an empty products array
                        mRequestSynchronizer.on3DRecognition(new JSONArray());

                    }else{

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

                        mRequestSynchronizer.on3DRecognition(products);
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

    public void close(){
        if(mWebSocket != null){
            mWebSocket.close();
        }else{
            isCancelled = true;
        }
    }

    public enum MethodType{

        SEND_IMAGE,
        SEND_IMAGE_URL
    }

    public interface OnTokenListener{

        public void onTokenReceived(String token);
    }
}
