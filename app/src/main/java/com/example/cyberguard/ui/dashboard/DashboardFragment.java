package com.example.cyberguard.ui.dashboard;

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
import androidx.lifecycle.ViewModelProvider;

import com.example.cyberguard.R;
import com.example.cyberguard.databinding.FragmentDashboardBinding;

import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        observeFeatures();

        return binding.getRoot();
    }

    private void observeFeatures() {
        viewModel.features.observe(getViewLifecycleOwner(), this::createFeatureCards);
    }

    private void createFeatureCards(List<DashboardViewModel.Feature> features) {
        GridLayout grid = binding.dashboardGridFeatures;
        grid.removeAllViews();

        int columnCount = 2;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < features.size(); i++) {
            DashboardViewModel.Feature feature = features.get(i);

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
        Toast.makeText(getContext(), "Opening " + featureId + "...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
