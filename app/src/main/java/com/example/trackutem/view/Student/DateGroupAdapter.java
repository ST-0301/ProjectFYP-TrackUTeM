// DateGroupAdapter.java
package com.example.trackutem.view.Student;

import android.content.Context;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Schedule;
import com.google.android.material.transition.MaterialSharedAxis;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DateGroupAdapter extends RecyclerView.Adapter<DateGroupAdapter.DateGroupViewHolder> {
    private HashMap<String, List<Schedule>> dateGroups;
    private List<String> sortedDates;
    private final Context context;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.ENGLISH);

    public DateGroupAdapter(Context context, HashMap<String, List<Schedule>> dateGroups) {
        this.context = context;
        this.dateGroups = dateGroups;
        this.sortedDates = sortDates(new ArrayList<>(dateGroups.keySet()));
    }

    @NonNull
    @Override
    public DateGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.header_date_group, parent, false);
        return new DateGroupViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull DateGroupViewHolder holder, int position) {
        String dateKey = sortedDates.get(position);
        List<Schedule> schedules = dateGroups.get(dateKey);

        if (schedules != null) {
            schedules.sort(Comparator.comparing(Schedule::getScheduledDatetime));
        }

        try {
            Date parsedDate = dateFormat.parse(dateKey);
            if (parsedDate == null) {
                holder.tvDate.setText(dateKey);
                return;
            }
            Date today = new Date();
            String todayKey = dateFormat.format(today);

            if (dateKey.equals(todayKey)) {
                holder.tvDate.setText("Today");
            } else {
                String formattedDate = displayFormat.format(parsedDate);
                holder.tvDate.setText(formattedDate);
            }
        } catch (Exception e) {
            holder.tvDate.setText(dateKey);
        }

        ScheduleStuAdapter scheduleAdapter = new ScheduleStuAdapter(context, schedules, null, null);
        holder.rvSchedules.setLayoutManager(new LinearLayoutManager(context));
        holder.rvSchedules.setAdapter(scheduleAdapter);

        holder.headerLayout.setOnClickListener(v -> {
            boolean isExpanded = holder.rvSchedules.getVisibility() == View.VISIBLE;

            MaterialSharedAxis transition = new MaterialSharedAxis(MaterialSharedAxis.Y, !isExpanded);
            transition.setDuration(300);

            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView);

            holder.rvSchedules.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            holder.divider.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            holder.ivExpand.setRotation(isExpanded ? 0 : 180);
        });
    }
    @Override
    public int getItemCount() {
        return sortedDates.size();
    }
    public void updateData(HashMap<String, List<Schedule>> newDateGroups) {
        this.dateGroups = filterCancelledSchedules(newDateGroups);
        this.sortedDates = sortDates(new ArrayList<>(this.dateGroups.keySet()));
        notifyDataSetChanged();
    }
    private HashMap<String, List<Schedule>> filterCancelledSchedules(HashMap<String, List<Schedule>> originalGroups) {
        HashMap<String, List<Schedule>> filteredGroups = new HashMap<>();

        for (Map.Entry<String, List<Schedule>> entry : originalGroups.entrySet()) {
            String dateKey = entry.getKey();
            List<Schedule> schedules = entry.getValue();
            List<Schedule> filteredSchedules = new ArrayList<>();
            if (schedules != null) {
                for (Schedule schedule : schedules) {
                    if (!"cancelled".equals(schedule.getStatus())) {
                        filteredSchedules.add(schedule);
                    }
                }
            }
            if (!filteredSchedules.isEmpty()) {
                filteredGroups.put(dateKey, filteredSchedules);
            }
        }
        return filteredGroups;
    }
    private List<String> sortDates(List<String> dates) {
        dates.sort((date1, date2) -> {
            try {
                Date d1 = dateFormat.parse(date1);
                Date d2 = dateFormat.parse(date2);
                if (d1 != null && d2 != null) {
                    return d1.compareTo(d2);
                }
                return 0;
            } catch (Exception e) {
                Log.e("DateGroupAdapter", "Error sorting dates", e);
                return 0;
            }
        });
        return dates;
    }
    static class DateGroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        RecyclerView rvSchedules;
        View divider;
        LinearLayout headerLayout;
        ImageView ivExpand;

        DateGroupViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            rvSchedules = itemView.findViewById(R.id.rvSchedules);
            divider = itemView.findViewById(R.id.divider);
            headerLayout = itemView.findViewById(R.id.headerLayout);
            ivExpand = itemView.findViewById(R.id.ivExpand);
        }
    }
}