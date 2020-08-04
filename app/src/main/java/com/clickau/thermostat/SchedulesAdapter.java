package com.clickau.thermostat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.eclipse.collections.impl.list.Interval;

import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

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
        private final TextView errorTextView;
        private int position;
        private boolean ignoreCheckedChange = false;

        private SchedulesViewHolder(@NonNull final View itemView, final WeakReference<ViewHolderResponder> responder){
            super(itemView);
            temperatureTextView = itemView.findViewById(R.id.schedule_view_temperature_text_view);
            repeatTextView = itemView.findViewById(R.id.schedule_view_repeat_text_view);
            startTextView = itemView.findViewById(R.id.schedule_view_start_text_view);
            endTextView = itemView.findViewById(R.id.schedule_view_end_text_view);
            weekdaysTextView = itemView.findViewById(R.id.schedule_view_weekdays_text_view);
            checkBox = itemView.findViewById(R.id.schedule_view_checkbox);
            errorTextView = itemView.findViewById(R.id.schedule_view_error_text_view);

            itemView.setOnClickListener(v -> {
                if (isSelectModeActive()) {
                    checkBox.toggle();
                } else {
                    ViewHolderResponder viewHolderResponder = responder.get();
                    if (viewHolderResponder == null)
                        return;
                    viewHolderResponder.onClickOnItem(VIEW_TYPE_NORMAL, position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (isSelectModeActive())
                    return false;
                checkBox.setChecked(true);
                return true;
            });

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (ignoreCheckedChange)
                    return;
                if (isSelectModeActive()) {
                    if (!isChecked) {
                        // it was checked before
                        selectedSet.remove(position);
                        if (selectedSet.isEmpty())
                            setSelectModeActive(false);
                    } else {
                        selectedSet.add(position);
                    }
                } else if (isChecked) {
                    setSelectModeActive(true);
                    selectedSet.add(position);
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
            ignoreCheckedChange = true;
            if (selectedSet.contains(position))
                checkBox.setChecked(true);
            else
                checkBox.setChecked(false);
            ignoreCheckedChange = false;
            if (conflictingSet.contains(position))
                errorTextView.setVisibility(View.VISIBLE);
            else
                errorTextView.setVisibility(View.GONE);
        }
    }

    public static class AddButtonViewHolder extends RecyclerView.ViewHolder {

        public AddButtonViewHolder(@NonNull View itemView, final WeakReference<ViewHolderResponder> responder) {
            super(itemView);
            itemView.setOnClickListener(v -> {
                ViewHolderResponder viewHolderResponder = responder.get();
                if (viewHolderResponder == null)
                    return;
                viewHolderResponder.onClickOnItem(VIEW_TYPE_ADD, 0);
            });
        }
    }

    private final ArrayList<Schedule> schedules;
    private final WeakReference<ViewHolderResponder> viewHolderResponderReference;
    private boolean schedulesModifiedLocally = false;
    private boolean selectModeActive = false;
    @SuppressWarnings("RedundantTypeArguments")
    private final Set<Integer> selectedSet = new TreeSet<>(Collections.<Integer>reverseOrder()); // make the set sorted in descending order
    private AddButtonViewHolder addButtonViewHolder;
    private final WeakReference<OnSelectModeChangedListener> selectModeChangedListenerReference;
    private final Set<Integer> conflictingSet = new HashSet<>();

    public SchedulesAdapter(ArrayList<Schedule> schedules, WeakReference<ViewHolderResponder> viewHolderResponderReference, WeakReference<OnSelectModeChangedListener> selectModeChangedListenerReference) {
        this.schedules = schedules;
        this.viewHolderResponderReference = viewHolderResponderReference;
        this.selectModeChangedListenerReference = selectModeChangedListenerReference;
        findConflictingSchedules();
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
            addButtonViewHolder = new AddButtonViewHolder(view, viewHolderResponderReference);
            return addButtonViewHolder;
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.schedule_view, parent, false);
        return new SchedulesViewHolder(view, viewHolderResponderReference);
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

    public void updateDataSet(Collection<? extends Schedule> newSchedules) {
        schedules.clear();
        schedules.addAll(newSchedules);
        schedulesModifiedLocally = false;
        // notifyDataSetChanged() is called in findConflictingSchedules();
        findConflictingSchedules();
    }

    public Schedule getItemAt(int position) {
        return schedules.get(position);
    }

    public void setItemAt(int position, Schedule newSchedule) {
        schedules.set(position, newSchedule);
        //notifyItemChanged(position + 1);
        schedulesModifiedLocally = true;
        // notifyDataSetChanged() is called in findConflictingSchedules();
        findConflictingSchedules();
    }

    public void addItem(Schedule newSchedule) {
        schedules.add(0, newSchedule);
        //notifyItemInserted(1);
        schedulesModifiedLocally = true;
        // notifyDataSetChanged() is called in findConflictingSchedules();
        findConflictingSchedules();
    }

    public void deleteSelected() {
        // the set is in descending order so the indices are not messed up
        for (int position : selectedSet) {
            schedules.remove(position);
        }
        selectedSet.clear();
        setSelectModeActive(false);
        schedulesModifiedLocally = true;
        // notifyDataSetChanged() is called in findConflictingSchedules();
        findConflictingSchedules();
    }

    public void setSelectModeActive(boolean selectModeActive) {
        addButtonViewHolder.itemView.setEnabled(!selectModeActive);
        this.selectModeActive = selectModeActive;
        OnSelectModeChangedListener listener = selectModeChangedListenerReference.get();
        listener.OnSelectModeChanged(selectModeActive);
    }

    public void clearSelected() {
        setSelectModeActive(false);
        selectedSet.clear();
        notifyDataSetChanged();
    }

    /**
     * Checks if any two schedules overlap and have the same repeat and if there are, stores their indices in conflictingSet
     */
    private void findConflictingSchedules() {
        conflictingSet.clear();
        for (int i = 0; i < schedules.size(); i++) {
            Schedule s1 = schedules.get(i);
            for (int j = i + 1; j < schedules.size(); j++) {
                Schedule s2 = schedules.get(j);
                if (s1.getRepeat() == s2.getRepeat()) {
                    if (s1.getRepeat() == Schedule.Repeat.Weekly) {
                        boolean sameWeekday = false;
                        for (int k = 1; k <= 7; k++) {
                            if (s1.getWeekdays()[k] && s2.getWeekdays()[k]) {
                                // they are both active on the same weekday
                                sameWeekday = true;
                            }
                        }
                        if (!sameWeekday) {
                            // they don't have any weekdays in common, clearly they can't overlap
                            continue;
                        }
                    }
                    if (s1.getEnd().compareTo(s2.getStart()) > 0 && s2.getEnd().compareTo(s1.getStart()) > 0) {
                        // they overlap
                        conflictingSet.add(i);
                        conflictingSet.add(j);
                    }
                }
            }
        }
        // update the error TextViews
        notifyDataSetChanged();
    }

    public void selectAll() {
        if (!isSelectModeActive())
            setSelectModeActive(true);
        selectedSet.addAll(Interval.zeroTo(schedules.size() - 1));
        notifyDataSetChanged();
    }

    public boolean isSchedulesConflicting() {
        return !conflictingSet.isEmpty();
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
