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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by davidsvilem on 3/25/15.
 */
public final class Slyce{

    private static final String TAG = Slyce.class.getSimpleName();

    private static Slyce mInstance;

    private SharedPrefHelper mSharedPrefHelper;

    private final Context mContext;

    private final MixpanelAPI mixpanel;

    private String mClientID;

    private AtomicBoolean isOpened = new AtomicBoolean(false);

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

        try {

            JSONObject peopleAnalytics = new JSONObject();

            mixpanel.getPeople().identify(mixpanel.getDistinctId());

            peopleAnalytics.put(Constants.USER_ID, mixpanel.getDistinctId());
            peopleAnalytics.put(Constants.MP_CLIENT_ID, getClientID());
            peopleAnalytics.put(Constants.NAME, Utils.getAccountName(context));
            peopleAnalytics.put(Constants.DEVICE_TYPE, Utils.getDeviceType());
            peopleAnalytics.put(Constants.DEVICE_NAME, Utils.getAccountName(context));
            peopleAnalytics.put(Constants.SYSTEM_TYPE, Constants.ANDROID);
            peopleAnalytics.put(Constants.SYSTEM_VERSION, Utils.getOSVersion());
            peopleAnalytics.put(Constants.HOSTING_APP_NAME, Utils.getHostAppName(context));
            peopleAnalytics.put(Constants.HOSTING_APP_VERSION, Utils.getHostAppVersion(context));
            peopleAnalytics.put(Constants.MP_SDK_VERSION, Constants.SDK_VERSION);

            // Make it user profile
            mixpanel.getPeople().set(peopleAnalytics);

            // Make it super property
            mixpanel.registerSuperProperties(peopleAnalytics);

            // Report time stamp (one shot event)
            JSONObject created = new JSONObject();
            created.put(Constants.CREATED, Utils.getTimestamp());
            mixpanel.registerSuperPropertiesOnce(created);

        } catch (JSONException e) {}
    }

    public void open(final OnSlyceOpenListener listener){

        if(listener == null){
            SlyceLog.e(TAG, "OnSlyceOpenListener can not be null");
            return;
        }

        if(mContext == null){
            SlyceLog.e(TAG, Constants.SLYCE_INIT_ERROR + Constants.CONTEXT_ERROR);

            // Send a message to host application
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

            // Send a message to host application
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

                    JSONObject moodstocksJson = jsonResponse.optJSONObject(Constants.MS);

                    if(moodstocksJson != null){
                        mSharedPrefHelper.setMSEnabled(moodstocksJson.optString(Constants.ENABLED));
                        mSharedPrefHelper.setMSkey(moodstocksJson.optString(Constants.KEY));
                        mSharedPrefHelper.setMSsecret(moodstocksJson.optString(Constants.SECRET));
                    }else{
                        //mSharedPrefHelper.setMSEnabled(Boolean.FALSE.toString());
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

    public void release(){
        mInstance = null;
    }
}
