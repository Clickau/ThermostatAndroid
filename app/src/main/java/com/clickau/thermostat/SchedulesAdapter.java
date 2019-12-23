package com.clickau.thermostat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SchedulesAdapter extends RecyclerView.Adapter<SchedulesAdapter.SchedulesViewHolder> {

    private ArrayList<Pair<String, String>> dataset;

    public static class SchedulesViewHolder extends RecyclerView.ViewHolder {

        public View view;

        public SchedulesViewHolder(@NonNull View itemView) {
            super(itemView);
            view  = itemView;
        }
    }

    public SchedulesAdapter(ArrayList<Pair<String, String>> data) {
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

        RelativeLayout relativeLayout = (RelativeLayout) holder.view;
        TextView titleTextView = relativeLayout.findViewById(R.id.schedule_view_title_text_view);
        TextView infoTextView = relativeLayout.findViewById(R.id.schedule_view_info_text_view);
        titleTextView.setText(dataset.get(position).first);
        infoTextView.setText(dataset.get(position).second);
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }
}
