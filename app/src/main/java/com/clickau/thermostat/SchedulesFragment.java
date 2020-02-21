package com.clickau.thermostat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class SchedulesFragment extends Fragment implements SchedulesAdapter.ViewHolderResponder {

    private static final String TAG = SchedulesFragment.class.getSimpleName();
    private static final int MODIFY_SCHEDULE_ACTIVITY_RESULT_CODE = 0;

    private SchedulesAdapter listAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedules, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fab = view.findViewById(R.id.fragment_schedules_fab);
        fab.setOnClickListener(new FABOnClickListener());

        swipeRefreshLayout = view.findViewById(R.id.fragment_schedules_swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                RefreshList();
            }
        });

        setHasOptionsMenu(true);

        RecyclerView recyclerView = view.findViewById(R.id.fragment_schedules_recycler_view);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        listAdapter = new SchedulesAdapter(new ArrayList<Schedule>(), new WeakReference<SchedulesAdapter.ViewHolderResponder>(this));
        recyclerView.setAdapter(listAdapter);

        swipeRefreshLayout.setRefreshing(true);
        RefreshList();
    }

    private void RefreshList() {
        Log.d(TAG, "Refreshing Schedules list");
        FirebaseService.getSchedules(getContext(), new ResultReceiver(new Handler()) {
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
                        Map<String, Schedule> map = null;
                        try {
                            map = gson.fromJson(str, mapType);
                        } catch (JsonParseException e) {
                            e.printStackTrace();
                            //TODO: Use a TextView above the RecyclerView to display this error (and maybe others)
                            new AlertDialog.Builder(activity)
                                    .setTitle(R.string.schedules_invalid_data_message_title)
                                    .setMessage(R.string.schedules_invalid_data)
                                    .setCancelable(true)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .create().show();
                        }
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
                                    .setTitle(R.string.schedules_invalid_schedules_message_title)
                                    //.setMessage(invalidSchedules == 1 ? getString(R.string.schedules_invalid_schedules_singular) : String.format(Locale.US, getString(R.string.schedules_invalid_schedules_plural), invalidSchedules))
                                    .setMessage(getResources().getQuantityString(R.plurals.schedules_invalid_schedules, invalidSchedules, invalidSchedules))
                                    .setCancelable(true)
                                    .setPositiveButton(android.R.string.ok, null)
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

    @Override
    public void onClickOnItem(int position) {
        Schedule schedule = listAdapter.getItemAt(position);
        Intent intent = new Intent(getContext(), ModifyScheduleActivity.class);
        intent.putExtra("schedule", schedule);
        intent.putExtra("position", position);
        intent.putExtra("action", ModifyScheduleActivity.ACTION_MODIFY);
        startActivityForResult(intent, MODIFY_SCHEDULE_ACTIVITY_RESULT_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == MODIFY_SCHEDULE_ACTIVITY_RESULT_CODE && data != null) {
            Schedule schedule = data.getParcelableExtra("schedule");
            int action = data.getIntExtra("action", -1);
            if (action == ModifyScheduleActivity.ACTION_MODIFY) {
                int position = data.getIntExtra("position", -1);
                listAdapter.setItemAt(position, schedule);
            } else {
                listAdapter.addItem(schedule);
            }
        }
    }

    private class FABOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getContext(), ModifyScheduleActivity.class);
            intent.putExtra("action", ModifyScheduleActivity.ACTION_ADD);
            startActivityForResult(intent, MODIFY_SCHEDULE_ACTIVITY_RESULT_CODE);
        }
    }
}
