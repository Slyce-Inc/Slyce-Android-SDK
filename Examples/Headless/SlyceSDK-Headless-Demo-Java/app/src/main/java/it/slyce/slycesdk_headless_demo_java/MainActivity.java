package it.slyce.slycesdk_headless_demo_java;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.GsonBuilder;

import java.util.List;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.SlycePrivacyPolicy;
import it.slyce.sdk.SlyceSearchRequest;
import it.slyce.sdk.SlyceSearchResponse;
import it.slyce.sdk.SlyceSearchResponseUpdate;
import it.slyce.sdk.SlyceSearchTask;
import it.slyce.sdk.SlyceSearchTaskListenerAdapter;
import it.slyce.sdk.SlyceSession;
import it.slyce.sdk.exception.SlyceError;
import it.slyce.sdk.exception.SlyceException;
import it.slyce.sdk.exception.SlyceNotOpenedException;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements CameraResultDisplayListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Add your Slyce account credentials here.

    private static final String SLYCE_ACCOUNT_ID = "";
    private static final String SLYCE_API_KEY = "";
    private static final String SLYCE_SPACE_ID = "";
    private static final String SLYCE_VISUAL_SEARCH_WORKFLOW_ID = "";
    private static final String SLYCE_LENS_ID_BARCODE = "slyce.1D";
    private static final String SLYCE_LENS_ID_IMAGE_MATCH = "slyce.2D";

    private boolean requireGDPR;
    private CameraResultFragment fragment;
    private ProgressBar openProgressBar;
    private ViewGroup barcodeButton;
    private ViewGroup imageMatchButton;
    private ViewGroup openButton;
    private ViewGroup visualSearchButton;

    // These methods are used to handle the results and updates from Slyce after starting a search.
    // Here, we only display the raw data in the text view, however, in production,
    // a view would be rendered to showcase the output.

    private SlyceSearchTaskListenerAdapter slyceSearchTaskListenerAdapter = new SlyceSearchTaskListenerAdapter() {

        @Override
        public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask task, SlyceSearchResponseUpdate.JobCreated update) {
            if (fragment != null) {
                fragment.addSearchUpdate("Job Created", (update != null ? new GsonBuilder().setPrettyPrinting().create().toJson(update) : "null"));
            }
        }

        @Override
        public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask task, SlyceSearchResponseUpdate.TagFound update) {
            if (fragment != null) {
                fragment.addSearchUpdate("Tag Found", (update != null ? new GsonBuilder().setPrettyPrinting().create().toJson(update) : "null"));
            }
        }

        @Override
        public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask task, SlyceSearchResponseUpdate.ResultsReceived update) {
            if (fragment != null) {
                fragment.addSearchUpdate("Result Received", (update != null ? new GsonBuilder().setPrettyPrinting().create().toJson(update) : "null"));
            }
        }

        @Override
        public void onSlyceSearchTaskFinished(SlyceSearchTask task, SlyceSearchResponse response) {
            if (fragment != null) {
                fragment.addSearchUpdate("Finished", (response != null ? new GsonBuilder().setPrettyPrinting().create().toJson(response) : "null"));
                fragment.notifySearchComplete();
            }
        }

        @Override
        public void onSlyceSearchTaskFailed(SlyceSearchTask task, List<SlyceError> errors) {
            if (fragment != null) {
                fragment.addSearchUpdate("Failed", (errors != null ? new GsonBuilder().setPrettyPrinting().create().toJson(errors) : "null"));
                fragment.notifySearchComplete();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barcodeButton = findViewById(R.id.barcode_button);
        barcodeButton.setOnClickListener(v -> CameraActivity.startActivity(MainActivity.this, SLYCE_LENS_ID_BARCODE));

        imageMatchButton = findViewById(R.id.image_match_button);
        imageMatchButton.setOnClickListener(v -> CameraActivity.startActivity(MainActivity.this, SLYCE_LENS_ID_IMAGE_MATCH));

        openProgressBar = findViewById(R.id.open_progress_bar);
        openProgressBar.setVisibility(GONE);
        openButton = findViewById(R.id.open_button);
        openButton.setOnClickListener(v -> {
            openProgressBar.setVisibility(VISIBLE);

            // GDPR Compliance support (optional)
            if (requireGDPR) {
                Slyce.getInstance(this).getGDPRComplianceManager().setUserRequiresGDPRCompliance(true);
            }

            Slyce.getInstance(this).open(SLYCE_ACCOUNT_ID, SLYCE_API_KEY, SLYCE_SPACE_ID, slyceError -> {
                openProgressBar.setVisibility(GONE);
                toggleOptions();
                if (slyceError != null) {
                    Toast.makeText(MainActivity.this, "Error opening Slyce.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (requireGDPR) {
                    try {

                        SlycePrivacyPolicy privacyPolicy = Slyce.getInstance(this).getGDPRComplianceManager().getPrivacyPolicy();

                        // Here you are required to display the information in the Privacy Policy and capture
                        // the user's consesnt. See documentation for `SlycePrivacyPolicy`.

                        // If user consensents...
                        Slyce.getInstance(this).getGDPRComplianceManager().setUserDidConsent(privacyPolicy);

                    } catch (SlyceNotOpenedException e) {
                        // failed to retrieve privacy policy
                    }
                }
            });
        });

        visualSearchButton = findViewById(R.id.visual_search_button);
        visualSearchButton.setOnClickListener(v -> {
            SlyceSession slyceSession = Slyce.getInstance(MainActivity.this).getDefaultSession();
            if (slyceSession != null) {
                try {

                    // We create a new request, which encapsulates the payload needed to start a
                    // visual search. Instead of attaching an bitmap, an imageURI or imageURL may
                    // also be used. Anchor is optional, and represents the tap point relative to
                    // the coordinates of the image.

                    SlyceSearchRequest request = new SlyceSearchRequest.Builder()
                            .bitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hammer))
                            .build();

                    // In order to receive updates after starting the task, we MUST add a
                    // SlyceSearchTaskListener (or SlyceSearchTaskListenerAdapter), which will be
                    // called whenever there are errors, updates, or a completed result.

                    slyceSession.startSearchTask(request, SLYCE_VISUAL_SEARCH_WORKFLOW_ID, slyceSearchTaskListenerAdapter);

                    fragment = CameraResultFragment.newInstanceWithImage(R.drawable.hammer);
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.camera_result_fragment_container, fragment)
                            .addToBackStack(null)
                            .commitAllowingStateLoss();
                } catch (SlyceException e) {
                    Log.d(TAG, "onClick", e);
                }
            }
        });

        toggleOptions();
    }

    @Override
    public void onBackPressed() {
        if (fragment != null && fragment.isAdded()) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCameraResultDisplayClosed() {
        getSupportFragmentManager().popBackStack();
    }

    private void toggleOptions() {
        boolean slyceIsOpen = Slyce.getInstance(this).isOpen();

        // Display only before Slyce is opened

        openButton.setVisibility(slyceIsOpen ? GONE : VISIBLE);

        // Display only after Slyce is opened

        barcodeButton.setVisibility(slyceIsOpen ? VISIBLE : GONE);
        imageMatchButton.setVisibility(slyceIsOpen ? VISIBLE : GONE);
        visualSearchButton.setVisibility(slyceIsOpen ? VISIBLE : GONE);
    }
}
