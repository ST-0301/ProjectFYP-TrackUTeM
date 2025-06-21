// MapController.java
package com.example.trackutem.controller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.trackutem.MainStuActivity;
import com.example.trackutem.R;
import com.example.trackutem.model.DirectionsResponse;
import com.example.trackutem.model.RoutePoint;
import com.example.trackutem.service.DirectionsService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.android.PolyUtil;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapController {
    private final Context context;
    private final GoogleMap mMap;
    private final FusedLocationProviderClient fusedLocationClient;
    private Polyline currentRoutePolyline;
    private Marker currentMarker;
    private Retrofit retrofit;
    private DirectionsService directionsService;

    public MapController(Context context, GoogleMap mMap, FusedLocationProviderClient fusedLocationClient) {
        this.context = context;
        this.mMap = mMap;
        this.fusedLocationClient = fusedLocationClient;

        retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/maps/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        directionsService = retrofit.create(DirectionsService.class);
    }

    // Public Methods
    public void initializeMapFeatures() {
        enableUserLocation();
        setMapStyle();
        setupMapClickListener();
        // addAllBusStops();
    }
    public void drawRoute(List<LatLng> path) {
        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
        }
        currentRoutePolyline = mMap.addPolyline(
                new PolylineOptions()
                        .addAll(path)
                        .width(12f)
                        .color(ContextCompat.getColor(context, R.color.route_color)) // Define in colors.xml
        );
    }
    public void addRouteMarkers(List<LatLng> rpoints, List<String> rpointNames) {
        if (rpoints == null || rpointNames == null || rpoints.size() != rpointNames.size()) {
            return;
        }
        for (int i = 0; i < rpoints.size(); i++) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(rpoints.get(i))
                    .title(rpointNames.get(i))
                    .icon(getBusStopIcon()));
            if (marker != null) {
                marker.setTag(rpointNames.get(i));
            }
        }
    }
    // Overloaded method for cases without route point names
    public void addRouteMarkers(List<LatLng> locations) {
        for (LatLng location : locations) {
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Route Point")
                    .icon(getBusStopIcon()));
        }
    }
    
    public void addRouteBusStopMarkers(List<RoutePoint> routePoints) {
        for (RoutePoint rpoint : routePoints) {
            if ("bus_stop".equals(rpoint.getType())) {
                com.google.firebase.firestore.GeoPoint geoPoint = rpoint.getCoordinates();
                if (geoPoint != null) {
                    LatLng position = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .title(rpoint.getName())
                            .icon(getBusStopIcon()));
                    if (marker != null) {
                        marker.setTag(rpoint);
                    }
                }
            }
        }
    }
    public void zoomToRoute(List<LatLng> path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : path) {
            builder.include(point);
        }
        try {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        } catch (Exception e) {
            // Handle exception for small routes
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(path.get(0), 14f));
        }
    }
    public void drawRouteFromPoints(List<LatLng> points, String apiKey) {
        if (points.size() < 2) return;

        LatLng origin = points.get(0);
        LatLng destination = points.get(points.size() - 1);
        List<LatLng> waypoints = points.subList(1, points.size() - 1);

        getRoutePathFromDirections(origin, destination, waypoints, apiKey);
    }

    private void getRoutePathFromDirections(LatLng origin, LatLng destination, List<LatLng> waypoints, String apiKey) {
        String originStr = origin.latitude + "," + origin.longitude;
        String destinationStr = destination.latitude + "," + destination.longitude;
        String waypointsStr = null;

        if (!waypoints.isEmpty()) {
            StringBuilder sb = new StringBuilder("optimize:true|");
            for (LatLng point : waypoints) {
                sb.append(point.latitude)
                        .append(",")
                        .append(point.longitude)
                        .append("|");
            }
            waypointsStr = sb.substring(0, sb.length() - 1);
        }

        directionsService.getDirections(originStr, destinationStr, waypointsStr, apiKey)
                .enqueue(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            DirectionsResponse directionsResponse = response.body();
                            if (directionsResponse.routes != null &&
                                    !directionsResponse.routes.isEmpty() &&
                                    directionsResponse.routes.get(0).overviewPolyline != null) {

                                String polyline = directionsResponse.routes.get(0).overviewPolyline.points;
                                List<LatLng> path = PolyUtil.decode(polyline);
                                drawRoute(path);
                                zoomToRoute(path);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e("MapController", "Directions request failed", t);
                    }
                });
    }

    // Location & Map Setup
    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        setupInitialLocation();
    }
    private void setupInitialLocation() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdates(1)
                .build();
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                android.location.Location location = locationResult.getLastLocation();

                if (location != null) {
                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    // Move camera to current location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16.5f));

                    if (currentMarker != null) {
                        currentMarker.remove();
                    }
                    // Add marker at current location
                    currentMarker = mMap.addMarker(new MarkerOptions()
                            .position(userLatLng)
                            .title("You are here"));
                    fusedLocationClient.removeLocationUpdates(this);
                }
            }
        };
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e("LocationError", "SecurityException in location updates: " + e.getMessage());
        }
    }
    private void setMapStyle() {
        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style));
            if (!success) {
                Log.e("MapStyle", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapStyle", "Can't find style. Error: ", e);
        }
    }

    // Marker & Interaction
    @SuppressLint("PotentialBehaviorOverride")
    private void setupMapClickListener() {
        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag != null && tag instanceof RoutePoint) {
                RoutePoint rpoint = (RoutePoint) tag;
                if (context instanceof MainStuActivity) {
                    ((MainStuActivity) context).showBusStopDetails(rpoint);
                }
                return true;
            }
