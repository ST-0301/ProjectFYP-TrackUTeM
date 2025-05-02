package com.example.trackutem.controller;

import android.annotation.SuppressLint;
import android.os.CountDownTimer;
import com.example.trackutem.utils.NotificationHelper;

public class TimerController {
    private static final long FIVE_MINUTES_WARNING = 1 * 60 * 1000;
    private CountDownTimer countDownTimer;
    private final TimerCallback timerCallback;
    private final NotificationHelper notificationHelper;
    private long timeLeftInMillis;
    private boolean fiveMinuteNotificationSent = false;

    public TimerController(TimerCallback callback, NotificationHelper notificationHelper) {
        this.timerCallback = callback;
        this.notificationHelper = notificationHelper;
    }

    public void startCountdown(long durationMillis) {
        stopCountdown(); // Ensure no duplicate timer
        fiveMinuteNotificationSent = false;

        countDownTimer = new CountDownTimer(durationMillis, 1000) {
            @SuppressLint("DefaultLocale")
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;

                if (timerCallback != null) {
                    timerCallback.onTimerTick(String.format("%02d:%02d", minutes, seconds));
                }

                // 5-minute warning
                if (millisUntilFinished <= FIVE_MINUTES_WARNING && !fiveMinuteNotificationSent) {
                    notificationHelper.showTimerNotification("5 minutes left. Tap Continue to resume", false);
                    fiveMinuteNotificationSent = true;
                }
            }
            @Override
            public void onFinish() {
                notificationHelper.showTimerNotification("Rest time over. Tap Continue to resume", true);

                if (timerCallback != null) {
                    timerCallback.onTimerFinish();
                }
            }
        }.start();
    }

    public void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    public interface TimerCallback {
        void onTimerTick(String formattedTime);
        void onTimerFinish();
    }
}