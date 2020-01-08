package com.clickau.thermostat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SchedulesAdapter extends RecyclerView.Adapter<SchedulesAdapter.SchedulesViewHolder> {

    private ArrayList<List<String>> dataset;

    public static class SchedulesViewHolder extends RecyclerView.ViewHolder {

        private TextView titleTextView;
        private TextView infoTextView;
        private TextView repeatTextView;
        private TextView priorityTextView;

        private SchedulesViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.schedule_view_title_text_view);
            infoTextView = itemView.findViewById(R.id.schedule_view_info_text_view);
            repeatTextView = itemView.findViewById(R.id.schedule_view_repeat_text_view);
            priorityTextView = itemView.findViewById(R.id.schedule_view_priority_text_view);
        }
    }

    public SchedulesAdapter(ArrayList<List<String>> data) {
        dataset = data;
    }

    @NonNull
    @Override
    public SchedulesAdapter.SchedulesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.schedule_view, parent, false);

        return new SchedulesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SchedulesViewHolder holder, int position) {

        holder.titleTextView.setText(dataset.get(position).get(0));
        holder.infoTextView.setText(dataset.get(position).get(1));
        holder.repeatTextView.setText(dataset.get(position).get(2));
        holder.priorityTextView.setText(dataset.get(position).get(3));
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }
}
