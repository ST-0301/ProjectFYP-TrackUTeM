package com.example.trackutem.view.Student;

import android.content.Context;
import android.content.Intent;
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
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ScheduleStuAdapter extends RecyclerView.Adapter<ScheduleStuAdapter.ViewHolder> {
    private List<Schedule> schedules;
    private WeakReference<Context> contextRef;
    private String fromRPointId;

    public ScheduleStuAdapter(Context context, List<Schedule> schedules, String fromRPointId) {
        this.contextRef = new WeakReference<>(context);
        this.schedules = schedules;
        this.fromRPointId = fromRPointId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_stu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Schedule schedule = schedules.get(position);

        // Find expDepTime for the selected fromRPointId
//        String expDepTime = null;
//        if (fromRPointId != null && schedule.getRPoints() != null) {
//            for (Schedule.RPointDetail rpd : schedule.getRPoints()) {
//                if (fromRPointId.equals(rpd.getRPointId())) {
//                    expDepTime = rpd.getExpDepTime();
//                    break;
//                }
//            }
//        }
//        holder.tvTime.setText(expDepTime != null && !expDepTime.isEmpty() ? expDepTime : schedule.getTime());
        // holder.tvTime.setText(schedule.getTime());

        // Format time
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeText = timeFormat.format(schedule.getScheduledDatetime());
        holder.tvTime.setText(timeText);

        holder.chipStatus.setText(schedule.getStatus().toUpperCase());
        int statusColor = getStatusColor(schedule.getStatus());
        holder.chipStatus.setChipBackgroundColorResource(statusColor);

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

        String type = schedule.getType() != null ? schedule.getType() : "";
        if (!type.isEmpty()) {
            type = Character.toUpperCase(type.charAt(0)) + type.substring(1);
            holder.tvType.setText("(" + type + ")");
        }

        holder.itemView.setOnClickListener(v -> {
            Context context = contextRef.get();
            if (context != null) {
                Intent intent = new Intent(context, ScheduleMapStuActivity.class);
                intent.putExtra("scheduleId", schedule.getScheduleId());
                intent.putExtra("driverId", schedule.getDriverId());
                intent.putExtra("busId", schedule.getBusId());
                if (fromRPointId != null) {
                    intent.putExtra("fromRPointId", fromRPointId);
                }
                context.startActivity(intent);
            }
        });
    }

    private int getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "in_progress":
                return R.color.status_in_progress;
            case "completed":
                return R.color.status_completed;
            case "cancelled":
                return R.color.status_cancelled;
            default: // scheduled
                return R.color.status_scheduled;
        }
    }
    public void updateSchedules(List<Schedule> newSchedules) {
        // this.schedules = newSchedules;
        this.schedules.clear();
        this.schedules.addAll(newSchedules);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvRoute, tvType;
        Chip chipStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvType = itemView.findViewById(R.id.tvType);
        }
    }
}