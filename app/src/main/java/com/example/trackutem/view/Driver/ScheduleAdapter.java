package com.example.trackutem.view.Driver;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Route;
import com.example.trackutem.model.Schedule;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {
    private List<Schedule> schedules;
    private Context context;

    // Constructor
    public ScheduleAdapter(List<Schedule> schedules) {
        this.schedules = schedules;
    }

    // Adapter Core Methods
    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule schedule = schedules.get(position);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeText = timeFormat.format(schedule.getScheduledDatetime());
        holder.tvTime.setText(timeText);

        String status = schedule.getStatus() != null ? schedule.getStatus().toLowerCase(Locale.ENGLISH) : "scheduled";

        if (status.equals("completed")) {
            holder.chipStatus.setText("Completed");
            holder.chipStatus.setChipBackgroundColorResource(R.color.white);
            holder.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.textPrimary));
            holder.chipStatus.setChipStrokeWidth(1f);
            holder.chipStatus.setChipStrokeColorResource(R.color.darkGray);
        } else {
            holder.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.chipStatus.setChipStrokeWidth(0f);

            switch (status) {
                case "scheduled":
                    holder.chipStatus.setText("Start Trip");
                    holder.chipStatus.setChipBackgroundColorResource(R.color.primaryBlue);
                    break;
                case "in_progress":
                    holder.chipStatus.setText("Continue");
                    holder.chipStatus.setChipBackgroundColorResource(R.color.tertiaryOrange);
                    break;
                default:
                    holder.chipStatus.setText("Unknown");
                    holder.chipStatus.setChipBackgroundColorResource(R.color.colorError);
                    break;
            }
        }
        holder.tvRoute.setText(schedule.getPreloadedRouteName());
        holder.tvBusPlate.setText(schedule.getPreloadedBusPlate());
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ScheduleDetailsActivity.class);
            intent.putExtra("routeId", schedule.getRouteId());
            intent.putExtra("scheduleId", schedule.getScheduleId());
            context.startActivity(intent);
        });
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
        final TextView tvTime, tvRoute, tvBusPlate;
        final Chip chipStatus;

        ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvBusPlate = itemView.findViewById(R.id.tvBusPlate);
            chipStatus = itemView.findViewById(R.id.chipStatus);
        }
    }

    // Interface
}