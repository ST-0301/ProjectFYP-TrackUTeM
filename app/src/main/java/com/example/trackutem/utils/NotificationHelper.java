package com.example.trackutem.utils;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.example.trackutem.R;
import com.example.trackutem.view.MainDrvActivity;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID_WARNING = "TRACKUTEM_TIMER_WARNING";
    private static final String CHANNEL_ID_FINISH = "TRACKUTEM_TIMER_FINISH";
    private static final String CHANNEL_ID_FOREGROUND = "TRACKUTEM_FOREGROUND";
    private static final int NOTIFICATION_ID_WARNING = 100;
    private static final int NOTIFICATION_ID_FINISH = 101;
    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Warning channel
            NotificationChannel warningChannel = new NotificationChannel(
                    CHANNEL_ID_WARNING,
                    "Timer Warnings",
                    NotificationManager.IMPORTANCE_HIGH);

            // Finish channel
            NotificationChannel finishChannel = new NotificationChannel(
                    CHANNEL_ID_FINISH,
                    "Timer Finish Alerts",
                    NotificationManager.IMPORTANCE_HIGH);

            // Foreground channel
            NotificationChannel foregroundChannel = new NotificationChannel(
                    CHANNEL_ID_FOREGROUND,
                    "Foreground Service",
                    NotificationManager.IMPORTANCE_LOW);

            notificationManager.createNotificationChannel(warningChannel);
            notificationManager.createNotificationChannel(finishChannel);
            notificationManager.createNotificationChannel(foregroundChannel);
        }
    }

    public void showTimerNotification(String message, boolean isFinalAlert) {
        // Check for POST_NOTIFICATIONS permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Notification permission not granted");
            return;
        }

        // Open app when notification is tapped
        Intent intent = new Intent(context, MainDrvActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Notification
        String channelId = isFinalAlert ? CHANNEL_ID_FINISH : CHANNEL_ID_WARNING;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("TrackUTeM Driver Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show notification
        int notificationId = isFinalAlert ? NOTIFICATION_ID_FINISH : NOTIFICATION_ID_WARNING;
        notificationManager.notify(notificationId, builder.build());
    }
    public Notification buildForegroundTrackingNotification() {
        Intent intent = new Intent(context, MainDrvActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, CHANNEL_ID_FOREGROUND)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("TrackUTeM is running")
                .setContentText("Tracking your bus location in background")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }
}