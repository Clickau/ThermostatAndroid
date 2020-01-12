package com.clickau.thermostat;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

        swipeRefreshLayout = getActivity().findViewById(R.id.fragment_schedules_swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                RefreshList();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        setHasOptionsMenu(true);

        recyclerView = getActivity().findViewById(R.id.fragment_schedules_recycler_view);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        listAdapter = new SchedulesAdapter(new ArrayList<Schedule>());
        recyclerView.setAdapter(listAdapter);

        swipeRefreshLayout.setRefreshing(true);

        FirebaseService.get(getContext(), "/Program", new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                switch (resultCode) {
                    case FirebaseService.RESULT_SUCCESS:
                        String str = resultData.getString("result");
                        Log.d(TAG, String.format("ScheduleString: %s", str));
                        Gson gson = new GsonBuilder().registerTypeAdapter(Schedule.class, new ScheduleDeserializer()).create();
                        Type mapType = new TypeToken<Map<String, Schedule>>() {}.getType();
                        Map<String, Schedule> map = gson.fromJson(str, mapType);
                        Log.d(TAG, String.format("map: %s", map.toString()));
                        listAdapter.updateSchedules(map.values());
                        swipeRefreshLayout.setRefreshing(false);
                        break;
                    default:
                        Log.d(TAG, String.format("resultCode: %d", resultCode));
                        break;
                }
            }
        });

    }

    public void RefreshList() {
        Log.d(TAG, "Refreshing Schedules list");
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
            swipeRefreshLayout.setRefreshing(false);
        }

        return super.onOptionsItemSelected(item);
    }

    private static class ScheduleDeserializer implements JsonDeserializer<Schedule> {

        //TODO: handle wrong schedule format exceptions

        @Override
        public Schedule deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject scheduleJson = json.getAsJsonObject();
            Schedule.Repeat repeat = Schedule.Repeat.valueOf(scheduleJson.get("repeat").getAsString()); // TODO: manage IllegalArgumentException
            float temperature = scheduleJson.get("setTemp").getAsFloat();
            int sH = scheduleJson.get("sH").getAsInt();
            int sM = scheduleJson.get("sM").getAsInt();
            int eH = scheduleJson.get("eH").getAsInt();
            int eM = scheduleJson.get("eM").getAsInt();
            Date startDate, endDate;
            int[] weekDays = new int[]{};
            Calendar cal = Calendar.getInstance();
            switch (repeat) {
                case Weekly:
                    weekDays = context.deserialize(scheduleJson.get("weekDays").getAsJsonArray(), int[].class);
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
            return new Schedule(repeat, temperature, startDate, endDate, weekDays);
        }
    }

}
