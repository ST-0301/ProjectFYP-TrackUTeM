// DateGroupAdapter.java
package com.example.trackutem.view.Student;

import android.content.Context;
import android.transition.TransitionManager;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DateGroupAdapter extends RecyclerView.Adapter<DateGroupAdapter.DateGroupViewHolder> {
    private HashMap<String, List<Schedule>> dateGroups;
    private List<String> sortedDates;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.ENGLISH);

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

        try {
            Date parsedDate = dateFormat.parse(dateKey);
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

        // Set up nested RecyclerView
        ScheduleStuAdapter scheduleAdapter = new ScheduleStuAdapter(context, schedules, null);
        holder.rvSchedules.setLayoutManager(new LinearLayoutManager(context));
        holder.rvSchedules.setAdapter(scheduleAdapter);

        // Set click listener for header
        holder.headerLayout.setOnClickListener(v -> {
            boolean isExpanded = holder.rvSchedules.getVisibility() == View.VISIBLE;

            // Create and configure the transition
            MaterialSharedAxis transition = new MaterialSharedAxis(MaterialSharedAxis.Y, !isExpanded);
            transition.setDuration(300);

            // Begin the transition
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
        this.dateGroups = newDateGroups;
        this.sortedDates = sortDates(new ArrayList<>(newDateGroups.keySet()));
        notifyDataSetChanged();
    }
    private List<String> sortDates(List<String> dates) {
        Collections.sort(dates, new Comparator<String>() {
            @Override
            public int compare(String date1, String date2) {
                try {
                    Date d1 = dateFormat.parse(date1);
                    Date d2 = dateFormat.parse(date2);
                    return d1.compareTo(d2);
                } catch (Exception e) {
                    return 0;
                }
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