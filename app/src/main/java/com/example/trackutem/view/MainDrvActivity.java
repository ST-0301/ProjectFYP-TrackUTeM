package com.example.trackutem.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.example.trackutem.R;
import com.example.trackutem.controller.MapController;
import com.example.trackutem.controller.RestLogController;
import com.example.trackutem.controller.TimerController;
import com.example.trackutem.service.TrackingService;
import com.example.trackutem.utils.AppStateManager;
import com.example.trackutem.utils.NotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;
import java.util.ArrayList;
import java.util.List;

public class MainDrvActivity extends AppCompatActivity implements TimerController.TimerCallback {
    private String driverId;
    private static final long REST_DURATION_MILLIS = 2 * 60 * 1000;
    private static final long FIVE_MINUTES_WARNING = 1 * 60 * 1000;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences prefs;
    private AppStateManager appStateManager;
    private NotificationHelper notificationHelper;
    private MapController mapController;
    private RestLogController restLogController;
    private TimerController timerController;

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

        // Initialize location and user's preferences
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        driverId = prefs.getString("driverId", null);

        // Initialize helpers and controllers
        appStateManager = new AppStateManager(this);
        notificationHelper = new NotificationHelper(this);
        restLogController = new RestLogController();
        timerController = new TimerController(this, notificationHelper);

        configureSystemBars();
        restoreUIState();
        checkPermissions();

        btnStart.setOnClickListener(v -> {
            btnStart.setVisibility(View.GONE);
            btnRest.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.VISIBLE);

            // Initialize rest log & Record start working time
            restLogController.createTodayRestLog(driverId, ref -> {});

            startService(new Intent(MainDrvActivity.this, TrackingService.class));
            appStateManager.saveState("start", 0, true);
        });

        btnRest.setOnClickListener(v -> {
            btnRest.setVisibility(View.GONE);
            btnContinue.setVisibility(View.VISIBLE);
            tvTimer.setVisibility(View.VISIBLE);

            // Increase rest count
            restLogController.incrementRestCount(driverId);

            // Stop tracking
            stopService(new Intent(MainDrvActivity.this, TrackingService.class));

            long endTime = System.currentTimeMillis() + REST_DURATION_MILLIS;
            appStateManager.saveState("rest", endTime, false);

            // Start 30-min timer
            timerController.startCountdown(REST_DURATION_MILLIS);
        });

        btnContinue.setOnClickListener(v -> {
            btnContinue.setVisibility(View.GONE);
            btnRest.setVisibility(View.VISIBLE);
            tvTimer.setVisibility(View.GONE);

            // Resume tracking
            startService(new Intent(MainDrvActivity.this, TrackingService.class));

            // Stop timer
            timerController.stopCountdown();

            appStateManager.saveState("continue", 0, true);
        });

        btnStop.setOnClickListener(v -> {
            // Record end working time
            restLogController.updateEndWorkTime(driverId);

            stopService(new Intent(MainDrvActivity.this, TrackingService.class));
            timerController.stopCountdown();
            appStateManager.clear();

            btnStart.setVisibility(View.VISIBLE);
            btnRest.setVisibility(View.GONE);
            btnContinue.setVisibility(View.GONE);
            btnStop.setVisibility(View.GONE);
            tvTimer.setVisibility(View.GONE);
        });
    }

    private void configureSystemBars() {
        // Configure window insets controller
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        // Set behavior to show transient bars by swipe
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        // Set status bar appearance
        windowInsetsController.setAppearanceLightStatusBars(true);
        // Hide both navigation bar and status bar initially
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());
    }

    @SuppressLint("SetTextI18n")
    private void restoreUIState() {
        String buttonState = appStateManager.getButtonState();
        long timerEnd = appStateManager.getTimerEndTime();
        boolean isTracking = appStateManager.isTracking();

        btnStart.setVisibility(View.GONE);
        btnRest.setVisibility(View.GONE);
        btnContinue.setVisibility(View.GONE);
        btnStop.setVisibility(View.GONE);
        tvTimer.setVisibility(View.GONE);

        switch (buttonState) {
            case "start":

            case "continue":
                btnRest.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.VISIBLE);
                break;

            case "rest":
                btnContinue.setVisibility(View.VISIBLE);
                tvTimer.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.VISIBLE);

                long remainingTime = timerEnd - System.currentTimeMillis();
                if (remainingTime > 0) {
                    timerController.startCountdown(remainingTime);
                } else {
                    tvTimer.setText("00:00");
                }
                break;

            default:
                btnStart.setVisibility(View.VISIBLE);
        }
        if (isTracking) {
            startService(new Intent(MainDrvActivity.this, TrackingService.class));
        }
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

    // TimerController callbacks
    @Override
    public void onTimerTick(String formattedTime) {
        tvTimer.setText(formattedTime);
    }
    @Override
    public void onTimerFinish() {
        tvTimer.setText("00:00");
    }
}