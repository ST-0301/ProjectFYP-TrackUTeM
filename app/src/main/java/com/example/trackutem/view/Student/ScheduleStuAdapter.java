package com.example.trackutem.view.Student;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Route;
import com.example.trackutem.model.RoutePoint;
import com.example.trackutem.model.Schedule;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ScheduleStuAdapter extends RecyclerView.Adapter<ScheduleStuAdapter.ViewHolder> {
    private List<Schedule> schedules;
    private WeakReference<Context> contextRef;
    private String fromRPointId;
    private String toRPointId;
    private Map<String, String> rpointNameCache = new HashMap<>();
    private Map<String, List<Schedule>> scheduleGroups = new HashMap<>();

    // CONSTRUCTOR
    public ScheduleStuAdapter(Context context, List<Schedule> schedules, String fromRPointId, String toRPointId) {
        this.contextRef = new WeakReference<>(context);
        this.schedules = schedules;
        this.fromRPointId = fromRPointId;
        this.toRPointId = toRPointId;
        groupAndFilterSchedules(schedules);
        preloadRPointNames();
    }

    // PRIVATE HELPER METHODS - Grouping & Filtering
// Update the groupAndFilterSchedules method
    private void groupAndFilterSchedules(List<Schedule> allSchedules) {
        scheduleGroups.clear();
        this.schedules = new ArrayList<>();

        for (Schedule schedule : allSchedules) {
            if ("cancelled".equals(schedule.getStatus())) {
                continue;
            }
            if (schedule.getBusDriverPairId() == null || schedule.getBusDriverPairId().isEmpty()) {
                continue;
            }
            String groupKey = getGroupKey(schedule);
            if (!scheduleGroups.containsKey(groupKey)) {
                scheduleGroups.put(groupKey, new ArrayList<>());
            }
            scheduleGroups.get(groupKey).add(schedule);
        }

        // Sort each group by scheduledDatetime
        for (List<Schedule> group : scheduleGroups.values()) {
            group.sort(new Comparator<Schedule>() {
                @Override
                public int compare(Schedule s1, Schedule s2) {
                    return s1.getScheduledDatetime().compareTo(s2.getScheduledDatetime());
                }
            });

            Schedule representative = getRepresentativeSchedule(group, fromRPointId, toRPointId);
            if (representative != null) {
                this.schedules.add(representative);
            }
        }

        // Sort the final list by scheduledDatetime
        this.schedules.sort(new Comparator<Schedule>() {
            @Override
            public int compare(Schedule s1, Schedule s2) {
                return s1.getScheduledDatetime().compareTo(s2.getScheduledDatetime());
            }
        });
    }
    private String getGroupKey(Schedule schedule) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String dateStr = dateFormat.format(schedule.getScheduledDatetime());
        return schedule.getRouteId() + "_" + schedule.getType() + "_" + dateStr;
    }
    private Schedule getRepresentativeSchedule(List<Schedule> group) {
        if (group.isEmpty()) return null;
        Schedule earliest = group.get(0);
        String earliestTime = getFirstPlanTime(earliest);

        for (int i = 1; i < group.size(); i++) {
            Schedule current = group.get(i);
            String currentTime = getFirstPlanTime(current);
            if (currentTime != null && earliestTime != null &&
                    currentTime.compareTo(earliestTime) < 0) {
                earliest = current;
                earliestTime = currentTime;
            }
        }
        return earliest;
    }
    private Schedule getRepresentativeSchedule(List<Schedule> group, String fromRPointId, String toRPointId) {
        if (group.isEmpty()) return null;
        if (fromRPointId != null && toRPointId != null) {
            Schedule earliestFrom = group.get(0);
            String earliestFromTime = getPlanTimeForRPoint(earliestFrom, fromRPointId);

            for (int i = 1; i < group.size(); i++) {
                Schedule current = group.get(i);
                String currentFromTime = getPlanTimeForRPoint(current, fromRPointId);
                if (currentFromTime != null && earliestFromTime != null &&
                        currentFromTime.compareTo(earliestFromTime) < 0) {
                    earliestFrom = current;
                    earliestFromTime = currentFromTime;
                }
            }
            return earliestFrom;
        } else {
            return getRepresentativeSchedule(group);
        }
    }

    // PRIVATE HELPER METHODS - Time Handling
    private String getFirstPlanTime(Schedule schedule) {
        if (schedule.getRPoints() != null && !schedule.getRPoints().isEmpty()) {
            return schedule.getRPoints().get(0).getPlanTime();
        }
        return null;
    }
    private String getPlanTimeForRPoint(Schedule schedule, String rpointId) {
        if (schedule.getRPoints() != null) {
            for (Schedule.RPointDetail rpd : schedule.getRPoints()) {
                if (rpointId.equals(rpd.getRpointId())) {
                    return rpd.getPlanTime();
                }
            }
        }
        return null;
    }

    // PRIVATE HELPER METHODS - Route Point Handling
    private void preloadRPointNames() {
        RoutePoint.getAllRPoints(new RoutePoint.AllRPointsCallback() {
            @Override
            public void onSuccess(List<RoutePoint> rpoints) {
                for (RoutePoint rpoint : rpoints) {
                    rpointNameCache.put(rpoint.getRPointId(), rpoint.getName());
                }
                notifyDataSetChanged();
            }
            @Override
            public void onError(Exception e) {
                Log.e("ScheduleStuAdapter", "Error preloading route points", e);
            }
        });
    }
    private void setRPointName(TextView textView, String rpointId, String defaultName) {
        if (rpointId != null && rpointNameCache.containsKey(rpointId)) {
            textView.setText(rpointNameCache.get(rpointId));
        } else {
            textView.setText(defaultName);
        }
    }

    // PRIVATE HELPER METHODS - Bus Counting
    private int countAvailableBuses(List<Schedule> schedules, String fromRPointId, String toRPointId) {
        Set<String> availableBusDriverPairIds = new HashSet<>();

        for (Schedule schedule : schedules) {
            String busDriverPairId = schedule.getBusDriverPairId();
            if (busDriverPairId != null && !busDriverPairId.isEmpty()) {
                if (areBothStopsAvailable(schedule, fromRPointId, toRPointId)) {
                    availableBusDriverPairIds.add(busDriverPairId);
                }
            }
        }
        return availableBusDriverPairIds.size();
    }
    private boolean areBothStopsAvailable(Schedule schedule, String fromRPointId, String toRPointId) {
        if (fromRPointId == null || toRPointId == null) {
            return false;
        }
        boolean fromAvailable = true;
        boolean toAvailable = true;

        if (schedule.getRPoints() != null) {
            for (Schedule.RPointDetail rpd : schedule.getRPoints()) {
                if (fromRPointId.equals(rpd.getRpointId())) {
                    if ("arrived".equalsIgnoreCase(rpd.getStatus())) {
                        fromAvailable = false;
                    }
                }
                if (toRPointId.equals(rpd.getRpointId())) {
                    if ("arrived".equalsIgnoreCase(rpd.getStatus())) {
                        toAvailable = false;
                    }
                }
            }
        }
        return fromAvailable && toAvailable;
    }
    private int countTotalBuses(List<Schedule> schedules) {
        Set<String> uniqueBusDriverPairIds = new HashSet<>();

        for (Schedule schedule : schedules) {
            if (schedule.getBusDriverPairId() != null && !schedule.getBusDriverPairId().isEmpty()) {
                uniqueBusDriverPairIds.add(schedule.getBusDriverPairId());
            }
        }
        return uniqueBusDriverPairIds.size();
    }

    // OVERRIDE METHODS
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
        List<Schedule.RPointDetail> rpoints = schedule.getRPoints();

        String fromTime = null;
        String toTime = null;
        String currentFromRPointId = null;
        String currentToRPointId = null;
        long durationMinutes = 0;

        if (fromRPointId != null && toRPointId != null && rpoints != null) {
            for (Schedule.RPointDetail rpd : schedule.getRPoints()) {
                if (fromRPointId.equals(rpd.getRpointId())) {
                    fromTime = rpd.getPlanTime();
                }
                if (toRPointId.equals(rpd.getRpointId())) {
                    toTime = rpd.getPlanTime();
                }
                if (fromTime != null && toTime != null) {
                    break;
                }
            }
            if (fromTime != null && toTime != null) {
                try {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    Date fromDate = timeFormat.parse(fromTime);
                    Date toDate = timeFormat.parse(toTime);

                    long fromMillis = fromDate.getTime();
                    long toMillis = toDate.getTime();
                    if (toMillis < fromMillis) {
                        toMillis += 24 * 60 * 60 * 1000;
                    }
                    durationMinutes = TimeUnit.MILLISECONDS.toMinutes(toMillis - fromMillis);
                } catch (Exception e) {
                    durationMinutes = 0;
                }
            }
        } else if (rpoints != null && !rpoints.isEmpty()) {
            currentFromRPointId = rpoints.get(0).getRpointId();
            currentToRPointId = rpoints.get(rpoints.size() - 1).getRpointId();
            fromTime = rpoints.get(0).getPlanTime();
            toTime = rpoints.get(rpoints.size() - 1).getPlanTime();

            if (fromTime != null && toTime != null) {
                try {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    Date fromDate = timeFormat.parse(fromTime);
                    Date toDate = timeFormat.parse(toTime);

                    long fromMillis = fromDate.getTime();
                    long toMillis = toDate.getTime();
                    if (toMillis < fromMillis) {
                        toMillis += 24 * 60 * 60 * 1000;
                    }
                    durationMinutes = TimeUnit.MILLISECONDS.toMinutes(toMillis - fromMillis);
                } catch (Exception e) {
                    durationMinutes = 0;
                }
            }
        }

        holder.tvPlanTimeFrom.setText(fromTime != null ? fromTime : "N/A");
        holder.tvPlanTimeTo.setText(toTime != null ? toTime : "N/A");

        if (fromRPointId != null && toRPointId != null) {
            setRPointName(holder.tvFromRPointName, fromRPointId, "From");
            setRPointName(holder.tvToRPointName, toRPointId, "To");
        } else {
            setRPointName(holder.tvFromRPointName, currentFromRPointId, "From");
            setRPointName(holder.tvToRPointName, currentToRPointId, "To");
        }

        if (durationMinutes > 0) {
            long hours = durationMinutes / 60;
            long minutes = durationMinutes % 60;
            String durationText = (hours > 0 ? hours + "h " : "") + minutes + "m";
            holder.tvDuration.setText(durationText);
        } else {
            holder.tvDuration.setText("N/A");
        }

        holder.tvRouteInfo.setText("Loading Route...");
        Route.resolveRouteName(schedule.getRouteId(), new Route.RouteNameCallback() {
            @Override
            public void onSuccess(String routeName) {
                if (holder.getAdapterPosition() == position) {
                    String type = schedule.getType() != null ? schedule.getType() : "";
                    if (!type.isEmpty()) {
                        type = Character.toUpperCase(type.charAt(0)) + type.substring(1);
                        routeName = routeName + " (" + type + ")";
                    }
                    holder.tvRouteInfo.setText(routeName);
                }
            }
            @Override
            public void onError(Exception e) {
                if (holder.getAdapterPosition() == position) {
                    holder.tvRouteInfo.setText("Route Not Found");
                }
            }
        });

        String groupKey = getGroupKey(schedule);
        List<Schedule> groupSchedules = scheduleGroups.get(groupKey);
        boolean isGetDirections = (fromRPointId != null && toRPointId != null);
        if (isGetDirections) {
            int availableBuses = countAvailableBuses(groupSchedules, fromRPointId, toRPointId);
            int totalBuses = countTotalBuses(groupSchedules);
            String busCountText = availableBuses + "/" + totalBuses + " buses\navailable";
            holder.tvBusCount.setText(busCountText);
            Context context = contextRef.get();
            if (context != null) {
                if (availableBuses == 0) {
                    holder.tvBusCount.setTextColor(context.getResources().getColor(R.color.colorError));
                } else {
                    holder.tvBusCount.setTextColor(context.getResources().getColor(R.color.primaryBlue));
                }
            }
        } else {
            int totalBuses = countTotalBuses(groupSchedules);
            holder.tvBusCount.setText(totalBuses + (totalBuses == 1 ? " bus" : " buses"));
            Context context = contextRef.get();
            if (context != null) {
                holder.tvBusCount.setTextColor(context.getResources().getColor(R.color.primaryBlue));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Context context = contextRef.get();
            if (context != null) {
                Intent intent = new Intent(context, ScheduleMapStuActivity.class);
                intent.putExtra("scheduleId", schedule.getScheduleId());
                intent.putExtra("busDriverPairId", schedule.getBusDriverPairId());
                intent.putExtra("routeId", schedule.getRouteId());
                intent.putExtra("type", schedule.getType());
                intent.putExtra("scheduledDatetime", schedule.getScheduledDatetime().getTime());
                intent.putExtra("initialBusDriverPairId", schedule.getBusDriverPairId());
                if (fromRPointId != null) {
                    intent.putExtra("fromRPointId", fromRPointId);
                }
                if (toRPointId != null) {
                    intent.putExtra("toRPointId", toRPointId);
                }
                context.startActivity(intent);
            }
        });
    }
    @Override
    public int getItemCount() {
        return schedules.size();
    }

    // INNER CLASSES
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlanTimeFrom, tvPlanTimeTo, tvDuration, tvRouteInfo, tvBusCount, tvFromRPointName, tvToRPointName;

        ViewHolder(View itemView) {
            super(itemView);
            tvPlanTimeFrom = itemView.findViewById(R.id.tvPlanTimeFrom);
            tvFromRPointName = itemView.findViewById(R.id.tvFromRPointName);
            tvPlanTimeTo = itemView.findViewById(R.id.tvPlanTimeTo);
            tvToRPointName = itemView.findViewById(R.id.tvToRPointName);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvRouteInfo = itemView.findViewById(R.id.tvRouteInfo);
            tvBusCount = itemView.findViewById(R.id.tvBusCount);
        }
    }
}