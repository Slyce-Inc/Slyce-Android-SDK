package it.slyce.snippets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.SlyceSearchParameters;
import it.slyce.sdk.SlyceSearchRequest;
import it.slyce.sdk.SlyceSession;

import static it.slyce.sdk.SlyceOptions.KEY_CAPTURE_MODE;
import static it.slyce.sdk.SlyceOptions.KEY_LENSES;
import static it.slyce.sdk.SlyceOptions.LensCaptureMode.LEGACY_MULTI;

public class SlyceSDK_Snippets {

    // Search Parameters

    /**
     * Demonstrates how to add custom workflow options for a single search task. This assumes that
     * the Slyce instance has been configured and opened.
     *
     * @param context
     * @throws {@link Exception}
     */
    public void addWorkflowOptionsForSingleTask(@NonNull Context context) throws Exception {

        // set up some example data
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.sample_img);

        // create a `SlyceSearchParameters` object and set the workflow options using a JSONObject.
        SlyceSearchParameters searchParams = new SlyceSearchParameters();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key1", "value1");
        jsonObject.put("key2", "value2");
        searchParams.setWorkflowOptions(jsonObject);

        SlyceSearchRequest request = new SlyceSearchRequest.Builder()
                .bitmap(bitmap)
                .searchParameters(searchParams)
                .build();

        SlyceSession defaultSession = Slyce.getInstance(context).getDefaultSession();
        if (defaultSession == null) {
            // handle error
        } else {
            defaultSession.startSearchTask(request, "your workflow id", null);
        }
    }

    /**
     * Demonstrates how to add default workflow options to a session. This assumes that the Slyce
     * instance has been configured and opened.
     *
     * @param context
     * @throws {@link Exception}
     */
    public void addDefaultWorkflowOptions(@NonNull Context context) throws Exception {

        // create a `SlyceSearchParameters` object and set the workflow options using a JSONObject.
        SlyceSearchParameters searchParams = new SlyceSearchParameters();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key1", "value1");
        jsonObject.put("key2", "value2");
        searchParams.setWorkflowOptions(jsonObject);

        // add to a custom session
        SlyceSession session = Slyce.getInstance(context).createSession();
        session.setDefaultSearchParameters(searchParams);

        // or alternatively, add to the default session
        SlyceSession defaultSession = Slyce.getInstance(context).getDefaultSession();
        if (defaultSession == null) {
            // handle error
        } else {
            defaultSession.setDefaultSearchParameters(searchParams);
        }
    }

    // Analytics

    /**
     * Demonstrates how to manually trigger analytics, for headless modes. This assumes that the
     * Slyce instance has been configured and opened.
     *
     * @param context
     * @throws {@link Exception}
     */
    public void reportManualAnalytics(Context context) throws Exception {

        // set up some simple example variables
        String jobId = "abc";
        String itemId = "123";
        String itemRevenue = "$9.99";
        String itemURL = "https://store.com/123";
        int itemQuantity = 1;

        // track that an image was captured outside of the SlyceSDK
        Slyce.getInstance(context).getEventTracker().trackCaptureImage();

        // track add to cart
        Slyce.getInstance(context).getEventTracker().trackAddToCartTap(jobId, itemId, itemRevenue, itemURL, itemQuantity);

        // track successful checkout
        Slyce.getInstance(context).getEventTracker().trackCheckoutTap(jobId, itemId, itemRevenue, itemURL,itemQuantity);
    }

    /**
     * Demonstrates how to use legacy multi search in universal mode. Add it as an option in
     * the lens.
     *
     */
    public HashMap<String, Object> getAdditionalOptions() {
        String LENS_ID_UNIVERSAL = "slyce.universal";

        HashMap<String, Object> lensOptions = new HashMap<>();

        // Set universal to legacy multi search
        HashMap<String, Object> lensOptionsUniversal = new HashMap<>();
        lensOptionsUniversal.put(KEY_CAPTURE_MODE, LEGACY_MULTI);
        lensOptions.put(LENS_ID_UNIVERSAL, lensOptionsUniversal);

        // Add lens options to parent options map
        HashMap<String, Object> options = new HashMap<>();
        options.put(KEY_LENSES, lensOptions);

        return options;
    }

}