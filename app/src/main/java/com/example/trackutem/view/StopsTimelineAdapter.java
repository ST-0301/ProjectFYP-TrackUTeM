package com.example.trackutem.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import java.util.List;

public class StopsTimelineAdapter extends RecyclerView.Adapter<StopsTimelineAdapter.ViewHolder> {
    private List<String> stopList;
    private int currentStopIndex = 0;
    private Context context;
    public StopsTimelineAdapter(Context context, List<String> stopList) {
        this.context = context;
        this.stopList = stopList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stop_timeline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String stopName = stopList.get(position);
        holder.tvStopName.setText(stopName);
        holder.tvStopName.setContentDescription(
                context.getString(R.string.stop_item_description, stopName)
        );
    }

    @Override
    public int getItemCount() {
        return stopList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStopName;
        ImageView stopIndicator;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStopName = itemView.findViewById(R.id.tvStopName);
            stopIndicator = itemView.findViewById(R.id.stopIndicator);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
    }
}
