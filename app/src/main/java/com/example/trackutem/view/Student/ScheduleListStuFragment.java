package com.example.trackutem.view.Student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Schedule;
import java.util.ArrayList;
import java.util.List;

public class ScheduleListStuFragment extends Fragment {
    private static final String ARG_SCHEDULES = "schedules";
    private List<Schedule> schedules = new ArrayList<>();

    public static ScheduleListStuFragment newInstance(List<Schedule> schedules) {
        ScheduleListStuFragment fragment = new ScheduleListStuFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SCHEDULES, new ArrayList<>(schedules));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            schedules = (List<Schedule>) getArguments().getSerializable(ARG_SCHEDULES);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule_list_stu, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.rvSchedules);
        TextView tvEmpty = view.findViewById(R.id.tvEmpty);

        if (schedules.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            ScheduleStuAdapter adapter = new ScheduleStuAdapter(requireContext(), schedules, null);
            recyclerView.setAdapter(adapter);
        }
        return view;
    }
}