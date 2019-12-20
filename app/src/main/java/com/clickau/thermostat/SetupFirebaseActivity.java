package com.clickau.thermostat;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class SetupFirebaseActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SetupFirebaseActivity";

    private TextInputEditText firebaseURLEditText;
    private TextInputEditText secretKeyEditText;
    private TextInputLayout firebaseURLInputLayout;
    private TextInputLayout secretKeyInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_firebase);
        Toolbar toolbar = findViewById(R.id.setup_firebase_toolbar);
        setSupportActionBar(toolbar);

        // if activity was opened manually by the user from the more menu, show the back button in action bar
        String openMethod = getIntent().getStringExtra("openMethod");
        if (openMethod != null && openMethod.equals("manual")) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.setDisplayHomeAsUpEnabled(true);
        }


        firebaseURLEditText = findViewById(R.id.setup_firebase_url);
        secretKeyEditText = findViewById(R.id.setup_firebase_secret_key);
        firebaseURLInputLayout = findViewById(R.id.setup_firebase_url_text_input_layout);
        secretKeyInputLayout = findViewById(R.id.setup_firebase_secret_key_text_input_layout);
        Button submitButton = findViewById(R.id.setup_firebase_submit_button);

        submitButton.setOnClickListener(this);

        firebaseURLEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    firebaseURLInputLayout.setError("Empty URL");
                } else {
                    firebaseURLInputLayout.setError(null);
                }
            }
        });

        secretKeyEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s) || !s.toString().matches("[a-zA-Z0-9]{40}")) {
                    secretKeyInputLayout.setError("Invalid Secret");
                } else {
                    secretKeyInputLayout.setError(null);
                }
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onClick(View v) {

        String urlStr = firebaseURLEditText.getText().toString();
        String secret = secretKeyEditText.getText().toString();

        if (TextUtils.isEmpty(urlStr)) {
            Toast.makeText(getApplicationContext(), "Empty URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // secret has to be made up of 40 alphanumeric characters
        if (TextUtils.isEmpty(secret) || !secret.matches("[a-zA-Z0-9]{40}")) {
            Toast.makeText(getApplicationContext(), "Invalid Secret", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder urlBuilder = new StringBuilder();

        urlBuilder.append("https://");
        urlBuilder.append(urlStr);
        urlBuilder.append(".firebaseio.com/.json?auth=");
        urlBuilder.append(secret);

        Log.d(TAG, String.format("Final URL: %s", urlBuilder.toString()));

        Intent intent = new Intent(Intent.ACTION_SYNC, null, this, FirebaseIntentService.class);
        intent.putExtra("urlStr", urlBuilder.toString());
        startService(intent);

    }


    public static class FirebaseIntentService extends IntentService {

        public FirebaseIntentService() {
            super(FirebaseIntentService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(@Nullable Intent intent) {
            if (intent == null) return;
            String urlStr = intent.getStringExtra("urlStr");

            URL url;
            try {
                url = new URL(urlStr);
            } catch(MalformedURLException e) {
                e.printStackTrace();
                return;
            }

            try {
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                switch (responseCode) {
                    case HttpsURLConnection.HTTP_OK:
                        Log.d(TAG, "OK");
                        break;
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        // problem with auth
                        Log.d(TAG, "Unauthorized");
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        // problem with url
                        Log.d(TAG, "Not Found");
                        break;
                    default:
                        Log.d(TAG, String.format("Error: %d", responseCode));
                        break;
                }

                InputStream in = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                Log.d(TAG, result.toString());


            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

        }
    }
}
