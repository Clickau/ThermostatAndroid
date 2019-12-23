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


        ArrayList<Pair<String, String>> list = new ArrayList<>();
        list.add(new Pair<String, String>("title1", "info1"));
        list.add(new Pair<String, String>("title2", "info2"));
        list.add(new Pair<String, String>("title3", "info3"));
        listAdapter = new SchedulesAdapter(list);
        recyclerView.setAdapter(listAdapter);
    }
}
