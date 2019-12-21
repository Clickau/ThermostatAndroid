package com.clickau.thermostat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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

        final String urlStr = firebaseURLEditText.getText().toString();
        final String secret = secretKeyEditText.getText().toString();

        if (TextUtils.isEmpty(urlStr)) {
            Toast.makeText(getApplicationContext(), "Empty URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // secret has to be made up of 40 alphanumeric characters
        if (TextUtils.isEmpty(secret) || !secret.matches("[a-zA-Z0-9]{40}")) {
            Toast.makeText(getApplicationContext(), "Invalid Secret", Toast.LENGTH_SHORT).show();
            return;
        }

        final String url = urlStr + ".firebaseio.com";
        Log.d(TAG, String.format("Final URL: %s", url));

        FirebaseService.initialize(url, secret);
        FirebaseService.get(this, "", new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                Log.d(TAG, Integer.toString(resultCode));
                switch (resultCode) {
                    case FirebaseService.RESULT_SUCCESS:
                        Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                        // store url and secret
                        SharedPreferences pref = getSharedPreferences("firebase_credentials", Context.MODE_PRIVATE);
                        pref.edit()
                                .putString("url", url)
                                .putString("secret", secret)
                                .apply();
                        // close all activities and restart main activity
                        finishAffinity();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        break;
                    case FirebaseService.RESULT_DATABASE_NOT_FOUND:
                        Toast.makeText(getApplicationContext(), "Database not found. Check your URL", Toast.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_MALFORMED_URL:
                        Toast.makeText(getApplicationContext(), "Provided URL is invalid", Toast.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_UNAUTHORIZED:
                        Toast.makeText(getApplicationContext(), "Unauthorized to access database. Check your secret", Toast.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_IO_EXCEPTION:
                        Toast.makeText(getApplicationContext(), "IO Exception", Toast.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_SERVER_ERROR:
                        Toast.makeText(getApplicationContext(), "Server Error", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
    }

}