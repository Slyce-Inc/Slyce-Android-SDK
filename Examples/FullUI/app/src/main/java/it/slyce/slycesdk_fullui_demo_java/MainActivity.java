package it.slyce.slycesdk_fullui_demo_java;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.SlyceCompletionHandler;
import it.slyce.sdk.SlyceSearchParameters;
import it.slyce.sdk.SlyceSession;
import it.slyce.sdk.SlyceUI;
import it.slyce.sdk.exception.SlyceError;

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

        Slyce.getInstance(this).open(SLYCE_ACCOUNT_ID, SLYCE_API_KEY, SLYCE_API_KEY, new SlyceCompletionHandler() {

            @Override
            public void onCompletion(@Nullable SlyceError slyceError) {
                if (slyceError != null) {
                    Toast.makeText(MainActivity.this, "Error opening Slyce: " + slyceError.getDetails(), Toast.LENGTH_SHORT).show();
                    return;
                }

                SlyceSession session = Slyce.getInstance(MainActivity.this).getDefaultSession();
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

                    // Lens IDs can be passed to full UI mode as a list, or as a single string
                    List<String> lensIds = new ArrayList<>();
                    lensIds.add(LENS_ID_BARCODE);
                    lensIds.add(LENS_ID_IMAGE_MATCH);
                    lensIds.add(LENS_ID_VISUAL_SEARCH);

                    SlyceUI.startCameraActivity(MainActivity.this, Slyce.getInstance(MainActivity.this), lensIds);
                    //SlyceUI.startCameraActivity(MainActivity.this, Slyce.getInstance(MainActivity.this), LENS_ID_UNIVERSAL);

                    // NOTE: If you wish to override the default item detail page with your
                    // own item detail page, you can provide a custom override of
                    // `SlyceCustomCameraActivity`, as shown below. Please see
                    // `CustomCameraActivity` as an example of how to re-direct to your
                    // desired detail page.
                    /*
                    try {
                        SlyceUI.startCustomCameraActivity(MainActivity.this, CustomCameraActivity.class.getName(), Slyce.getInstance(MainActivity.this), lensIds);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    */
                }
            }
        });
    }
}
