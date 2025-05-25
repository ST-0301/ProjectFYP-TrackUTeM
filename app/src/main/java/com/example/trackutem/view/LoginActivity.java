package com.example.trackutem.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.trackutem.MainDrvActivity;
import com.example.trackutem.MainStuActivity;
import com.example.trackutem.R;
import com.example.trackutem.controller.LoginController;
import com.example.trackutem.model.Driver;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout ilLoginEmail;
    private EditText etLoginEmail, etLoginPassword;
    private CheckBox cbRememberMe;
    private SharedPreferences prefs;
    private LoginController loginController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ilLoginEmail = findViewById(R.id.ilLoginEmail);
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView btnRegisterUser = findViewById(R.id.btnRegisterUser);

        prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        loginController = new LoginController();

        // !!! LATER SET THIS TO true !!!
        if(prefs.getBoolean("remember_me", true)) {
            String savedEmail = prefs.getString("email", "").toLowerCase();
            if (savedEmail.endsWith("@student.utem.edu.my")) {
                startActivity(new Intent(this, MainStuActivity.class));
                finish();
            } else if (savedEmail.endsWith("@utem.edu.my")) {
                startActivity(new Intent(this, MainDrvActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Invalid email domain", Toast.LENGTH_SHORT).show();
                // Clear invalid saved credentials
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("remember_me", false);
                editor.apply();
            }
        }

        btnLogin.setOnClickListener(v -> {
            String email = etLoginEmail.getText().toString().trim().toLowerCase();
            String password = etLoginPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isLoginEmailValid(email)) {
                new AlertDialog.Builder(LoginActivity.this)
                        .setTitle("Invalid Email")
                        .setMessage("Please use a valid UTeM email:\n\n• studentID@student.utem.edu.my\n• staff@utem.edu.my")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("OK", null)
                        .setCancelable(false)
                        .show();
                return;
            }

            if (email.endsWith("@utem.edu.my")) {
                loginController.loginDriver(email, password, new LoginController.DriverLoginCallback() {
                    @Override
                    public void onDriverLoginSuccess(Driver driver) {
                        handleSuccessfulLogin(driver.getEmail(), driver);
                    }
                    @Override
                    public void onDriverLoginFailure(String errorMessage) {
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                loginController.loginUser(email, password, new LoginController.StudentLoginCallback() {
                    @Override
                    public void onStudentLoginSuccess(String email) {
                        // Email verification check
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            handleSuccessfulLogin(email, null);
                        } else {
                            Toast.makeText(LoginActivity.this, "Please verify your email first", Toast.LENGTH_SHORT).show();
                            FirebaseAuth.getInstance().signOut();
                        }
                    }
                    @Override
                    public void onStudentLoginFailure(String errorMessage) {
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }

        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = etLoginEmail.getText().toString().trim();
            if (email.isEmpty()) {
                ilLoginEmail.setError("Please enter your email");
                return;
            } else {
                ilLoginEmail.setError(null);
            }

            loginController.resetPassword(email, new LoginController.ResetPasswordCallback() {
                @Override
                public void onResetPasswordSuccess() {
                    Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onResetPasswordFailure(String errorMessage) {
                    Toast.makeText(LoginActivity.this, "Failed to send reset email: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnRegisterUser.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterStuActivity.class);
            startActivity(intent);
        });
    }

    private boolean isLoginEmailValid(String email) {
        return email.endsWith("@student.utem.edu.my") || email.endsWith("@utem.edu.my");
    }

    private void handleSuccessfulLogin(String email, @Nullable Driver driver) {
        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

        // Save login state if "Remember Me" is checked
        SharedPreferences.Editor editor = prefs.edit();
        if (cbRememberMe.isChecked()) {
            editor.putBoolean("remember_me", true);
            editor.putString("email", email);
        } else {
            editor.putBoolean("remember_me", false);
        }
        // Only add driverId if it's a driver login
        if (driver != null) {
            editor.putString("driverId", driver.getDriverId());
        }
        editor.apply();

        if (driver != null) {
            startActivity(new Intent(this, MainDrvActivity.class));
        } else {
            startActivity(new Intent(this, MainStuActivity.class));
        }
        finish();
    }
}