package com.example.trackutem.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;

import com.example.trackutem.MainDrvActivity;
import com.example.trackutem.MainStuActivity;
import com.example.trackutem.utils.NotificationHelper;
import com.example.trackutem.view.LoginActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        uploadTokenToFirestore(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            String type = remoteMessage.getData().get("type");
            if ("delay".equals(type)) {
                handleDelayNotification(remoteMessage);
            } else {
                showDefaultNotification(remoteMessage);
            }
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            showDefaultNotification(remoteMessage);
        }
    }

    private void handleDelayNotification(RemoteMessage remoteMessage) {
        String title = remoteMessage.getNotification() != null ?
                remoteMessage.getNotification().getTitle() : "Bus Delay Alert";
        String body = remoteMessage.getNotification() != null ?
                remoteMessage.getNotification().getBody() : "Your bus is delayed";

        String routeName = remoteMessage.getData().get("routeName");
        String busPlate = remoteMessage.getData().get("busPlateNumber");
        String lateness = remoteMessage.getData().get("latenessMinutes");

        if (routeName != null && busPlate != null && lateness != null) {
            body = String.format("Bus %s on route %s is delayed by %s minutes",
                    busPlate, routeName, lateness);
        }

        NotificationHelper helper = new NotificationHelper(getApplicationContext());
        helper.showDelayNotification(title, body);
    }

    private void showDefaultNotification(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() == null) {
            Log.e("FCM_NOTIFICATION", "Notification is null");
            return;
        }

        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();

        NotificationHelper helper = new NotificationHelper(getApplicationContext());
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String driverId = prefs.getString("driverId", null);
        String studentId = prefs.getString("studentId", null);

        Intent intent;
        if (driverId != null) {
            intent = new Intent(getApplicationContext(), MainDrvActivity.class);
            intent.putExtra("openFragment", "notifications");
        } else if (studentId != null) {
            intent = new Intent(getApplicationContext(), MainStuActivity.class);
        } else {
            intent = new Intent(getApplicationContext(), LoginActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        helper.showDelayNotification(title, body);
    }

    private void uploadTokenToFirestore(String token) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String driverId = prefs.getString("driverId", null);
        String studentId = prefs.getString("studentId", null);

        if (driverId == null && studentId == null) {
            Log.d("FCM_TOKEN", "No user logged in, token not uploaded");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId;
        String collectionName;

        if (driverId != null) {
            userId = driverId;
            collectionName = "drivers";
        } else {
            userId = studentId;
            collectionName = "students";
        }

        db.collection(collectionName).document(userId)
                .update("pushToken", token)
                .addOnSuccessListener(aVoid -> Log.d("FCM_TOKEN", "Token updated to Firestore"))
                .addOnFailureListener(e -> Log.e("FCM_TOKEN", "Error updating token", e));
    }

    public static void uploadTokenToFirestore(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String driverId = prefs.getString("driverId", null);
        String studentId = prefs.getString("studentId", null);

        if (driverId == null && studentId == null) {
            Log.d("FCM_TOKEN", "No user logged in, token not uploaded");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId;
        String collectionName;

        if (driverId != null) {
            userId = driverId;
            collectionName = "drivers";
        } else {
            userId = studentId;
            collectionName = "students";
        }

        db.collection(collectionName).document(userId)
                .update("pushToken", token)
                .addOnSuccessListener(aVoid -> Log.d("FCM_TOKEN", "Token updated to Firestore"))
                .addOnFailureListener(e -> Log.e("FCM_TOKEN", "Error updating token", e));
    }

    public static void clearToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String driverId = prefs.getString("driverId", null);
        String studentId = prefs.getString("studentId", null);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId;
        String collectionName;

        if (driverId != null) {
            userId = driverId;
            collectionName = "drivers";
        } else {
            userId = studentId;
            collectionName = "students";
        }
        if (userId == null) {
            Log.e("FCM_TOKEN", "User ID is null, cannot clear token");
            return;
        }

        db.collection(collectionName).document(userId)
                .update("pushToken", null)
                .addOnSuccessListener(aVoid -> Log.d("FCM_TOKEN", "Token cleared"))
                .addOnFailureListener(e -> Log.e("FCM_TOKEN", "Error clearing token", e));
    }
}