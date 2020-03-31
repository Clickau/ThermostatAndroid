package com.clickau.thermostat;

import android.app.Activity;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SetupFirebaseActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SetupFirebaseActivity";

    private TextInputEditText firebaseURLEditText;
    private TextInputEditText secretKeyEditText;
    private TextInputEditText schedulesPathEditText;
    private TextInputLayout firebaseURLInputLayout;
    private TextInputLayout secretKeyInputLayout;
    private TextInputLayout schedulesPathInputLayout;
    private Button submitButton;
    private ProgressBar progressBar;

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
        schedulesPathEditText = findViewById(R.id.setup_firebase_schedules_path);
        firebaseURLInputLayout = findViewById(R.id.setup_firebase_url_text_input_layout);
        secretKeyInputLayout = findViewById(R.id.setup_firebase_secret_key_text_input_layout);
        schedulesPathInputLayout = findViewById(R.id.setup_firebase_schedules_path_text_input_layout);
        submitButton = findViewById(R.id.setup_firebase_submit_button);
        progressBar = findViewById(R.id.setup_firebase_progress_bar);

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
                // must contain at least one alphanumeric character and optionally more alphanumeric characters and dashes
                if (TextUtils.isEmpty(s) || !s.toString().matches("[a-zA-Z0-9][a-zA-Z0-9-]*")) {
                    firebaseURLInputLayout.setError(getString(R.string.setup_firebase_invalid_url));
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
                // secret has to be made up of 40 alphanumeric characters
                if (TextUtils.isEmpty(s) || !s.toString().matches("[a-zA-Z0-9]{40}")) {
                    secretKeyInputLayout.setError(getString(R.string.setup_firebase_invalid_secret));
                } else {
                    secretKeyInputLayout.setError(null);
                }
            }
        });

        schedulesPathEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s) && !s.toString().matches("[^\\.\\$\\#\\[\\]]*(\\.json)?")) {
                    schedulesPathInputLayout.setError(getString(R.string.setup_firebase_invalid_schedules_path));
                } else {
                    schedulesPathInputLayout.setError(null);
                }
            }
        });

        progressBar.setVisibility(View.INVISIBLE);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onClick(View view) {

        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        @SuppressWarnings("ConstantConditions") // suppress null warning
        final String urlStr = firebaseURLEditText.getText().toString();
        @SuppressWarnings("ConstantConditions")
        final String secret = secretKeyEditText.getText().toString();
        @SuppressWarnings("ConstantConditions")
        final String schedulesPath = schedulesPathEditText.getText().toString();

        // must contain at least one alphanumeric character and optionally more alphanumeric characters and dashes
        if (TextUtils.isEmpty(urlStr) || !urlStr.matches("[a-zA-Z0-9][a-zA-Z0-9-]*")) {
            Snackbar.make(findViewById(android.R.id.content), R.string.setup_firebase_invalid_url, Snackbar.LENGTH_LONG).show();
            firebaseURLInputLayout.setError(getString(R.string.setup_firebase_invalid_url));
            return;
        }

        // secret has to be made up of 40 alphanumeric characters
        if (TextUtils.isEmpty(secret) || !secret.matches("[a-zA-Z0-9]{40}")) {
            Snackbar.make(findViewById(android.R.id.content), R.string.setup_firebase_invalid_secret, Snackbar.LENGTH_LONG).show();
            secretKeyInputLayout.setError(getString(R.string.setup_firebase_invalid_secret));
            return;
        }

        if (!TextUtils.isEmpty(schedulesPath) && !schedulesPath.matches("[^\\.\\$\\#\\[\\]]*(\\.json)?")) {
            Snackbar.make(findViewById(android.R.id.content), R.string.setup_firebase_invalid_schedules_path, Snackbar.LENGTH_LONG).show();
            schedulesPathInputLayout.setError(getString(R.string.setup_firebase_invalid_schedules_path));
            return;
        }

        submitButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        final String url = urlStr + ".firebaseio.com";

        FirebaseService.initialize(url, secret, schedulesPath);
        FirebaseService.getSchedules(this, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                Log.d(TAG, Integer.toString(resultCode));
                submitButton.setEnabled(true);
                progressBar.setVisibility(View.INVISIBLE);
                switch (resultCode) {
                    case FirebaseService.RESULT_SUCCESS:
                        Toast.makeText(getApplicationContext(), R.string.firebase_success, Toast.LENGTH_SHORT).show();
                        // the SnackBar wouldn't be visible when switching activities
                        //SnackBar.make(findViewById(android.R.id.content), R.string.setup_firebase_success, SnackBar.LENGTH_LONG).show();
                        // store url, secret and path to schedules
                        SharedPreferences pref = getSharedPreferences("firebase_credentials", Context.MODE_PRIVATE);
                        pref.edit()
                                .putString("url", url)
                                .putString("secret", secret)
                                .putString("schedules_path", schedulesPath)
                                .apply();
                        // close all activities and restart main activity
                        finishAffinity();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        return;
                    case FirebaseService.RESULT_MALFORMED_URL:
                    case FirebaseService.RESULT_DATABASE_NOT_FOUND:
                        Snackbar.make(findViewById(android.R.id.content), R.string.firebase_database_not_found, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_UNAUTHORIZED:
                        Snackbar.make(findViewById(android.R.id.content), R.string.firebase_unauthorized, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_IO_EXCEPTION:
                        Snackbar.make(findViewById(android.R.id.content), R.string.firebase_io_exception, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_SERVER_ERROR:
                        Snackbar.make(findViewById(android.R.id.content), R.string.firebase_server_error, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_TIMEOUT:
                        Snackbar.make(findViewById(android.R.id.content), R.string.firebase_timeout, Snackbar.LENGTH_SHORT).show();
                        break;
                }

                SharedPreferences pref = getSharedPreferences("firebase_credentials", Context.MODE_PRIVATE);
                final String oldUrl = pref.getString("url", null);
                final String oldSecret = pref.getString("secret", null);
                final String oldPath = pref.getString("schedules_path", null);
                FirebaseService.initialize(oldUrl, oldSecret, oldPath);
            }
        });
    }

}
