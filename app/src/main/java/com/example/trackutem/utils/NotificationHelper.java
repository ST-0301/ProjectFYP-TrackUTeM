package com.example.trackutem.utils;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.trackutem.MainDrvActivity;
import com.example.trackutem.MainStuActivity;
import com.example.trackutem.R;
import com.example.trackutem.view.Driver.ScheduleDetailsActivity;
import com.example.trackutem.view.LoginActivity;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID_FOREGROUND = "TRACKUTEM_FOREGROUND";
    private static final String CHANNEL_ID_GEOFENCE = "TRACKUTEM_GEOFENCE";
    private static final String CHANNEL_ID_BUS_DELAY = "TRACKUTEM_BUS_DELAY";
    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Foreground channel
            NotificationChannel foregroundChannel = new NotificationChannel(
                    CHANNEL_ID_FOREGROUND,
                    "Foreground Service",
                    NotificationManager.IMPORTANCE_LOW);

            // Geofence channel
            NotificationChannel geofenceChannel = new NotificationChannel(
                    CHANNEL_ID_GEOFENCE,
                    "Geofence Alerts",
                    NotificationManager.IMPORTANCE_HIGH);

            // Delay channel
            NotificationChannel busDelayChannel = new NotificationChannel(
                    CHANNEL_ID_BUS_DELAY,
                    "Bus Delay Alerts",
                    NotificationManager.IMPORTANCE_HIGH);

            notificationManager.createNotificationChannel(foregroundChannel);
            notificationManager.createNotificationChannel(geofenceChannel);
            notificationManager.createNotificationChannel(busDelayChannel);
        }
    }

    public Notification buildForegroundTrackingNotification() {
        Intent intent = new Intent(context, ScheduleDetailsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, CHANNEL_ID_FOREGROUND)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("TrackUTeM is running")
                .setContentText("Tracking your bus location in background")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    public Notification sendGeofenceNotification(String rpointName) {
        Intent intent = new Intent(context, ScheduleDetailsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(context, CHANNEL_ID_GEOFENCE)
                .setSmallIcon(R.drawable.ic_location)
                .setContentTitle("Bus Stop Ahead!")
                .setContentText("You are arriving at " + rpointName + ". Tap to confirm arrival.")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();
    }

    public void showDelayNotification(String title, String message) {
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String driverId = prefs.getString("driverId", null);
        String studentId = prefs.getString("studentId", null);

        Intent intent;
        if (driverId != null) {
            intent = new Intent(context, MainDrvActivity.class);
            intent.putExtra("openFragment", "notifications");
        } else if (studentId != null) {
            intent = new Intent(context, MainStuActivity.class);
        } else {
            intent = new Intent(context, LoginActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        showDefaultNotification(title, message, intent);
    }

    public void showDefaultNotification(String title, String message, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Notification permission not granted");
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_BUS_DELAY)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ALARM);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}