// DirectionsService.java
package com.example.trackutem.service;

import com.example.trackutem.model.DirectionsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DirectionsService {
    @GET("directions/json")
    Call<DirectionsResponse> getDirections(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("waypoints") String waypoints,
            @Query("key") String apiKey
    );
}