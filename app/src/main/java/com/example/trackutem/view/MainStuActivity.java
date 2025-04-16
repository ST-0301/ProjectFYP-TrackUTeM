package com.example.trackutem.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.trackutem.R;
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

//    private void enableUserLocation() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        mMap.setMyLocationEnabled(true);
//
//        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
//            if (location != null) {
//                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
//
//                // Move camera to current location
//                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 17f));
//
//                // Add marker at current location
//                currentMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title("You are here"));
//            }
//        });
//    }
//
//    private void setMapStyle() {
//        try {
//            boolean success = mMap.setMapStyle(
//                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
//            );
//            if (!success) {
//                Log.e("MapStyle", "Style parsing failed.");
//            }
//        } catch (Resources.NotFoundException e) {
//            Log.e("MapStyle", "Can't find style. Error: ", e);
//        }
//    }
//
//    private void setupMapClickListener() {
//        mMap.setOnMapClickListener(latLng -> {
//            if (currentMarker != null) {
//                currentMarker.remove();
//            }
//
//            currentMarker = mMap.addMarker(new MarkerOptions()
//                    .position(latLng)
//                    .title("Selected location"));
//        });
//    }
//
//    private void addBusStations() {
//        LatLng[] stationLocations = {
//                new LatLng(2.3123, 102.3201),
//                new LatLng(2.3135, 102.3190),
//                new LatLng(2.3150, 102.3178)
//        };
//
//        String[] stationNames = {
//                "UTeM Main Gate",
//                "FKEKK Bus Stop",
//                "UTeM Library Stop"
//        };
//
//        for (int i = 0; i < stationLocations.length; i++) {
//            Marker marker = mMap.addMarker(new MarkerOptions()
//                    .position(stationLocations[i])
//                    .title(stationNames[i])
//                    .icon(getBusIcon(this)));
////                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_marker)));
//            marker.setTag(stationNames[i]);
//        }
//
//        // Click listener
//        mMap.setOnMarkerClickListener(marker -> {
//            String name = (String) marker.getTag();
//            Toast.makeText(this, "You selected: " + name, Toast.LENGTH_SHORT).show();
//            return false;
//        });
//    }
//
//    private BitmapDescriptor getBusIcon(Context context) {
//        int height = 110;
//        int width = 80;
//        BitmapDrawable bitmapdraw = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.bus_station_marker);
//        Bitmap b = bitmapdraw.getBitmap();
//        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
//        return BitmapDescriptorFactory.fromBitmap(smallMarker);
//    }
}