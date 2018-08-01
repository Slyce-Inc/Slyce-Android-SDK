package it.slyce.slycesdk_fullui_demo_java;

import it.slyce.sdk.SlyceCustomActivity;
import it.slyce.sdk.SlyceItemDescriptor;

public class CustomCameraActivity extends SlyceCustomActivity {

    @Override
    public boolean shouldDisplayDefaultItemDetailLayerForItem(SlyceItemDescriptor itemDescriptor) {
        ItemDetailActivity.startActivity(this, itemDescriptor.getItem());
        return false;
    }
}
