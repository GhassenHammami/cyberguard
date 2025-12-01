package com.example.cyberguard.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cyberguard.R;

import java.util.ArrayList;
import java.util.List;

public class DashboardViewModel extends ViewModel {

    public static class Feature {
        public String title;
        public String description;
        public int iconRes;
        public String featureId;

        public Feature(String title, String description, int iconRes, String featureId) {
            this.title = title;
            this.description = description;
            this.iconRes = iconRes;
            this.featureId = featureId;
        }
    }

    private final MutableLiveData<List<Feature>> _features = new MutableLiveData<>();
    public LiveData<List<Feature>> features = _features;

    public DashboardViewModel() {
        List<Feature> featureList = new ArrayList<>();
        featureList.add(new Feature("Breach Check", "Check if your accounts were breached", R.drawable.ic_shield, "breach_check"));
        featureList.add(new Feature("Secure Notes", "Keep your notes safe", R.drawable.ic_lock, "secure_notes"));
        featureList.add(new Feature("Cyber News", "Latest cybersecurity news", R.drawable.ic_newspaper, "cyber_news"));
        featureList.add(new Feature("Phishing Training", "Learn to avoid phishing attacks", R.drawable.ic_email, "phishing_training"));
        featureList.add(new Feature("Network Security", "Monitor your network", R.drawable.ic_wifi, "network_security"));
        featureList.add(new Feature("Cyber Quiz", "Test your cybersecurity knowledge", R.drawable.ic_quiz, "cyber_quiz"));
        featureList.add(new Feature("Security Checklist", "Step-by-step security guide", R.drawable.ic_checklist, "security_checklist"));
        featureList.add(new Feature("Password Generator", "Generate strong passwords", R.drawable.ic_key, "password_generator"));

        _features.setValue(featureList);
    }
}
