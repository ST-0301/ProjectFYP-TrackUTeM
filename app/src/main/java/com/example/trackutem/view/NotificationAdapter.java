package com.example.trackutem.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Notification;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private final List<Notification> notificationList;

    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    private static String getRelativeTime(Date date) {
        if (date == null) return "Unknown";

        long now = System.currentTimeMillis();
        long time = date.getTime();
        long diff = now - time;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (diff < TimeUnit.DAYS.toMillis(30)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (diff < TimeUnit.DAYS.toMillis(365)) {
            long months = TimeUnit.MILLISECONDS.toDays(diff) / 30;
            return months + " month" + (months > 1 ? "s" : "") + " ago";
        } else {
            long years = TimeUnit.MILLISECONDS.toDays(diff) / 365;
            return years + " year" + (years > 1 ? "s" : "") + " ago";
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvBody;
        private final TextView tvTimestamp;
        private final TextView tvDetails;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvBody = itemView.findViewById(R.id.tvBody);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvDetails = itemView.findViewById(R.id.tvDetails);
        }

        void bind(Notification notification) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String departureTime = notification.getScheduledDatetime() != null ?
                    timeFormat.format(notification.getScheduledDatetime()) : "Unknown";

            String title;
            String body;
            String details;

            switch (notification.getType()) {
                case "delay":
                    title = "[DELAYED] Bus Delay Alert";
                    body = String.format(Locale.getDefault(), "Bus %s on your %s route is delayed by %d minutes.",
                            notification.getBusPlateNumber(), departureTime, notification.getLatenessMinutes());
                    details = String.format(Locale.getDefault(), "Route: %s (%s) | Bus: %s | Departure %s",
                            notification.getRouteName(), notification.getScheduleType(),
                            notification.getBusPlateNumber(), departureTime);
                    break;

                case "new_assignment":
                    title = "[NEW] New Assignment";
                    body = "You have been assigned to a new trip.";
                    details = String.format("Route: %s (%s) | Bus: %s | Departure %s",
                            notification.getRouteName(), notification.getScheduleType(),
                            notification.getBusPlateNumber(), departureTime);
                    break;

                case "cancelled_assignment":
                    title = "[CANCELLED] Assignment Cancelled";
                    body = "Your scheduled trip has been cancelled.";
                    details = String.format("Route: %s (%s) | Bus: %s | Departure %s",
                            notification.getRouteName(), notification.getScheduleType(),
                            notification.getBusPlateNumber(), departureTime);
                    break;

                default:
                    title = "Notification";
                    body = "You have a new notification.";
                    details = "";
            }
            tvTitle.setText(title);
            tvBody.setText(body);
            tvDetails.setText(details);
            tvDetails.setVisibility(details.isEmpty() ? View.GONE : View.VISIBLE);

            if (notification.getCreated() != null) {
                String relativeTime = getRelativeTime(notification.getCreated());
                tvTimestamp.setText(relativeTime);
            } else {
                tvTimestamp.setText("Unknown");
            }
        }
    }
}