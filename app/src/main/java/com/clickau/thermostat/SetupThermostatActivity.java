package com.clickau.thermostat;

import android.app.Activity;
import android.app.IntentService;
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
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;


public class SetupThermostatActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SetupThermostatActivity";

    private TextInputEditText ipEditText;
    private TextInputEditText ssidEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText timezoneEditText;
    private TextInputLayout ipTextInputLayout;
    private TextInputLayout ssidTextInputLayout;
    private TextInputLayout passwordTextInputLayout;
    private TextInputLayout timezoneTextInputLayout;
    private Button submitButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_thermostat);

        submitButton = findViewById(R.id.setup_thermostat_submit_button);
        ipEditText = findViewById(R.id.setup_thermostat_ip_edit_text);
        ssidEditText = findViewById(R.id.setup_thermostat_ssid_edit_text);
        passwordEditText = findViewById(R.id.setup_thermostat_password_edit_text);
        timezoneEditText = findViewById(R.id.setup_thermostat_timezone_edit_text);
        ipTextInputLayout = findViewById(R.id.setup_thermostat_ip_text_input_layout);
        ssidTextInputLayout = findViewById(R.id.setup_thermostat_ssid_text_input_layout);
        passwordTextInputLayout = findViewById(R.id.setup_thermostat_password_text_input_layout);
        timezoneTextInputLayout = findViewById(R.id.setup_thermostat_timezone_input_layout);
        progressBar = findViewById(R.id.setup_thermostat_progress_bar);
        Toolbar toolbar = findViewById(R.id.setup_thermostat_toolbar);
        setSupportActionBar(toolbar);

        // display the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        progressBar.setVisibility(View.INVISIBLE);

        ipEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s) || !Patterns.IP_ADDRESS.matcher(s).matches())
                    ipTextInputLayout.setError(getString(R.string.setup_thermostat_ip_invalid_error));
                else
                    ipTextInputLayout.setError(null);
            }
        });

        ssidEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s))
                    ssidTextInputLayout.setError(getString(R.string.setup_thermostat_ssid_empty_error));
                else
                    ssidTextInputLayout.setError(null);
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s))
                    passwordTextInputLayout.setError(getString(R.string.setup_thermostat_password_empty_error));
                else
                    passwordTextInputLayout.setError(null);
            }
        });

        timezoneEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s))
                    timezoneTextInputLayout.setError(getString(R.string.setup_thermostat_timezone_empty_error));
                else
                    timezoneTextInputLayout.setError(null);
            }
        });

        submitButton.setOnClickListener(this);
    }

    // when pressing the back button in the action bar, it will not restart the main activity, just go back to it
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    // happens when the user clicks the Submit button
    @Override
    public void onClick(View view) {

        // hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        @SuppressWarnings("ConstantConditions")
        String ip = ipEditText.getText().toString();
        @SuppressWarnings("ConstantConditions")
        String ssid = ssidEditText.getText().toString();
        @SuppressWarnings("ConstantConditions")
        String password = passwordEditText.getText().toString();
        @SuppressWarnings("ConstantConditions")
        String timezone = timezoneEditText.getText().toString();

        SharedPreferences pref = getSharedPreferences("firebase_credentials", Context.MODE_PRIVATE);
        final String firebaseUrl = pref.getString("url", null);
        final String firebaseSecret = pref.getString("secret", null);
        if (firebaseSecret == null || firebaseUrl == null) {
            Log.e(TAG, getString(R.string.setup_thermostat_firebase_not_init));
            Snackbar.make(findViewById(android.R.id.content), R.string.setup_thermostat_firebase_not_init, Snackbar.LENGTH_LONG).show();
            return;
        }

        // ip address is empty or invalid
        if (TextUtils.isEmpty(ip) || !Patterns.IP_ADDRESS.matcher(ip).matches())
        {
            Log.e(TAG, getString(R.string.setup_thermostat_ip_invalid_error));
            Snackbar.make(findViewById(android.R.id.content), R.string.setup_thermostat_ip_invalid_error, Snackbar.LENGTH_LONG).show();
            ipTextInputLayout.setError(getString(R.string.setup_thermostat_ip_invalid_error));
            return;
        }
        if (TextUtils.isEmpty(ssid))
        {
            Log.e(TAG, getString(R.string.setup_thermostat_ssid_empty_error));
            Snackbar.make(findViewById(android.R.id.content), R.string.setup_thermostat_ssid_empty_error, Snackbar.LENGTH_LONG).show();
            ssidTextInputLayout.setError(getString(R.string.setup_thermostat_ssid_empty_error));
            return;
        }
        if (TextUtils.isEmpty(password))
        {
            Log.e(TAG, getString(R.string.setup_thermostat_password_empty_error));
            Snackbar.make(findViewById(android.R.id.content), R.string.setup_thermostat_password_empty_error, Snackbar.LENGTH_LONG).show();
            passwordTextInputLayout.setError(getString(R.string.setup_thermostat_password_empty_error));
            return;
        }
        if (TextUtils.isEmpty(timezone)) {
            Log.e(TAG, getString(R.string.setup_thermostat_timezone_empty_error));
            Snackbar.make(findViewById(android.R.id.content), R.string.setup_thermostat_timezone_empty_error, Snackbar.LENGTH_LONG).show();
            timezoneTextInputLayout.setError(getString(R.string.setup_thermostat_timezone_empty_error));
            return;
        }

        submitButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        ResultReceiver receiver = new ResultReceiver(new Handler()) {

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {

                switch (resultCode) {
                    case SendRequestIntentService.SUCCESS:
                        Snackbar.make(findViewById(android.R.id.content), R.string.setup_thermostat_success_toast, Snackbar.LENGTH_LONG).show();
                        break;
                    case SendRequestIntentService.BAD_IP:
                        Snackbar.make(findViewById(android.R.id.content), R.string.setup_thermostat_bad_ip_toast, Snackbar.LENGTH_LONG).show();
                        break;
                    case SendRequestIntentService.BAD_SERVER_RESPONSE:
                        Snackbar.make(findViewById(android.R.id.content), R.string.setup_thermostat_bad_server_response_toast, Snackbar.LENGTH_LONG).show();
                        break;
                    case SendRequestIntentService.IO_EXCEPTION:
                        Snackbar.make(findViewById(android.R.id.content), R.string.setup_thermostat_io_exception_toast, Snackbar.LENGTH_LONG).show();
                        break;
                    case SendRequestIntentService.TIMEOUT:
                        Snackbar.make(findViewById(android.R.id.content), R.string.setup_thermostat_timed_out_toast, Snackbar.LENGTH_LONG).show();
                        break;
                }
                submitButton.setEnabled(true);
                progressBar.setVisibility(View.INVISIBLE);
            }
        };

        final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, SendRequestIntentService.class);
        intent.putExtra("receiver", receiver);
        intent.putExtra("ip", ip);
        intent.putExtra("ssid", ssid);
        intent.putExtra("password", password);
        intent.putExtra("firebaseURL", firebaseUrl);
        intent.putExtra("firebaseSecret", firebaseSecret);
        intent.putExtra("timezone", timezone);
        startService(intent);
    }

    public static class SendRequestIntentService extends IntentService {

        private static final String TAG = SendRequestIntentService.class.getSimpleName();
        private static final int SUCCESS = 0;
        private static final int BAD_IP = 1;
        private static final int BAD_SERVER_RESPONSE = 2;
        private static final int TIMEOUT = 3;
        private static final int IO_EXCEPTION = 4;


        public SendRequestIntentService() {
            super(SendRequestIntentService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(@Nullable Intent intent) {

            if (intent == null) return;

            ResultReceiver receiver = intent.getParcelableExtra("receiver");
            String ip = intent.getStringExtra("ip");
            String ssid = intent.getStringExtra("ssid");
            String password = intent.getStringExtra("password");
            String firebaseURL = intent.getStringExtra("firebaseURL");
            String firebaseSecret = intent.getStringExtra("firebaseSecret");
            String timezone = intent.getStringExtra("timezone");

            assert receiver != null;

            JSONObject obj = new JSONObject();
            try {
                obj.put("ssid", ssid);
                obj.put("password", password);
                obj.put("firebaseURL", firebaseURL);
                obj.put("firebaseSecret", firebaseSecret);
                obj.put("timezone", timezone);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String requestBody = obj.toString();

            URL url, urlRestart;
            try {
                url = new URL("http", ip, "/settings");
                urlRestart = new URL("http", ip, "/restart");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                receiver.send(BAD_IP, Bundle.EMPTY);
                return;
            }

            try {
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    urlConnection.setConnectTimeout(5000);
                    urlConnection.setReadTimeout(5000);
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod("POST");

                    DataOutputStream out = new DataOutputStream(urlConnection.getOutputStream());
                    out.writeBytes(requestBody);
                    out.flush();
                    out.close();

                    int responseCode = urlConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder result = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);
                        }

                        Log.d(TAG, String.format("result:%s", result));
                        if (result.toString().equals("OK")) {
                            Log.i(TAG, "Success!");
                            receiver.send(SUCCESS, Bundle.EMPTY);
                        } else {
                            Bundle bundle = new Bundle();
                            bundle.putString("response", result.toString());
                            receiver.send(BAD_SERVER_RESPONSE, bundle);
                        }
                    } else {
                        InputStream errorStream = new BufferedInputStream(urlConnection.getErrorStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder result = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);
                        }

                        Log.d(TAG, String.format("result:%s", result));
                        Bundle bundle = new Bundle();
                        bundle.putString("response", result.toString());
                        receiver.send(BAD_SERVER_RESPONSE, bundle);
                    }
                } catch (SocketTimeoutException timeoutEx) {

                    // request or read timed out
                    receiver.send(TIMEOUT, Bundle.EMPTY);

                } catch (IOException ioEx) {

                    // generic IOException
                    ioEx.printStackTrace();
                    receiver.send(IO_EXCEPTION, Bundle.EMPTY);

                } finally {

                    urlConnection.disconnect();
                }
            } catch (IOException ioEx) {

                // URL openConnection error
                ioEx.printStackTrace();
                receiver.send(IO_EXCEPTION, Bundle.EMPTY);
            }

            try {
                HttpURLConnection connection = (HttpURLConnection) urlRestart.openConnection();
                connection.getResponseCode();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
