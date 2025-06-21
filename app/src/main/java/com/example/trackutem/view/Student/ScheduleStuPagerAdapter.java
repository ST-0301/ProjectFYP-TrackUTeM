package com.example.trackutem.view.Student;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.trackutem.model.Schedule;

import java.util.List;

public class ScheduleStuPagerAdapter extends FragmentStateAdapter {
    private final List<Schedule> todaySchedules;
    private final List<Schedule> upcomingSchedules;

    public ScheduleStuPagerAdapter(FragmentActivity fa,
                                   List<Schedule> todaySchedules,
                                   List<Schedule> upcomingSchedules) {
        super(fa);
        this.todaySchedules = todaySchedules;
        this.upcomingSchedules = upcomingSchedules;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return (position == 0) ?
                com.example.trackutem.view.Student.ScheduleListStuFragment.newInstance(todaySchedules) :
                com.example.trackutem.view.Student.ScheduleListStuFragment.newInstance(upcomingSchedules);
    }

    @Override
    public int getItemCount() {
        return 2; // Today and Upcoming tabs
    }
}