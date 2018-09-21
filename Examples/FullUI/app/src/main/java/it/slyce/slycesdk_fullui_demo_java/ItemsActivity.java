package it.slyce.slycesdk_fullui_demo_java;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

public class ItemsActivity extends AppCompatActivity {

    private static final String KEY_ITEMS_JSON = "KEY_ITEMS_JSON";

    /**
     * Starts the Activity.
     *
     * @param context The context.
     * @param items   The items to display.
     */
    public static void startActivity(Context context, JSONArray items) {
        Intent intent = new Intent(context, ItemsActivity.class);

        try {
            intent.putExtra(KEY_ITEMS_JSON, items.toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items);

        TextView itemJson = findViewById(R.id.items_textView);
        itemJson.setText(getItemJson());
    }

    private String getItemJson() {
        String itemJson = null;

        Intent intent = getIntent();
        if (intent != null) {
            itemJson = intent.getStringExtra(KEY_ITEMS_JSON);
        }

        return itemJson != null ? itemJson : "";
    }
}
