package it.slyce.slycesdk_fullui_demo_java;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.SlyceActivityMode;
import it.slyce.sdk.SlyceFragmentCloseDelegate;
import it.slyce.sdk.SlyceUI;

public class CustomHeaderFooterActivity extends AppCompatActivity {

    private final SlyceFragmentCloseDelegate fragmentCloseDelegate = new FragmentCloseDelegate();

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, CustomHeaderFooterActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_header_footer);

        try {
            new SlyceUI.FragmentLauncher(Slyce.getInstance(this), SlyceActivityMode.PICKER, R.id.fragment_container)
                    .fragmentCloseDelegate(fragmentCloseDelegate)
                    .launch(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class FragmentCloseDelegate implements SlyceFragmentCloseDelegate {

        @Override
        public void closeSlyceFragment() {
            finish();
        }
    }
}