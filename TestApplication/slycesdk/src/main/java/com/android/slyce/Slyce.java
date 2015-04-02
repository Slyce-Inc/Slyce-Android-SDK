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
                if(jsonResponse != null && jsonResponse.optString("status").equalsIgnoreCase("success")){

                    mSharedPrefHelper.setPremium(jsonResponse.optString("premium"));

                    JSONObject moodstocksJson = jsonResponse.optJSONObject("ms");

                    if(moodstocksJson != null){
                        mSharedPrefHelper.setMSEnabled(moodstocksJson.optString("enabled"));
                        mSharedPrefHelper.setMSkey(moodstocksJson.optString("key"));
                        mSharedPrefHelper.setMSsecret(moodstocksJson.optString("secret"));
                    }

                }else{
                    // Client info did not returned with a valid response
                    mixpanel.track("SDK.Init.Failed", null);
                }
            }
        });

        JSONObject peopleProperties = new JSONObject();

        mixpanel.registerSuperProperties(peopleProperties);

        JSONObject peopleAnalytics = new JSONObject();
        try {

            mixpanel.getPeople().identify(mixpanel.getDistinctId());

            peopleAnalytics.put("userID", mixpanel.getDistinctId());
            peopleAnalytics.put("clientID", getClientID());
            peopleAnalytics.put("name", Utils.getDeviceModel());
            peopleAnalytics.put("deviceType", Utils.getDeviceManufacturer() + " " + Utils.getDeviceModel());
            peopleAnalytics.put("deviceName", "...");
            peopleAnalytics.put("systemType", "Android");
            peopleAnalytics.put("systemVersion", Utils.getOSVersion());
            peopleAnalytics.put("hostingAppName", Utils.getHostAppName(context));
            peopleAnalytics.put("hostingAppVersion", Utils.getHostAppVersion(context));
            peopleAnalytics.put("SDKVersion", Constants.SDK_VERSION);

            // Make it user profile
            mixpanel.getPeople().set(peopleAnalytics);

            // Make it super property
            mixpanel.registerSuperProperties(peopleAnalytics);

            // Report time stamp
            JSONObject created = new JSONObject();
            created.put("created", Utils.getTimestamp());
            mixpanel.registerSuperPropertiesOnce(created);

            mixpanel.track("SDK.Init.Succeeded", null);

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
