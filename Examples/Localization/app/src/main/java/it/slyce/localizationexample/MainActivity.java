package it.slyce.localizationexample;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Toast;

import it.slyce.sdk.Slyce;
import it.slyce.sdk.SlyceActivityMode;
import it.slyce.sdk.SlyceCompletionHandler;
import it.slyce.sdk.SlyceUI;
import it.slyce.sdk.exception.SlyceError;
import it.slyce.sdk.exception.initialization.SlyceNotOpenedException;

public class MainActivity extends AppCompatActivity {

    private static final String SLYCE_ACCOUNT_ID = ""; // Account ID must be added here
    private static final String SLYCE_API_KEY = "";  // API Key myst be added here
    private static final String SLYCE_SPACE_ID = ""; // Space ID must be added here

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

                try {
                    new SlyceUI.ActivityLauncher(slyce, SlyceActivityMode.UNIVERSAL)
                            .launch(MainActivity.this);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (SlyceNotOpenedException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
