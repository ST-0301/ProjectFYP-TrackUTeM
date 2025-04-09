package com.example.trackutem.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.trackutem.R;
import com.example.trackutem.controller.RegisterStudController;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Objects;

public class RegisterStudActivity extends AppCompatActivity {
    private TextInputLayout ilRegisterEmail, ilRegisterPassword, ilRegisterConPassword;
    private TextInputEditText etRegisterName, etRegisterEmail, etRegisterPassword, etRegisterConPassword;
    private LinearLayout llPasswordConditions;
    private TextView tvPasswordLength, tvPasswordUppercase, tvPasswordLowercase, tvPasswordNumber, tvPasswordSpecialChar;
    private Button btnRegister;
    private RegisterStudController registerStudController;
    private final Handler handler = new Handler();
    private boolean isEmailValid = false;
    private boolean isEmailAvailable = false;
    private boolean isPasswordValid = false;
    private boolean isConPasswordValid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registerstud);

        ilRegisterEmail = findViewById(R.id.ilRegisterEmail);
        ilRegisterPassword = findViewById(R.id.ilRegisterPassword);
        ilRegisterConPassword = findViewById(R.id.ilRegisterConPassword);

        etRegisterName = findViewById(R.id.etRegisterName);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConPassword = findViewById(R.id.etRegisterConPassword);

        llPasswordConditions = findViewById(R.id.llPasswordConditions);
        tvPasswordLength = findViewById(R.id.tvPasswordLength);
        tvPasswordUppercase = findViewById(R.id.tvPasswordUppercase);
        tvPasswordLowercase = findViewById(R.id.tvPasswordLowercase);
        tvPasswordNumber = findViewById(R.id.tvPasswordNumber);
        tvPasswordSpecialChar = findViewById(R.id.tvPasswordSpecialChar);

        Button btnMsftRegister = findViewById(R.id.btnMsftRegister);
        btnRegister = findViewById(R.id.btnRegister);
        registerStudController = new RegisterStudController(this);

        etRegisterEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                // Validate email format as user types
                String email = Objects.requireNonNull(etRegisterEmail.getText()).toString().trim().toLowerCase();
                isEmailValid = registerStudController.isValidEmail(email);
                if(!isEmailValid) {
                    showError(ilRegisterEmail, "Enter a valid email (e.g., matricNumber@student.utem.edu.my)");
                    isEmailAvailable = false;
                } else {
                    hideError(ilRegisterEmail);
                    checkEmailAvailability(email);
                }
                updateBtnRegister();
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
        llPasswordConditions.setVisibility(View.GONE);
        etRegisterPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                // Validate password format as user types
                llPasswordConditions.setVisibility(View.VISIBLE);
                String password = Objects.requireNonNull(etRegisterPassword.getText()).toString().trim();
                isPasswordValid = registerStudController.isValidPassword(password);
                if(!isPasswordValid) {
                    showError(ilRegisterPassword, "Password must be at least 8 characters, with letters, numbers, and symbols");
                } else {
                    hideError(ilRegisterPassword);
                }
                checkPasswordConditions(password);
                updateBtnRegister();
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
        etRegisterConPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String password = Objects.requireNonNull(etRegisterPassword.getText()).toString().trim();
                String conPassword = Objects.requireNonNull(etRegisterConPassword.getText()).toString().trim();
                isConPasswordValid = password.equals(conPassword);
                if (!isConPasswordValid) {
                    showError(ilRegisterConPassword, "Password do not match");
                } else {
                    hideError(ilRegisterConPassword);
                }
                updateBtnRegister();
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        btnRegister.setOnClickListener(v -> {
            String name = Objects.requireNonNull(etRegisterName.getText()).toString().trim();
            String email = Objects.requireNonNull(etRegisterEmail.getText()).toString().trim().toLowerCase();
            String password = Objects.requireNonNull(etRegisterPassword.getText()).toString().trim();
            String conPassword = Objects.requireNonNull(etRegisterConPassword.getText()).toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || conPassword.isEmpty()) {
                Toast.makeText(RegisterStudActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEmailValid && isEmailAvailable && isPasswordValid && isConPasswordValid) {
                registerStudController.registerUser(name, email, password, new RegisterStudController.RegistrationCallback() {
                            @Override
                            public void onSuccess() {
                                Intent intent = new Intent(RegisterStudActivity.this, StudentMainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(RegisterStudActivity.this,"Registration failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            } else {
                Toast.makeText(RegisterStudActivity.this, "Please ensure all fields are valid", Toast.LENGTH_SHORT).show();
            }
        });

        btnMsftRegister.setOnClickListener(v -> {

        });
    }

    private void checkEmailAvailability(String email) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> registerStudController.checkEmailAvailability(email, isAvailable -> {
            isEmailAvailable = isAvailable;
            if (!isAvailable) {
                showError(ilRegisterEmail, "Email is already registered.");
            } else {
                hideError(ilRegisterEmail);
            }
        }), 1000);
    }
    private void checkPasswordConditions(String password) {
        if(password.length() >= 8) {
            tvPasswordLength.setTextColor(ContextCompat.getColor(this, R.color.secondaryGreen));
        } else {
            tvPasswordLength.setTextColor(ContextCompat.getColor(this, R.color.colorError));
        }
        if(password.matches(".*[A-Z].*")) {
            tvPasswordUppercase.setTextColor(ContextCompat.getColor(this, R.color.secondaryGreen));
        } else {
            tvPasswordUppercase.setTextColor(ContextCompat.getColor(this, R.color.colorError));
        }
        if (password.matches(".*[a-z].*")) {
            tvPasswordLowercase.setTextColor(ContextCompat.getColor(this, R.color.secondaryGreen));
        } else {
            tvPasswordLowercase.setTextColor(ContextCompat.getColor(this, R.color.colorError));
        }
        if (password.matches(".*\\d.*")) {
            tvPasswordNumber.setTextColor(ContextCompat.getColor(this, R.color.secondaryGreen));
        } else {
            tvPasswordNumber.setTextColor(ContextCompat.getColor(this, R.color.colorError));
        }
        if (password.matches(".*[@$!%*?&#^;:,./].*")) {
            tvPasswordSpecialChar.setTextColor(ContextCompat.getColor(this, R.color.secondaryGreen));
        } else {
            tvPasswordSpecialChar.setTextColor(ContextCompat.getColor(this, R.color.colorError));
        }
    }

    private void showError(TextInputLayout inputLayout, String errorMessage) {
        inputLayout.setErrorEnabled(true);
        inputLayout.setError(errorMessage);
        inputLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.colorError));
    }
    private void hideError(TextInputLayout inputLayout) {
        inputLayout.setErrorEnabled(false);
        inputLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.primaryBlue));
    }

    private void updateBtnRegister() {
        btnRegister.setEnabled(isEmailValid && isEmailAvailable && isPasswordValid && isConPasswordValid);
    }
}