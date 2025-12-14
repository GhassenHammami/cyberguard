package com.example.cyberguard.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cyberguard.R;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class PasswordGeneratorFragment extends Fragment {

    private static final int MIN_LEN = 2;
    private static final int MAX_LEN = 32;
    private static final int DEFAULT_LEN = 16;

    private TextView tvPassword, tvLengthLabel, tvStrength, tvStrengthHint;
    private SeekBar seekLength;
    private CheckBox cbUpper, cbLower, cbNumbers, cbSymbols;
    private ProgressBar strengthBar;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMS = "0123456789";
    private static final String SYMS = "!@#$%^&*()-_=+[]{};:,.?/<>";
    private static final String DEFAULT_TEXT = "Tap Generate";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_password_generator, container, false);

        tvPassword = v.findViewById(R.id.tvPassword);
        tvLengthLabel = v.findViewById(R.id.tvLengthLabel);
        tvStrength = v.findViewById(R.id.tvStrength);
        strengthBar = v.findViewById(R.id.strengthBar);
        tvStrengthHint = v.findViewById(R.id.tvStrengthHint);

        seekLength = v.findViewById(R.id.seekLength);

        cbUpper = v.findViewById(R.id.cbUpper);
        cbLower = v.findViewById(R.id.cbLower);
        cbNumbers = v.findViewById(R.id.cbNumbers);
        cbSymbols = v.findViewById(R.id.cbSymbols);

        Button btnGenerate = v.findViewById(R.id.btnGenerate);
        Button btnCopy = v.findViewById(R.id.btnCopy);

        seekLength.setMax(MAX_LEN - MIN_LEN);

        seekLength.setProgress(DEFAULT_LEN - MIN_LEN);
        updateLengthLabel(DEFAULT_LEN);
        updateStrengthLabel(DEFAULT_LEN);

        seekLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int len = MIN_LEN + progress;
                updateLengthLabel(len);
                updateStrengthLabel(len);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        CompoundButton.OnCheckedChangeListener enforceAtLeastOne = (buttonView, isChecked) -> {
            if (!isChecked && getCheckedCount() == 0) {
                buttonView.setChecked(true);
                Toast.makeText(requireContext(), "At least one option must be selected", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!DEFAULT_TEXT.contentEquals(tvPassword.getText())) {
                generateAndDisplay();
            }
        };

        cbUpper.setOnCheckedChangeListener(enforceAtLeastOne);
        cbLower.setOnCheckedChangeListener(enforceAtLeastOne);
        cbNumbers.setOnCheckedChangeListener(enforceAtLeastOne);
        cbSymbols.setOnCheckedChangeListener(enforceAtLeastOne);

        btnGenerate.setOnClickListener(vv -> generateAndDisplay());

        btnCopy.setOnClickListener(vv -> {
            String pwd = tvPassword.getText().toString();
            if (pwd.trim().isEmpty() || DEFAULT_TEXT.equals(pwd)) {
                Toast.makeText(requireContext(), "Generate a password first", Toast.LENGTH_SHORT).show();
                return;
            }
            copyToClipboard(pwd);
            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        return v;
    }

    private int getCheckedCount() {
        int c = 0;
        if (cbUpper.isChecked()) c++;
        if (cbLower.isChecked()) c++;
        if (cbNumbers.isChecked()) c++;
        if (cbSymbols.isChecked()) c++;
        return c;
    }

    private void generateAndDisplay() {
        int length = getSelectedLength();

        String pool = buildPool();
        if (pool.isEmpty()) {
            Toast.makeText(requireContext(), "Select at least one option", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Character> chars = new ArrayList<>();

        if (cbUpper.isChecked() && chars.size() < length) chars.add(randomCharFrom(UPPER));
        if (cbLower.isChecked() && chars.size() < length) chars.add(randomCharFrom(LOWER));
        if (cbNumbers.isChecked() && chars.size() < length) chars.add(randomCharFrom(NUMS));
        if (cbSymbols.isChecked() && chars.size() < length) chars.add(randomCharFrom(SYMS));

        while (chars.size() < length) {
            chars.add(randomCharFrom(pool));
        }

        for (int i = chars.size() - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char tmp = chars.get(i);
            chars.set(i, chars.get(j));
            chars.set(j, tmp);
        }

        StringBuilder sb = new StringBuilder();
        for (char c : chars) sb.append(c);

        tvPassword.setText(sb.toString());
        updateStrengthLabel(length);
    }

    private int getSelectedLength() {
        return MIN_LEN + seekLength.getProgress();
    }

    private void updateLengthLabel(int len) {
        tvLengthLabel.setText("Length: " + len);
    }

    private String buildPool() {
        StringBuilder pool = new StringBuilder();
        if (cbUpper.isChecked()) pool.append(UPPER);
        if (cbLower.isChecked()) pool.append(LOWER);
        if (cbNumbers.isChecked()) pool.append(NUMS);
        if (cbSymbols.isChecked()) pool.append(SYMS);
        return pool.toString();
    }

    private char randomCharFrom(String s) {
        return s.charAt(secureRandom.nextInt(s.length()));
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Generated Password", text);
        clipboard.setPrimaryClip(clip);
    }

    private void updateStrengthLabel(int length) {
        String label;
        int color;

        if (length <= 3) {
            label = "Very weak";
            color = Color.parseColor("#FF0000");
        } else if (length <= 8) {
            label = "Weak";
            color = Color.parseColor("#FFB370");
        } else if (length <= 11) {
            label = "Strong";
            color = Color.parseColor("#D5F2A5");
        } else {
            label = "Very strong";
            color = Color.parseColor("#9AE437");
        }

        int progress = (int) Math.round(((length - MIN_LEN) * 100.0) / (MAX_LEN - MIN_LEN));
        if (progress < 0) progress = 0;
        if (progress > 100) progress = 100;

        tvStrength.setText("Strength");
        tvStrengthHint.setText(label);
        strengthBar.setProgress(progress);
        strengthBar.setProgressTintList(ColorStateList.valueOf(color));
    }
}
