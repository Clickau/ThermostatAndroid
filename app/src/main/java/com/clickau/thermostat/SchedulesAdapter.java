package com.clickau.thermostat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class SchedulesAdapter extends RecyclerView.Adapter<SchedulesAdapter.SchedulesViewHolder> {

    public interface ViewHolderResponder {
        void onClickOnItem(View v, int position);
    }

    public static class SchedulesViewHolder extends RecyclerView.ViewHolder {

        private final TextView temperatureTextView;
        private final TextView repeatTextView;
        private final TextView startTextView;
        private final TextView endTextView;
        private final TextView weekdaysTextView;
        private int position;
        private WeakReference<ViewHolderResponder> responder;

        private SchedulesViewHolder(@NonNull View itemView, final WeakReference<ViewHolderResponder> responder){
            super(itemView);
            temperatureTextView = itemView.findViewById(R.id.schedule_view_temperature_text_view);
            repeatTextView = itemView.findViewById(R.id.schedule_view_repeat_text_view);
            startTextView = itemView.findViewById(R.id.schedule_view_start_text_view);
            endTextView = itemView.findViewById(R.id.schedule_view_end_text_view);
            weekdaysTextView = itemView.findViewById(R.id.schedule_view_weekdays_text_view);
            this.responder = responder;

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    responder.get().onClickOnItem(v, position);
                }
            });
        }

        private void bind(@NonNull Schedule schedule, int position) {
            //TODO: Ask for preferred temperature scale or get it from system
            temperatureTextView.setText(String.format(Locale.US,"%.1f°C", schedule.getTemperature()));
            repeatTextView.setText(schedule.getRepeat().toString());
            startTextView.setText(String.format(Locale.US, App.getRes().getString(R.string.schedules_start) + ": %s", schedule.getStartString()));
            endTextView.setText(String.format(Locale.US, App.getRes().getString(R.string.schedules_end) + ": %s", schedule.getEndString()));
            if (schedule.getRepeat().equals(Schedule.Repeat.Weekly)) {
                StringBuilder weekdayBuilder = new StringBuilder(App.getRes().getString(R.string.schedules_weekdays_on));
                weekdayBuilder.append(": ");
                String[] weekdayStrings = new DateFormatSymbols(Locale.getDefault()).getShortWeekdays(); // index 1 is Sunday, index 2 is Monday etc (as specified by Calendar.MONDAY etc)
                int[] weekdays = schedule.getWeekdays();
                for (int i = 0; i < weekdays.length; i++) {
                    weekdayBuilder.append(weekdayStrings[weekdays[i]]);

                    if (i != weekdays.length - 1)
                        weekdayBuilder.append(", ");
                }
                weekdaysTextView.setText(weekdayBuilder.toString());
                weekdaysTextView.setVisibility(View.VISIBLE);
            } else {
                weekdaysTextView.setVisibility(View.GONE);
            }
            this.position = position;
        }
    }

    private final ArrayList<Schedule> schedules;
    private final WeakReference<ViewHolderResponder> responder;

    public SchedulesAdapter(ArrayList<Schedule> schedules, WeakReference<ViewHolderResponder> responder) {
        this.schedules = schedules;
        this.responder = responder;
    }

    @NonNull
    @Override
    public SchedulesAdapter.SchedulesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.schedule_view, parent, false);

        return new SchedulesViewHolder(view, responder);
    }

    @Override
    public void onBindViewHolder(@NonNull SchedulesViewHolder holder, int position) {
        Schedule schedule = schedules.get(position);
        holder.bind(schedule, position);
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

    public Schedule getItemAt(int position) {
        return schedules.get(position);
    }
}
