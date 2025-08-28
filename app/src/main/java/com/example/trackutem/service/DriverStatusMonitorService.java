package com.example.trackutem.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.trackutem.receiver.DriverStatusReceiver;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class DriverStatusMonitorService extends Service {
    private static final String TAG = "DriverStatusMonitor";
    private ListenerRegistration statusListener;
    private String currentDriverId;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentDriverId = prefs.getString("driverId", null);
        if (currentDriverId != null) {
            startMonitoring();
        } else {
            stopSelf();
        }
        return START_STICKY;
    }

    private void startMonitoring() {
        statusListener = FirebaseFirestore.getInstance()
                .collection("drivers")
                .document(currentDriverId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed: " + error);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        if ("disabled".equals(status)) {
                            Intent broadcastIntent = new Intent(this, DriverStatusReceiver.class);
                            broadcastIntent.setAction("com.example.trackutem.DRIVER_DISABLED");
                            sendBroadcast(broadcastIntent);
                            stopSelf();
                        }
                    }
                });
    }

    @Override
    public void onDestroy() {
        if (statusListener != null) {
            statusListener.remove();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}