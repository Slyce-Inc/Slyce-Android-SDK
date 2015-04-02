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
import com.mixpanel.android.mpmetrics.MixpanelAPI;

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

        String ticket = Ticket.createTicket("createTicket", "imageURL", imageUrl);

        mWebSocket.send(ticket);
    }

    public void sendRequestImage(Bitmap bitmap){

        mBitmap = bitmap;

        String ticket = Ticket.createTicket("createTicket", "ticketType", "productSearch");

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
                append("clientID=").
                append(clientID).
                append("&").
                append("installID=").
                append(installID);

        return url.toString();
    }

    // StringCallback
    @Override
    public void onStringAvailable(String s) {
        Log.i(TAG, "onStringAvailable");

        Log.i(TAG, s);

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
            String event = response.optString("event");

            // Get the token
            JSONObject data = response.optJSONObject("data");

            switch(event){

                case "ticketCreated":

                    token = data.optString("token");

                    mTokenListener.onTokenReceived(token);

                    // Report Foundation ticket (super property)
                    JSONObject props = new JSONObject();
                    props.put("foundationTicket", token);
                    mixpanel.registerSuperProperties(props);

                    // Create ticket
                    ticket = Ticket.createTicket("imageUploaded", "token", token);

                    // Image destination URL
                    String uploadUrl = data.optString("uploadURL");

                    // Image Url for MixPanel report
                    imageURL = data.optString("imageURL");

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

                case "progress":



                    JSONObject imageSentReport = new JSONObject();
                    imageSentReport.put("imageURL", imageURL);
                    mixpanel.track("Image.Sent", imageSentReport);

                    String message = data.optString("message");

                    // Saving these filed to report to MixPanel on "result" event
                    keywords = data.optString("keywords");
                    category = data.optString("category");
                    color = data.optString("color");
                    gender = data.optString("gender");
                    brand = data.optString("brand");

                    // Report to MixPanel
                    JSONObject searchProfressReport = new JSONObject();
                    searchProfressReport.put("progressMessageContent", message);
                    mixpanel.track("Search.Progress", searchProfressReport);// TODO: ask Nathan if this should be the key

                    long progress = data.optLong("progress");

                    mSynchronizer.onSlyceProgress(progress, message, token);

                    ticket = Ticket.createTicket("results", "token", token);

                    mWebSocket.send(ticket);
                    mWebSocket.send(new byte[10]);

                    break;


                case "results":

                    // Notify the app developer for the results
                    JSONArray products = data.optJSONArray("products");

                    if(products == null){

                        mixpanel.track("Search.Not.Found", null);

                        mSynchronizer.onError("Products is null");

                    }else{

                        // Report to MixPanel
                        JSONObject imageDetectReport = new JSONObject();
                        imageDetectReport.put("imageURL", imageURL);
                        imageDetectReport.put("detectionType", "3D"); // TODO: change it according to the type 3D, 2D, UPC, QR

                        JSONObject contentReport = new JSONObject();
                        contentReport.put("keywords", keywords);
                        contentReport.put("category", category);
                        contentReport.put("color", color);
                        contentReport.put("gender", gender);
                        contentReport.put("brand", brand);

                        imageDetectReport.put("Content",contentReport);

                        mixpanel.track("Image.Detected", imageDetectReport);

                        mSynchronizer.on3DRecognition(products);
                    }

                    break;

                case "workflowEnded":

                    mWebSocket.close();

                    break;

                case "ticketCreationFailed":

                    String error = data.optString("error");

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
