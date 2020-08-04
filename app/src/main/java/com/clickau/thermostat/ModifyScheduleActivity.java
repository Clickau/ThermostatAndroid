package com.clickau.thermostat;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormatSymbols;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ModifyScheduleActivity extends AppCompatActivity {

    public static final int ACTION_MODIFY = 0;
    public static final int ACTION_ADD = 1;

    private static final String TAG = ModifyScheduleActivity.class.getSimpleName();

    private MaterialButton temperatureButton;
    private MaterialButton repeatButton;
    private LinearLayout weekdaysLayout;
    private MaterialButton startTimeButton;
    private MaterialButton startDateButton;
    private MaterialButton endTimeButton;
    private MaterialButton endDateButton;

    private Schedule schedule;
    private int position;
    private int action;
    private final java.text.DateFormat dateFormat = SimpleDateFormat.getDateInstance(java.text.DateFormat.FULL);
    private final java.text.DateFormat timeFormat = SimpleDateFormat.getTimeInstance(java.text.DateFormat.SHORT);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_schedule);

        Toolbar toolbar = findViewById(R.id.modify_schedule_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        temperatureButton = findViewById(R.id.modify_schedule_temperature_button);
        repeatButton = findViewById(R.id.modify_schedule_repeat_button);
        weekdaysLayout = findViewById(R.id.modify_schedule_weekdays_layout);
        startTimeButton = findViewById(R.id.modify_schedule_start_time_button);
        startDateButton = findViewById(R.id.modify_schedule_start_date_button);
        endTimeButton = findViewById(R.id.modify_schedule_end_time_button);
        endDateButton = findViewById(R.id.modify_schedule_end_date_button);

        schedule = getIntent().getParcelableExtra("schedule");
        position = getIntent().getIntExtra("position", -1);
        action = getIntent().getIntExtra("action", ACTION_ADD);

        if (action == ACTION_ADD) {
            actionBar.setTitle(R.string.modify_schedule_add_title);
            Calendar cal = Calendar.getInstance();
            Date start, end;
            cal.clear();
            cal.set(Calendar.HOUR_OF_DAY, 8);
            start = cal.getTime();
            cal.set(Calendar.HOUR_OF_DAY, 20);
            end = cal.getTime();
            // set some default values
            schedule = new Schedule(Schedule.Repeat.Daily, 20.0f, start, end, new boolean[8]);
        } else {
            actionBar.setTitle(R.string.modify_schedule_modify_title);
            if (schedule == null || position == -1) {
                finish();
                return;
            }
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

        temperatureButton.setText(String.format(Locale.getDefault(),"%.1f°C", schedule.getTemperature()));
        temperatureButton.setOnClickListener(new TemperatureButtonOnClickListener());

        repeatButton.setText(schedule.getRepeat().toString());
        repeatButton.setOnClickListener(new RepeatButtonOnClickListener());

        LayoutInflater inflater = getLayoutInflater();
        Locale l = Locale.getDefault();
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(l);
        String[] weekdayStrings = dateFormatSymbols.getWeekdays();
        Calendar cal = Calendar.getInstance();
        boolean[] scheduleWeekdays = schedule.getWeekdays();
        int firstWeekday = cal.getFirstDayOfWeek();
        for (int i = 1, weekday = firstWeekday; i <= 7; i++, weekday = weekday % 7 + 1) {
            String name = weekdayStrings[weekday].toUpperCase();
            char letter;
            // if the locale is china, taiwan, or singapore chinese, the first characters of all days are the same, so we get the last one
            if (l != Locale.CHINESE && l != Locale.SIMPLIFIED_CHINESE && l != Locale.TRADITIONAL_CHINESE)
                letter = name.charAt(0);
            else
                letter = name.charAt(name.length() - 1);
            View weekdayButtonFrame = inflater.inflate(R.layout.weekday_button, weekdaysLayout, false);
            CheckBox weekdayButton = weekdayButtonFrame.findViewById(R.id.weekday_button_checkbox);
            weekdayButton.setText(Character.toString(letter));
            weekdayButton.setChecked(scheduleWeekdays[weekday]);
            if (scheduleWeekdays[weekday]) {
                weekdayButton.setTextColor(getResources().getColor(android.R.color.white));
            }
            weekdayButton.setOnCheckedChangeListener(new WeekdayButtonOnCheckedChangeListener(weekday));
            weekdaysLayout.addView(weekdayButtonFrame);
        }

        switch (schedule.getRepeat()) {
            case Once:
                weekdaysLayout.setVisibility(View.GONE);
                startDateButton.setVisibility(View.VISIBLE);
                endDateButton.setVisibility(View.VISIBLE);
                break;
            case Daily:
                weekdaysLayout.setVisibility(View.GONE);
                startDateButton.setVisibility(View.GONE);
                endDateButton.setVisibility(View.GONE);
                break;
            case Weekly:
                weekdaysLayout.setVisibility(View.VISIBLE);
                startDateButton.setVisibility(View.GONE);
                endDateButton.setVisibility(View.GONE);
                break;
        }

        startTimeButton.setText(timeFormat.format(schedule.getStart()));
        startDateButton.setText(dateFormat.format(schedule.getStart()));
        endTimeButton.setText(timeFormat.format(schedule.getEnd()));
        endDateButton.setText(dateFormat.format(schedule.getEnd()));

        startTimeButton.setOnClickListener(new TimeButtonOnClickListener(TimeButtonOnClickListener.START));
        startDateButton.setOnClickListener(new DateButtonOnClickListener(DateButtonOnClickListener.START));
        endTimeButton.setOnClickListener(new TimeButtonOnClickListener(TimeButtonOnClickListener.END));
        endDateButton.setOnClickListener(new DateButtonOnClickListener(DateButtonOnClickListener.END));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_modify_schedule_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.activity_modify_schedule_menu_save) {
            if (schedule.getStart().compareTo(schedule.getEnd()) >= 0) {
                Snackbar.make(findViewById(android.R.id.content), R.string.modify_schedule_start_not_before_end, Snackbar.LENGTH_LONG).show();
                return true;
            }
            if (schedule.getRepeat() != Schedule.Repeat.Once) {
                // get rid of date, leave only time
                Date startDate = schedule.getStart();
                Date endDate = schedule.getEnd();
                Calendar newCalendar = Calendar.getInstance();
                newCalendar.clear();

                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                newCalendar.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
                newCalendar.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
                schedule.setStart(newCalendar.getTime());

                cal.setTime(endDate);
                newCalendar.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
                newCalendar.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
                schedule.setEnd(newCalendar.getTime());
            }

            // check if at least one weekday is selected or if all of them are selected
            if (schedule.getRepeat() == Schedule.Repeat.Weekly) {
                boolean oneSelected = false, allSelected = true;
                boolean[] weekdays = schedule.getWeekdays();
                for (int i = 1; i <= 7; i++) {
                    if (weekdays[i])
                        oneSelected = true;
                    else
                        allSelected = false;
                }

                if (!oneSelected) {
                    // none of the weekdays are selected
                    Snackbar.make(findViewById(android.R.id.content), R.string.modify_schedule_select_at_least_one_weekday, Snackbar.LENGTH_SHORT).show();
                    return true;
                }

                if (allSelected) {
                    // change schedule to daily
                    schedule.setRepeat(Schedule.Repeat.Daily);
                    schedule.clearWeekdays();
                    Toast.makeText(this, R.string.modify_schedule_turned_into_daily, Toast.LENGTH_SHORT).show();
                }
            }

            Log.d(TAG, String.format("Repeat: %s", schedule.getRepeat().toString()));
            Log.d(TAG, String.format("Temp: %f", schedule.getTemperature()));
            Log.d(TAG, String.format("Start: %s", schedule.getStartString()));
            Log.d(TAG, String.format("End: %s", schedule.getEndString()));
            Log.d(TAG, String.format("Weekdays: %s", Arrays.toString(schedule.getWeekdays())));

            Intent result = new Intent();
            result.putExtra("schedule", schedule);
            if (action == ACTION_MODIFY) {
                result.putExtra("position", position);
                result.putExtra("action", ACTION_MODIFY);
            } else {
                result.putExtra("action", ACTION_ADD);
            }
            setResult(RESULT_OK, result);
            finish();
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    private class TemperatureButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            @SuppressLint("InflateParams")
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_temperature_picker, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(ModifyScheduleActivity.this)
                    .setTitle(R.string.dialog_temperature_picker_title)
                    .setView(dialogView);
            final NumberPicker primary = dialogView.findViewById(R.id.dialog_temperature_picker_primary_picker);
            final NumberPicker secondary = dialogView.findViewById(R.id.dialog_temperature_picker_secondary_picker);
            final TextView separator = dialogView.findViewById(R.id.dialog_temperature_picker_separator);
            float temperature = schedule.getTemperature();
            int temperatureWhole = (int) temperature;
            int temperatureDecimal = (int) (temperature * 10) - temperatureWhole * 10;
            primary.setMinValue(5);
            primary.setMaxValue(40);
            primary.setWrapSelectorWheel(false);
            primary.setValue(temperatureWhole);
            secondary.setMinValue(0);
            secondary.setMaxValue(9);
            secondary.setWrapSelectorWheel(true);
            secondary.setValue(temperatureDecimal);
            separator.setText(String.valueOf(DecimalFormatSymbols.getInstance().getDecimalSeparator()));
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                float result = primary.getValue() + (float) secondary.getValue() / 10;
                schedule.setTemperature(result);
                temperatureButton.setText(String.format(Locale.getDefault(),"%.1f°C", schedule.getTemperature()));
            });
            builder.setNegativeButton(android.R.string.cancel, null)
                    .create().show();
        }
    }

    private class RepeatButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            new AlertDialog.Builder(ModifyScheduleActivity.this)
                    .setItems(Schedule.Repeat.getStrings(), (dialog, which) -> {
                        Schedule.Repeat selected = Schedule.Repeat.values()[which];
                        repeatButton.setText(selected.toString());
                        schedule.setRepeat(selected);
                        switch (selected) {
                            case Once:
                                weekdaysLayout.setVisibility(View.GONE);
                                startDateButton.setVisibility(View.VISIBLE);
                                endDateButton.setVisibility(View.VISIBLE);
                                break;
                            case Daily:
                                weekdaysLayout.setVisibility(View.GONE);
                                startDateButton.setVisibility(View.GONE);
                                endDateButton.setVisibility(View.GONE);
                                break;
                            case Weekly:
                                weekdaysLayout.setVisibility(View.VISIBLE);
                                startDateButton.setVisibility(View.GONE);
                                endDateButton.setVisibility(View.GONE);
                                break;
                        }
                        startTimeButton.setText(timeFormat.format(schedule.getStart()));
                        startDateButton.setText(dateFormat.format(schedule.getStart()));
                        endTimeButton.setText(timeFormat.format(schedule.getEnd()));
                        endDateButton.setText(dateFormat.format(schedule.getEnd()));
                    })
                    .create().show();
        }
    }

    private class WeekdayButtonOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {

        private final int which;

        private WeekdayButtonOnCheckedChangeListener(int which) {
            this.which = which;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            schedule.setWeekday(which, isChecked);
            if (isChecked) {
                buttonView.setTextColor(getResources().getColor(android.R.color.white));
            } else {
                buttonView.setTextColor(App.resolveColorAttr(ModifyScheduleActivity.this, android.R.attr.textColorPrimary));
            }
        }
    }

    private class TimeButtonOnClickListener implements  View.OnClickListener {

        private static final int START = 0;
        private static final int END = 1;

        private final int which;

        private TimeButtonOnClickListener(int which) {
            this.which = which;
        }

        @Override
        public void onClick(View v) {
            final Calendar calendar = Calendar.getInstance();
            if (which == START)
                calendar.setTime(schedule.getStart());
            else
                calendar.setTime(schedule.getEnd());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            TimePickerDialog timePickerDialog = new TimePickerDialog(ModifyScheduleActivity.this, (view, hourOfDay, minute1) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute1);
                if (which == START) {
                    schedule.setStart(calendar.getTime());
                    startTimeButton.setText(timeFormat.format(schedule.getStart()));
                }
                else {
                    schedule.setEnd(calendar.getTime());
                    endTimeButton.setText(timeFormat.format(schedule.getEnd()));
                }
                if (schedule.getStart().compareTo(schedule.getEnd()) >= 0) {
                    startTimeButton.setTextColor(getResources().getColor(R.color.errorRed));
                    startDateButton.setTextColor(getResources().getColor(R.color.errorRed));
                } else {
                    startTimeButton.setTextColor(App.resolveColorAttr(ModifyScheduleActivity.this, android.R.attr.textColorPrimary));
                    startDateButton.setTextColor(App.resolveColorAttr(ModifyScheduleActivity.this, android.R.attr.textColorPrimary));
                }
            }, hour, minute, android.text.format.DateFormat.is24HourFormat(ModifyScheduleActivity.this));
            timePickerDialog.show();
        }
    }

    private class DateButtonOnClickListener implements View.OnClickListener {

        private static final int START = 0;
        private static final int END = 1;

        private final int which;

        private DateButtonOnClickListener(int which) {
            this.which = which;
        }

        @Override
        public void onClick(View v) {
            final Calendar calendar = Calendar.getInstance();
            if (which == START)
                calendar.setTime(schedule.getStart());
            else
                calendar.setTime(schedule.getEnd());
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(ModifyScheduleActivity.this, (view, year1, month1, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year1);
                calendar.set(Calendar.MONTH, month1);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                if (which == START) {
                    schedule.setStart(calendar.getTime());
                    startDateButton.setText(dateFormat.format(schedule.getStart()));
                }
                else {
                    schedule.setEnd(calendar.getTime());
                    endDateButton.setText(dateFormat.format(schedule.getEnd()));
                }
                if (schedule.getStart().compareTo(schedule.getEnd()) >= 0) {
                    startTimeButton.setTextColor(getResources().getColor(R.color.errorRed));
                    startDateButton.setTextColor(getResources().getColor(R.color.errorRed));
                } else {
                    startTimeButton.setTextColor(App.resolveColorAttr(ModifyScheduleActivity.this, android.R.attr.textColorPrimary));
                    startDateButton.setTextColor(App.resolveColorAttr(ModifyScheduleActivity.this, android.R.attr.textColorPrimary));
                }
            }, year, month, day);
            datePickerDialog.show();
        }
    }

}
