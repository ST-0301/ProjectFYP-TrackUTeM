package com.example.trackutem;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.example.trackutem.databinding.ActivityMainDrvBinding;
import com.example.trackutem.model.Schedule;
import com.example.trackutem.service.MyFirebaseMessagingService;
import com.example.trackutem.view.Driver.ScheduleAdapter;
import com.example.trackutem.view.Driver.ScheduleDetailsActivity;
import com.example.trackutem.view.SettingsActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.messaging.FirebaseMessaging;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainDrvActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private SharedPreferences prefs;
    private ActivityMainDrvBinding binding;
    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView rvSchedulesDrv;
    private TextView tvEmpty;
    private ScheduleAdapter adapter;
    private String currentDriverId;
    private List<Schedule> allSchedules = new ArrayList<>();
    private final List<Schedule> todaySchedules = new ArrayList<>();
    private final List<Schedule> upcomingSchedules = new ArrayList<>();
    private final List<Schedule> completedSchedules = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainDrvBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        rvSchedulesDrv = findViewById(R.id.rvSchedulesDrv);
        rvSchedulesDrv.setLayoutManager(new LinearLayoutManager(this));
        tvEmpty = findViewById(R.id.tvEmpty);
        adapter = new ScheduleAdapter(new ArrayList<>(), schedule -> {
            Intent intent = new Intent(MainDrvActivity.this, ScheduleDetailsActivity.class);
            intent.putExtra("routeId", schedule.getRouteId());
            intent.putExtra("scheduleId", schedule.getScheduleId());
            startActivity(intent);
            //            ScheduleDetailsFragment fragment = new ScheduleDetailsFragment();
//            Bundle args = new Bundle();
//            args.putString("routeId", schedule.getRouteId());
//            args.putString("scheduleId", schedule.getScheduleId());
//            fragment.setArguments(args);
//
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.container, fragment)
//                    .addToBackStack("schedule_details")
//                    .commit();
//            updateToolbar("Detailssss", true);
        });
        rvSchedulesDrv.setAdapter(adapter);

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentDriverId = prefs.getString("driverId", null);
        if (currentDriverId == null) {
            Toast.makeText(this, "Driver not logged in", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            loadDriverRoutes();
        }
        uploadCurrentToken();
        setupMenuProvider();
        checkPermissions();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (tabLayout != null) {
            tabLayout.removeAllTabs();
            tabLayout.clearOnTabSelectedListeners();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (tabLayout != null && !allSchedules.isEmpty()) {
            setupTabs();
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
    private void setupMenuProvider() {
        addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.main_toolbar_menu, menu);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_settings) {
//                    getSupportFragmentManager()
//                            .beginTransaction()
//                            .replace(R.id.nav_host_fragment, SettingsFragment.newInstance(false))
//                            .addToBackStack("settings_fragment")
//                            .commit();
//                    updateToolbar("Settings", true);
                    startActivity(new Intent(MainDrvActivity.this, SettingsActivity.class));
                    return true;
                }
                return false;
            }
        }, this, Lifecycle.State.RESUMED);
    }
    private void loadDriverRoutes() {
        if (currentDriverId == null) {
            Toast.makeText(this, "Driver not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Schedule.getSchedulesByDriverId(currentDriverId, new Schedule.OnSchedulesRetrieved() {
            @Override
            public void onSuccess(List<Schedule> schedules) {
                categorizeSchedules(schedules);
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(MainDrvActivity.this, "Error loading schedules: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void categorizeSchedules(List<Schedule> schedules) {
        allSchedules = schedules;
        todaySchedules.clear();
        upcomingSchedules.clear();
        completedSchedules.clear();

        // Get current day
        Calendar calendar = Calendar.getInstance();
        int currentDayNumber = calendar.get(Calendar.DAY_OF_WEEK);
        String[] days = new DateFormatSymbols(Locale.ENGLISH).getWeekdays();
        String currentDayName = days[currentDayNumber].toLowerCase();

        for (Schedule schedule : allSchedules) {
            String status = schedule.getStatus() != null ? schedule.getStatus().toLowerCase(Locale.ENGLISH) : "scheduled";
            String scheduleDay = (schedule.getDay() != null) ? schedule.getDay().toLowerCase(Locale.ENGLISH) : "";

            if (status.equals("completed")) {
                completedSchedules.add(schedule);
            } else if (scheduleDay.equals(currentDayName)) {
                todaySchedules.add(schedule);
            } else {
                upcomingSchedules.add(schedule);
            }
        }
        setupTabs();
        showSelectedTab(todaySchedules);
    }
    private void setupTabs() {
        if (tabLayout == null) return;
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Today (" + todaySchedules.size() + ")"));
        tabLayout.addTab(tabLayout.newTab().setText("Upcoming (" + upcomingSchedules.size() + ")"));
        tabLayout.addTab(tabLayout.newTab().setText("Completed"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        showSelectedTab(todaySchedules);
                        break;
                    case 1:
                        showSelectedTab(upcomingSchedules);
                        break;
                    case 2:
                        showSelectedTab(completedSchedules);
                        break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    private void showSelectedTab(List<Schedule> schedulesToShow) {
        adapter.updateSchedules(schedulesToShow);
        rvSchedulesDrv.setVisibility(schedulesToShow.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(schedulesToShow.isEmpty() ? View.VISIBLE : View.GONE);
    }

    //    private void switchTo(Fragment fragment, String title) {
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setTitle(title);
//
//            // Show back button only when there are fragments in back stack
//            getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
//        }
//        getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.nav_host_fragment, fragment)
//                .commit();
//        invalidateOptionsMenu();
//        if (fragment instanceof ScheduleFragment) {
//            tabLayout.setVisibility(View.VISIBLE);
//        } else {
//            tabLayout.setVisibility(View.GONE);
//            tabLayout.removeAllTabs();
//        }
//    }
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
            updateToolbar("Trips", false);

            // Update toolbar after popping back stack
//            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
//            if (currentFragment instanceof ScheduleDetailsFragment) {
//                updateToolbar("Schedule Details", false);
//            } else {
//                updateToolbar("Trips", false);
//            }
        } else {
            super.onBackPressed();
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}