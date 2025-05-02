package com.example.trackutem.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        NotificationHelper notificationHelper = new NotificationHelper(context);

        if (action == null) return;
        if ("REST_TIME_WARNING".equals(action)) {
            notificationHelper.showTimerNotification("5 minutes left. Tap Continue to resume", false);
        } else if ("REST_TIME_OVER".equals(action)) {
            notificationHelper.showTimerNotification("Rest time over. Tap Continue to resume", true);
        }
    }
}