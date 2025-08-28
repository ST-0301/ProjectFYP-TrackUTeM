package com.example.trackutem.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.trackutem.service.DriverStatusMonitorService;
import com.example.trackutem.view.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.example.trackutem.service.MyFirebaseMessagingService;

public class DriverStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.trackutem.DRIVER_DISABLED".equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("driver_disabled", true);
            editor.apply();

            // Log out the driver
            FirebaseAuth.getInstance().signOut();
            FirebaseMessaging.getInstance().deleteToken();
            MyFirebaseMessagingService.clearToken(context);

            editor.remove("email");
            editor.remove("user_type");
            editor.remove("driverId");
            editor.remove("remember_me");
            editor.apply();
            context.getSharedPreferences("MyFirebaseMessagingService", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            Intent serviceIntent = new Intent(context, DriverStatusMonitorService.class);
            context.stopService(serviceIntent);

            Intent loginIntent = new Intent(context, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(loginIntent);
        }
    }
}