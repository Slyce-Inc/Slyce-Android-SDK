package it.slyce.snippets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.SlyceSearchParameters;
import it.slyce.sdk.SlyceSearchRequest;
import it.slyce.sdk.SlyceSession;
import it.slyce.sdk.SlyceWorkflowNames;
import it.slyce.sdk.exception.SlyceInvalidSessionException;
import it.slyce.sdk.exception.SlyceMissingGDPRComplianceException;
import it.slyce.sdk.exception.SlyceNotOpenedException;
import it.slyce.sdk.exception.SlyceSearchTaskBuilderException;

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
     * Demonstrates how to use the find similar feature. This can be used with an item_id or
     * an url to am image of a product
     *
     * @param context
     */
    public void onFindSimilarClick(@NonNull Context context) {
        // A SlyceSession is needed.
        SlyceSession session = null;

        try {
            session = Slyce.getInstance(context).createSession();
        } catch (SlyceMissingGDPRComplianceException e) {
            e.printStackTrace();
        } catch (SlyceNotOpenedException e) {
            e.printStackTrace();
        }

        // The payload is what is used to compare against the the other products
        String payload = "http://inventory.store.com/product";

        // Set up a SlyceSearchRequest
        SlyceSearchRequest request = null;

        // Fill the request based on the type of payload.
        if (payload.startsWith("http")) {
            try {
                request = new SlyceSearchRequest.Builder().imageUrl(payload).build();
            } catch (SlyceSearchTaskBuilderException e) {
                e.printStackTrace();
            }
        } else {
            SlyceSearchParameters parameters = new SlyceSearchParameters();
            JSONObject workflowOptions = new JSONObject();
            try {
                workflowOptions.put("item_id", payload);
                parameters.setWorkflowOptions(workflowOptions);
                request = new SlyceSearchRequest.Builder().searchParameters(parameters).build();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (SlyceSearchTaskBuilderException e) {
                e.printStackTrace();
            }
        }

        // Start the search task and listen for the results with a slyceSearchTaskListener
        try {
            session.startSearchTask(request, SlyceWorkflowNames.WORKFLOW_FIND_SIMILAR, null);
        } catch (SlyceInvalidSessionException e) {
            e.printStackTrace();
        }
    }
}