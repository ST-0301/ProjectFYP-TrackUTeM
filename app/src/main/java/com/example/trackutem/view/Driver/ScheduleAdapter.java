package com.example.trackutem.view.Driver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Route;
import com.example.trackutem.model.Schedule;
import com.google.android.material.chip.Chip;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {
    private List<Schedule> schedules;
    private OnScheduleClickListener listener;

    // Constructor
    public ScheduleAdapter(List<Schedule> schedules, OnScheduleClickListener listener) {
        this.schedules = schedules;
        this.listener = listener;
    }

    // Adapter Core Methods
    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule schedule = schedules.get(position);

        String day = schedule.getDay();
        String chipDayText = "";
        if (day != null && !day.isEmpty()) {
            String formattedDay = day.substring(0, 1).toUpperCase() + day.substring(1);

            String type = schedule.getType() != null ? schedule.getType() : "";
            String date = null;
            try {
                java.lang.reflect.Method getDateMethod = schedule.getClass().getMethod("getDate");
                date = (String) getDateMethod.invoke(schedule);
            } catch (Exception ignored) {
            }

            if ("event".equalsIgnoreCase(type) && date != null && !date.isEmpty()) {
                chipDayText = formattedDay + " " + date;
            } else {
                chipDayText = formattedDay;
            }
            holder.chipDay.setText(chipDayText);
            holder.chipDay.setVisibility(View.VISIBLE);
        } else {
            holder.chipDay.setVisibility(View.GONE);
        }
        holder.tvTime.setText(schedule.getTime() != null ? schedule.getTime() : "");

        String type = schedule.getType() != null ? schedule.getType() : "";
        if (!type.isEmpty()) {
            type = Character.toUpperCase(type.charAt(0)) + type.substring(1);
            holder.tvType.setText("(" + type + ")");
            holder.tvType.setVisibility(View.VISIBLE);
        } else {
            holder.tvType.setVisibility(View.GONE);
        }

        // Load route name asynchronously
        holder.tvRoute.setText("Loading Route...");
        Route.resolveRouteName(schedule.getRouteId(), new Route.RouteNameCallback() {
            @Override
            public void onSuccess(String routeName) {
                if (holder.getAdapterPosition() == position) {
                    holder.tvRoute.setText(routeName);
                }
            }

            @Override
            public void onError(Exception e) {
                if (holder.getAdapterPosition() == position) {
                    holder.tvRoute.setText("Route Not Found");
                }
            }
        });

        holder.chipStatus.setText(schedule.getStatus() != null ? schedule.getStatus() : "scheduled");
        holder.itemView.setOnClickListener(v -> listener.onScheduleClick(schedule));
    }

    @Override
    public int getItemCount() {
        return schedules != null ? schedules.size() : 0;
    }
    public void updateSchedules(List<Schedule> newSchedules) {
        this.schedules = newSchedules;
        notifyDataSetChanged();
    }

    // ViewHolder
    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTime, tvRoute, tvType;
        final Chip chipDay, chipStatus;

        ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            chipDay = itemView.findViewById(R.id.chipDay);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvType = itemView.findViewById(R.id.tvType);
            chipStatus = itemView.findViewById(R.id.chipStatus);
        }
    }

    // Interface
    public interface OnScheduleClickListener {
        void onScheduleClick(Schedule schedule);
    }
}