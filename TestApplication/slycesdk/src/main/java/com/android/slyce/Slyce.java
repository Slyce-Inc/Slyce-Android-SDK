package com.android.slyce;

import android.content.Context;
import com.android.slyce.communication.ComManager;
import com.android.slyce.utils.Constants;
import com.android.slyce.utils.SharedPrefHelper;
import com.android.slyce.utils.Utils;
import com.android.slyce.report.mpmetrics.MixpanelAPI;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by davidsvilem on 3/25/15.
 */
public final class Slyce{

    private final String TAG = Slyce.class.getSimpleName();

    private static Slyce mInstance;

    private SharedPrefHelper mSharedPrefHelper;

    private Context mContext;

    public static Slyce getInstance(Context context, String clientID) {

        if(mInstance == null){
            synchronized (Slyce.class){
                if(mInstance == null){
                    mInstance = new Slyce(context, clientID);
                }
            }
        }
        return mInstance;
    }

    private Slyce(Context context, String clientID) {

        // Get Mixpanel object
        final MixpanelAPI mixpanel = MixpanelAPI.getInstance(context.getApplicationContext(), Constants.MIXPANEL_TOKEN);

        mContext = context;

        // Get shared preferences
        mSharedPrefHelper = SharedPrefHelper.getInstance(context);

        // Save the client id
        mSharedPrefHelper.setClientID(clientID);

        // Get client info
        ComManager.getInstance().getClientIDInfo(clientID, new ComManager.OnResponseListener() {
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
                    }

                }else{
                    // Client info did not returned with a valid response
                    mixpanel.track(Constants.SDK_INIT_FAILED, null);
                }
            }
        });

        JSONObject peopleProperties = new JSONObject();

        mixpanel.registerSuperProperties(peopleProperties);

        JSONObject peopleAnalytics = new JSONObject();

        try {

            mixpanel.getPeople().identify(mixpanel.getDistinctId());

            peopleAnalytics.put(Constants.USER_ID, mixpanel.getDistinctId());
            peopleAnalytics.put(Constants.MP_CLIENT_ID, getClientID());
            peopleAnalytics.put(Constants.NAME, Utils.getDeviceModel());
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

            // Report time stamp
            JSONObject created = new JSONObject();
            created.put(Constants.CREATED, Utils.getTimestamp());
            mixpanel.registerSuperPropertiesOnce(created);

            mixpanel.track(Constants.SDK_INIT_SUCCEEDED, null);

        } catch (JSONException e) {
        }
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
}
