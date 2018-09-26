package it.slyce.slycesdk_fullui_demo_java;

import org.json.JSONArray;

import java.util.List;

import it.slyce.sdk.SlyceCustomActivity;
import it.slyce.sdk.SlyceItemDescriptor;

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
}
