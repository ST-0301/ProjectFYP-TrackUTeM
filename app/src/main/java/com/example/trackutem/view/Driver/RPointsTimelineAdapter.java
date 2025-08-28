// RPointsTimelineAdapter.java
package com.example.trackutem.view.Driver;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.RoutePoint;
import com.example.trackutem.model.Schedule;
import com.google.android.gms.maps.model.LatLng;
import java.util.List;

public class RPointsTimelineAdapter extends RecyclerView.Adapter<RPointsTimelineAdapter.ViewHolder> {
    private final Context context;
    private final List<String> rpointList;
    private List<Schedule.RPointDetail> rpointDetails;
    private OnActionButtonClickListener listener;
    private OnRPointClickListener rpointClickListener;
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

            holder.tvPlanTime.setText(rpoint.getPlanTime());

            boolean showButton = (position == currentRPointIndex && "departed".equals(rpoint.getStatus()) && (currentRPointIndex != -1));
            holder.btnArrival.setVisibility(showButton ? View.VISIBLE : View.GONE);

            // Update status indicator
            switch (rpoint.getStatus()) {
                case "arrived":
                    holder.rpointIndicator.setImageResource(R.drawable.ic_arrived);
                    holder.btnArrival.setVisibility(View.GONE);
                    break;
                case "departed":
                    holder.rpointIndicator.setImageResource(R.drawable.ic_departed);
                    holder.btnArrival.setVisibility(showButton ? View.VISIBLE : View.GONE);
                    break;
                default:
                    holder.rpointIndicator.setImageResource(R.drawable.ic_location);
                    holder.btnArrival.setVisibility(View.GONE);
                    break;
            }

            // Show lateness if available
            if (rpoint.getLatenessMinutes() != 0) {
                String latenessText = rpoint.getLatenessMinutes() > 0 ? "+" + rpoint.getLatenessMinutes() + " min" : rpoint.getLatenessMinutes() + " min";
                holder.tvLateness.setText(latenessText);
                holder.tvLateness.setVisibility(View.VISIBLE);
            } else {
                holder.tvLateness.setVisibility(View.GONE);
            }
        } else {
            // If no rpointDetails or out of bounds, hide dynamic elements
            holder.tvPlanTime.setVisibility(View.GONE);
            holder.tvLateness.setVisibility(View.GONE);
            holder.btnArrival.setVisibility(View.GONE);
            holder.rpointIndicator.setImageResource(R.drawable.ic_location);
        }

        holder.itemView.setOnClickListener(v -> {
            if (rpointClickListener != null && rpointDetails != null && position < rpointDetails.size()) {
                String rpointId = rpointDetails.get(position).getRpointId();
                new RoutePoint().getRPointLocationById(rpointId, new RoutePoint.RPointLocationCallback() {
                    @Override
                    public void onSuccess(LatLng location) {
                        rpointClickListener.onRPointClick(position, location);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("RPointClick", "Error getting location: " + e.getMessage());
                    }
                });
            }
        });
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
        TextView tvPlanTime;
        Button btnArrival;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRPointName = itemView.findViewById(R.id.tvRPointName);
            rpointIndicator = itemView.findViewById(R.id.rpointIndicator);

            tvLateness = itemView.findViewById(R.id.tvLateness);
            tvPlanTime = itemView.findViewById(R.id.tvPlanTime);
            btnArrival = itemView.findViewById(R.id.btnArrival);
        }
    }

    public interface OnActionButtonClickListener {
        void onArrivalClicked(int position, boolean isLastRPoint);
    }
    public void setOnActionButtonClickListener(OnActionButtonClickListener listener) {
        this.listener = listener;
    }
    public interface OnRPointClickListener {
        void onRPointClick(int position, LatLng location);
    }
    public void setOnRPointClickListener(OnRPointClickListener listener) {
        this.rpointClickListener = listener;
    }
}