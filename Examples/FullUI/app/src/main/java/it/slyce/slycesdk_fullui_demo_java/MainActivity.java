package it.slyce.slycesdk_fullui_demo_java;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.SlyceActivityMode;
import it.slyce.sdk.SlyceCompletionHandler;
import it.slyce.sdk.SlyceHeaderStyleCenter;
import it.slyce.sdk.SlyceHeaderStyleLeft;
import it.slyce.sdk.SlyceHeaderStyleRight;
import it.slyce.sdk.SlyceSearchParameters;
import it.slyce.sdk.SlyceSession;
import it.slyce.sdk.SlyceUI;
import it.slyce.sdk.exception.SlyceError;
import it.slyce.sdk.exception.initialization.SlyceMissingGDPRComplianceException;
import it.slyce.sdk.exception.initialization.SlyceNotOpenedException;

public class MainActivity extends AppCompatActivity {

    private static final String SLYCE_ACCOUNT_ID = "";
    private static final String SLYCE_API_KEY = "";
    private static final String SLYCE_SPACE_ID = "";


    private enum SlyceUIExampleType {
        DEFAULT,
        CUSTOM_HEADER_FOOTER, // use this for custom header / footer example
        CUSTOM_SEARCH_DETAIL, // use this for custom search detail example
        FRAGMENT // use this for slyce fragment container example
    }

    private boolean applyCustomTheme = false; // set to true to use custom theme values
    private SlyceUIExampleType exampleType = SlyceUIExampleType.FRAGMENT;

    private Slyce slyce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        slyce = Slyce.getInstance(this);

        // Slyce should be opened once, generally at application startup. We're doing
        // it here for demo purposes.
        slyce.open(SLYCE_ACCOUNT_ID, SLYCE_API_KEY, SLYCE_SPACE_ID, new SlyceCompletionHandler() {

            @Override
            public void onCompletion(@Nullable SlyceError slyceError) {
                if (slyceError != null) {
                    Toast.makeText(MainActivity.this, "Error opening Slyce: " + slyceError.getDetails(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (applyCustomTheme) {
                    applySlyceTheme();
                }

                SlyceSession session = null;
                try {
                    session = Slyce.getInstance(getBaseContext()).createSession();
                } catch (SlyceMissingGDPRComplianceException e) {
                    e.printStackTrace();
                } catch (SlyceNotOpenedException e) {
                    e.printStackTrace();
                }
                if (session != null) {

                    SlyceSearchParameters searchParams = new SlyceSearchParameters();

                    // All search parameters are optional and independent

                    // Country Code
                    //searchParams.setCountryCode("BE");

                    // Language Code
                    //searchParams.setLanguageCode("fr");

                    // Demo Mode. Set to `true` to receive test data
                    //searchParams.setDemoMode(true);

                    // Setting search parameters here will automatically include them for all
                    // SearchRequests in the session.
                    session.setDefaultSearchParameters(searchParams);

                    switch (exampleType) {

                        case DEFAULT:
                            launchDefault();
                            break;

                        case CUSTOM_HEADER_FOOTER:
                            launchWithCustomHeaderFooter();
                            break;

                        case CUSTOM_SEARCH_DETAIL:
                            launchWithCustomSearchDetail();
                            break;

                        case FRAGMENT:
                            SlyceFragmentContainerActivity.startActivity(MainActivity.this, SlyceActivityMode.UNIVERSAL);
                    }
                }
            }
        });
    }

    private void applySlyceTheme() {
        try {

            // - Header
            slyce.getTheme().setAppearanceStyle("appearance_headerStyle_left", SlyceHeaderStyleLeft.BACK_BUTTON);
            slyce.getTheme().setAppearanceStyle("appearance_headerStyle_center", SlyceHeaderStyleCenter.TITLE);
            slyce.getTheme().setAppearanceStyle("appearance_headerStyle_right", SlyceHeaderStyleRight.HIDDEN);

            // - Coaching Tips
            slyce.getTheme().setString("string_coachingTip_headline_visualSearch", "Your custom headline here");
            slyce.getTheme().setString("string_coachingTip_body_visualSearch", "Your custom subhead here");
            slyce.getTheme().setResourceId("bg_coachingTip_visualSearch", R.drawable.your_drawable_here);

        } catch (SlyceNotOpenedException e) {
            e.printStackTrace();
        }
    }

    private void launchDefault() {
        try {

            new SlyceUI.ActivityLauncher(slyce, SlyceActivityMode.PICKER)
                    .launch(MainActivity.this);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SlyceNotOpenedException e) {
            e.printStackTrace();
        }
    }

    private void launchWithCustomHeaderFooter() {

        // navigate to `CustomHeaderFooterActivity` for implementation details
        CustomHeaderFooterActivity.startActivity(this);
    }

    private void launchWithCustomSearchDetail() {
        try {

            // navigate to `CustomSearchDetailActivity` for example of how to extend
            // `SlyceCustomActivity`
            new SlyceUI.ActivityLauncher(slyce, SlyceActivityMode.PICKER)
                    .customClassName(CustomSearchDetailActivity.class.getName())
                    .launch(this);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SlyceNotOpenedException e) {
            e.printStackTrace();
        }
    }
}
