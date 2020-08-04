package com.clickau.thermostat;

import android.app.Activity;
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
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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
import java.util.Iterator;

public class SchedulesFragment extends Fragment implements SchedulesAdapter.ViewHolderResponder, SchedulesAdapter.OnSelectModeChangedListener {

    private static final String TAG = SchedulesFragment.class.getSimpleName();
    private static final int MODIFY_SCHEDULE_ACTIVITY_RESULT_CODE = 0;

    private SchedulesAdapter listAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private FloatingActionButton uploadFAB;
    private FloatingActionButton deleteFAB;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedules, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.fragment_schedules_progress_bar);
        progressBar.setVisibility(View.GONE);

        uploadFAB = view.findViewById(R.id.fragment_schedules_upload_fab);
        uploadFAB.setOnClickListener(new UploadOnClickListener());

        deleteFAB = view.findViewById(R.id.fragment_schedules_delete_fab);
        deleteFAB.setOnClickListener(v -> listAdapter.deleteSelected());
        deleteFAB.setVisibility(View.GONE);

        swipeRefreshLayout = view.findViewById(R.id.fragment_schedules_swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::OnRefresh);

        setHasOptionsMenu(true);

        RecyclerView recyclerView = view.findViewById(R.id.fragment_schedules_recycler_view);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        if (savedInstanceState == null) {
            //noinspection Convert2Diamond
            listAdapter = new SchedulesAdapter(new ArrayList<Schedule>(), new WeakReference<SchedulesAdapter.ViewHolderResponder>(this), new WeakReference<SchedulesAdapter.OnSelectModeChangedListener>(this));
        } else {
            ArrayList<Schedule> list = savedInstanceState.getParcelableArrayList("schedules");
            //noinspection Convert2Diamond
            listAdapter = new SchedulesAdapter(list, new WeakReference<SchedulesAdapter.ViewHolderResponder>(this), new WeakReference<SchedulesAdapter.OnSelectModeChangedListener>(this));
        }
        recyclerView.setAdapter(listAdapter);

        if (savedInstanceState == null) {
            swipeRefreshLayout.setRefreshing(true);
            RefreshList();
        }
    }

    private void OnRefresh() {
        listAdapter.clearSelected();
        if (listAdapter.isSchedulesModifiedLocally()) {
            Activity activity = getActivity();
            if (activity == null) return;
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.schedules_discard_changes_message_title)
                    .setMessage(R.string.schedules_discard_changes_message)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> RefreshList())
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> swipeRefreshLayout.setRefreshing(false))
                    .create().show();
        } else {
            RefreshList();
        }
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
                        Type mapType = new TypeToken<ArrayList<Schedule>>() {}.getType();
                        ArrayList<Schedule> list = null;
                        try {
                            list = gson.fromJson(str, mapType);
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
                        if (list == null)
                            list = new ArrayList<>();
                        for (Iterator<Schedule> it = list.iterator(); it.hasNext();) {
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
                        listAdapter.updateDataSet(list);
                        break;
                    case FirebaseService.RESULT_DATABASE_NOT_FOUND:
                        Snackbar.make(activity.findViewById(R.id.fragment_schedules_coordinator_layout), R.string.firebase_database_not_found, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_MALFORMED_URL:
                        Snackbar.make(activity.findViewById(R.id.fragment_schedules_coordinator_layout), R.string.setup_firebase_invalid_url, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_UNAUTHORIZED:
                        Snackbar.make(activity.findViewById(R.id.fragment_schedules_coordinator_layout), R.string.firebase_unauthorized, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_IO_EXCEPTION:
                        Snackbar.make(activity.findViewById(R.id.fragment_schedules_coordinator_layout), R.string.firebase_io_exception, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_SERVER_ERROR:
                        Snackbar.make(activity.findViewById(R.id.fragment_schedules_coordinator_layout), R.string.firebase_server_error, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_TIMEOUT:
                        Snackbar.make(activity.findViewById(R.id.fragment_schedules_coordinator_layout), R.string.firebase_timeout, Snackbar.LENGTH_LONG).show();
                        break;
                    case FirebaseService.RESULT_NOT_INITIALIZED:
                        Snackbar.make(activity.findViewById(R.id.fragment_schedules_coordinator_layout), R.string.firebase_not_initialized, Snackbar.LENGTH_LONG).show();
                        break;
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelableArrayList("schedules", listAdapter.getSchedules());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_schedules_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.fragment_schedules_menu_refresh:
                swipeRefreshLayout.setRefreshing(true);
                OnRefresh();
                return true;
            case R.id.fragment_schedules_menu_select_all:
                listAdapter.selectAll();
                return true;
            case R.id.fragment_schedules_menu_deselect_all:
                listAdapter.clearSelected();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClickOnItem(int viewType, int position) {

        if (viewType == SchedulesAdapter.VIEW_TYPE_ADD) {
            Intent intent = new Intent(getContext(), ModifyScheduleActivity.class);
            intent.putExtra("action", ModifyScheduleActivity.ACTION_ADD);
            startActivityForResult(intent, MODIFY_SCHEDULE_ACTIVITY_RESULT_CODE);
            return;
        }

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

    @Override
    public void OnSelectModeChanged(boolean selectMode) {
        if (selectMode) {
            // replace upload FAB with delete FAB
            uploadFAB.setVisibility(View.GONE);
            deleteFAB.setVisibility(View.VISIBLE);
        } else {
            // bring back upload FAB
            uploadFAB.setVisibility(View.VISIBLE);
            deleteFAB.setVisibility(View.GONE);
        }
    }

    private class UploadOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (listAdapter.isSchedulesConflicting()) {
                Activity activity = getActivity();
                if (activity == null) return;
                Snackbar.make(activity.findViewById(R.id.fragment_schedules_coordinator_layout), "Some schedules of the same repeat overlap. Please resolve the conflict before committing", Snackbar.LENGTH_LONG).show();
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            Gson gson = new GsonBuilder().registerTypeAdapter(Schedule.class, new Schedule.Serializer()).create();
            String json = gson.toJson(listAdapter.getSchedules());
            Log.d(TAG, json);
            FirebaseService.setSchedules(getContext(), json, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    Activity activity = getActivity();
                    if (activity == null) return;
                    CoordinatorLayout layout = activity.findViewById(R.id.fragment_schedules_coordinator_layout);
                    Log.d(TAG, String.format("result: %d", resultCode));
                    progressBar.setVisibility(View.GONE);
                    switch (resultCode) {
                        case FirebaseService.RESULT_SUCCESS:
                            listAdapter.setSchedulesModifiedLocally(false);
                            Snackbar.make(layout, R.string.firebase_success, Snackbar.LENGTH_LONG).show();
                            break;
                        case FirebaseService.RESULT_DATABASE_NOT_FOUND:
                        case FirebaseService.RESULT_MALFORMED_URL:
                            Snackbar.make(layout, R.string.firebase_database_not_found, Snackbar.LENGTH_LONG).show();
                            break;
                        case FirebaseService.RESULT_UNAUTHORIZED:
                            Snackbar.make(layout, R.string.firebase_unauthorized, Snackbar.LENGTH_LONG).show();
                            break;
                        case FirebaseService.RESULT_IO_EXCEPTION:
                            Snackbar.make(layout, R.string.firebase_io_exception, Snackbar.LENGTH_LONG).show();
                            break;
                        case FirebaseService.RESULT_SERVER_ERROR:
                            Snackbar.make(layout, R.string.firebase_server_error, Snackbar.LENGTH_LONG).show();
                            break;
                        case FirebaseService.RESULT_BAD_REQUEST:
                            throw new RuntimeException("Firebase returned Bad Request when sending schedules");
                        case FirebaseService.RESULT_NOT_INITIALIZED:
                            Snackbar.make(layout, R.string.firebase_not_initialized, Snackbar.LENGTH_LONG).show();
                            break;
                        case FirebaseService.RESULT_TIMEOUT:
                            Snackbar.make(layout, R.string.firebase_timeout, Snackbar.LENGTH_LONG).show();
                            break;
                    }
                }
            });
        }
    }
}
