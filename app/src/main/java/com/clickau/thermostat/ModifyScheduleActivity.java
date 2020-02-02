package com.clickau.thermostat;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ModifyScheduleActivity extends AppCompatActivity {

    private static final String TAG = ModifyScheduleActivity.class.getSimpleName();

    private TextView temperatureTextView;
    private Spinner repeatSpinner;
    private LinearLayout weekdaysLayout;
    private TextView startTextView;
    private TextView endTextView;

    private Schedule schedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_schedule);

        Toolbar toolbar = findViewById(R.id.modify_schedule_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        temperatureTextView = findViewById(R.id.modify_schedule_temperature_text_view);
        repeatSpinner = findViewById(R.id.modify_schedule_repeat_spinner);
        weekdaysLayout = findViewById(R.id.modify_schedule_weekdays_layout);
        startTextView = findViewById(R.id.modify_schedule_start_text_view);
        endTextView = findViewById(R.id.modify_schedule_end_text_view);

        schedule = getIntent().getParcelableExtra("schedule");
        if (schedule == null) {
            finish();
            return;
        }
        Log.d(TAG, String.format("Repeat: %s", schedule.getRepeat().toString()));
        Log.d(TAG, String.format("Temp: %f", schedule.getTemperature()));
        Log.d(TAG, String.format("Start: %s", schedule.getStartString()));
        Log.d(TAG, String.format("End: %s", schedule.getEndString()));
        Log.d(TAG, String.format("Weekdays: %s", Arrays.toString(schedule.getWeekdays())));

        // set the start and end times to include the current date, so that if we change the schedule repeat to Once, it will start from the current date
        if (schedule.getRepeat() != Schedule.Repeat.Once) {
            Date startDate = schedule.getStart();
            Date endDate = schedule.getEnd();
            Calendar currentCal = Calendar.getInstance();
            currentCal.set(Calendar.SECOND, 0);
            currentCal.set(Calendar.MILLISECOND, 0);
            currentCal.set(Calendar.HOUR_OF_DAY, 1);

            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            currentCal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
            currentCal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
            schedule.setStart(currentCal.getTime());

            cal.setTime(endDate);
            currentCal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
            currentCal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
            schedule.setEnd(currentCal.getTime());
        }

        temperatureTextView.setText(String.format(Locale.getDefault(),"%.1f°C", schedule.getTemperature()));
        temperatureTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Temperature click");
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_temperature_picker, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(ModifyScheduleActivity.this)
                        .setTitle(R.string.dialog_temperature_picker_title)
                        .setView(dialogView);
                final NumberPicker primary = dialogView.findViewById(R.id.dialog_temperature_picker_primary_picker);
                final NumberPicker secondary = dialogView.findViewById(R.id.dialog_temperature_picker_secondary_picker);
                final TextView separator = dialogView.findViewById(R.id.dialog_temperature_picker_separator);
                primary.setMinValue(5);
                primary.setMaxValue(40);
                primary.setWrapSelectorWheel(false);
                primary.setValue(20);
                secondary.setMinValue(0);
                secondary.setMaxValue(9);
                secondary.setWrapSelectorWheel(true);
                separator.setText(String.valueOf(DecimalFormatSymbols.getInstance().getDecimalSeparator()));
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        float result = primary.getValue() + (float) secondary.getValue() / 10;
                        Log.d(TAG, String.format("Temperature: %f", result));
                        schedule.setTemperature(result);
                        temperatureTextView.setText(String.format(Locale.getDefault(),"%.1f°C", schedule.getTemperature()));
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null)
                    .create().show();
            }
        });

        Schedule.Repeat[] values = Schedule.Repeat.values();
        String[] strings = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strings[i] = values[i].toString();
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, strings);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(spinnerAdapter);
        repeatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Schedule.Repeat selected = Schedule.Repeat.values()[position];
                schedule.setRepeat(selected);
                switch (selected) {
                    case Once:
                    case Daily:
                        weekdaysLayout.setVisibility(View.GONE);
                        break;
                    case Weekly:
                        weekdaysLayout.setVisibility(View.VISIBLE);
                        break;
                }
                startTextView.setText(String.format(Locale.US, getString(R.string.schedules_start) + ": %s", schedule.getStartString()));
                endTextView.setText(String.format(Locale.US, getString(R.string.schedules_end) + ": %s", schedule.getEndString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        repeatSpinner.setSelection(schedule.getRepeat().ordinal());

        startTextView.setText(String.format(Locale.US, getString(R.string.schedules_start) + ": %s", schedule.getStartString()));
        endTextView.setText(String.format(Locale.US, getString(R.string.schedules_end) + ": %s", schedule.getEndString()));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}
