package com.example.trackutem;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.trackutem.controller.MapController;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;

public class MainStuActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private MapController mapController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainstu);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermissions();
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
        } else {
            initializeMap();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               initializeMap();
            } else {
                Toast.makeText(this, "Permission to ", Toast.LENGTH_SHORT).show();
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

//    DatabaseReference locationRef = FirebaseDatabase.getInstance()
//            .getReference("bus_locations")
//            .child("bus1");
//
//locationRef.addValueEventListener(new ValueEventListener() {
//        @Override
//        public void onDataChange(@NonNull DataSnapshot snapshot) {
//            Double lat = snapshot.child("lat").getValue(Double.class);
//            Double lng = snapshot.child("lng").getValue(Double.class);
//            long timestamp = snapshot.child("timestamp").getValue(Long.class);
//
//            // Update marker on Google Map
//            LatLng newPos = new LatLng(lat, lng);
//            // e.g., move marker or animate camera here
//        }
//
//        @Override
//        public void onCancelled(@NonNull DatabaseError error) {
//            Log.e("Firebase", "Failed to read location", error.toException());
//        }
//    });
}