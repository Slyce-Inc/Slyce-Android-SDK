package it.slyce.slycesdk_fullui_demo_java;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.SlyceActivityMode;
import it.slyce.sdk.SlyceFragmentCloseDelegate;
import it.slyce.sdk.SlyceUI;
import it.slyce.sdk.ui.SlyceFragment;


public class SlyceFragmentContainerActivity extends AppCompatActivity {

    private static final String FRAGMENT_TAG = "FRAGMENT_TAG";

    private static final String KEY_MODE = "KEY_MODE";

    private final SlyceFragmentCloseDelegate fragmentCloseDelegate = new FragmentCloseDelegate();


    public static void startActivity(Context context, SlyceActivityMode mode) {
        Intent intent = new Intent(context, SlyceFragmentContainerActivity.class);
        intent.putExtra(KEY_MODE, mode.toString());
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slyce_fragment_container_activity);

        if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG) == null) {
            try {
                Slyce slyce = Slyce.getInstance(this);
                new SlyceUI.FragmentLauncher(slyce, getMode(), R.id.slyce_fragment_container)
                        .fragmentCloseDelegate(fragmentCloseDelegate)
                        .fragmentTag(FRAGMENT_TAG)
                        .launch(this);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
            if (fragment != null) {
                ((SlyceFragment) fragment).setSlyceFragmentCloseDelegate(fragmentCloseDelegate);
            }
        }
    }


    @Override
    public void onBackPressed() {
        // NOTE: If you want to forward device back navigation events to the SlyceFragment,
        // you will need to get the Fragment by the Fragment tag that you (optionally) passed in
        // when launching SlyceFragment.
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment != null) {
            ((SlyceFragment) fragment).onBackPressed();
        } else {
            super.onBackPressed();
        }
    }


    private SlyceActivityMode getMode() {
        Intent intent = getIntent();
        if (intent != null) {
            String mode = intent.getStringExtra(KEY_MODE);
            if (!TextUtils.isEmpty(mode)) {
                return SlyceActivityMode.valueOf(mode);
            }
        }

        throw new Error("Mode cannot be null");
    }


    private class FragmentCloseDelegate implements SlyceFragmentCloseDelegate {

        @Override
        public void closeSlyceFragment() {
            finish();
        }
    }
}
