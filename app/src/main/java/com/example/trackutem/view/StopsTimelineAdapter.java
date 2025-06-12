// StopsTimelineAdapter.java
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

public class StopsTimelineAdapter extends RecyclerView.Adapter<StopsTimelineAdapter.ViewHolder> {
    private final Context context;
    private final List<String> stopList;
    private List<Schedule.StopDetail> stopDetails;
    private OnActionButtonClickListener listener;
    private int currentStopIndex = -1;

    public StopsTimelineAdapter(Context context, List<String> stopList) {
        this.context = context;
        this.stopList = stopList;
    }
    public void setStopDetails(List<Schedule.StopDetail> stopDetails) {
        this.stopDetails = stopDetails;
    }
    public void setCurrentStopIndex(int index) {
        currentStopIndex = index;
    }

    // Adapter Implementation
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stop_timeline, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String stopName = stopList.get(position);
        holder.tvStopName.setText(stopName);

        boolean isLastStop = (position == stopList.size() - 1);
        holder.btnArrival.setOnClickListener(v -> {
            if (listener != null) listener.onArrivalClicked(position, isLastStop);
        });

        if (stopDetails != null && position < stopDetails.size()) {
            Schedule.StopDetail stop = stopDetails.get(position);

            // Only show button for current departed stop
            boolean showButton = (position == currentStopIndex && "departed".equals(stop.getStatus()) && (currentStopIndex != -1));
            holder.btnArrival.setVisibility(showButton ? View.VISIBLE : View.GONE);

            // Update status indicator
            switch (stop.getStatus()) {
                case "arrived":
                    holder.stopIndicator.setImageResource(R.drawable.ic_arrived);
                    holder.btnArrival.setVisibility(showButton ? View.VISIBLE : View.GONE);
                    break;
                case "departed":
                    holder.stopIndicator.setImageResource(R.drawable.ic_departed);
                    holder.btnArrival.setVisibility(showButton ? View.VISIBLE : View.GONE);
                    break;
                default:
                    holder.stopIndicator.setImageResource(R.drawable.ic_location);
                    break;
            }

            // Show lateness if available
            if (stop.getLatenessMinutes() != 0) {
                String latenessText = stop.getLatenessMinutes() > 0 ?
                        "+" + stop.getLatenessMinutes() + " min" :
                        stop.getLatenessMinutes() + " min";
                holder.tvLateness.setText(latenessText);
                holder.tvLateness.setVisibility(View.VISIBLE);
            } else {
                holder.tvLateness.setVisibility(View.GONE);
            }
        }
    }
    @Override
    public int getItemCount() {
        return stopList.size();
    }
    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
    }

    // ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStopName;
        ImageView stopIndicator;
        TextView tvLateness;
        Button btnArrival;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStopName = itemView.findViewById(R.id.tvStopName);
            stopIndicator = itemView.findViewById(R.id.stopIndicator);

            tvLateness = itemView.findViewById(R.id.tvLateness);
            btnArrival = itemView.findViewById(R.id.btnArrival);
        }
    }

    public interface OnActionButtonClickListener {
        void onArrivalClicked(int position, boolean isLastStop);
    }
    public void setOnActionButtonClickListener(OnActionButtonClickListener listener) {
        this.listener = listener;
    }
}