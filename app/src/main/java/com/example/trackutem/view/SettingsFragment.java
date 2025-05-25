package com.example.trackutem.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.trackutem.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends Fragment {
    private SharedPreferences prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> handleLogout());
        view.findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> handleAccountDeletion());
    }
    private void handleLogout() {
        requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        startActivity(intent);
        requireActivity().finish();
    }
    private void handleAccountDeletion() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure? This action cannot be undone")
                .setPositiveButton("Delete", (dialog, which) -> {
//                    deleteAccountFromServer();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}