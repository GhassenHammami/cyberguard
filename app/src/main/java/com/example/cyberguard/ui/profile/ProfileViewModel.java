package com.example.cyberguard.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileViewModel extends ViewModel {

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<String> _fullName = new MutableLiveData<>();
    public LiveData<String> fullName = _fullName;

    private final MutableLiveData<String> _email = new MutableLiveData<>();
    public LiveData<String> email = _email;

    private final MutableLiveData<String> _phone = new MutableLiveData<>();
    public LiveData<String> phone = _phone;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _message = new MutableLiveData<>();
    public LiveData<String> message = _message;

    public void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            _email.setValue(currentUser.getEmail());

            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            _fullName.setValue(task.getResult().getString("fullName"));
                            _phone.setValue(task.getResult().getString("phone"));
                        }
                    });
        }
    }

    public void saveProfile(String fullNameInput, String phoneInput) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            _message.setValue("User not logged in");
            return;
        }

        if (fullNameInput.trim().isEmpty()) {
            _message.setValue("Full name is required");
            return;
        }

        _isLoading.setValue(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullNameInput.trim());
        if (!phoneInput.trim().isEmpty()) {
            updates.put("phone", phoneInput.trim());
        }

        db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnCompleteListener(task -> {
                    _isLoading.setValue(false);
                    if (task.isSuccessful()) {
                        _message.setValue("Profile updated successfully!");
                        loadUserData();
                    } else {
                        _message.setValue("Failed to update profile");
                    }
                });
    }
}
