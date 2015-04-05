package com.android.slyce.socket;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import com.android.slyce.handler.Synchronizer;
import com.android.slyce.listeners.OnImageUploadListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.utils.Constants;
import com.android.slyce.models.Ticket;
import com.android.slyce.utils.Utils;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.android.slyce.report.mpmetrics.MixpanelAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by davidsvilem on 3/22/15.
 */
public class WSConnection implements
        WebSocket.StringCallback,
        DataCallback,
        CompletedCallback{

    private final String TAG = WSConnection.class.getSimpleName();

    private WebSocket mWebSocket;

    private Bitmap mBitmap;

    private Synchronizer mSynchronizer;

    private String mImageUrl;

    private String mRequestUrl;

    private MethodType mMethodType;

    private OnTokenListener mTokenListener;

    private MixpanelAPI mixpanel;

    private String imageURL;

    private String token;

    private String keywords;
    private String category;
    private String color;
    private String gender;
    private String brand;

    public WSConnection(Context context, String clientID, OnSlyceRequestListener listener){

        mixpanel = MixpanelAPI.getInstance(context, Constants.MIXPANEL_TOKEN);

        mSynchronizer = new Synchronizer(listener);

        mRequestUrl = createRequestUrl(clientID, Utils.getAndroidID(context));
    }

    public void setOnTokenListener(OnTokenListener listener){
        mTokenListener = listener;
    }

    public void connect(final MethodType methodType){

        AsyncHttpClient client = AsyncHttpClient.getDefaultInstance();

        client.websocket(mRequestUrl, null, new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {

                if (ex != null) {

                    StringBuilder error = new StringBuilder();
                    error.append("Message: ").append(ex.getMessage()).append(" Cause: ").append(ex.getCause());

                    mSynchronizer.onError(error.toString());

                }else{

                    mWebSocket = webSocket;

                    setCallbacks();

                    setMethodType(methodType);

                    // Ready to perform a request
                    callMethod(methodType);
                }
            }
        });
    }

    private void callMethod(MethodType type){

        switch (type){

            case SEND_IMAGE:

                sendRequestImage(mBitmap);

                break;

            case SEND_IMAGE_URL:

                sendRequestImageUrl(mImageUrl);

                break;
        }
    }

    public void sendRequestImageUrl(String imageUrl){

        String ticket = Ticket.createTicket(Constants.CREATE_TICKET, Constants.IMAGE_URL, imageUrl);

        mWebSocket.send(ticket);
    }

    public void sendRequestImage(Bitmap bitmap){

        mBitmap = bitmap;

        String ticket = Ticket.createTicket(Constants.CREATE_TICKET, Constants.TICKET_TYPE, Constants.PRODUCT_SEARCH);

        mWebSocket.send(ticket);
    }

    public void setBitmap(Bitmap bitmap){
        mBitmap = bitmap;
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
        Log.i(TAG, "onStringAvailable");

        handleResult(s);
    }

    // DataCallback
    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        Log.i(TAG, "onDataAvailable");
    }

    // CompletedCallback
    @Override
    public void onCompleted(Exception ex) {
        Log.i(TAG, "onCompleted");
        if(ex != null){
            StringBuilder error = new StringBuilder();
            error.append("Message: ").append(ex.getMessage()).append(" Cause: ").append(ex.getCause());

            mSynchronizer.onError(error.toString());
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
                    props.put(Constants.FOUNDATION_TICKET, token);
                    mixpanel.registerSuperProperties(props);

                    // Create ticket
                    ticket = Ticket.createTicket(Constants.IMAGE_UPLOADED, Constants.TOKEN, token);

                    // Image destination URL
                    String uploadUrl = data.optString(Constants.UPLOAD_URL);

                    // Image Url for MixPanel report
                    imageURL = data.optString(Constants.IMAGE_URL);

                    if(mMethodType == MethodType.SEND_IMAGE){

                        uploadBitmapToServer(uploadUrl, mBitmap, new OnImageUploadListener() {

                            @Override
                            public void onImageUploaded(int responseCode) {

                                if(responseCode == 200){

                                    mWebSocket.send(ticket);
                                    mWebSocket.send(new byte[10]);
                                }
                            }
                        });

                    }else{

                        mWebSocket.send(ticket);
                        mWebSocket.send(new byte[10]);
                    }

                    break;

                case Constants.PROGRESS:

                    JSONObject imageSentReport = new JSONObject();
                    imageSentReport.put(Constants.IMAGE_URL, imageURL);
                    mixpanel.track(Constants.IMAGE_SENT, imageSentReport);

                    String message = data.optString(Constants.MESSAGE);

                    // Saving these filed to report to MixPanel on "result" event
                    keywords = data.optString(Constants.KEYWORDS);
                    category = data.optString(Constants.CATEGORY);
                    color = data.optString(Constants.COLOR);
                    gender = data.optString(Constants.GENDER);
                    brand = data.optString(Constants.BRAND);

                    long progress = data.optLong(Constants.PROGRESS);

                    if(progress != -1){

                        // Report to MixPanel
                        JSONObject searchProfressReport = new JSONObject();
                        searchProfressReport.put(Constants.PROGRESS_MESSAGE_CONTENT, message);
                        mixpanel.track(Constants.SEARCH_PROGRESS, searchProfressReport);

                        mSynchronizer.onSlyceProgress(progress, message, token);

                        // Keep sending tickets as long progress != -1
                        ticket = Ticket.createTicket(Constants.RESULTS, Constants.TOKEN, token);

                        mWebSocket.send(ticket);
                        mWebSocket.send(new byte[10]);

                    }else{

                        // If progress = -1 received then no products found
                        JSONObject searchNotFound = new JSONObject();
                        searchNotFound.put(Constants.DETECTION_TYPE, Constants._3D);
                        mixpanel.track(Constants.SEARCH_NOT_FOUND, searchNotFound);

                        // Send an empty products array
                        mSynchronizer.on3DRecognition(new JSONArray());
                    }

                    break;

                case Constants.RESULTS:

                    // Notify the app developer for the results
                    JSONArray products = data.optJSONArray(Constants.PRODUCTS);

                    if(products == null){

                        JSONObject searchNotFound = new JSONObject();
                        searchNotFound.put(Constants.DETECTION_TYPE, Constants._3D);
                        mixpanel.track(Constants.SEARCH_NOT_FOUND, searchNotFound);

                        // Send an empty products array
                        mSynchronizer.on3DRecognition(new JSONArray());

                    }else{

                        // Report to MixPanel
                        JSONObject imageDetectReport = new JSONObject();
                        imageDetectReport.put(Constants.IMAGE_URL, imageURL);
                        // TODO: change it according to the type 3D, 2D, UPC, QR
                        imageDetectReport.put(Constants.DETECTION_TYPE, Constants._3D);

                        JSONObject contentReport = new JSONObject();
                        contentReport.put(Constants.KEYWORDS, keywords);
                        contentReport.put(Constants.CATEGORY, category);
                        contentReport.put(Constants.COLOR, color);
                        contentReport.put(Constants.GENDER, gender);
                        contentReport.put(Constants.BRAND, brand);

                        imageDetectReport.put(Constants.CONTENT,contentReport);

                        mixpanel.track(Constants.IMAGE_DETECTED, imageDetectReport);

                        mSynchronizer.on3DRecognition(products);
                    }

                    break;

                case Constants.WORK_FLOW_ENDED:

                    mWebSocket.close();

                    break;

                case Constants.TICKET_CREATION_FAILED:

                    String error = data.optString(Constants.ERROR);

                    mSynchronizer.onError(error);

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

                    Bitmap scaledBitmap = Utils.scaleDown(bitmap, 450);

                    int responseCode = Utils.uploadBitmap(scaledBitmap, uploadUrl);

                    listener.onImageUploaded(responseCode);
                }
            }
        }).start();
    }

    public enum MethodType{

        SEND_IMAGE,
        SEND_IMAGE_URL
    }

    public interface OnTokenListener{

        public void onTokenReceived(String token);
    }
}
