package com.example.trackutem.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.example.trackutem.MainDrvActivity;
import com.example.trackutem.MainStuActivity;
import com.example.trackutem.R;
import com.example.trackutem.controller.LoginController;
import com.example.trackutem.model.Driver;
import com.example.trackutem.model.Student;
import com.example.trackutem.view.Student.RegisterStuActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout ilLoginEmail;
    private EditText etLoginEmail, etLoginPassword;
    private CheckBox cbRememberMe;
    private Button btnLogin;
    private ProgressBar progressBar;
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
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView btnRegisterUser = findViewById(R.id.btnRegisterUser);
        progressBar = findViewById(R.id.progressBar);

        checkIfDriverWasDisabled();
        prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        loginController = new LoginController();

        if (prefs.getBoolean("remember_me", false)) {
            String savedEmail = prefs.getString("email", "").toLowerCase();
            String userType = prefs.getString("user_type", "");

            if (!savedEmail.isEmpty() && !userType.isEmpty()) {
                if (savedEmail.endsWith("@student.utem.edu.my") && "student".equals(userType)) {
                    startActivity(new Intent(this, MainStuActivity.class));
                    finish();
                } else if (savedEmail.endsWith("@utem.edu.my") && "driver".equals(userType)) {
                    startActivity(new Intent(this, MainDrvActivity.class));
                    finish();
                } else {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("remember_me", false);
                    editor.remove("email");
                    editor.remove("user_type");
                    editor.apply();
                }
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
                Drawable icon = ContextCompat.getDrawable(LoginActivity.this, android.R.drawable.ic_dialog_alert);
                if (icon != null) {
                    Drawable tintedIcon = DrawableCompat.wrap(icon.mutate());
                    DrawableCompat.setTint(tintedIcon, Color.parseColor("#F59E0B"));
                    new MaterialAlertDialogBuilder(LoginActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                            .setTitle("Invalid Email")
                            .setMessage("Please use a valid UTeM email:\n\n• studentID@student.utem.edu.my\n• staff@utem.edu.my")
                            .setIcon(tintedIcon)
                            .setPositiveButton("OK", null)
                            .setCancelable(false)
                            .show();
                }
                    return;
            }
            showLoading(true);
            if (email.endsWith("@utem.edu.my")) {
                loginController.checkDriverStatusBeforeLogin(email, password, new LoginController.DriverLoginCallback() {
                    @Override
                    public void onDriverLoginSuccess(Driver driver) {
                        showLoading(false);
                        handleSuccessfulLogin(driver.getEmail(), driver, null);
                    }
                    @Override
                    public void onDriverLoginFailure(String errorMessage) {
                        showLoading(false);

                        if (errorMessage.contains("disabled")) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("remember_me", false);
                            editor.remove("email");
                            editor.remove("user_type");
                            editor.remove("driverId");
                            editor.apply();

                            new MaterialAlertDialogBuilder(LoginActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                                    .setTitle("Account Disabled")
                                    .setMessage(errorMessage)
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else {
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }                    }
                });
            } else {
                loginController.loginStudent(email, password, new LoginController.StudentLoginCallback() {
                    @Override
                    public void onStudentLoginSuccess(Student student) {
                        showLoading(false);
                        handleSuccessfulLogin(student.getEmail(), null, student);
                    }
                    @Override
                    public void onStudentLoginFailure(String errorMessage) {
                        showLoading(false);
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

            new MaterialAlertDialogBuilder(LoginActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                    .setTitle("Reset Password")
                    .setMessage("A password reset link will be sent to " + email + ". Continue?")
                    .setPositiveButton("Send Link", (dialog, which) -> {
                        loginController.resetPassword(email, new LoginController.ResetPasswordCallback() {
                            @Override
                            public void onResetPasswordSuccess() {
                                new MaterialAlertDialogBuilder(LoginActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                                        .setTitle("Success")
                                        .setMessage("Password reset email sent to " + email)
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                            @Override
                            public void onResetPasswordFailure(String errorMessage) {
                                new MaterialAlertDialogBuilder(LoginActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                                        .setTitle("Error")
                                        .setMessage("Failed to send reset email: " + errorMessage)
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnRegisterUser.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterStuActivity.class);
            startActivity(intent);
        });
    }

    private void checkIfDriverWasDisabled() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        boolean wasDriverDisabled = prefs.getBoolean("driver_disabled", false);

        if (wasDriverDisabled) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("driver_disabled", false);
            editor.apply();
            new MaterialAlertDialogBuilder(LoginActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                    .setTitle("Account Disabled")
                    .setMessage("Your account has been disabled by administrator. Please contact support.")
                    .setPositiveButton("OK", null)
                    .setCancelable(false)
                    .show();
        }
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(android.view.View.VISIBLE);
            btnLogin.setEnabled(false);
            btnLogin.setAlpha(0.5f);
        } else {
            progressBar.setVisibility(android.view.View.GONE);
            btnLogin.setEnabled(true);
            btnLogin.setAlpha(1.0f);
        }
    }

    private boolean isLoginEmailValid(String email) {
        return email.endsWith("@student.utem.edu.my") || email.endsWith("@utem.edu.my");
    }

    private void handleSuccessfulLogin(String email, @Nullable Driver driver, @Nullable Student student) {
        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

        SharedPreferences.Editor editor = prefs.edit();
        if (cbRememberMe.isChecked()) {
            editor.putBoolean("remember_me", true);
            editor.putString("email", email);
        } else {
            editor.putBoolean("remember_me", false);
            editor.remove("email");
            editor.remove("user_type");
        }
        if (driver != null) {
            editor.putString("user_type", "driver");
            editor.putString("driverId", driver.getDriverId());
        } else if (student != null) {
            editor.putString("user_type", "student");
            editor.putString("studentId", student.getStudentId());
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