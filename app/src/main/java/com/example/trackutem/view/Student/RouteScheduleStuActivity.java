package com.example.trackutem.view.Student;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Schedule;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RouteScheduleStuActivity extends AppCompatActivity {
    public static final String EXTRA_ROUTE_ID = "routeId";
    private DateGroupAdapter adapter;
    private final HashMap<String, List<Schedule>> dateGroups = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_schedule);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView rvDateGroups = findViewById(R.id.rvSchedules); // Using same RecyclerView ID
        rvDateGroups.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DateGroupAdapter(this, dateGroups);
        rvDateGroups.setAdapter(adapter);

        String routeId = getIntent().getStringExtra(EXTRA_ROUTE_ID);
        if (routeId != null) {
            loadSchedules(routeId);
        }
    }
    private void loadSchedules(String routeId) {
        FirebaseFirestore.getInstance()
                .collection("schedules")
                .whereEqualTo("routeId", routeId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    dateGroups.clear();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date today = new Date();

                    try {
                        Date todayDateOnly = dateFormat.parse(dateFormat.format(today));
                        for (var doc : querySnapshot) {
                            Schedule schedule = doc.toObject(Schedule.class);
                            if ("cancelled".equals(schedule.getStatus())) {
                                continue;
                            }
                            if (schedule.getBusDriverPairId() == null || schedule.getBusDriverPairId().isEmpty()) {
                                continue;
                            }
                            Date scheduleDate = schedule.getScheduledDatetime();
                            if (scheduleDate != null && !scheduleDate.before(todayDateOnly)) {
                                String dateKey = dateFormat.format(scheduleDate);

                                List<Schedule> schedulesList = dateGroups.computeIfAbsent(dateKey, k -> new ArrayList<>());
                                schedulesList.add(schedule);
                            }
                        }
                        for (List<Schedule> schedules : dateGroups.values()) {
                            schedules.sort(Comparator.comparing(Schedule::getScheduledDatetime));
                        }
                        adapter.updateData(dateGroups);
                    } catch (Exception e) {
                        Log.e("RouteScheduleActivity", "Error processing schedule dates", e);
                        Toast.makeText(this, "Error processing schedule dates", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RouteScheduleActivity", "Failed to load schedules", e);
                    Toast.makeText(this, "Failed to load schedules", Toast.LENGTH_SHORT).show();
                });
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}