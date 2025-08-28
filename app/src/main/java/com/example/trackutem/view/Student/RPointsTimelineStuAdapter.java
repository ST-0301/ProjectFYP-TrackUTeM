package com.example.trackutem.view.Student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Schedule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RPointsTimelineStuAdapter extends RecyclerView.Adapter<RPointsTimelineStuAdapter.ViewHolder> {
    private final List<Schedule.RPointDetail> rpoints;
    private final List<String> rpointNames;
    private OnItemClickListener listener;
    private OnRPointLocationClickListener locationClickListener;
    private boolean isClickable;

    public RPointsTimelineStuAdapter(List<Schedule.RPointDetail> rpoints, List<String> rpointNames,
                                     OnItemClickListener listener, OnRPointLocationClickListener locationClickListener,
                                     boolean isClickable) {
        this.rpoints = rpoints;
        this.rpointNames = rpointNames;
        this.listener = listener;
        this.locationClickListener = locationClickListener;
        this.isClickable = isClickable;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rpoint_timeline_stu, parent, false);
        return new ViewHolder(v, listener, locationClickListener, rpoints, rpointNames);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Schedule.RPointDetail rpoint = rpoints.get(position);
        holder.tvRPointName.setText(rpointNames.get(position));

        int effectiveLateness = 0;
        for (int i = 0; i <= position; i++) {
            if (rpoints.get(i).getLatenessMinutes() > effectiveLateness) {
                effectiveLateness = rpoints.get(i).getLatenessMinutes();
            }
        }
        String status = rpoint.getStatus();
        if (effectiveLateness > 0 && !"arrived".equals(status)) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date planTimeDate = sdf.parse(rpoint.getPlanTime());
                Calendar calendar = Calendar.getInstance();
                if (planTimeDate != null) {
                    calendar.setTime(planTimeDate);
                }
                calendar.add(Calendar.MINUTE, effectiveLateness);
                String newTime = sdf.format(calendar.getTime());

                String formattedTime = String.format(Locale.getDefault(),
                        "%s → %s", rpoint.getPlanTime(), newTime);
                holder.tvPlanTime.setText(formattedTime);

                holder.tvLateness.setText(String.format(Locale.getDefault(), "(+%d min)", effectiveLateness));
                holder.tvLateness.setVisibility(View.VISIBLE);
            } catch (ParseException e) {
                holder.tvPlanTime.setText(rpoint.getPlanTime());
                holder.tvLateness.setVisibility(View.GONE);
                e.printStackTrace();
            }
        } else {
            holder.tvPlanTime.setText(rpoint.getPlanTime());
            holder.tvLateness.setVisibility(View.GONE);
        }

        switch (rpoint.getStatus()) {
            case "arrived":
                holder.ivStatus.setImageResource(R.drawable.ic_arrived);
                holder.ivStatus.setColorFilter(holder.itemView.getContext().getColor(R.color.secondaryGreen));
                break;
            case "departed":
                holder.ivStatus.setImageResource(R.drawable.ic_departed);
                holder.ivStatus.setColorFilter(holder.itemView.getContext().getColor(R.color.primaryBlue));
                break;
            default:
                holder.ivStatus.setImageResource(R.drawable.ic_location);
                holder.ivStatus.setColorFilter(holder.itemView.getContext().getColor(R.color.textSecondary));
                break;
        }

        boolean itemIsActuallyClickable = this.isClickable && !"arrived".equals(rpoint.getStatus());
        holder.itemView.setClickable(itemIsActuallyClickable);
        holder.itemView.setFocusable(itemIsActuallyClickable);

        holder.itemView.setOnClickListener(view -> {
            if (itemIsActuallyClickable) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    String rpointId = rpoints.get(pos).getRpointId();
                    String rpointName = rpointNames.get(pos);
                    if (locationClickListener != null) {
                        locationClickListener.onRPointLocationClick(rpointId, rpointName);
                    }
                    if (listener != null) {
                        listener.onItemClick(rpointId, rpointName);
                    }
                }
            }
        });
    }
    @Override
    public int getItemCount() {
        return rpoints.size();
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStatus;
        TextView tvRPointName, tvPlanTime, tvLateness;

        ViewHolder(View v, OnItemClickListener listener, OnRPointLocationClickListener locationClickListener,
                   List<Schedule.RPointDetail> rpoints, List<String> rpointNames) {
            super(v);
            ivStatus = v.findViewById(R.id.ivStatus);
            tvRPointName = v.findViewById(R.id.tvRPointName);
            tvPlanTime = v.findViewById(R.id.tvPlanTime);
            tvLateness = v.findViewById(R.id.tvLateness);
        }
    }
    public interface OnItemClickListener {
        void onItemClick(String rpointId, String rpointName);
    }

    public interface OnRPointLocationClickListener {
        void onRPointLocationClick(String rpointId, String rpointName);
    }
}