package com.example.trackutem.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Schedule;
import com.google.android.material.chip.Chip;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {
    private List<Schedule> schedules;
    private OnScheduleClickListener listener;

    // region Constructor
    public ScheduleAdapter(List<Schedule> schedules, OnScheduleClickListener listener) {
        this.schedules = schedules;
        this.listener = listener;
    }

    // region Adapter Core Methods
    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule schedule = schedules.get(position);

        holder.tvTime.setText(schedule.getTime());
        holder.tvRoute.setText(schedule.getRouteName());
        holder.chipStatus.setText(schedule.getStatus() != null ? schedule.getStatus() : "scheduled");

        holder.itemView.setOnClickListener(v -> listener.onScheduleClick(schedule)
        );
    }
    @Override
    public int getItemCount() {
        return schedules != null ? schedules.size() : 0;
    }
    public void updateSchedules(List<Schedule> newSchedules) {
        this.schedules = newSchedules;
        notifyDataSetChanged();
    }

    // region ViewHolder
    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTime, tvRoute;
        final Chip chipStatus;

        ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            chipStatus = itemView.findViewById(R.id.chipStatus);
        }
    }

    // region Interface
    public interface OnScheduleClickListener {
        void onScheduleClick(Schedule schedule);
    }
}