package com.example.trackutem.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppStateManager {
    private static final String KEY_BUTTON_STATE = "button_state"; // start, rest, continue
    private static final String KEY_TIMER_END = "timer_end_time";
    private static final String KEY_IS_TRACKING = "is_tracking";
    private final SharedPreferences prefs;

    public AppStateManager(Context context) {
        prefs = context.getSharedPreferences("AppStatePrefs", Context.MODE_PRIVATE);
    }

    public void saveState(String buttonState, long timerEndMillis, boolean isTracking) {
        prefs.edit()
                .putString(KEY_BUTTON_STATE, buttonState)
                .putLong(KEY_TIMER_END, timerEndMillis)
                .putBoolean(KEY_IS_TRACKING, isTracking)
                .apply();
    }

    public String getButtonState() {
        return prefs.getString(KEY_BUTTON_STATE, "stop");
    }

    public long getTimerEndTime() {
        return prefs.getLong(KEY_TIMER_END, 0);
    }

    public boolean isTracking() {
        return prefs.getBoolean(KEY_IS_TRACKING, false);
    }

    public void clear() {
//        prefs.edit().clear().apply();
        prefs.edit()
                .remove(KEY_BUTTON_STATE)
                .remove(KEY_TIMER_END)
                .remove(KEY_IS_TRACKING)
                .apply();
    }
}