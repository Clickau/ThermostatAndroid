package com.clickau.thermostat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

@SuppressWarnings("WeakerAccess")
public class SchedulesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_NORMAL = 0;
    public static final int VIEW_TYPE_ADD = 1;

    public interface ViewHolderResponder {
        void onClickOnItem(int viewType, int position);
    }

    public interface OnSelectModeChangedListener {
        void OnSelectModeChanged(boolean selectMode);
    }

    public class SchedulesViewHolder extends RecyclerView.ViewHolder {

        private final TextView temperatureTextView;
        private final TextView repeatTextView;
        private final TextView startTextView;
        private final TextView endTextView;
        private final TextView weekdaysTextView;
        private final CheckBox checkBox;
        private int position;

        private SchedulesViewHolder(@NonNull final View itemView, final WeakReference<ViewHolderResponder> responder){
            super(itemView);
            temperatureTextView = itemView.findViewById(R.id.schedule_view_temperature_text_view);
            repeatTextView = itemView.findViewById(R.id.schedule_view_repeat_text_view);
            startTextView = itemView.findViewById(R.id.schedule_view_start_text_view);
            endTextView = itemView.findViewById(R.id.schedule_view_end_text_view);
            weekdaysTextView = itemView.findViewById(R.id.schedule_view_weekdays_text_view);
            checkBox = itemView.findViewById(R.id.schedule_view_checkbox);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isSelectModeActive()) {
                        if (selectedArray.contains(position)) {
                            checkBox.setChecked(false);
                        } else {
                            checkBox.setChecked(true);
                        }
                    } else {
                        responder.get().onClickOnItem(VIEW_TYPE_NORMAL, position);
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (isSelectModeActive())
                        return false;
                    checkBox.setChecked(true);
                    return true;
                }
            });

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isSelectModeActive()) {
                        if (!isChecked) {
                            // it was checked before
                            selectedArray.remove((Integer) position);
                            if (selectedArray.isEmpty())
                                setSelectModeActive(false);
                        } else {
                            selectedArray.add(position);
                        }
                    } else if (isChecked) {
                        setSelectModeActive(true);
                        selectedArray.add(position);
                    }
                    // if the checkbox is unchecked programmatically when deleting schedules, when isSelectModeActive is false, don't do anything
                }
            });
        }

        private void bind(@NonNull Schedule schedule, int position) {
            //TODO: Ask for preferred temperature scale or get it from system
            temperatureTextView.setText(String.format(Locale.getDefault(),"%.1f°C", schedule.getTemperature()));
            repeatTextView.setText(schedule.getRepeat().toString());
            startTextView.setText(String.format(Locale.US, App.getRes().getString(R.string.schedules_start) + ": %s", schedule.getStartString()));
            endTextView.setText(String.format(Locale.US, App.getRes().getString(R.string.schedules_end) + ": %s", schedule.getEndString()));
            if (schedule.getRepeat().equals(Schedule.Repeat.Weekly)) {
                StringBuilder weekdayBuilder = new StringBuilder(App.getRes().getString(R.string.schedules_weekdays_on));
                weekdayBuilder.append(": ");
                String[] weekdayStrings = new DateFormatSymbols(Locale.getDefault()).getShortWeekdays(); // index 1 is Sunday, index 2 is Monday etc (as specified by Calendar.MONDAY etc)
                boolean[] weekdays = schedule.getWeekdays();
                for (int i = 1; i <= 7; i++) {
                    if (weekdays[i]) {
                        weekdayBuilder.append(weekdayStrings[i]);
                        weekdayBuilder.append(", ");
                    }
                }
                weekdayBuilder.delete(weekdayBuilder.length() - 2, weekdayBuilder.length());
                weekdaysTextView.setText(weekdayBuilder.toString());
                weekdaysTextView.setVisibility(View.VISIBLE);
            } else {
                weekdaysTextView.setVisibility(View.GONE);
            }
            this.position = position;
        }
    }

    public static class AddButtonViewHolder extends RecyclerView.ViewHolder {

        public AddButtonViewHolder(@NonNull View itemView, final WeakReference<ViewHolderResponder> responder) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    responder.get().onClickOnItem(VIEW_TYPE_ADD, 0);
                }
            });
        }
    }

    private final ArrayList<Schedule> schedules;
    private final WeakReference<ViewHolderResponder> responder;
    private boolean schedulesModifiedLocally = false;
    private boolean selectModeActive = false;
    private final ArrayList<Integer> selectedArray = new ArrayList<>();
    private AddButtonViewHolder addButtonViewHolder;
    private final WeakReference<OnSelectModeChangedListener> selectModeChangedListener;
    private final ArrayList<WeakReference<SchedulesViewHolder>> scheduleViewHolders = new ArrayList<>();

    public SchedulesAdapter(ArrayList<Schedule> schedules, WeakReference<ViewHolderResponder> responder, WeakReference<OnSelectModeChangedListener> selectModeChangedListener) {
        this.schedules = schedules;
        this.responder = responder;
        this.selectModeChangedListener = selectModeChangedListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_ADD;
        }
        return VIEW_TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == VIEW_TYPE_ADD) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_schedule_button, parent, false);
            addButtonViewHolder = new AddButtonViewHolder(view, responder);
            return addButtonViewHolder;
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.schedule_view, parent, false);
        SchedulesViewHolder holder =  new SchedulesViewHolder(view, responder);
        scheduleViewHolders.add(new WeakReference<>(holder));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder.getItemViewType() == VIEW_TYPE_NORMAL) {
            SchedulesViewHolder scheduleHolder = (SchedulesViewHolder) holder;
            Schedule schedule = schedules.get(position - 1);
            scheduleHolder.bind(schedule, position - 1);
        }
    }

    @Override
    public int getItemCount() {
        return schedules.size() + 1;
    }

    public void updateSchedules(Collection<? extends Schedule> newSchedules) {
        schedules.clear();
        schedules.addAll(newSchedules);
        notifyDataSetChanged();
        schedulesModifiedLocally = false;
    }

    public Schedule getItemAt(int position) {
        return schedules.get(position);
    }

    public void setItemAt(int position, Schedule newSchedule) {
        schedules.set(position, newSchedule);
        notifyItemChanged(position + 1);
        schedulesModifiedLocally = true;
    }

    public void addItem(Schedule newSchedule) {
        schedules.add(0, newSchedule);
        notifyItemInserted(1);
        schedulesModifiedLocally = true;
    }

    public void deleteSelected() {
        // sort in descending order so we don't mess up indices when removing
        Collections.sort(selectedArray, Collections.<Integer>reverseOrder());
        for (int position : selectedArray) {
            schedules.remove(position);
        }
        selectedArray.clear();
        setSelectModeActive(false);
        notifyDataSetChanged();
        schedulesModifiedLocally = true;
        // do this at the end, so that select mode is not active, so the checked listeners don't do anything
        for (WeakReference<SchedulesViewHolder> holder : scheduleViewHolders) {
            holder.get().checkBox.setChecked(false);
        }
    }

    public void setSelectModeActive(boolean selectModeActive) {
        addButtonViewHolder.itemView.setEnabled(!selectModeActive);
        this.selectModeActive = selectModeActive;
        selectModeChangedListener.get().OnSelectModeChanged(selectModeActive);
    }

    public void clearSelected() {
        setSelectModeActive(false);
        selectedArray.clear();
        // do this at the end, so that select mode is not active, so the checked listeners don't do anything
        for (WeakReference<SchedulesViewHolder> holder : scheduleViewHolders) {
            holder.get().checkBox.setChecked(false);
        }
    }

    public ArrayList<Schedule> getSchedules() {
        return schedules;
    }

    public boolean isSchedulesModifiedLocally() {
        return schedulesModifiedLocally;
    }

    public void setSchedulesModifiedLocally(boolean value) {
        schedulesModifiedLocally = value;
    }

    public boolean isSelectModeActive() {
        return selectModeActive;
    }

}
