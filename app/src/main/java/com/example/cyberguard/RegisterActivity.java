package com.example.cyberguard;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private Button btnRegister;
    private EditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private ProgressBar btnLoader;
    private CountryCodePicker ccp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ccp = findViewById(R.id.register_ccp);
        ccp.setDefaultCountryUsingNameCode("TN");
        ccp.resetToDefaultCountry();

        TextView tvGoToLogin = findViewById(R.id.register_tvGoToLogin);
        tvGoToLogin.setOnClickListener(view -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        etFullName = findViewById(R.id.register_etFullName);
        etEmail = findViewById(R.id.register_etEmail);
        etPhone = findViewById(R.id.register_etPhone);
        etPassword = findViewById(R.id.register_etPassword);
        etConfirmPassword = findViewById(R.id.register_etConfirmPassword);
        ImageView btnReturn = findViewById(R.id.register_iv_back);

        btnRegister = findViewById(R.id.register_btnRegister);
        btnLoader = findViewById(R.id.register_btnLoader);

        ccp.registerCarrierNumberEditText(etPhone);

        btnReturn.setOnClickListener(view -> finish());

        btnRegister.setOnClickListener(view -> registerUser());

        setupVersion();
    }

    private void setupVersion() {
        TextView tvVersion = findViewById(R.id.register_tvVersion);
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText(getString(R.string.app_version) + " " + versionName);
        } catch (Exception e) {
            tvVersion.setText(getString(R.string.app_version));
        }
    }

    private void registerUser() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        String fullPhone = ccp.getFullNumberWithPlus();

        boolean hasError = false;

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Please enter your full name");
            hasError = true;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Please enter your email");
            hasError = true;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Please enter your phone number");
            hasError = true;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Please enter a password");
            hasError = true;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Please confirm your password");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        startLoading();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                assert mAuth.getCurrentUser() != null;
                String uid = mAuth.getCurrentUser().getUid();

                Map<String, Object> user = new HashMap<>();
                user.put("fullName", fullName);
                user.put("email", email);
                user.put("phone", fullPhone);

                db.collection("users").document(uid).set(user).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        showErrorDialog("Database Error", "Could not save your information.\n\n" + Objects.requireNonNull(task1.getException()).getMessage());
                    }
                });
            } else {
                Exception e = task.getException();
                assert e != null;
                showErrorDialog("Registration Failed", e.getMessage());
                if (e instanceof com.google.firebase.auth.FirebaseAuthException) {
                    String errorCode = ((com.google.firebase.auth.FirebaseAuthException) e).getErrorCode();
                    handleAuthError(errorCode);
                }
            }
            stopLoading();
        });
    }


    private void showErrorDialog(String title, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(title).setMessage(message).setCancelable(true).setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).show();
    }

    private void startLoading() {
        btnRegister.setEnabled(false);
        btnRegister.setText("");
        btnLoader.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        btnRegister.setEnabled(true);
        btnRegister.setText("Register");
        btnLoader.setVisibility(View.GONE);
    }

    private void handleAuthError(String errorCode) {
        switch (errorCode) {
            case "ERROR_EMAIL_ALREADY_IN_USE":
                etEmail.setError("This email is already registered");
                etEmail.requestFocus();
                break;

            case "ERROR_INVALID_EMAIL":
                etEmail.setError("Invalid email format");
                etEmail.requestFocus();
                break;

            case "ERROR_WEAK_PASSWORD":
                etPassword.setError("Your password is too weak");
                etPassword.requestFocus();
                break;

            case "ERROR_MISSING_PASSWORD":
                etPassword.setError("Please enter a password");
                etPassword.requestFocus();
                break;

            case "ERROR_INVALID_PHONE_NUMBER":
                etPhone.setError("Invalid phone number");
                etPhone.requestFocus();
                break;

            default:
                showErrorDialog("Registration Failed", "Error: " + errorCode);
                break;
        }
    }

}