//            if (marker.getTag() != null) {
//                String rpointName = (String) marker.getTag();
//                if (currentMarker != null) {
//                    currentMarker.remove();
//                }
//                currentMarker = mMap.addMarker(new MarkerOptions()
//                        .position(marker.getPosition())
//                        .title("Selected: " + rpointName)
//                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
//                        .zIndex(1.0f));
////                currentMarker.showInfoWindow();
//                if (currentMarker != null) {
//                    currentMarker.showInfoWindow();
//                }
//                return true;
//            }
            return false;
        });
    }
//    private void addBusStops() {
//        LatLng[] stopLocations = {
//                new LatLng(2.3123, 102.3201),
//                new LatLng(2.3135, 102.3190),
//                new LatLng(2.3150, 102.3178)
//        };
//
//        String[] stopNames = {
//                "UTeM Main Gate",
//                "FKEKK Bus Stop",
//                "UTeM Library Stop"
//        };
//
//        for (int i = 0; i < stopLocations.length; i++) {
//            Marker marker = mMap.addMarker(new MarkerOptions()
//                    .position(stopLocations[i])
//                    .title(stopNames[i])
//                    .icon(getBusIcon()));
////                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_marker)));
//            marker.setTag(stopNames[i]);
//        }
//    }
public void addAllBusStops() {
    RoutePoint.getAllRPoints(new RoutePoint.AllRPointsCallback() {
        @Override
        public void onSuccess(List<RoutePoint> rpoints) {
            for (RoutePoint rpoint : rpoints) {
                if ("bus_stop".equals(rpoint.getType())) {
                    GeoPoint geoPoint = rpoint.getCoordinates();
                    if (geoPoint != null) {
                        LatLng position = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(rpoint.getName())
                                .icon(getBusStopIcon()));
                        if (marker != null) {
                            marker.setTag(rpoint);
                        }
                    }
                }
            }
        }
        @Override
        public void onError(Exception e) {
            Log.e("MapController", "Error fetching bus stops", e);
        }
    });
}
    private BitmapDescriptor getBusStopIcon() {
        // int height = 110;
        // int width = 80;
        // BitmapDrawable bitmapdraw = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.bus_stop_marker);
        // if (bitmapdraw == null) {
        //     return BitmapDescriptorFactory.defaultMarker();
        // }
        // Bitmap b = bitmapdraw.getBitmap();
        // Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        // return BitmapDescriptorFactory.fromBitmap(smallMarker);

        // return BitmapDescriptorFactory.fromResource(R.drawable.bus_stop_marker);
        Drawable vectorDrawable = ContextCompat.getDrawable(context, R.drawable.bus_stop_marker);
        if (vectorDrawable == null)
            return BitmapDescriptorFactory.defaultMarker();
        Bitmap bitmap = Bitmap.createBitmap(
                vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
    public BitmapDescriptor getBusLocationIcon() {
        // int height = 110;
        // int width = 120;
        // BitmapDrawable bitmapdraw = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.bus_marker);
        // if (bitmapdraw == null) {
        //     return BitmapDescriptorFactory.defaultMarker();
        // }
        // Bitmap b = bitmapdraw.getBitmap();
        // Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        // return BitmapDescriptorFactory.fromBitmap(smallMarker);

        // return BitmapDescriptorFactory.fromResource(R.drawable.bus_marker);
        Drawable vectorDrawable = ContextCompat.getDrawable(context, R.drawable.bus_marker);
        if (vectorDrawable == null)
            return BitmapDescriptorFactory.defaultMarker();
        Bitmap bitmap = Bitmap.createBitmap(
                vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}