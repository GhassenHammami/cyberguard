package com.example.cyberguard;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private ImageView ivMenu;
    private EditText etFullName, etEmail, etPhone;
    private TextView tvSave;
    private ProgressBar progressBar;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeViews();
        setupDrawer(R.id.nav_profile);
        setupHamburgerMenu(R.id.profile_ivMenu);
        loadUserData();
        setupVersion();
    }

    private void setupVersion() {
        TextView tvVersion = findViewById(R.id.profile_tvVersion);
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText(getString(R.string.app_version) + " " + versionName);
        } catch (Exception e) {
            tvVersion.setText(getString(R.string.app_version));
        }
    }

    private void initializeViews() {
        ivMenu = findViewById(R.id.profile_ivMenu);
        etFullName = findViewById(R.id.profile_etFullName);
        etEmail = findViewById(R.id.profile_etEmail);
        etPhone = findViewById(R.id.profile_etPhone);
        tvSave = findViewById(R.id.profile_tvSave);
        progressBar = findViewById(R.id.profile_progressBar);

        // Make fields non-editable initially
        setFieldsEditable(false);

        // Edit button
        TextView tvEdit = findViewById(R.id.profile_tvEdit);
        tvEdit.setOnClickListener(v -> toggleEditMode());

        // Save button
        tvSave.setOnClickListener(v -> saveProfile());
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            etEmail.setText(currentUser.getEmail());
            etEmail.setEnabled(false); // Email cannot be changed

            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            com.google.firebase.firestore.DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String fullName = document.getString("fullName");
                                String phone = document.getString("phone");

                                if (fullName != null) {
                                    etFullName.setText(fullName);
                                }
                                if (phone != null) {
                                    etPhone.setText(phone);
                                }
                            }
                        }
                    });
        }
    }

    private void setFieldsEditable(boolean editable) {
        etFullName.setEnabled(editable);
        etPhone.setEnabled(editable);
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        setFieldsEditable(isEditMode);
        TextView tvEdit = findViewById(R.id.profile_tvEdit);
        if (isEditMode) {
            tvEdit.setText("Cancel");
            tvSave.setVisibility(View.VISIBLE);
        } else {
            tvEdit.setText("Edit");
            tvSave.setVisibility(View.GONE);
            loadUserData(); // Reload original data
        }
    }

    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvSave.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        if (!phone.isEmpty()) {
            updates.put("phone", phone);
        }

        db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    tvSave.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        toggleEditMode();
                        loadUserInfoInDrawer(); // Update drawer info
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
