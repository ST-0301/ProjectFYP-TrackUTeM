package com.example.trackutem.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.trackutem.R;
import com.example.trackutem.service.MyFirebaseMessagingService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        findViewById(R.id.btnLogout).setOnClickListener(v -> handleLogout());
        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> handleAccountDeletion());
    }

    private void handleLogout() {
        FirebaseMessaging.getInstance().deleteToken();
        MyFirebaseMessagingService.clearToken(this);
        prefs.edit().clear().apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    private void handleAccountDeletion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure? This action cannot be undone")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // deleteAccountFromServer();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}