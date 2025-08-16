package com.example.trackutem.view.Student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Schedule;
import java.util.List;

public class RPointsTimelineStuAdapter extends RecyclerView.Adapter<RPointsTimelineStuAdapter.ViewHolder> {
    private final List<Schedule.RPointDetail> rpoints;
    private final List<String> rpointNames;
    private OnItemClickListener listener;
    private boolean isClickable;

    public RPointsTimelineStuAdapter(List<Schedule.RPointDetail> rpoints, List<String> rpointNames, OnItemClickListener listener, boolean isClickable) {
        this.rpoints = rpoints;
        this.rpointNames = rpointNames;
        this.listener = listener;
        this.isClickable = isClickable;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rpoint_timeline_stu, parent, false);
        return new ViewHolder(v, listener, rpoints, rpointNames);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Schedule.RPointDetail rpoint = rpoints.get(position);
        holder.tvRPointName.setText(rpointNames.get(position));
        holder.tvExpDepTime.setText(rpoint.getPlanTime());

        // Set icon based on status
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
        holder.itemView.setAlpha(itemIsActuallyClickable ? 1.0f : 0.5f);
        holder.itemView.setClickable(itemIsActuallyClickable);
        holder.itemView.setFocusable(itemIsActuallyClickable);
    }
    @Override
    public int getItemCount() {
        return rpoints.size();
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStatus;
        TextView tvRPointName, tvExpDepTime;
        ViewHolder(View v, OnItemClickListener listener, List<Schedule.RPointDetail> rpoints, List<String> rpointNames) {
            super(v);
            ivStatus = v.findViewById(R.id.ivStatus);
            tvRPointName = v.findViewById(R.id.tvRPointName);
            tvExpDepTime = v.findViewById(R.id.tvExpDepTime);
            v.setOnClickListener(view -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(rpoints.get(position).getRPointId(), rpointNames.get(position));
                    }
                }
            });
        }
    }
    public interface OnItemClickListener {
        void onItemClick(String rpointId, String rpointName);
    }
}