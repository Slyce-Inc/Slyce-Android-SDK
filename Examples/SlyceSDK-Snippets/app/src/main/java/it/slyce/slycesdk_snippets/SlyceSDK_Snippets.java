package it.slyce.slycesdk_snippets;

import android.content.Context;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.exception.SlyceNotOpenedException;

public class SlyceSDK_Snippets {

    // Analytics
    /**
     * Demonstrates how to manually trigger analytics, for headless modes. This assumes that the 
     * Slyce instance has been configured and opened.
     * 
     * @param context
     * @throws {@link SlyceNotOpenedException}
     */
    void reportManualAnalytics(Context context) throws SlyceNotOpenedException {

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
}