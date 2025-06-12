// TrackingService.java
package com.example.trackutem.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.trackutem.controller.TrackingController;
import com.example.trackutem.model.Stop;
import com.example.trackutem.view.ScheduleDetailsFragment;
import com.example.trackutem.utils.NotificationHelper;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;
import java.util.Objects;

public class TrackingService extends Service {
    private static final String TAG = "TrackingService";
    private TrackingController trackingController;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        NotificationHelper notificationHelper = new NotificationHelper(this);
        trackingController = new TrackingController(
                FirebaseDatabase.getInstance().getReference("bus_locations").child("bus1"),
                LocationServices.getFusedLocationProviderClient(this),
                this);

        startForeground(1, notificationHelper.buildForegroundTrackingNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called with action: " + (intent != null ? intent.getAction() : "null"));
        if (intent != null && ScheduleDetailsFragment.GEOFENCE_BROADCAST_ACTION.equals(intent.getAction())) {
            handleGeofenceTransition(intent);
        } else {
            trackingController.startLocationUpdates();
        }
        return START_STICKY;
    }
    private void handleGeofenceTransition(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null. Invalid intent?");
            return;
        }

        if (geofencingEvent.hasError()) {
            int errorCode = geofencingEvent.getErrorCode();
            Log.e(TAG, "Geofence error: " + errorCode);
            return;
        }
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            for (Geofence geofence : Objects.requireNonNull(triggeringGeofences)) {
                String geofenceId = geofence.getRequestId();
                Log.d(TAG, "Entered geofence ID: " + geofenceId);

                new Stop().getStopNameById(geofenceId, new Stop.StopCallback() {
                    @Override
                    public void onSuccess(String stopName) {
                        Intent broadcastIntent = new Intent(ScheduleDetailsFragment.GEOFENCE_BROADCAST_ACTION);
                        broadcastIntent.putExtra("transitionType", geofenceTransition);
                        broadcastIntent.putExtra("geofenceId", geofenceId);
                        broadcastIntent.putExtra("stopName", stopName);
                        sendBroadcast(broadcastIntent);

                        Notification notification = new NotificationHelper(TrackingService.this).sendGeofenceNotification(stopName);
                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.notify(2, notification);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error getting stop name for geofence: " + e.getMessage());
                    }
                });
            }
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        trackingController.stopLocationUpdates();
        Log.d(TAG, "Service destroyed, tracking stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}