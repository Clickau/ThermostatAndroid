package com.clickau.thermostat;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Schedule {

    public enum Repeat {
        Once,
        Daily,
        Weekly
    }

    private Repeat repeat;
    private float temperature;
    private Date start;
    private Date end;
    private int[] weekdays; // can't use DayOfWeek because it is introduced only in api 26

    public Schedule(Repeat repeat, float temperature, Date start, Date end, int[] weekdays) {
        this.setWeekdays(weekdays);
        this.setRepeat(repeat);
        this.setTemperature(temperature);
        this.setStart(start);
        this.setEnd(end);
    }

    public int[] getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(int[] weekdays) {
        this.weekdays = weekdays;
    }

    public Repeat getRepeat() {
        return repeat;
    }

    public void setRepeat(Repeat repeat) {
        this.repeat = repeat;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public String getStartString() {
        switch (repeat) {
            case Once:
                DateFormat format = SimpleDateFormat.getDateTimeInstance(DateFormat.LONG,  DateFormat.SHORT);
                return format.format(start);
            case Daily:
            case Weekly:
                format = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
                return format.format(start);
        }
        return "";
    }

    public String getEndString() {
        switch (repeat) {
            case Once:
                DateFormat format = SimpleDateFormat.getDateTimeInstance(DateFormat.LONG,  DateFormat.SHORT);
                return format.format(end);
            case Daily:
            case Weekly:
                format = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
                return format.format(end);
        }
        return "";
    }

    @NonNull
    @Override
    public String toString() {
        return "{repeat=" +
                getRepeat().toString() +
                ",temperature=" +
                temperature +
                ",start=" +
                getStartString() +
                ",end=" +
                getEndString() +
                ",weekdays=" +
                Arrays.toString(weekdays) +
                "}";
    }
}
