package com.example.trackutem.view.Student;

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
import com.example.trackutem.controller.RegisterStuController;
import com.example.trackutem.view.LoginActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Objects;

public class RegisterStuActivity extends AppCompatActivity {
    private TextInputLayout ilRegisterEmail, ilRegisterPassword, ilRegisterConPassword;
    private TextInputEditText etRegisterName, etRegisterEmail, etRegisterPassword, etRegisterConPassword;
    private LinearLayout llPasswordConditions;
    private TextView tvPasswordLength, tvPasswordUppercase, tvPasswordLowercase, tvPasswordNumber, tvPasswordSpecialChar;
    private Button btnRegister;
    private final Handler handler = new Handler();
    private boolean isEmailValid = false;
    private boolean isEmailAvailable = false;
    private boolean isPasswordValid = false;
    private boolean isConPasswordValid = false;
    private RegisterStuController registerStuController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_stu);

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

        btnRegister = findViewById(R.id.btnRegister);
        registerStuController = new RegisterStuController(this);

        etRegisterEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                // Validate email format as type
                String email = Objects.requireNonNull(etRegisterEmail.getText()).toString().trim().toLowerCase();
                isEmailValid = registerStuController.isValidEmail(email);
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
                // Validate password format as type
                llPasswordConditions.setVisibility(View.VISIBLE);
                String password = Objects.requireNonNull(etRegisterPassword.getText()).toString().trim();
                isPasswordValid = registerStuController.isValidPassword(password);
                if(!isPasswordValid) {
                    showError(ilRegisterPassword, "Password must contain at least 8 characters with uppercase, lowercase, number, and special character");
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
                Toast.makeText(RegisterStuActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEmailValid && isEmailAvailable && isPasswordValid && isConPasswordValid) {
                registerStuController.registerStudent(name, email, password, new RegisterStuController.RegistrationCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(RegisterStuActivity.this, "Verification email sent. Please check your inbox", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterStuActivity.this, LoginActivity.class));
                                finish();
                            }
                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(RegisterStuActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        }

                );
            } else {
                Toast.makeText(RegisterStuActivity.this, "Please ensure all fields are valid", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkEmailAvailability(String email) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> registerStuController.checkEmailAvailability(email, isAvailable -> {
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