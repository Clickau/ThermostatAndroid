package com.clickau.thermostat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SetupWifiActivity extends AppCompatActivity implements View.OnClickListener {

    final String TAG = "SetupWifiActivity";

    private TextInputEditText ipEditText;
    private TextInputEditText ssidEditText;
    private TextInputEditText passwordEditText;
    private TextInputLayout ipTextInputLayout;
    private TextInputLayout ssidTextInputLayout;
    private TextInputLayout passwordTextInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_wifi);

        // display the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        Button submitButton = findViewById(R.id.setup_wifi_submit_button);
        ipEditText = findViewById(R.id.setup_wifi_ip_edit_text);
        ssidEditText = findViewById(R.id.setup_wifi_ssid_edit_text);
        passwordEditText = findViewById(R.id.setup_wifi_password_edit_text);
        ipTextInputLayout = findViewById(R.id.setup_wifi_ip_text_input_layout);
        ssidTextInputLayout = findViewById(R.id.setup_wifi_ssid_text_input_layout);
        passwordTextInputLayout = findViewById(R.id.setup_wifi_password_text_input_layout);

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
                    ipTextInputLayout.setError(getString(R.string.setup_wifi_ip_invalid_error));
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
                    ssidTextInputLayout.setError(getString(R.string.setup_wifi_ssid_empty_error));
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
                    passwordTextInputLayout.setError(getString(R.string.setup_wifi_password_empty_error));
                else
                    passwordTextInputLayout.setError(null);
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
    public void onClick(View v) {
        String ip = ipEditText.getText().toString();
        String ssid = ssidEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        Log.d(TAG, String.format("Received ip:%s ssid:%s password:%s", ip, ssid, password));
        // ip address is empty or invalid
        if (TextUtils.isEmpty(ip) || !Patterns.IP_ADDRESS.matcher(ip).matches())
        {
            Log.e(TAG, getString(R.string.setup_wifi_ip_invalid_error));
            Toast.makeText(getApplicationContext(), getString(R.string.setup_wifi_ip_invalid_error), Toast.LENGTH_SHORT).show();
            ipTextInputLayout.setError(getString(R.string.setup_wifi_ip_invalid_error));
            return;
        }
        if (TextUtils.isEmpty(ssid))
        {
            Log.e(TAG, getString(R.string.setup_wifi_ssid_empty_error));
            Toast.makeText(getApplicationContext(), getString(R.string.setup_wifi_ssid_empty_error), Toast.LENGTH_SHORT).show();
            ssidTextInputLayout.setError(getString(R.string.setup_wifi_ssid_empty_error));
            return;
        }
        if (TextUtils.isEmpty(password))
        {
            Log.e(TAG, getString(R.string.setup_wifi_password_empty_error));
            Toast.makeText(getApplicationContext(), getString(R.string.setup_wifi_password_empty_error), Toast.LENGTH_SHORT).show();
            passwordTextInputLayout.setError(getString(R.string.setup_wifi_password_empty_error));
            return;
        }

        ResultReceiver receiver = new ResultReceiver(new Handler()) {

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == SendRequestIntentService.SUCCESS) {
                    Toast.makeText(getApplicationContext(), "Succes!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Fail!", Toast.LENGTH_SHORT).show();
                }
            }

        };


        final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, SendRequestIntentService.class);
        intent.putExtra("receiver", receiver);
        intent.putExtra("ip", ip);
        intent.putExtra("ssid", ssid);
        intent.putExtra("password", password);
        startService(intent);
    }


    public static class SendRequestIntentService extends IntentService {

        private static final String TAG = SendRequestIntentService.class.getSimpleName();
        private static final int SUCCESS = 1;
        private static final int FAIL = 0;


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

            String requestBody = String.format("ssid=%s&password=%s", ssid, password);
            Log.d(TAG, String.format("requestBody:%s", requestBody));

            URL url;
            try {
                url = new URL("http", ip, "/post");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                // show error
                receiver.send(FAIL, Bundle.EMPTY);
                return;
            }
            Log.d(TAG, String.format("url:%s", url.toString()));

            try {
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                try {
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod("POST");

                    DataOutputStream out = new DataOutputStream(urlConnection.getOutputStream());
                    out.writeBytes(requestBody);
                    out.flush();
                    out.close();

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
                    }
                    else {
                        receiver.send(FAIL, Bundle.EMPTY);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    // show error
                    receiver.send(FAIL, Bundle.EMPTY);
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
                // show error
                receiver.send(FAIL, Bundle.EMPTY);
            }
        }
    }
}
