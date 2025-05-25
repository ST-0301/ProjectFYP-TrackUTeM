package com.example.trackutem.view;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.example.trackutem.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileFragment extends Fragment {
    private TextInputEditText etName, etEmail, etPhone;
    private MaterialButton btnSettings, btnSave;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnSave = view.findViewById(R.id.btnSave);

        btnSettings.setOnClickListener(v -> navigateToSettings());

        setupTextWatchers();
        return view;
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isValid = !TextUtils.isEmpty(etName.getText())
                        && !TextUtils.isEmpty(etEmail.getText())
                        && isValidPhone(etPhone.getText());
                btnSave.setEnabled(true);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etName.addTextChangedListener(textWatcher);
        etEmail.addTextChangedListener(textWatcher);
        etPhone.addTextChangedListener(textWatcher);
    }
    private boolean isValidPhone(CharSequence phone) {
        return phone != null && phone.length() >= 10;
    }
    private void navigateToSettings() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, new SettingsFragment())
                .addToBackStack(null)  // Optional: Add to back stack
                .commit();
    }
}
