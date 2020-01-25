package com.clickau.thermostat;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class SchedulesFragment extends Fragment {

    private static final String TAG = SchedulesFragment.class.getSimpleName();

    private RecyclerView recyclerView;
    private SchedulesAdapter listAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedules, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout = view.findViewById(R.id.fragment_schedules_swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                RefreshList();
            }
        });

        setHasOptionsMenu(true);

        recyclerView = view.findViewById(R.id.fragment_schedules_recycler_view);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        listAdapter = new SchedulesAdapter(new ArrayList<Schedule>());
        recyclerView.setAdapter(listAdapter);

        swipeRefreshLayout.setRefreshing(true);
        RefreshList();
    }

    private void RefreshList() {
        Log.d(TAG, "Refreshing Schedules list");

        FirebaseService.get(getContext(), "/Program", new ResultReceiver(new Handler()) { // TODO: Ask for path to the schedules in Setup Firebase
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {

                Activity activity = getActivity();
                if (activity == null) return;

                switch (resultCode) {
                    case FirebaseService.RESULT_SUCCESS:
                        String str = resultData.getString("result");
                        Log.d(TAG, String.format("ScheduleString: %s", str));
                        Gson gson = new GsonBuilder().registerTypeAdapter(Schedule.class, new Schedule.Deserializer()).create();
                        Type mapType = new TypeToken<Map<String, Schedule>>() {}.getType();
                        Map<String, Schedule> map = gson.fromJson(str, mapType);
                        int invalidSchedules = 0;
                        if (map == null)
                            map = Collections.emptyMap();
                        for (Iterator<Schedule> it = map.values().iterator(); it.hasNext();) {
                            Schedule schedule = it.next();
                            if (schedule == null) {
                                it.remove();
                                invalidSchedules++;
                            }
                        }
                        Log.d(TAG, String.format("Invalid Schedules: %d", invalidSchedules));
                        if (invalidSchedules != 0) {
                            //TODO: Use a TextView above the RecyclerView to display this error (and maybe others)
                            new AlertDialog.Builder(activity)
                                    .setTitle("Invalid Schedules")
                                    //.setMessage(invalidSchedules == 1 ? getString(R.string.schedules_invalid_schedules_singular) : String.format(Locale.US, getString(R.string.schedules_invalid_schedules_plural), invalidSchedules))
                                    .setMessage(getResources().getQuantityString(R.plurals.schedules_invalid_schedules, invalidSchedules, invalidSchedules))
                                    .setCancelable(true)
                                    .setPositiveButton("OK", null)
                                    .create().show();
                        }
                        listAdapter.updateSchedules(map.values());
                        break;
                    case FirebaseService.RESULT_DATABASE_NOT_FOUND:
                        Snackbar.make(activity.findViewById(android.R.id.content), R.string.setup_firebase_database_not_found, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_MALFORMED_URL:
                        Snackbar.make(activity.findViewById(android.R.id.content), R.string.setup_firebase_invalid_url, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_UNAUTHORIZED:
                        Snackbar.make(activity.findViewById(android.R.id.content), R.string.setup_firebase_unauthorized, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_IO_EXCEPTION:
                        Snackbar.make(activity.findViewById(android.R.id.content), R.string.setup_firebase_io_exception, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_SERVER_ERROR:
                        Snackbar.make(activity.findViewById(android.R.id.content), R.string.setup_firebase_server_error, Snackbar.LENGTH_LONG).show();
                        break;
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_schedules_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.fragment_schedules_menu_refresh) {
            swipeRefreshLayout.setRefreshing(true);
            RefreshList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



}
