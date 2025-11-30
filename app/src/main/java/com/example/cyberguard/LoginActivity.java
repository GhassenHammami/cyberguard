package com.example.cyberguard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar btnLoader;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextView tvGoToRegister = findViewById(R.id.login_tvGoToRegister);
        tvGoToRegister.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        ImageView btnReturn = findViewById(R.id.login_iv_back);

        btnReturn.setOnClickListener(view -> finish());

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.login_etEmail);
        etPassword = findViewById(R.id.login_etPassword);
        btnLogin = findViewById(R.id.login_btnLogin);
        btnLoader = findViewById(R.id.login_btnLoader);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Please enter your email");
                etEmail.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Please enter your password");
                etPassword.requestFocus();
                return;
            }

            startLoading();
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    finish();
                } else {
                    String message = "Login failed. Please try again.";
                    Exception e = task.getException();

                    if (e instanceof com.google.firebase.auth.FirebaseAuthException) {
                        String code = ((com.google.firebase.auth.FirebaseAuthException) e).getErrorCode();
                        Log.d("FirebaseAuthException", "Error code: " + code);
                        switch (code) {
                            case "USER_DISABLED":
                                message = "This account has been disabled";
                                break;
                            case "ERROR_INVALID_CREDENTIAL":
                                message = "Incorrect email or password";
                                break;
                        }
                    }
                    showErrorDialog(message);
                }
            });
            stopLoading();
        });

        setupVersion();
    }

    private void setupVersion() {
        TextView tvVersion = findViewById(R.id.login_tvVersion);
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText(getString(R.string.app_version) + " " + versionName);
        } catch (Exception e) {
            tvVersion.setText(getString(R.string.app_version));
        }
    }

    private void showErrorDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Login Failed").setMessage(message).setCancelable(true).setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).show();
    }

    private void startLoading() {
        btnLogin.setEnabled(false);
        btnLogin.setText("");
        btnLoader.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
        btnLoader.setVisibility(View.GONE);
    }

}