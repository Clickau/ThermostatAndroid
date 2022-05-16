package com.clickau.thermostat;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("WeakerAccess")
public class DashboardFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView stateTemperatureText;
    private TextView stateHumidityText;
    private TextView stateHeaterText;
    private TextView stateTimeText;
    private TextView temporaryTemperatureText;
    private TextView temporaryTimeText;
    private TextView temporaryTitleText;
    private LinearLayout temporaryLayout;
    private Timer refreshTimer;

    private int humidity;
    private double temperature;
    private boolean heaterState;
    private long timestamp;

    private boolean temporaryActive;
    private double temporaryTemperature;
    private long temporaryTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout = view.findViewById(R.id.fragment_dashboard_swipe_refresh_layout);
        stateTemperatureText = view.findViewById(R.id.fragment_dashboard_state_temperature);
        stateHumidityText = view.findViewById(R.id.fragment_dashboard_state_humidity);
        stateHeaterText = view.findViewById(R.id.fragment_dashboard_state_heater);
        stateTimeText = view.findViewById(R.id.fragment_dashboard_state_time);
        temporaryTitleText = view.findViewById(R.id.fragment_dashboard_temporary_title);
        temporaryTemperatureText = view.findViewById(R.id.fragment_dashboard_temporary_temperature);
        temporaryTimeText = view.findViewById(R.id.fragment_dashboard_temporary_time);
        temporaryLayout = view.findViewById(R.id.fragment_dashboard_temporary_layout);


        swipeRefreshLayout.setOnRefreshListener(this::onRefresh);

        refreshTimer = new Timer();
        refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity == null) return;
                activity.runOnUiThread(() -> onRefresh());
            }
        }, 0, 60 * 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshTimer.cancel();
    }

    private void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        FirebaseService.get(getContext(), "/State.json", new String[] { "orderBy=\"time\"" }, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                Activity activity = getActivity();
                if (activity == null)
                    return;

                switch (resultCode) {
                case FirebaseService.RESULT_SUCCESS:
                    String str = resultData.getString("result");
                    if (str == null) return;
                    int i = str.lastIndexOf('{');
                    if (i == -1) return;
                    String state = str.substring(i, str.length() - 1);
                    try {
                        JSONObject obj = new JSONObject(state);
                        humidity = obj.getInt("humidity");
                        heaterState = obj.getBoolean("state");
                        temperature = obj.getDouble("temperature");
                        timestamp = obj.getLong("time");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case FirebaseService.RESULT_DATABASE_NOT_FOUND:
                    Snackbar.make(activity.findViewById(R.id.fragment_dashboard_layout), R.string.firebase_database_not_found, Snackbar.LENGTH_LONG).show();
                    break;
                case FirebaseService.RESULT_MALFORMED_URL:
                    Snackbar.make(activity.findViewById(R.id.fragment_dashboard_layout), R.string.setup_firebase_invalid_url, Snackbar.LENGTH_LONG).show();
                    break;
                case FirebaseService.RESULT_UNAUTHORIZED:
                    Snackbar.make(activity.findViewById(R.id.fragment_dashboard_layout), R.string.firebase_unauthorized, Snackbar.LENGTH_LONG).show();
                    break;
                case FirebaseService.RESULT_IO_EXCEPTION:
                    Snackbar.make(activity.findViewById(R.id.fragment_dashboard_layout), R.string.firebase_io_exception, Snackbar.LENGTH_LONG).show();
                    break;
                case FirebaseService.RESULT_SERVER_ERROR:
                    Snackbar.make(activity.findViewById(R.id.fragment_dashboard_layout), R.string.firebase_server_error, Snackbar.LENGTH_LONG).show();
                    break;
                case FirebaseService.RESULT_TIMEOUT:
                    Snackbar.make(activity.findViewById(R.id.fragment_dashboard_layout), R.string.firebase_timeout, Snackbar.LENGTH_LONG).show();
                    break;
                case FirebaseService.RESULT_NOT_INITIALIZED:
                    Snackbar.make(activity.findViewById(R.id.fragment_dashboard_layout), R.string.firebase_not_initialized, Snackbar.LENGTH_LONG).show();
                    break;
                }

                stateTemperatureText.setText(String.format(Locale.getDefault(), "%.1f°C", temperature));
                stateHumidityText.setText(String.format(Locale.getDefault(), "%d%%", humidity));
                stateHeaterText.setText(heaterState ? R.string.dashboard_state_heater_on : R.string.dashboard_state_heater_off);
                long now = new Date().getTime();
                stateTimeText.setText(DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.MINUTE_IN_MILLIS));
                if (now - timestamp > 5 * 60 * 1000) {
                    stateTimeText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        FirebaseService.get(getContext(), "/TemporarySchedule.json", null, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                Activity activity = getActivity();
                if (activity == null)
                    return;

                if (resultCode == FirebaseService.RESULT_SUCCESS) {
                    String str = resultData.getString("result");
                    if (str == null) return;
                    try {
                        JSONObject obj = new JSONObject(str);
                        temporaryActive = obj.getBoolean("active");
                        if (temporaryActive) {
                            temporaryTemperature = obj.getDouble("temperature");
                            temporaryTime = obj.getLong("time") + obj.getLong("remaining");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (temporaryActive) {
                    temporaryTitleText.setText(R.string.dashboard_temporary_title);
                    temporaryLayout.setVisibility(View.VISIBLE);
                    temporaryTemperatureText.setText(String.format(Locale.getDefault(), "%.1f°C", temporaryTemperature));
                    temporaryTimeText.setText(String.format(Locale.getDefault(), getString(R.string.dashboard_temporary_time_format),
                            DateUtils.getRelativeTimeSpanString(temporaryTime, new Date().getTime(), DateUtils.MINUTE_IN_MILLIS)));
                }
                else {
                    temporaryTitleText.setText(R.string.dashboard_temporary_not_active);
                    temporaryLayout.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
}
