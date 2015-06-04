package com.android.slyce;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.android.slyce.communication.ComManager;
import com.android.slyce.listeners.OnSlyceOpenListener;
import com.android.slyce.utils.Constants;
import com.android.slyce.utils.SharedPrefHelper;
import com.android.slyce.utils.SlyceLog;
import com.android.slyce.utils.Utils;
import com.android.slyce.report.mpmetrics.MixpanelAPI;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Scanner;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Slyce {

    private static final String TAG = Slyce.class.getSimpleName();

    /* Slyce SDK object instance */
    private static Slyce mInstance;

    private final MixpanelAPI mixpanel;

    private SharedPrefHelper mSharedPrefHelper;

    private final Context mContext;

    private AtomicBoolean isOpened = new AtomicBoolean(false);

    /* Indicates if sound should on/off */
    private boolean isSoundOn = true;

    /* Indicates if vibrate should on/off */
    private boolean isVibrateOn = true;

    /* MS */
    private Scanner scanner;
    private boolean compatible;

    /**
     * Get and create a new instance of Slyce SDK if one does not exist
     *
     * @param context The application context
     * @return an instance of Slyce SDK
     *
     * <p>The best practice is to call getInstance, and use the returned Slyce,
     * object from a single thread</p>
     */
    public static Slyce getInstance(Context context) {

        if(mInstance == null){
            synchronized (Slyce.class){
                if(mInstance == null){
                    mInstance = new Slyce(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private Slyce(Context context) {

        // Get Mixpanel object
        mixpanel = MixpanelAPI.getInstance(context.getApplicationContext(), Constants.MIXPANEL_TOKEN);

        // Application Context
        mContext = context.getApplicationContext();

        // Get shared preferences
        mSharedPrefHelper = SharedPrefHelper.getInstance(context);
    }

    /**
      * Opens the Slyce object, making it available for use.
      * @param clientID Your client id
      * @param listener notify when the Slyce object opened.
     */
    public void open(String clientID, final OnSlyceOpenListener listener){

        if(listener == null){
            SlyceLog.e(TAG, Constants.SLYCE_INIT_ERROR + Constants.LISTENER_ERROR);
            return;
        }

        if(mContext == null){
            SlyceLog.e(TAG, Constants.SLYCE_INIT_ERROR + Constants.CONTEXT_ERROR);

            // Send an error message to host application
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    listener.onOpenFail(Constants.SLYCE_INIT_ERROR + Constants.CONTEXT_ERROR);
                }
            });

            return;
        }

        if(TextUtils.isEmpty(clientID)){
            SlyceLog.e(TAG, Constants.SLYCE_INIT_ERROR + Constants.CLIENT_ID_ERROR);

            // Send an error message to host application
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    listener.onOpenFail(Constants.SLYCE_INIT_ERROR + Constants.CLIENT_ID_ERROR);
                }
            });

            return;
        }

        // Save the Client ID to shared preferences
        mSharedPrefHelper.setClientID(clientID);

        //
        initMixPanel();

        //
        initClientInfo(clientID, listener);
    }

    /**
     * Indicates whether the user can use premium features of the SDK.
     *
     * @return boolean value
     */
    public boolean isPremiumUser() {
        return mSharedPrefHelper.isPremium();
    }

    /**
     * Indicates whether the user can ask the SDK to perform 2D searches.
     *
     * @return boolean value
     */
    public boolean is2DSearchEnabled() {
        return mSharedPrefHelper.isMSEnbaled();
    }

    /**
     * @return Slyce client id
     */
    public String getClientID(){
        return mSharedPrefHelper.getClientID();
    }

    public Context getContext(){
        return mContext;
    }

    /**
     * Checks if Slyce SDK is open.
     *
     * @return true if open
     */
    public boolean isOpen(){
        return isOpened.get();
    }

    /**
     * Closes the automatic scanner for premium user
     */
    public void close(){
        // Required by MS
        release();
    }

    /**
     * If set, the device will play a sound when the a barcode/2D item auto-matched.
     * @param value
     */
    public void setSound(boolean value){
        isSoundOn = value;
    }

    /**
     * If set, the device will vibrate when the a barcode/2D item auto-matched.
     * @param value
     */
    public void setVibrate(boolean value){
        isSoundOn = value;
    }

    public boolean isSoundOn(){
        return isSoundOn;
    }

    public boolean isVibrateOn(){
        return isVibrateOn;
    }

    public static String getVersion(){
        return Constants.SDK_VERSION;
    }

    private void initMixPanel(){

        // Check if should report "created" field
        String created = mSharedPrefHelper.getCreated();
        if(TextUtils.isEmpty(created)){
            created = Utils.getTimestamp();
            mSharedPrefHelper.setCreated(created);
        }

        try {

            final JSONObject peopleAnalytics = new JSONObject();

            mixpanel.getPeople().identify(mixpanel.getDistinctId());

            peopleAnalytics.put(Constants.USER_ID, mixpanel.getDistinctId());
            peopleAnalytics.put(Constants.MP_CLIENT_ID, getClientID());
            peopleAnalytics.put(Constants.DEVICE_TYPE, Utils.getDeviceType());
            peopleAnalytics.put(Constants.SYSTEM_TYPE, Constants.ANDROID);
            peopleAnalytics.put(Constants.SYSTEM_VERSION, Utils.getOSVersion());
            peopleAnalytics.put(Constants.HOSTING_APP_NAME, Utils.getHostAppName(mContext));
            peopleAnalytics.put(Constants.HOSTING_APP_VERSION, Utils.getHostAppVersion(mContext));
            peopleAnalytics.put(Constants.MP_SDK_VERSION, Constants.SDK_VERSION);
            peopleAnalytics.put(Constants.CREATED, created);

            // Get Google Advertising ID for MixPanel
            Utils.getGoogleAdvertisingID(mContext, new Utils.CallBack() {
                @Override
                public void onReady(String value) {

                    try {
                        peopleAnalytics.put(Constants.GOOGLE_ADVERTISING_ID, value);
                    } catch (JSONException e) {}

                    // Make it user profile
                    mixpanel.getPeople().set(peopleAnalytics);

                    // Make it super property
                    mixpanel.registerSuperProperties(peopleAnalytics);
                }
            });

            // Report time stamp (one shot event)
            JSONObject createdJson = new JSONObject();
            createdJson.put(Constants.CREATED, Utils.getTimestamp());
            mixpanel.registerSuperPropertiesOnce(createdJson);

        } catch (JSONException e) {}
    }

    private void initClientInfo(final String clientId, final OnSlyceOpenListener listener){

        // Get client info
        ComManager.getInstance().getClientIDInfo(clientId, new ComManager.OnResponseListener() {
            @Override
            public void onResponse(JSONObject jsonResponse) {

                // parsing and saving client info data
                if(jsonResponse != null && jsonResponse.optString(Constants.STATUS).equalsIgnoreCase(Constants.SUCCESS)){

                    mSharedPrefHelper.setPremium(jsonResponse.optString(Constants.PREMIUM));

                    final JSONObject msJson = jsonResponse.optJSONObject(Constants.MS);

                    if(msJson != null){

                        final String isEnabled = msJson.optString(Constants.ENABLED);
                        final String key = msJson.optString(Constants.KEY);
                        final String secret = msJson.optString(Constants.SECRET);

                        mSharedPrefHelper.setMSEnabled(isEnabled);
                        mSharedPrefHelper.setMSkey(key);
                        mSharedPrefHelper.setMSsecret(secret);

                        // If 2D is enabled initiate MS
                        if(Boolean.valueOf(isEnabled)){

                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {

                                    String fileName = getClientID()+"_scanner.db";

                                    // Init MoodStocks
                                    compatible = Scanner.isCompatible();
                                    if(compatible){
                                        try {
                                            scanner = Scanner.get();
                                            String path = Scanner.pathFromFilesDir(mContext, fileName);
                                            scanner.open(path, key, secret);
                                            scanner.setSyncListener(new AutoScannerSynceListener());
                                            scanner.sync();
                                        } catch (MoodstocksError e) {
                                            SlyceLog.e(TAG, e.getMessage());
                                        }
                                    }
                                }
                            });
                        }
                    }else{

                        mSharedPrefHelper.setMSEnabled(String.valueOf(Boolean.FALSE));
                    }

                    // Set boolean
                    isOpened.set(true);

                    // Send a message to host application
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onOpenSuccess();
                        }
                    });

                    // Report a success initialization
                    mixpanel.track(Constants.SDK_INIT_SUCCEEDED, null);

                }else{

                    // Client info did not returned with a valid response
                    mixpanel.track(Constants.SDK_INIT_FAILED, null);

                    // Send a message to host application
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onOpenFail("");
                        }
                    });
                }
            }
        });
    }

    private class AutoScannerSynceListener implements Scanner.SyncListener{
        /*
         * 2D Scanner.SyncListener
         */
        @Override
        public void onSyncStart() {
            SlyceLog.d(TAG, "MS Sync will start.");
        }

        @Override
        public void onSyncComplete() {
            try {
                SlyceLog.d(TAG, "MS Sync succeeded (" + scanner.count() + " images)");
            } catch (MoodstocksError e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSyncFailed(MoodstocksError e) {
            SlyceLog.d(TAG, "MS Sync error #" + e.getErrorCode() + ": " + e.getMessage());
        }

        @Override
        public void onSyncProgress(int total, int current) {
            int percent = (int) ((float) current / (float) total * 100);
            SlyceLog.d(TAG, "MS Sync progressing: " + percent + "%");
        }
    }

    private void release(){
        if (compatible) {
            try {
                scanner.close();
                scanner.destroy();
                scanner = null;
            } catch (MoodstocksError e) {
                e.printStackTrace();
            }
        }
    }
}
