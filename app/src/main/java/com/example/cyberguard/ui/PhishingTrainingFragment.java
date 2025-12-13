package com.example.cyberguard.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cyberguard.R;

public class PhishingTrainingFragment extends Fragment {

    private TextView headerRedFlags, headerExamples, headerActions;
    private LinearLayout bodyRedFlags, bodyExamples, bodyActions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_phishing_training, container, false);

        headerRedFlags = root.findViewById(R.id.pt_header_redflags);
        headerExamples = root.findViewById(R.id.pt_header_examples);
        headerActions = root.findViewById(R.id.pt_header_actions);

        bodyRedFlags = root.findViewById(R.id.pt_body_redflags);
        bodyExamples = root.findViewById(R.id.pt_body_examples);
        bodyActions = root.findViewById(R.id.pt_body_actions);

        headerRedFlags.setOnClickListener(v -> toggle(bodyRedFlags, headerRedFlags, "🚩 Red Flags"));
        headerExamples.setOnClickListener(v -> toggle(bodyExamples, headerExamples, "🧪 Common Phishing Examples"));
        headerActions.setOnClickListener(v -> toggle(bodyActions, headerActions, "✅ What To Do If You Receive One"));

        // Optional: open the first section by default
        toggle(bodyRedFlags, headerRedFlags, "🚩 Red Flags");

        return root;
    }

    private void toggle(LinearLayout body, TextView header, String title) {
        boolean open = body.getVisibility() == View.VISIBLE;
        body.setVisibility(open ? View.GONE : View.VISIBLE);
        header.setText(title + (open ? " (tap to expand)" : " (tap to collapse)"));
    }
}
