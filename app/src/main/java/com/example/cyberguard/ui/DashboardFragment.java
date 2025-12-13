package com.example.cyberguard.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cyberguard.R;
import com.example.cyberguard.databinding.FragmentDashboardBinding;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    private static class Feature {
        final String title;
        final String description;
        final int iconRes;
        final String featureId;

        Feature(String title, String description, int iconRes, String featureId) {
            this.title = title;
            this.description = description;
            this.iconRes = iconRes;
            this.featureId = featureId;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDashboardBinding.inflate(inflater, container, false);

        List<Feature> features = buildFeatures();
        createFeatureCards(features);

        return binding.getRoot();
    }

    private List<Feature> buildFeatures() {
        List<Feature> featureList = new ArrayList<>();
        featureList.add(new Feature("Breach Check", "Check if your accounts were breached", R.drawable.ic_shield, "breach_check"));
        featureList.add(new Feature("Secure Notes", "Keep your notes safe", R.drawable.ic_lock, "secure_notes"));
        featureList.add(new Feature("Cyber News", "Latest cybersecurity news", R.drawable.ic_newspaper, "cyber_news"));
        featureList.add(new Feature("Phishing Training", "Learn to avoid phishing attacks", R.drawable.ic_email, "phishing_training"));
        featureList.add(new Feature("Network Security", "Monitor your network", R.drawable.ic_wifi, "network_security"));
        featureList.add(new Feature("Cyber Quiz", "Test your cybersecurity knowledge", R.drawable.ic_quiz, "cyber_quiz"));
        featureList.add(new Feature("Security Checklist", "Step-by-step security guide", R.drawable.ic_checklist, "security_checklist"));
        featureList.add(new Feature("Password Generator", "Generate strong passwords", R.drawable.ic_key, "password_generator"));
        return featureList;
    }

    private void createFeatureCards(List<Feature> features) {
        GridLayout grid = binding.dashboardGridFeatures;
        grid.removeAllViews();

        int columnCount = 2;
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (int i = 0; i < features.size(); i++) {
            Feature feature = features.get(i);

            View cardView = inflater.inflate(R.layout.item_feature_card, grid, false);

            ImageView icon = cardView.findViewById(R.id.feature_icon);
            TextView title = cardView.findViewById(R.id.feature_title);
            TextView subtitle = cardView.findViewById(R.id.feature_subtitle);

            icon.setImageResource(feature.iconRes);
            title.setText(feature.title);
            subtitle.setText(feature.description);

            cardView.setOnClickListener(v -> onFeatureClick(feature.featureId));

            GridLayout.Spec rowSpec = GridLayout.spec(i / columnCount, 1f);
            GridLayout.Spec colSpec = GridLayout.spec(i % columnCount, 1f);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(10, 10, 10, 10);

            cardView.setLayoutParams(params);
            grid.addView(cardView);
        }
    }

    private void onFeatureClick(String featureId) {
        switch (featureId) {
            case "breach_check":
                NavHostFragment.findNavController(this)
                        .navigate(R.id.breachCheckFragment);
                break;

            case "secure_notes":
                NavHostFragment.findNavController(this)
                        .navigate(R.id.unlockSecureNotesFragment);
                break;

            case "cyber_news":
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://thehackernews.com"));
                startActivity(intent);
                break;

            case "phishing_training":
                NavHostFragment.findNavController(this)
                        .navigate(R.id.phishingTrainingFragment);
                break;

            case "network_security":
                NavHostFragment.findNavController(this).navigate(R.id.networkSecurityFragment);
                break;


            default:
                Toast.makeText(getContext(), "Coming soon: " + featureId, Toast.LENGTH_SHORT).show();
                break;
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
