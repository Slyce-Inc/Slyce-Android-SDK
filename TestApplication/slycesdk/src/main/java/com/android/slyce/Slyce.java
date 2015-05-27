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

    private static Slyce mInstance;

    private SharedPrefHelper mSharedPrefHelper;

    private final Context mContext;

    private final MixpanelAPI mixpanel;

    private String mClientID;

    private AtomicBoolean isOpened = new AtomicBoolean(false);

    /* MS */
    private Scanner scanner;
    private boolean compatible;

    public static Slyce getInstance(Context context, String clientID) {

        if(mInstance == null){
            synchronized (Slyce.class){
                if(mInstance == null){
                    mInstance = new Slyce(context.getApplicationContext(), clientID);
                }
            }
        }
        return mInstance;
    }

    public static Slyce get(){
        return mInstance;
    }

    private Slyce(Context context, String clientID) {

        // Get Mixpanel object
        mixpanel = MixpanelAPI.getInstance(context.getApplicationContext(), Constants.MIXPANEL_TOKEN);

        // Context
        mContext = context;

        // Client Id
        mClientID = clientID;

        // Get shared preferences
        mSharedPrefHelper = SharedPrefHelper.getInstance(context);

        // Save the client id
        mSharedPrefHelper.setClientID(clientID);

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
            peopleAnalytics.put(Constants.HOSTING_APP_NAME, Utils.getHostAppName(context));
            peopleAnalytics.put(Constants.HOSTING_APP_VERSION, Utils.getHostAppVersion(context));
            peopleAnalytics.put(Constants.MP_SDK_VERSION, Constants.SDK_VERSION);
            peopleAnalytics.put(Constants.CREATED, created);

            // Get Google Advertising ID for MixPanel
            Utils.getGoogleAdvertisingID(context, new Utils.CallBack() {
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

    public void open(final OnSlyceOpenListener listener){

        if(listener == null){
            SlyceLog.e(TAG, "OnSlyceOpenListener can not be null");
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

        if(TextUtils.isEmpty(mClientID)){
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

        // Get client info
        ComManager.getInstance().getClientIDInfo(mClientID, new ComManager.OnResponseListener() {
            @Override
            public void onResponse(JSONObject jsonResponse) {

                // parsing and saving client info data
                if(jsonResponse != null && jsonResponse.optString(Constants.STATUS).equalsIgnoreCase(Constants.SUCCESS)){

                    mSharedPrefHelper.setPremium(jsonResponse.optString(Constants.PREMIUM));

                    JSONObject msJson = jsonResponse.optJSONObject(Constants.MS);

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

                                    // Init MoodStocks
                                    compatible = Scanner.isCompatible();
                                    if(compatible){
                                        try {
                                            scanner = Scanner.get();
                                            String path = Scanner.pathFromFilesDir(mContext, "scanner.db");
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

    public boolean isPremiumUser() {
        return mSharedPrefHelper.isPremium();
    }

    public boolean is2DSearchEnabled() {
        return mSharedPrefHelper.isMSEnbaled();
    }

    public String getClientID(){
        return mSharedPrefHelper.getClientID();
    }

    public Context getContext(){
        return mContext;
    }

    public boolean isOpen(){
        return isOpened.get();
    }

    public void close(){
        // Required by MS
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

    /**
     * Do Not call this method
     */
    protected void release(){
        mInstance = null;
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
}
