package com.example.trackutem.view.Student;

import android.os.Bundle;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RouteScheduleActivity extends AppCompatActivity {
    public static final String EXTRA_ROUTE_ID = "routeId";
//    private ScheduleStuAdapter adapter;
    private DateGroupAdapter adapter;
    private HashMap<String, List<Schedule>> dateGroups = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_schedule);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

//        RecyclerView rvSchedules = findViewById(R.id.rvSchedules);
//        rvSchedules.setLayoutManager(new LinearLayoutManager(this));
//        adapter = new ScheduleStuAdapter(this, new ArrayList<>(), null);
//        rvSchedules.setAdapter(adapter);
        RecyclerView rvDateGroups = findViewById(R.id.rvSchedules); // Using same RecyclerView ID
        rvDateGroups.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DateGroupAdapter(this, dateGroups);
        rvDateGroups.setAdapter(adapter);

        String routeId = getIntent().getStringExtra(EXTRA_ROUTE_ID);
        if (routeId != null) {
            loadSchedules(routeId);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    private void loadSchedules(String routeId) {
        FirebaseFirestore.getInstance()
                .collection("schedules")
                .whereEqualTo("routeId", routeId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
//                    List<Schedule> schedules = new ArrayList<>();
//                    for (var doc : querySnapshot) {
//                        Schedule schedule = doc.toObject(Schedule.class);
//                        schedules.add(schedule);
//                    }
//                    adapter.updateSchedules(schedules);
                    dateGroups.clear();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date today = new Date();
                    String todayKey = dateFormat.format(today);
                    for (var doc : querySnapshot) {
                        Schedule schedule = doc.toObject(Schedule.class);
                        String dateKey = dateFormat.format(schedule.getScheduledDatetime());

                        // Only add if date is today or in the future
                        try {
                            Date scheduleDate = dateFormat.parse(dateKey);
                            if (!scheduleDate.before(dateFormat.parse(todayKey))) {
                                if (!dateGroups.containsKey(dateKey)) {
                                    dateGroups.put(dateKey, new ArrayList<>());
                                }
                                dateGroups.get(dateKey).add(schedule);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    adapter.updateData(dateGroups);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load schedules", Toast.LENGTH_SHORT).show());
    }
}