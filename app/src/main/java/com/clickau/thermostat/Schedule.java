package com.clickau.thermostat;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

@SuppressWarnings("WeakerAccess")
public class Schedule implements Parcelable {

    public static final Creator<Schedule> CREATOR = new Creator<Schedule>() {
        @Override
        public Schedule createFromParcel(Parcel in) {
            Repeat repeat = Repeat.values()[in.readInt()];
            float temperature = in.readFloat();
            Date start = new Date(in.readLong());
            Date end = new Date(in.readLong());
            boolean[] weekdays = in.createBooleanArray();
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
        dest.writeBooleanArray(weekdays);
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
    private boolean[] weekdays = new boolean[8];

    public Schedule(Repeat repeat, float temperature, Date start, Date end, boolean[] weekdays) {
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

    public void setWeekday(int weekday, boolean value) {
        if (1 <= weekday && weekday <= 7)
            this.weekdays[weekday] = value;
    }

    public void clearWeekdays() {
        for (int i = 1; i <= 7; i++) {
            this.weekdays[i] = false;
        }
    }

    /**
     * Helper function that checks if there are any schedules that overlap and that have the same repeat
     * @param list ArrayList that contains the schedules
     * @return true if there are no overlapping schedules, and false otherwise
     */
    public static boolean isScheduleListValid(ArrayList<Schedule> list) {
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                Schedule s1 = list.get(i), s2 = list.get(j);
                if (s1.getRepeat() == s2.getRepeat()) {
                    if (s1.getRepeat() == Repeat.Weekly) {
                        boolean sameWeekday = false;
                        for (int k = 1; k <= 7; k++) {
                            if (s1.getWeekdays()[k] && s2.getWeekdays()[k]) {
                                // they are both active on the same weekday
                                sameWeekday = true;
                            }
                        }
                        if (!sameWeekday) {
                            // they don't have any weekdays in common, clearly they can't overlap
                            continue;
                        }
                    }
                    if (s1.getEnd().compareTo(s2.getStart()) > 0 && s2.getEnd().compareTo(s1.getStart()) > 0) {
                        // they overlap
                        return false;
                    }
                }
            }
        }
        return true;
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
                boolean[] weekDays = new boolean[8];
                Calendar cal = Calendar.getInstance();
                cal.clear();
                switch (repeat) {
                    case Weekly:
                        int[] weekDaysIntArray = context.deserialize(scheduleJson.get("weekDays").getAsJsonArray(), int[].class);
                        for (int day : weekDaysIntArray) {
                            if (1 <= day && day <= 7)
                                weekDays[day] = true;
                        }
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
                if (startDate.compareTo(endDate) >= 0)
                    return null;
                return new Schedule(repeat, temperature, startDate, endDate, weekDays);

            } catch (RuntimeException e) {
                //throw new JsonParseException("Malformed schedule");
                return null;
            }
        }
    }

    public static class Serializer implements JsonSerializer<Schedule> {

        @Override
        public JsonElement serialize(Schedule schedule, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            object.addProperty("setTemp", schedule.getTemperature());
            object.addProperty("repeat", schedule.getRepeat().name());
            Calendar calendarStart = Calendar.getInstance();
            calendarStart.setTime(schedule.getStart());
            object.addProperty("sH", calendarStart.get(Calendar.HOUR_OF_DAY));
            object.addProperty("sM", calendarStart.get(Calendar.MINUTE));
            Calendar calendarEnd = Calendar.getInstance();
            calendarEnd.setTime(schedule.getEnd());
            object.addProperty("eH", calendarEnd.get(Calendar.HOUR_OF_DAY));
            object.addProperty("eM", calendarEnd.get(Calendar.MINUTE));
            switch (schedule.getRepeat()) {
                case Daily:
                    break;
                case Once:
                    object.addProperty("sY", calendarStart.get(Calendar.YEAR));
                    object.addProperty("sMth", calendarStart.get(Calendar.MONTH));
                    object.addProperty("sD", calendarStart.get(Calendar.DAY_OF_MONTH));
                    object.addProperty("eY", calendarEnd.get(Calendar.YEAR));
                    object.addProperty("eMth", calendarEnd.get(Calendar.MONTH));
                    object.addProperty("eD", calendarEnd.get(Calendar.DAY_OF_MONTH));
                    break;
                case Weekly:
                    JsonArray array = new JsonArray();
                    for (int i = 1; i <= 7; i++) {
                        if (schedule.getWeekdays()[i]) {
                            array.add(i);
                        }
                    }
                    object.add("weekDays", array);
                    break;
            }
            return object;
        }
    }

    // Getters and setters

    public boolean[] getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(boolean[] weekdays) {
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
