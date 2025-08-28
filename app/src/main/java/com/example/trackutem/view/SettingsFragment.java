package com.example.trackutem.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.controller.LoginController;
import com.example.trackutem.model.Notification;
import com.example.trackutem.service.MyFirebaseMessagingService;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.List;

public class SettingsFragment extends Fragment {
    private AppBarLayout appBarLayout;
    private LinearLayout titleLayout;
    private MaterialToolbar toolbar;
    private MaterialCardView cardInformation, cardAppSettings;
    private TextView tvUserName, tvUserEmail;
    private ProgressBar pbLoadingAccount;
    private MaterialButton btnLogout, btnDeleteAccount, btnChangePassword, btnTermsAndConditions, btnContactUs;
    private String userId;
    private String userType;
    private String userEmail;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

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

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        pbLoadingAccount = view.findViewById(R.id.pbLoadingAccount);
        cardInformation = view.findViewById(R.id.cardInformation);
        cardAppSettings = view.findViewById(R.id.cardAppSettings);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnTermsAndConditions = view.findViewById(R.id.btnTermsAndConditions);
        btnContactUs = view.findViewById(R.id.btnContactUs);

        loadUserProfile();

        btnLogout.setOnClickListener(v -> handleLogout());
        btnDeleteAccount.setOnClickListener(v -> handleAccountDeletion());
        btnChangePassword.setOnClickListener(v -> handleChangePassword());
        btnTermsAndConditions.setOnClickListener(v -> handleTermsAndConditions());
        btnContactUs.setOnClickListener(v -> handleContactUs());

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
                    activity.getSupportActionBar().setTitle("Settings");
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

    private void loadUserProfile() {
        if (userId != null && userType != null) {
            pbLoadingAccount.setVisibility(View.VISIBLE);

            String collectionPath = userType.equals("driver") ? "drivers" : "students";
            FirebaseFirestore.getInstance().collection(collectionPath)
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (isAdded() && documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            userEmail = documentSnapshot.getString("email");
                            tvUserName.setText(name);
                            tvUserEmail.setText(userEmail);
                        }
                        pbLoadingAccount.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            tvUserName.setText("Error loading profile");
                            tvUserEmail.setText("");
                        }
                        pbLoadingAccount.setVisibility(View.GONE);
                    });
        } else {
            // Hide loading if no user ID
            pbLoadingAccount.setVisibility(View.GONE);
        }
    }

    private void handleChangePassword() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Reset Password")
                .setMessage("A password reset link will be sent to " + userEmail + ". Continue?")
                .setPositiveButton("Send Link", (dialog, which) -> {
                    resetPassword(userEmail);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetPassword(String email) {
        LoginController loginController = new LoginController();
        loginController.resetPassword(email, new LoginController.ResetPasswordCallback() {
            @Override
            public void onResetPasswordSuccess() {
                new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                        .setTitle("Success")
                        .setMessage("Password reset email sent to " + email)
                        .setPositiveButton("OK", null)
                        .show();
            }
            @Override
            public void onResetPasswordFailure(String errorMessage) {
                new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                        .setTitle("Error")
                        .setMessage("Failed to send reset email: " + errorMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void handleTermsAndConditions() {
        Intent intent = new Intent(getActivity(), TermsAndConditionsActivity.class);
        startActivity(intent);
    }

    private void handleContactUs() {
        String[] contactOptions = {"Email Support", "Call Support", "Visit Website"};

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Contact Us")
                .setItems(contactOptions, (dialog, which) -> {
                    switch (which) {
                        case 0: // Email
                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                            emailIntent.setData(Uri.parse("mailto:helpdeskinfo@utem.edu.my"));
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "TrackUTeM App Support");
                            if (emailIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                                startActivity(emailIntent);
                            } else {
                                Toast.makeText(getContext(), "No email app installed", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 1: // Phone
                            Intent callIntent = new Intent(Intent.ACTION_DIAL);
                            callIntent.setData(Uri.parse("tel:062701102"));
                            if (callIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                                startActivity(callIntent);
                            } else {
                                Toast.makeText(getContext(), "No phone app installed", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 2: // Website
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://www.utem.edu.my/en/help-services.html"));
                            startActivity(browserIntent);
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleLogout() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    if (userId != null && userType != null) {
                        String collectionPath = userType.equals("driver") ? "drivers" : "students";
                        FirebaseFirestore.getInstance().collection(collectionPath).document(userId)
                                .update("pushToken", null)
                                .addOnCompleteListener(task -> proceedToLoginScreen());
                    } else {
                        proceedToLoginScreen();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void proceedToLoginScreen() {
        if (getContext() == null) return;

        FirebaseMessaging.getInstance().deleteToken();
        MyFirebaseMessagingService.clearToken(requireContext());

        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void handleAccountDeletion() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Delete Account")
                .setMessage("Are you sure? This action cannot be undone")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || userId == null || userType == null) {
            Toast.makeText(getContext(), "Error: User not found. Please re-login.", Toast.LENGTH_SHORT).show();
            return;
        }

        String collectionPath = userType.equals("driver") ? "drivers" : "students";
        FirebaseFirestore.getInstance().collection(collectionPath).document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    user.delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(getContext(), "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                                proceedToLoginScreen();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to delete account. Please re-login and try again. " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
