package com.clickau.thermostat;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

@SuppressWarnings({"WeakerAccess", "unused"})
public class FirebaseService extends IntentService {

    private static final String TAG = FirebaseService.class.getSimpleName();
    private static final String ACTION_GET = "com.clickau.thermostat.action.firebase.get";
    private static final String ACTION_SET = "com.clickau.thermostat.action.firebase.set";
    private static final String ACTION_PUSH = "com.clickau.thermostat.action.firebase.push";
    private static final String SCHEDULES_PATH = "/Schedules.json";

    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_MALFORMED_URL = 1;
    public static final int RESULT_IO_EXCEPTION = 2;
    public static final int RESULT_UNAUTHORIZED = 3;
    public static final int RESULT_DATABASE_NOT_FOUND = 4;
    public static final int RESULT_SERVER_ERROR = 5;
    public static final int RESULT_BAD_REQUEST = 6;
    public static final int RESULT_NOT_INITIALIZED = 7;
    public static final int RESULT_TIMEOUT = 8;

    private static String firebaseUrl = null;
    private static String firebaseSecret = null;
    private static boolean initialized = false;

    public static void initialize(String url, String secret) {
        firebaseUrl = url;
        firebaseSecret = secret;
        initialized = true;
    }


    public FirebaseService() {
        super(TAG);
    }

    public static void getSchedules(Context context, ResultReceiver receiver) {
        get(context, SCHEDULES_PATH, receiver);
    }

    public static void setSchedules(Context context, String data, ResultReceiver receiver) {
        set(context, SCHEDULES_PATH, data, receiver);
    }

    public static void set(Context context, String path, String data, ResultReceiver receiver) {
        Intent intent = new Intent(context, FirebaseService.class);
        intent.setAction(ACTION_SET);
        intent.putExtra("path", path);
        intent.putExtra("data", data);
        intent.putExtra("receiver", receiver);
        context.startService(intent);
    }

    public static void get(Context context, String path, ResultReceiver receiver) {
        Intent intent = new Intent(context, FirebaseService.class);
        intent.setAction(ACTION_GET);
        intent.putExtra("path", path);
        intent.putExtra("data", "");
        intent.putExtra("receiver", receiver);
        context.startService(intent);
    }

    public static void push(Context context, String path, String data, ResultReceiver receiver) {
        Intent intent = new Intent(context, FirebaseService.class);
        intent.setAction(ACTION_PUSH);
        intent.putExtra("path", path);
        intent.putExtra("data", data);
        intent.putExtra("receiver", receiver);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        final String action = intent.getAction();
        final String path = intent.getStringExtra("path");
        final String data = intent.getStringExtra("data");
        final ResultReceiver receiver = intent.getParcelableExtra("receiver");

        assert action != null && path != null && data != null && receiver != null;

        if (!initialized) {
            receiver.send(RESULT_NOT_INITIALIZED, Bundle.EMPTY);
            return;
        }

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://");
        urlBuilder.append(firebaseUrl);
        if (!path.startsWith("/"))
            urlBuilder.append("/");
        urlBuilder.append(path);
        if (!path.endsWith(".json"))
            urlBuilder.append(".json");
        urlBuilder.append("?auth=");
        urlBuilder.append(firebaseSecret);

        URL url;
        try {
            url = new URL(urlBuilder.toString());
        } catch(MalformedURLException e) {
            e.printStackTrace();
            receiver.send(RESULT_MALFORMED_URL, Bundle.EMPTY);
            return;
        }

        try {
             HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
             connection.setConnectTimeout(5000);
             connection.setReadTimeout(5000);

             switch (action) {
                 case ACTION_PUSH: {
                     connection.setDoOutput(true);
                     connection.setRequestMethod("POST");

                     DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                     out.writeBytes(data);
                     out.flush();
                     out.close();

                     break;
                 }
                 case ACTION_SET: {
                     connection.setDoOutput(true);
                     connection.setRequestMethod("PUT");

                     DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                     out.writeBytes(data);
                     out.flush();
                     out.close();

                     break;
                 }
                 case ACTION_GET:
                     connection.setRequestMethod("GET");

                     connection.connect();
                     break;
             }

             int responseCode = connection.getResponseCode();
             switch (responseCode) {
                 case HttpsURLConnection.HTTP_OK:
                     Log.d(TAG, "OK");
                     if (action.equals(ACTION_GET)) {
                         InputStream in = new BufferedInputStream(connection.getInputStream());
                         BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                         StringBuilder result = new StringBuilder();
                         String line;
                         while ((line = reader.readLine()) != null) {
                             result.append(line);
                         }
                         Log.d(TAG, result.toString());
                         Bundle bundle = new Bundle();
                         bundle.putString("result", result.toString());
                         receiver.send(RESULT_SUCCESS, bundle);
                     } else {
                         receiver.send(RESULT_SUCCESS, Bundle.EMPTY);
                     }
                     break;
                 case HttpsURLConnection.HTTP_UNAUTHORIZED:
                     // problem with secret
                     Log.d(TAG, "Unauthorized");
                     receiver.send(RESULT_UNAUTHORIZED, Bundle.EMPTY);
                     break;
                 case HttpsURLConnection.HTTP_NOT_FOUND:
                 case HttpsURLConnection.HTTP_UNAVAILABLE:
                     // problem with url or requested database temporarily unavailable
                     Log.d(TAG, "Not Found");
                     receiver.send(RESULT_DATABASE_NOT_FOUND, Bundle.EMPTY);
                     break;
                 case HttpsURLConnection.HTTP_INTERNAL_ERROR:
                     // internal error
                     Log.d(TAG, "Internal Server Error");
                     receiver.send(RESULT_SERVER_ERROR, Bundle.EMPTY);
                     break;
                 case HttpsURLConnection.HTTP_BAD_REQUEST:
                     // shouldn't happen with get
                     // with put or post, it means that there is a problem with the passed data string
                     Log.d(TAG, "Bad Request");
                     receiver.send(RESULT_BAD_REQUEST, Bundle.EMPTY);
                     break;
                 default:
                     // we should never end up here as all the possible error codes specified by firebase rest api have been handled above
                     Log.d(TAG, String.format("Error: %d", responseCode));
                     throw new RuntimeException("Server sent code " + responseCode + " which is not specified in the firebase REST api");
             }
             connection.disconnect();

        } catch (SocketTimeoutException e) {
            receiver.send(RESULT_TIMEOUT, Bundle.EMPTY);

        } catch (IOException e) {
            e.printStackTrace();
            receiver.send(RESULT_IO_EXCEPTION, Bundle.EMPTY);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
