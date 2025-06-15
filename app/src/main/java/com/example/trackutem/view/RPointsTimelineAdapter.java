// RPointsTimelineAdapter.java
package com.example.trackutem.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Schedule;
import java.util.List;

public class RPointsTimelineAdapter extends RecyclerView.Adapter<RPointsTimelineAdapter.ViewHolder> {
    private final Context context;
    private final List<String> rpointList;
    private List<Schedule.RPointDetail> rpointDetails;
    private OnActionButtonClickListener listener;
    private int currentRPointIndex = -1;

    public RPointsTimelineAdapter(Context context, List<String> rpointList) {
        this.context = context;
        this.rpointList = rpointList;
    }
    public void setRPointDetails(List<Schedule.RPointDetail> rpointDetails) {
        this.rpointDetails = rpointDetails;
    }
    public void setCurrentRPointIndex(int index) {
        currentRPointIndex = index;
    }

    // Adapter Implementation
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rpoint_timeline, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String rpointName = rpointList.get(position);
        holder.tvRPointName.setText(rpointName);

        boolean isLastRPoint = (position == rpointList.size() - 1);
        holder.btnArrival.setOnClickListener(v -> {
            if (listener != null) listener.onArrivalClicked(position, isLastRPoint);
        });

        if (rpointDetails != null && position < rpointDetails.size()) {
            Schedule.RPointDetail rpoint = rpointDetails.get(position);

            // Only show button for current departed route point
            boolean showButton = (position == currentRPointIndex && "departed".equals(rpoint.getStatus()) && (currentRPointIndex != -1));
            holder.btnArrival.setVisibility(showButton ? View.VISIBLE : View.GONE);

            // Update status indicator
            switch (rpoint.getStatus()) {
                case "arrived":
                    holder.rpointIndicator.setImageResource(R.drawable.ic_arrived);
                    holder.btnArrival.setVisibility(showButton ? View.VISIBLE : View.GONE);
                    break;
                case "departed":
                    holder.rpointIndicator.setImageResource(R.drawable.ic_departed);
                    holder.btnArrival.setVisibility(showButton ? View.VISIBLE : View.GONE);
                    break;
                default:
                    holder.rpointIndicator.setImageResource(R.drawable.ic_location);
                    break;
            }

            // Show lateness if available
            if (rpoint.getLatenessMinutes() != 0) {
                String latenessText = rpoint.getLatenessMinutes() > 0 ?
                        "+" + rpoint.getLatenessMinutes() + " min" :
                        rpoint.getLatenessMinutes() + " min";
                holder.tvLateness.setText(latenessText);
                holder.tvLateness.setVisibility(View.VISIBLE);
            } else {
                holder.tvLateness.setVisibility(View.GONE);
            }
        }
    }
    @Override
    public int getItemCount() {
        return rpointList.size();
    }
    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
    }

    // ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRPointName;
        ImageView rpointIndicator;
        TextView tvLateness;
        Button btnArrival;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRPointName = itemView.findViewById(R.id.tvRPointName);
            rpointIndicator = itemView.findViewById(R.id.rpointIndicator);

            tvLateness = itemView.findViewById(R.id.tvLateness);
            btnArrival = itemView.findViewById(R.id.btnArrival);
        }
    }

    public interface OnActionButtonClickListener {
        void onArrivalClicked(int position, boolean isLastRPoint);
    }
    public void setOnActionButtonClickListener(OnActionButtonClickListener listener) {
        this.listener = listener;
    }
}