package com.example.cyberguard.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cyberguard.R;
import com.example.cyberguard.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private boolean isEditMode = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupObservers();
        setupClickListeners();
        viewModel.loadUserData();

        setFieldsEditable(false);

        return binding.getRoot();
    }

    private void setupObservers() {
        viewModel.fullName.observe(getViewLifecycleOwner(), s -> binding.profileEtFullName.setText(s));
        viewModel.email.observe(getViewLifecycleOwner(), s -> binding.profileEtEmail.setText(s));
        viewModel.phone.observe(getViewLifecycleOwner(), s -> binding.profileEtPhone.setText(s));
        viewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            binding.profileProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.profileTvSave.setEnabled(!loading);
        });
        viewModel.message.observe(getViewLifecycleOwner(), msg -> {
            toggleEditMode();
            if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupClickListeners() {
        binding.profileTvEdit.setOnClickListener(v -> toggleEditMode());
        binding.profileTvSave.setOnClickListener(v -> viewModel.saveProfile(binding.profileEtFullName.getText().toString(), binding.profileEtPhone.getText().toString()));
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        setFieldsEditable(isEditMode);

        if (isEditMode) {
            binding.profileTvEdit.setText(R.string.profile_cancel);
            binding.profileTvSave.setVisibility(View.VISIBLE);
        } else {
            binding.profileTvEdit.setText(R.string.profile_edit);
            binding.profileTvSave.setVisibility(View.GONE);
            viewModel.loadUserData();
        }
    }

    private void setFieldsEditable(boolean editable) {
        binding.profileEtFullName.setEnabled(editable);
        binding.profileEtPhone.setEnabled(editable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
