package it.slyce.slycesdk_fullui_demo_java;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.SlyceActivityMode;
import it.slyce.sdk.SlyceUI;

public class NestedUiExampleActivity extends AppCompatActivity {

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, NestedUiExampleActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nested_ui_example);

        try {
            new SlyceUI.FragmentLauncher(Slyce.getInstance(this), SlyceActivityMode.PICKER, R.id.fragment_container)
                    .launch(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}