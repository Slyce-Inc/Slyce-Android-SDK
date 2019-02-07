package it.slyce.slycesdk_find_similar;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.SlyceCompletionHandler;
import it.slyce.sdk.SlyceSearchParameters;
import it.slyce.sdk.SlyceSearchRequest;
import it.slyce.sdk.SlyceSearchResponse;
import it.slyce.sdk.SlyceSearchResponseUpdate;
import it.slyce.sdk.SlyceSearchTask;
import it.slyce.sdk.SlyceSearchTaskListener;
import it.slyce.sdk.SlyceSession;
import it.slyce.sdk.SlyceWorkflowNames;
import it.slyce.sdk.exception.SlyceError;
import it.slyce.sdk.exception.SlyceInvalidSessionException;
import it.slyce.sdk.exception.SlyceMissingGDPRComplianceException;
import it.slyce.sdk.exception.SlyceNotOpenedException;
import it.slyce.sdk.exception.SlyceSearchTaskBuilderException;

public class MainActivity extends AppCompatActivity implements SlyceSearchTaskListener {

    private static final String SLYCE_ACCOUNT_ID = "";
    private static final String SLYCE_API_KEY = "";
    private static final String SLYCE_SPACE_ID = "";

    private EditText input;
    private TextView findSimilarResult;
    private ProgressBar progressBar;
    private Button similarButton;

    private Slyce slyce;
    private SlyceSession session;

    private static String FindSimilarDemoLastTextKey = "FindSimilarDemoLastText";
    private static final String TAG = "Find Similar Example";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        input = findViewById(R.id.example_find_similar_input);
        findSimilarResult = findViewById(R.id.example_find_similar_result_text_view);
        progressBar = findViewById(R.id.example_find_similar_progress);
        similarButton = findViewById(R.id.example_find_similar_button);

        if(slyce == null){
            slyce = Slyce.getInstance(this);
        }

        if (!slyce.isOpen()) {
            Log.d(TAG, "Opening Slyce");
            slyce.open(SLYCE_ACCOUNT_ID, SLYCE_API_KEY, SLYCE_SPACE_ID, new SlyceCompletionHandler() {
                @Override
                public void onCompletion(@Nullable SlyceError slyceError) {
                    if (slyceError != null) {
                        Toast.makeText(MainActivity.this, "Error opening Slyce: " + slyceError.getDetails(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Slyce open failed");
                    } else {
                        Log.d(TAG, "Slyce open succeeded");
                    }

                    try {
                        session = Slyce.getInstance(getBaseContext()).createSession();
                    } catch (SlyceMissingGDPRComplianceException e) {
                        e.printStackTrace();
                    } catch (SlyceNotOpenedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        input.setText(getPreferences(Context.MODE_PRIVATE).getString(FindSimilarDemoLastTextKey, ""));

        similarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFindSimilarClick();
            }
        });
    }

    void onFindSimilarClick() {
        String payload = input.getText().toString();

        if (payload.isEmpty()) {
            findSimilarResult.setText("Payload can't be empty");
        } else {
            getPreferences(Context.MODE_PRIVATE).edit().putString(FindSimilarDemoLastTextKey, payload).apply();
        }

        SlyceSearchRequest request = null;

        if (payload.startsWith("http")) {
            try {
                request = new SlyceSearchRequest.Builder().imageUrl(payload).build();
            } catch (SlyceSearchTaskBuilderException e) {
                e.printStackTrace();
            }
        } else {
            SlyceSearchParameters parameters = new SlyceSearchParameters();
            JSONObject workflowOptions = new JSONObject();
            try {
                workflowOptions.put("item_id", payload);
                parameters.setWorkflowOptions(workflowOptions);
                request = new SlyceSearchRequest.Builder().searchParameters(parameters).build();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (SlyceSearchTaskBuilderException e) {
                e.printStackTrace();
            }
        }

        try {
            session.startSearchTask(request, SlyceWorkflowNames.WORKFLOW_FIND_SIMILAR, this);
            progressBar.setVisibility(View.VISIBLE);

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
        } catch (SlyceInvalidSessionException e) {
            findSimilarResult.setText("Failed to execute request");
            e.printStackTrace();
        }
    }

    @Override
    public void onSlyceSearchTaskFinished(SlyceSearchTask task, SlyceSearchResponse response) {
        progressBar.setVisibility(View.GONE);
        findSimilarResult.setText(response.getResults().get(0).getItems().toString());
    }

    @Override
    public void onSlyceSearchTaskFailed(SlyceSearchTask task, List<SlyceError> errors) {
        findSimilarResult.setText("Search failed");
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask slyceSearchTask, SlyceSearchResponseUpdate.JobCreated jobCreated) {

    }

    @Override
    public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask slyceSearchTask, SlyceSearchResponseUpdate.TagFound tagFound) {

    }

    @Override
    public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask slyceSearchTask, SlyceSearchResponseUpdate.ImageUploaded imageUploaded) {

    }

    @Override
    public void onSlyceSearchTaskReceivedUpdate(SlyceSearchTask slyceSearchTask, SlyceSearchResponseUpdate.ResultsReceived resultsReceived) {

    }
}
