package com.example.trackutem.controller;

import android.os.CountDownTimer;
import com.example.trackutem.utils.NotificationHelper;

public class TimerController {
    private static final long FIVE_MINUTES_WARNING_THRESHOLD = 2 * 60 * 1000;
    private static final long FIVE_MINUTES_WARNING_START = 1 * 60 * 1000;
    private CountDownTimer countDownTimer;
    private TimerCallback timerCallback;
    private NotificationHelper notificationHelper;
    private long timeLeftInMillis;

    public interface TimerCallback {
        void onTimerTick(String formattedTime);
        void onTimerFinish();
    }

    public TimerController(TimerCallback callback, NotificationHelper notificationHelper) {
        this.timerCallback = callback;
        this.notificationHelper = notificationHelper;
    }

    public void startCountdown(long durationMillis) {
        stopCountdown(); // Ensure no duplicate timer

        countDownTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;

                if (timerCallback != null) {
                    timerCallback.onTimerTick(String.format("%02d:%02d", minutes, seconds));
                }

                // 5-minute warning
                if (millisUntilFinished <= FIVE_MINUTES_WARNING_THRESHOLD && millisUntilFinished > FIVE_MINUTES_WARNING_START) {
                    notificationHelper.showTimerNotification("5 minutes left. Tap Continue to resume", false);
                }
            }
            @Override
            public void onFinish() {
                if (timerCallback != null) {
                    timerCallback.onTimerFinish();
                }
//                tvTimer.setText("00:00");
//                // Auto-vibrate when time finishes
//                vibrateDevice(1500);
            }
        }.start();
    }

    public void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }
}