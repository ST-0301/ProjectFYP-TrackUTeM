package com.example.trackutem.view.Student;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Schedule;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class RouteScheduleActivity extends AppCompatActivity {
    public static final String EXTRA_ROUTE_ID = "routeId";
    private ScheduleStuAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_schedule);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView rvSchedules = findViewById(R.id.rvSchedules);
        rvSchedules.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScheduleStuAdapter(this, new ArrayList<>(), null);
        rvSchedules.setAdapter(adapter);

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
                    List<Schedule> schedules = new ArrayList<>();
                    for (var doc : querySnapshot) {
                        Schedule schedule = doc.toObject(Schedule.class);
                        schedules.add(schedule);
                    }                                                                                                                       
                    adapter.updateSchedules(schedules);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load schedules", Toast.LENGTH_SHORT).show());
    }
}