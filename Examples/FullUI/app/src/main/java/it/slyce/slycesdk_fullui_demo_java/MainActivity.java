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
import it.slyce.sdk.exception.SlyceNotOpenedException;

public class MainActivity extends AppCompatActivity {

    private static final String SLYCE_ACCOUNT_ID = "";
    private static final String SLYCE_API_KEY = "";
    private static final String SLYCE_SPACE_ID = "";

    private static final String LENS_ID_BARCODE = "slyce.1D";
    private static final String LENS_ID_IMAGE_MATCH = "slyce.2D";
    private static final String LENS_ID_VISUAL_SEARCH = "slyce.3D";
    private static final String LENS_ID_UNIVERSAL = "slyce.universal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Slyce should be opened once, generally at application startup. We're doing
        // it here for demo purposes.

        Slyce.getInstance(this).open(SLYCE_ACCOUNT_ID, SLYCE_API_KEY, SLYCE_SPACE_ID, new SlyceCompletionHandler() {

            @Override
            public void onCompletion(@Nullable SlyceError slyceError) {
                if (slyceError != null) {
                    Toast.makeText(MainActivity.this, "Error opening Slyce: " + slyceError.getDetails(), Toast.LENGTH_SHORT).show();
                    return;
                }

                Slyce slyce = Slyce.getInstance(MainActivity.this);

                // Uncomment the following lines to customize the Full UI experience using SlyceTheme
                /*
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
                    // This exception is thrown if theme is set before Slyce is opened
                }
                */

                SlyceSession session = slyce.getDefaultSession();
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

                    /*
                    try {

                         new SlyceUI.ActivityLauncher(slyce, SlyceActivityMode.PICKER)
                                .launch(MainActivity.this);

                        // new SlyceUI.ActivityLauncher(slyce, SlyceActivityMode.UNIVERSAL)
                        //         .launch(MainActivity.this);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    */

                    // NOTE: If you wish to override the default item detail page with your
                    // own item detail page, you can provide a custom override of
                    // `SlyceCustomCameraActivity` to `SlyceUI`, as shown below. Please see
                    // `CustomCameraActivity` (in this package) as an example of how to
                    // re-direct to your desired detail page.
                    /*

                    try {
                        new SlyceUI.ActivityLauncher(slyce, SlyceActivityMode.PICKER)
                                .customClassName(CustomCameraActivity.class.getName())
                                .launch(MainActivity.this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    */

                    // NOTE: If you wish to provide a custom header or footer around the SlyceUI,
                    // please see the `NestedUiExampleActivity`, which is launched by the line
                    // below.
                    //
                    // NestedUiExampleActivity.startActivity(MainActivity.this);
                }
            }
        });
    }
}
