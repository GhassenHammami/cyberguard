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
import com.example.cyberguard.util.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;


public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    private static class Feature {
        final String title;
        final String description;
        final int iconRes;
        final String featureId;
        final boolean requiresInternet;

        Feature(String title, String description, int iconRes, String featureId, boolean requiresInternet) {
            this.title = title;
            this.description = description;
            this.iconRes = iconRes;
            this.featureId = featureId;
            this.requiresInternet = requiresInternet;
        }
    }

    private final java.util.Map<String, Feature> featureMap = new java.util.HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDashboardBinding.inflate(inflater, container, false);

        List<Feature> features = buildFeatures();

        featureMap.clear();
        for (Feature f : features) {
            featureMap.put(f.featureId, f);
        }

        createFeatureCards(features);

        return binding.getRoot();
    }

    private List<Feature> buildFeatures() {
        List<Feature> featureList = new ArrayList<>();
        featureList.add(new Feature("Breach Check", "Check if your accounts were breached", R.drawable.ic_shield, "breach_check", true));
        featureList.add(new Feature("Secure Notes", "Keep your notes safe", R.drawable.ic_lock, "secure_notes", true));
        featureList.add(new Feature("Cyber News", "Latest cybersecurity news", R.drawable.ic_newspaper, "cyber_news", true));
        featureList.add(new Feature("Phishing Training", "Learn to avoid phishing attacks", R.drawable.ic_email, "phishing_training", false));
        featureList.add(new Feature("Network Security", "Monitor your network", R.drawable.ic_wifi, "network_security", true));
        featureList.add(new Feature("Cyber Quiz", "Test your cybersecurity knowledge", R.drawable.ic_quiz, "cyber_quiz", false));
        featureList.add(new Feature("Password Generator", "Generate strong passwords", R.drawable.ic_key, "password_generator", false));
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

            GridLayout.Spec colSpec;
            if ("password_generator".equals(feature.featureId)) {
                // Span across both columns (full row)
                colSpec = GridLayout.spec(0, columnCount, 1f);
            } else {
                colSpec = GridLayout.spec(i % columnCount, 1f);
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(10, 10, 10, 10);

            cardView.setLayoutParams(params);
            grid.addView(cardView);
        }
    }

    private void onFeatureClick(String featureId) {
        Feature f = featureMap.get(featureId);
        if (f != null && f.requiresInternet && !NetworkUtils.hasInternetConnection(requireContext())) {
            showNoInternetDialog();
            return;
        }
        switch (featureId) {
            case "breach_check":
                NavHostFragment.findNavController(this).navigate(R.id.breachCheckFragment);
                break;

            case "secure_notes":
                NavHostFragment.findNavController(this).navigate(R.id.unlockSecureNotesFragment);
                break;

            case "cyber_news":
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://thehackernews.com"));
                startActivity(intent);
                break;

            case "phishing_training":
                NavHostFragment.findNavController(this).navigate(R.id.phishingTrainingFragment);
                break;

            case "network_security":
                NavHostFragment.findNavController(this).navigate(R.id.networkSecurityFragment);
                break;

            case "cyber_quiz":
                NavHostFragment.findNavController(this).navigate(R.id.cyberQuizFragment);
                break;

            case "password_generator":
                NavHostFragment.findNavController(this).navigate(R.id.passwordGeneratorFragment);
                break;


            default:
                Toast.makeText(getContext(), "Coming soon: " + featureId, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void showNoInternetDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext()).setTitle("No Internet Connection").setMessage("Please check your Wi-Fi or mobile data and try again.").setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).show();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
