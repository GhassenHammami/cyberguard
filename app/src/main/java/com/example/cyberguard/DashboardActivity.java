package com.example.cyberguard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends BaseActivity {

    private ImageView ivMenu;
    private GridLayout gridFeatures;
    private TextView tvVersion;

    private static class Feature {
        String title;
        String description;
        int iconRes;
        String featureId;

        Feature(String title, String description, int iconRes, String featureId) {
            this.title = title;
            this.description = description;
            this.iconRes = iconRes;
            this.featureId = featureId;
        }
    }

    private final List<Feature> features = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeViews();
        setupFeatures();
        setupDrawer(R.id.nav_dashboard);
        setupHamburgerMenu(R.id.dashboard_ivMenu);
        setupVersion();
    }

    private void initializeViews() {
        ivMenu = findViewById(R.id.dashboard_ivMenu);
        gridFeatures = findViewById(R.id.dashboard_gridFeatures);
        tvVersion = findViewById(R.id.dashboard_tvVersion);
    }

    private void setupFeatures() {
        features.add(new Feature(
                getString(R.string.feature_breach_check),
                getString(R.string.feature_breach_check_desc),
                R.drawable.ic_shield,
                "breach_check"
        ));
        features.add(new Feature(
                getString(R.string.feature_secure_notes),
                getString(R.string.feature_secure_notes_desc),
                R.drawable.ic_lock,
                "secure_notes"
        ));
        features.add(new Feature(
                getString(R.string.feature_cyber_news),
                getString(R.string.feature_cyber_news_desc),
                R.drawable.ic_newspaper,
                "cyber_news"
        ));
        features.add(new Feature(
                getString(R.string.feature_phishing_training),
                getString(R.string.feature_phishing_training_desc),
                R.drawable.ic_email,
                "phishing_training"
        ));
        features.add(new Feature(
                getString(R.string.feature_network_security),
                getString(R.string.feature_network_security_desc),
                R.drawable.ic_wifi,
                "network_security"
        ));
        features.add(new Feature(
                getString(R.string.feature_cyber_quiz),
                getString(R.string.feature_cyber_quiz_desc),
                R.drawable.ic_quiz,
                "cyber_quiz"
        ));
        features.add(new Feature(
                getString(R.string.feature_security_checklist),
                getString(R.string.feature_security_checklist_desc),
                R.drawable.ic_checklist,
                "security_checklist"
        ));
        features.add(new Feature(
                getString(R.string.feature_password_generator),
                getString(R.string.feature_password_generator_desc),
                R.drawable.ic_key,
                "password_generator"
        ));

        createFeatureCards();
    }

    private void createFeatureCards() {
        LayoutInflater inflater = LayoutInflater.from(this);
        int columnCount = 2;
        int totalFeatures = features.size();

        for (int i = 0; i < totalFeatures; i++) {
            Feature feature = features.get(i);
            View cardView = inflater.inflate(R.layout.item_feature_card, gridFeatures, false);

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
            params.setMargins(6, 6, 6, 6);

            cardView.setLayoutParams(params);
            gridFeatures.addView(cardView);
        }
    }

    private void onFeatureClick(String featureId) {
        Toast.makeText(this, "Opening " + featureId + "...", Toast.LENGTH_SHORT).show();
        // TODO: Navigate to respective activity when implemented
        // switch (featureId) {
        //     case "breach_check":
        //         startActivity(new Intent(this, BreachCheckActivity.class));
        //         break;
        //     ...
        // }
    }

    private void setupVersion() {
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText(getString(R.string.app_version) + " " + versionName);
        } catch (Exception e) {
            tvVersion.setText(getString(R.string.app_version));
        }
    }
}
