package com.example.trackutem.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.Notification;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {
    private AppBarLayout appBarLayout;
    private MaterialToolbar toolbar;
    private RecyclerView rvNotifications;
    private List<Notification> notificationList;
    private NotificationAdapter notificationAdapter;
    private ProgressBar pbLoadingNotifications;
    private TextView tvLoadingNotifications;
    private LinearLayout titleLayout, llEmpty;
    private FirebaseFirestore db;
    private DocumentSnapshot lastVisible;
    private boolean isScrolling = false;
    private boolean isLastItemReached = false;
    private String userId;
    private String userType;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userId = prefs.getString("driverId", null);
        if (userId != null) {
            userType = "driver";
        } else {
            userId = prefs.getString("studentId", null);
            userType = "student";
        }

        // Setup toolbar
        titleLayout = view.findViewById(R.id.titleLayout);
        appBarLayout = view.findViewById(R.id.appBarLayout);
        toolbar = view.findViewById(R.id.toolbar);
//        setupToolbar();

        rvNotifications = view.findViewById(R.id.rvNotifications);
        pbLoadingNotifications = view.findViewById(R.id.pbLoadingNotifications);
        tvLoadingNotifications = view.findViewById(R.id.tvLoadingNotifications);
        llEmpty = view.findViewById(R.id.llEmpty);

        db = FirebaseFirestore.getInstance();
        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(notificationList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvNotifications.setLayoutManager(layoutManager);
        rvNotifications.setAdapter(notificationAdapter);

        loadNotifications();

        rvNotifications.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isScrolling = true;
                }
            }
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (linearLayoutManager != null) {
                    int firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
                    int visibleItemCount = linearLayoutManager.getChildCount();
                    int totalItemCount = linearLayoutManager.getItemCount();
                    if (isScrolling && (firstVisibleItemPosition + visibleItemCount == totalItemCount) && !isLastItemReached) {
                        isScrolling = false;
                        loadMoreNotifications();
                    }
                }
            }
        });
        return view;
    }

    private void setupToolbar() {
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            boolean isStudentApp = activity.findViewById(R.id.nav_view) == null;
            if (isStudentApp) {
                appBarLayout.setVisibility(View.VISIBLE);
                titleLayout.setVisibility(View.GONE);
                activity.setSupportActionBar(toolbar);
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setTitle("Notifications");
                    activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
                }
                toolbar.setNavigationOnClickListener(v -> {
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                });
            } else {
                // Driver app - hide toolbar, show title
                appBarLayout.setVisibility(View.GONE);
                titleLayout.setVisibility(View.VISIBLE);

                View root = getView();
                if (root instanceof ConstraintLayout) {
                    ConstraintSet set = new ConstraintSet();
                    set.clone((ConstraintLayout) root);

                    int contentId = (getView().findViewById(R.id.scrollView) != null)
                            ? R.id.scrollView
                            : R.id.rvNotifications;

                    set.connect(contentId, ConstraintSet.TOP, R.id.titleLayout, ConstraintSet.BOTTOM);
                    set.applyTo((ConstraintLayout) root);
                }
            }
        }
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            pbLoadingNotifications.setVisibility(View.VISIBLE);
            tvLoadingNotifications.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            llEmpty.setVisibility(View.GONE);
        } else {
            pbLoadingNotifications.setVisibility(View.GONE);
            tvLoadingNotifications.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    private void loadNotifications() {
        showLoading(true);
        Query query;
        if ("student".equals(userType)) {
            query = db.collection("notifications")
                    .whereEqualTo("audienceType", "student")
                    .whereEqualTo("audienceId", "allStudents")
                    .orderBy("created", Query.Direction.DESCENDING)
                    .limit(20);
        } else {
            query = db.collection("notifications")
                    .whereEqualTo("audienceType", "driver")
                    .whereEqualTo("audienceId", userId)
                    .orderBy("created", Query.Direction.DESCENDING)
                    .limit(20);
        }

        query.get().addOnSuccessListener(documentSnapshots -> {
            showLoading(false);
            if (documentSnapshots.isEmpty()) {
                llEmpty.setVisibility(View.VISIBLE);
            } else {
                notificationList.clear();
                for (DocumentSnapshot document : documentSnapshots.getDocuments()) {
                    Notification notification = document.toObject(Notification.class);
                    if (notification != null) {
                        notificationList.add(notification);
                    }
                }
                notificationAdapter.notifyItemRangeInserted(0, notificationList.size());
                if (!documentSnapshots.getDocuments().isEmpty()) {
                    lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
                }
            }
            pbLoadingNotifications.setVisibility(View.GONE);
        }).addOnFailureListener(e -> {
            showLoading(false);
            llEmpty.setVisibility(View.VISIBLE);
        });
    }

    private void loadMoreNotifications() {
        pbLoadingNotifications.setVisibility(View.VISIBLE);
        Query query;
        if ("student".equals(userType)) {
            query = db.collection("notifications")
                    .whereEqualTo("audienceType", "student")
                    .whereEqualTo("audienceId", "allStudents")
                    .orderBy("created", Query.Direction.DESCENDING)
                    .startAfter(lastVisible)
                    .limit(20);
        } else {
            query = db.collection("notifications")
                    .whereEqualTo("audienceType", "driver")
                    .whereEqualTo("audienceId", userId)
                    .orderBy("created", Query.Direction.DESCENDING)
                    .startAfter(lastVisible)
                    .limit(20);
        }

        query.get().addOnSuccessListener(documentSnapshots -> {
            pbLoadingNotifications.setVisibility(View.GONE);
            if (!documentSnapshots.isEmpty()) {
                int startPosition = notificationList.size();
                for (DocumentSnapshot document : documentSnapshots.getDocuments()) {
                    Notification notification = document.toObject(Notification.class);
                    if (notification != null) {
                        notificationList.add(notification);
                    }
                }
                notificationAdapter.notifyItemRangeInserted(startPosition, documentSnapshots.size());
                lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
            } else {
                isLastItemReached = true;
            }
        }).addOnFailureListener(e -> pbLoadingNotifications.setVisibility(View.GONE));
    }
}