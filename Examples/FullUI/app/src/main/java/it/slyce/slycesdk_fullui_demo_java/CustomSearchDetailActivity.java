package it.slyce.slycesdk_fullui_demo_java;

import android.support.annotation.NonNull;

import org.json.JSONArray;

import java.util.List;

import it.slyce.sdk.SlyceCustomActivity;
import it.slyce.sdk.SlyceItemDescriptor;
import it.slyce.sdk.SlyceSession;

public class CustomSearchDetailActivity extends SlyceCustomActivity {

    @Override
    public boolean shouldDisplayDefaultItemDetailLayerForItem(SlyceItemDescriptor itemDescriptor) {

        // to display your custom detail layer for the selected item,
        // handle that here and return false.
        ItemDetailActivity.startActivity(this, itemDescriptor.getItem());
        return false;
    }

    @Override
    public boolean shouldDisplayDefaultListLayerForItems(List<SlyceItemDescriptor> itemDescriptors) {

        // to display your custom list layer for the search result items,
        // handle that here and return false.
        JSONArray items = new JSONArray();

        for (SlyceItemDescriptor itemDescriptor : itemDescriptors) {
            items.put(itemDescriptor.getItem());
        }

        ItemsActivity.startActivity(this, items);
        return false;
    }

    @Override
    public void didOpenSession(@NonNull SlyceSession slyceSession) {
        super.didOpenSession(slyceSession);
        // All search parameters are optional and independent

        // Country Code
        //searchParams.setCountryCode("BE");

        // Language Code
        //searchParams.setLanguageCode("fr");

        // Demo Mode. Set to `true` to receive test data
        //searchParams.setDemoMode(true);

        // Setting search parameters here will automatically include them for all
        // SearchRequests in the session.
    }
}
