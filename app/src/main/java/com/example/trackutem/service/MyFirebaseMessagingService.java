package com.example.trackutem.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.trackutem.utils.NotificationHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("FCM_TOKEN", token);
        // TODO: Send this token to Firestore (store it by driverId)
        uploadTokenToFirestore(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        // Show notification
        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();
        // Display using NotificationManager
        NotificationHelper helper = new NotificationHelper(getApplicationContext());
        helper.showPushNotification(title, body);
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

//        Map<String, Object> tokenData = new HashMap<>();
//        tokenData.put("token", token);

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
        db.collection(collectionName).document(userId)
                .update("pushToken", null)
                .addOnSuccessListener(aVoid -> Log.d("FCM_TOKEN", "Token cleared"))
                .addOnFailureListener(e -> Log.e("FCM_TOKEN", "Error clearing token", e));
    }
}