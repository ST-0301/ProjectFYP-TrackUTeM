package com.example.trackutem;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.trackutem.databinding.ActivityMainDrvBinding;
import com.example.trackutem.view.NotificationsFragment;
import com.example.trackutem.view.SettingsFragment;
import com.example.trackutem.view.Driver.ScheduleFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class MainDrvActivity extends AppCompatActivity {
    private ActivityMainDrvBinding binding;
    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private BottomNavigationView navView;
    private String driverId;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainDrvBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tabLayout = findViewById(R.id.tabLayout);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        driverId = prefs.getString("driverId", null);

        navView = findViewById(R.id.nav_view);
//        navView.setSelectedItemId(R.id.navigation_home);
//        switchTo(new HomeDrvFragment(), "Home");
        //hide home
        ScheduleFragment scheduleFragment = new ScheduleFragment();
        Bundle args = new Bundle();
        args.putString("driverId", driverId);
        scheduleFragment.setArguments(args);
        navView.setSelectedItemId(R.id.navigation_trip);
        switchTo(scheduleFragment, "Trips");

        navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment;
            String title;

            //hide home
            /*if (id == R.id.navigation_home) {
                fragment = new HomeDrvFragment();
                title = "Home";
            }
            else */if (id == R.id.navigation_trip) {
                fragment = new ScheduleFragment();
                Bundle args1 = new Bundle();
                args1.putString("driverId", driverId);
                fragment.setArguments(args1);
                title = "Trips";
            }
            else if (id == R.id.navigation_notifications) {
                fragment = new NotificationsFragment();
                title = "Notifications";
            }
            else if (id == R.id.navigation_settings) {
                fragment = new SettingsFragment();
                title = "Settings";
            }
            else {
                return false;
            }
            //hide home
//            if (fragment != null) {
////                Bundle args = new Bundle();
//                args.putString("driverId", driverId);
//                fragment.setArguments(args);
//            }
            switchTo(fragment, title);
            return true;
        });
        checkPermissions();
    }
    private void switchTo(Fragment fragment, String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);

            // Show back button only when there are fragments in back stack
            getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
        invalidateOptionsMenu();
        if (fragment instanceof ScheduleFragment) {
            tabLayout.setVisibility(View.VISIBLE);
        } else {
            tabLayout.setVisibility(View.GONE);
            tabLayout.removeAllTabs();
        }
    }
    public TabLayout getTabLayout() {
        return tabLayout;
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

            // Update toolbar after popping back stack
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (currentFragment instanceof ScheduleFragment) {
                updateToolbar("Trips", false);
            }
        } else {
            super.onBackPressed();
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    public void showBottomNav() {
        if (navView != null) {
            navView.setVisibility(View.VISIBLE);
        }
    }
    public void hideBottomNav() {
        if (navView != null) {
            navView.setVisibility(View.GONE);
        }
    }
}