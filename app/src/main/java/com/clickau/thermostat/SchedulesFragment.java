package com.clickau.thermostat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchedulesFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter listAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedules, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = getActivity().findViewById(R.id.fragment_schedules_recycler_view);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);


        ArrayList<List<String>> list = new ArrayList<>();
        list.add(Arrays.asList("8:00 to 17:00", "Temperature: 20.5°C", "Daily", "Priority: 3"));
        list.add(Arrays.asList("Monday, 12:15 to 15:45", "Temperature: 18.0°C", "Weekly", "Priority: 2"));
        list.add(Arrays.asList("From December 24th 2019, 8:00 to December 25th, 22:00", "Temperature: 22.0°C", "Once","Priority: 1"));
        listAdapter = new SchedulesAdapter(list);
        recyclerView.setAdapter(listAdapter);
    }
}
