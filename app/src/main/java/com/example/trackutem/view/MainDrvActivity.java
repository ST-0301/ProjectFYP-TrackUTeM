package com.example.trackutem.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.trackutem.R;
import com.example.trackutem.controller.MapController;
import com.example.trackutem.controller.TimerController;
import com.example.trackutem.controller.TrackingController;
import com.example.trackutem.service.TrackingService;
import com.example.trackutem.utils.NotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;

public class MainDrvActivity extends AppCompatActivity implements TimerController.TimerCallback {
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private NotificationHelper notificationHelper;
    private MapController mapController;
    private TrackingController trackingController;
    private TimerController timerController;
    private DatabaseReference busLocationRef;

    private Button btnStart, btnRest, btnContinue, btnStop;
    private TextView tvTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maindrv);

        btnStart = findViewById(R.id.btnStart);
        btnRest = findViewById(R.id.btnRest);
        btnContinue = findViewById(R.id.btnContinue);
        btnStop = findViewById(R.id.btnStop);
        tvTimer = findViewById(R.id.tvTimer);

        // Initialize Firebase and Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        busLocationRef = FirebaseDatabase.getInstance()
                .getReference("bus_locations")
                .child("bus1");

        // Initialize helpers controllers
        notificationHelper = new NotificationHelper(this);
        trackingController = new TrackingController(busLocationRef, fusedLocationClient, this);
        timerController = new TimerController(this, notificationHelper);

        checkPermissions();

        btnStart.setOnClickListener(v -> {
            btnStart.setVisibility(View.GONE);
            btnRest.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.VISIBLE);
            startService(new Intent(MainDrvActivity.this, TrackingService.class));
        });

        btnRest.setOnClickListener(v -> {
            btnRest.setVisibility(View.GONE);
            btnContinue.setVisibility(View.VISIBLE);
            tvTimer.setVisibility(View.VISIBLE);
            // Stop tracking
            stopService(new Intent(MainDrvActivity.this, TrackingService.class));
            // Start timer
            timerController.startCountdown(3 * 60 * 1000); // start a 30-min timer
        });

        btnContinue.setOnClickListener(v -> {
            btnContinue.setVisibility(View.GONE);
            btnRest.setVisibility(View.VISIBLE);
            tvTimer.setVisibility(View.GONE);
            // Resume tracking
            startService(new Intent(MainDrvActivity.this, TrackingService.class));
            // Stop timer
            timerController.stopCountdown();
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(MainDrvActivity.this, TrackingService.class));
            btnStart.setVisibility(View.VISIBLE);
            btnRest.setVisibility(View.GONE);
            btnContinue.setVisibility(View.GONE);
            btnStop.setVisibility(View.GONE);
            tvTimer.setVisibility(View.GONE);
        });
    }

    // TimerController callbacks
    @Override
    public void onTimerTick(String formattedTime) {
        tvTimer.setText(formattedTime);
    }

    @Override
    public void onTimerFinish() {
        notificationHelper.showTimerNotification("Rest time over. Tap Continue to resume", true);
        notificationHelper.vibrateDevice(1500);
    }

    // Permissions
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Always request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_LOCATION_PERMISSION);
        } else {
            initializeMap();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            boolean allGranted = true;

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Toast.makeText(this, "Location permission required for tracking", Toast.LENGTH_LONG).show();
                    }
                    allGranted = false;
                }
            }
            if (allGranted) {
                initializeMap();
            }
        }
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mapController = new MapController(this, googleMap, fusedLocationClient);
                mapController.initializeMapFeatures();
            });
        }
    }
}