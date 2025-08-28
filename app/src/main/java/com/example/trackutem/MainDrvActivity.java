package com.example.trackutem;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.trackutem.databinding.ActivityMainDrvBinding;
import com.example.trackutem.service.DriverStatusMonitorService;
import com.example.trackutem.service.MyFirebaseMessagingService;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import java.util.ArrayList;
import java.util.List;

public class MainDrvActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private SharedPreferences prefs;
    private ActivityMainDrvBinding binding;
    private BottomNavigationView bottomNavigationView;
    private NavHostFragment navHostFragment;
    private NavController navController;
    private AppBarLayout appBarLayout;
    private MaterialToolbar toolbar;
    private String currentDriverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainDrvBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        appBarLayout = findViewById(R.id.appBarLayout);
        appBarLayout.setVisibility(View.GONE);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initializeNavController();
        setupBottomNavigation();

        Intent serviceIntent = new Intent(this, DriverStatusMonitorService.class);
        startService(serviceIntent);
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentDriverId = prefs.getString("driverId", null);
        if (currentDriverId == null) {
            Toast.makeText(this, "Driver not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uploadCurrentToken();
        checkPermissions();

        if (getIntent() != null && getIntent().hasExtra("openFragment")) {
            String fragmentToOpen = getIntent().getStringExtra("openFragment");
            if ("notifications".equals(fragmentToOpen)) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_notifications);
            }
        }
    }
    private void initializeNavController() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            Toast.makeText(this, "Navigation host not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.nav_view);
        if (navController == null) {
            initializeNavController();
            if (navController == null) {
                Toast.makeText(this, "Navigation not available", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            updateToolbarForDestination(destination.getId());
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (navController == null) {
                initializeNavController();
                if (navController == null) {
                    return false;
                }
            }
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                navController.navigate(R.id.navigation_home);
                return true;
            } else if (itemId == R.id.navigation_trip) {
                navController.navigate(R.id.navigation_trip);
                return true;
            } else if (itemId == R.id.navigation_notifications) {
                navController.navigate(R.id.navigation_notifications);
                return true;
            } else if (itemId == R.id.navigation_settings) {
                navController.navigate(R.id.navigation_settings);
                return true;
            }
            return false;
        });
    }
    private void updateToolbarForDestination(int destinationId) {
        String title = "TrackUTeM";

        if (destinationId == R.id.navigation_home) {
            title = "Home";
        } else if (destinationId == R.id.navigation_trip) {
            title = "My Trips";
        } else if (destinationId == R.id.navigation_notifications) {
            title = "Notifications";
        } else if (destinationId == R.id.navigation_settings) {
            title = "Settings";
        }

        updateToolbar(title, false);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (currentDriverId != null) {
            Intent serviceIntent = new Intent(this, DriverStatusMonitorService.class);
            startService(serviceIntent);
        }
        if (navController != null && navController.getCurrentDestination() != null) {
            int currentDestination = navController.getCurrentDestination().getId();
            if (currentDestination == R.id.navigation_home) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_home);
            } else if (currentDestination == R.id.navigation_trip) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_trip);
            } else if (currentDestination == R.id.navigation_notifications) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_notifications);
            } else if (currentDestination == R.id.navigation_settings) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_settings);
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            Intent serviceIntent = new Intent(this, DriverStatusMonitorService.class);
            stopService(serviceIntent);
        }
    }
    private void uploadCurrentToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        MyFirebaseMessagingService.uploadTokenToFirestore(this, token);
                    }
                });
    }
    public void updateToolbar(String title, boolean showBackButton) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(showBackButton);
            getSupportActionBar().setHomeButtonEnabled(showBackButton);
        }
    }
    // Permissions
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Always request location permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_LOCATION_PERMISSION);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Toast.makeText(this, "Location permission required for tracking", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            updateToolbar(getCurrentFragmentTitle(), false);
            return;
        }
        if (navController != null && navController.getCurrentDestination() != null) {
            int currentDestination = navController.getCurrentDestination().getId();
            if (currentDestination != R.id.navigation_home) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                return;
            }
        }
        super.onBackPressed();
    }
    private String getCurrentFragmentTitle() {
        if (navController != null && navController.getCurrentDestination() != null) {
            int currentDestination = navController.getCurrentDestination().getId();
            if (currentDestination == R.id.navigation_home) {
                return "Home";
            } else if (currentDestination == R.id.navigation_trip) {
                return "My Trips";
            } else if (currentDestination == R.id.navigation_notifications) {
                return "Notifications";
            } else if (currentDestination == R.id.navigation_settings) {
                return "Settings";
            }
        }
        return "TrackUTeM";
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}