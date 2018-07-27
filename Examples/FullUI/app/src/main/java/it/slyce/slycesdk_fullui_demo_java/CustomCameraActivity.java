package it.slyce.slycesdk_fullui_demo_java;

import it.slyce.sdk.SlyceCustomCameraActivity;
import it.slyce.sdk.SlyceItemDescriptor;

public class CustomCameraActivity extends SlyceCustomCameraActivity {

    @Override
    public boolean shouldDisplayDefaultItemDetailLayerForItem(SlyceItemDescriptor itemDescriptor) {
        ItemDetailActivity.startActivity(this, itemDescriptor.getItem());
        return false;
    }
}
