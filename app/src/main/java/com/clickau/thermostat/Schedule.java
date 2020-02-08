package com.clickau.thermostat;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Schedule implements Parcelable {

    public static final Creator<Schedule> CREATOR = new Creator<Schedule>() {
        @Override
        public Schedule createFromParcel(Parcel in) {
            Repeat repeat = Repeat.values()[in.readInt()];
            float temperature = in.readFloat();
            Date start = new Date(in.readLong());
            Date end = new Date(in.readLong());
            int[] weekdays = in.createIntArray();
            return new Schedule(repeat, temperature, start, end, weekdays);
        }

        @Override
        public Schedule[] newArray(int size) {
            return new Schedule[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeInt(repeat.ordinal());
        dest.writeFloat(temperature);
        dest.writeLong(start.getTime());
        dest.writeLong(end.getTime());
        dest.writeIntArray(weekdays);
    }

    public enum Repeat {
        Once {
            @NonNull
            @Override
            public String toString() {
                return App.getRes().getString(R.string.schedules_repeat_once);
            }
        },
        Daily {
            @NonNull
            @Override
            public String toString() {
                return App.getRes().getString(R.string.schedules_repeat_daily);
            }
        },
        Weekly {
            @NonNull
            @Override
            public String toString() {
                return App.getRes().getString(R.string.schedules_repeat_weekly);
            }
        };

        public static String[] getStrings() {
            final String[] strings = new String[values().length];
            for (int i = 0; i < values().length; i++) {
                strings[i] = values()[i].toString();
            }
            return strings;
        }
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

    public String getStartString() {
        switch (repeat) {
            case Once:
                DateFormat format = SimpleDateFormat.getDateTimeInstance(DateFormat.FULL,  DateFormat.SHORT);
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
                DateFormat format = SimpleDateFormat.getDateTimeInstance(DateFormat.FULL,  DateFormat.SHORT);
                return format.format(end);
            case Daily:
            case Weekly:
                format = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
                return format.format(end);
        }
        return "";
    }

    public static class Deserializer implements JsonDeserializer<Schedule> {

        @Override
        public Schedule deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            try {

                JsonObject scheduleJson = json.getAsJsonObject();
                Schedule.Repeat repeat = Schedule.Repeat.valueOf(scheduleJson.get("repeat").getAsString());
                float temperature = scheduleJson.get("setTemp").getAsFloat();
                int sH = scheduleJson.get("sH").getAsInt();
                int sM = scheduleJson.get("sM").getAsInt();
                int eH = scheduleJson.get("eH").getAsInt();
                int eM = scheduleJson.get("eM").getAsInt();
                Date startDate, endDate;
                int[] weekDays = new int[]{};
                Calendar cal = Calendar.getInstance();
                cal.clear();
                switch (repeat) {
                    case Weekly:
                        weekDays = context.deserialize(scheduleJson.get("weekDays").getAsJsonArray(), int[].class);
                    case Daily:
                        cal.set(Calendar.HOUR_OF_DAY, sH);
                        cal.set(Calendar.MINUTE, sM);
                        startDate = cal.getTime();
                        cal.clear();
                        cal.set(Calendar.HOUR_OF_DAY, eH);
                        cal.set(Calendar.MINUTE, eM);
                        endDate = cal.getTime();
                        break;
                    case Once:
                        int sY = scheduleJson.get("sY").getAsInt();
                        int sMth = scheduleJson.get("sMth").getAsInt();
                        int sD = scheduleJson.get("sD").getAsInt();
                        int eY = scheduleJson.get("eY").getAsInt();
                        int eMth = scheduleJson.get("eMth").getAsInt();
                        int eD = scheduleJson.get("eD").getAsInt();
                        cal.set(sY, sMth, sD, sH, sM);
                        startDate = cal.getTime();
                        cal.clear();
                        cal.set(eY, eMth, eD, eH, eM);
                        endDate = cal.getTime();
                        break;
                    default: // to suppress variable might not be initialized warning
                        startDate = new Date();
                        endDate = new Date();
                }
                return new Schedule(repeat, temperature, startDate, endDate, weekDays);

            } catch (RuntimeException e) {
                //throw new JsonParseException("Malformed schedule");
                return null;
            }
        }
    }


    // Getters and setters

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
}
