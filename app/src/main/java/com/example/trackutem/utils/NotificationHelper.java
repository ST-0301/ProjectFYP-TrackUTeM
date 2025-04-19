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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.example.trackutem.R;
import com.example.trackutem.view.MainDrvActivity;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private final String CHANNEL_ID = "TRACKUTEM_TIMER_CHANNEL";
    private static final int NOTIFICATION_ID_WARNING = 100;
    private static final int NOTIFICATION_ID_FINISH = 101;
    private final Context context;
    private final NotificationManager notificationManager;
    private final Vibrator vibrator;

    public NotificationHelper(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Timer Notifications";
            String description = "Notifications for rest timer updates";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});

            notificationManager.createNotificationChannel(channel);
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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("TrackUTeM Driver Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
//                .addAction(R.drawable.ic_play, "Continue Now", createResumePendingIntent());

        // Vibration
        long[] vibrationPattern = isFinalAlert ? new long[]{0, 1000, 500, 1000} : new long[]{0, 500, 200, 500};
        builder.setVibrate(vibrationPattern);
        vibrateDevice(isFinalAlert ? 1000 : 500);

        // Show notification
        int notificationId = isFinalAlert ? NOTIFICATION_ID_FINISH : NOTIFICATION_ID_WARNING;
        notificationManager.notify(notificationId, builder.build());
    }

    public void vibrateDevice(long milliseconds) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(milliseconds);
            }
        }
    }

    public Notification buildForegroundTrackingNotification() {
        Intent intent = new Intent(context, MainDrvActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("TrackUTeM is running")
                .setContentText("Tracking your bus location in background")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }
}