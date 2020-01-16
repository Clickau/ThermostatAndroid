package com.clickau.thermostat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class SchedulesAdapter extends RecyclerView.Adapter<SchedulesAdapter.SchedulesViewHolder> {

    private ArrayList<Schedule> schedules;

    public static class SchedulesViewHolder extends RecyclerView.ViewHolder {

        private TextView temperatureTextView;
        private TextView repeatTextView;
        private TextView startTextView;
        private TextView endTextView;
        private TextView weekdaysTextView;

        private SchedulesViewHolder(@NonNull View itemView) {
            super(itemView);
            temperatureTextView = itemView.findViewById(R.id.schedule_view_temperature_text_view);
            repeatTextView = itemView.findViewById(R.id.schedule_view_repeat_text_view);
            startTextView = itemView.findViewById(R.id.schedule_view_start_text_view);
            endTextView = itemView.findViewById(R.id.schedule_view_end_text_view);
            weekdaysTextView = itemView.findViewById(R.id.schedule_view_weekdays_text_view);
        }
    }

    public SchedulesAdapter(ArrayList<Schedule> schedules) {
        this.schedules = schedules;
    }

    @NonNull
    @Override
    public SchedulesAdapter.SchedulesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.schedule_view, parent, false);

        return new SchedulesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SchedulesViewHolder holder, int position) {
        Schedule schedule = schedules.get(position);

        //TODO: Ask for preferred temperature scale or get it from system
        holder.temperatureTextView.setText(String.format(Locale.US,"%.1f°C", schedule.getTemperature()));
        holder.repeatTextView.setText(schedule.getRepeat().toString());
        holder.startTextView.setText(String.format(Locale.US, App.getRes().getString(R.string.schedules_start) + ": %s", schedule.getStartString()));
        holder.endTextView.setText(String.format(Locale.US, App.getRes().getString(R.string.schedules_end) + ": %s", schedule.getEndString()));
        if (schedules.get(position).getRepeat().equals(Schedule.Repeat.Weekly)) {
            StringBuilder weekdayBuilder = new StringBuilder(App.getRes().getString(R.string.schedules_weekdays_on));
            weekdayBuilder.append(": ");
            String[] weekdayStrings = new DateFormatSymbols(Locale.getDefault()).getShortWeekdays(); // index 1 is Sunday, index 2 is Monday etc (as specified by Calendar.MONDAY etc)
            int[] weekdays = schedule.getWeekdays();
            for (int i = 0; i < weekdays.length; i++) {
                weekdayBuilder.append(weekdayStrings[weekdays[i]]);

                if (i != weekdays.length - 1)
                    weekdayBuilder.append(", ");
            }
            holder.weekdaysTextView.setText(weekdayBuilder.toString());
            holder.weekdaysTextView.setVisibility(View.VISIBLE);
        } else {
            holder.weekdaysTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public void updateSchedules(Collection<? extends Schedule> newSchedules) {
        schedules.clear();
        schedules.addAll(newSchedules);
        notifyDataSetChanged();
    }
}
