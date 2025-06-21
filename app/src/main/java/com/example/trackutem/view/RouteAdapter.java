package com.example.trackutem.view;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Route;
import com.example.trackutem.view.Student.RouteScheduleActivity;
import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.ViewHolder> {
    private List<Route> routes;
    private OnRouteClickListener listener;
    private Context context;

    public interface OnRouteClickListener {
        void onRouteClick(Route route);
    }
    public RouteAdapter(List<Route> routes, OnRouteClickListener listener) {
        this.routes = routes;
        this.listener = listener;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_route_card, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Route route = routes.get(position);
        holder.tvRouteName.setText(route.getName());
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RouteScheduleActivity.class);
            intent.putExtra(RouteScheduleActivity.EXTRA_ROUTE_ID, route.getRouteId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return routes.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvRouteName;
        public ViewHolder(View itemView) {
            super(itemView);
            tvRouteName = itemView.findViewById(R.id.tvRouteName);
        }
    }
    public void updateRoutes(List<Route> newRoutes) {
        this.routes = newRoutes;
        notifyDataSetChanged();
    }
}