package it.slyce.slycesdk_fullui_demo_java;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class ItemDetailActivity extends AppCompatActivity {

    private static final String KEY_ITEM_JSON = "KEY_ITEM_JSON";

    /**
     * Starts the Activity.
     *
     * @param context The context.
     * @param item    The item to display.
     */
    public static void startActivity(Context context, JSONObject item) {
        Intent intent = new Intent(context, ItemDetailActivity.class);

        try {
            intent.putExtra(KEY_ITEM_JSON, item.toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        TextView itemJson = findViewById(R.id.itemDetail_textView);
        itemJson.setText(getItemJson());
    }

    private String getItemJson() {
        String itemJson = null;

        Intent intent = getIntent();
        if (intent != null) {
            itemJson = intent.getStringExtra(KEY_ITEM_JSON);
        }

        return itemJson != null ? itemJson : "";
    }
}
