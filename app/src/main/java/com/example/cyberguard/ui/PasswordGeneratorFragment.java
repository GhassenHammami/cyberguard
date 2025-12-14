package com.example.cyberguard.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.widget.ProgressBar;


public class PasswordGeneratorFragment extends Fragment {

    private static final int MIN_LEN = 8;
    private static final int MAX_LEN = 32;

    private TextView tvPassword, tvLengthLabel, tvStrength, tvStrengthHint;
    private SeekBar seekLength;
    private CheckBox cbUpper, cbLower, cbNumbers, cbSymbols;
    private Button btnGenerate, btnCopy;
    private ProgressBar strengthBar;


    private final SecureRandom secureRandom = new SecureRandom();

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMS  = "0123456789";
    private static final String SYMS  = "!@#$%^&*()-_=+[]{};:,.?/<>";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

        btnGenerate = v.findViewById(R.id.btnGenerate);
        btnCopy = v.findViewById(R.id.btnCopy);

        seekLength.setMax(MAX_LEN - MIN_LEN);

        int defaultLen = 16;
        seekLength.setProgress(defaultLen - MIN_LEN);
        updateLengthLabel(defaultLen);
        updateStrengthLabel(defaultLen);

        seekLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int len = MIN_LEN + progress;
                updateLengthLabel(len);
                updateStrengthLabel(len);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        View.OnClickListener regenerateOnChange = vv -> {
            if (!"Tap Generate".contentEquals(tvPassword.getText())) {
                generateAndDisplay();
            }
            updateStrengthLabel(getSelectedLength());
        };

        cbUpper.setOnClickListener(regenerateOnChange);
        cbLower.setOnClickListener(regenerateOnChange);
        cbNumbers.setOnClickListener(regenerateOnChange);
        cbSymbols.setOnClickListener(regenerateOnChange);

        btnGenerate.setOnClickListener(vv -> generateAndDisplay());

        btnCopy.setOnClickListener(vv -> {
            String pwd = tvPassword.getText().toString();
            if (pwd == null || pwd.trim().isEmpty() || "Tap Generate".equals(pwd)) {
                Toast.makeText(requireContext(), "Generate a password first", Toast.LENGTH_SHORT).show();
                return;
            }
            copyToClipboard(pwd);
            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        return v;
    }

    private void generateAndDisplay() {
        int length = getSelectedLength();

        String pool = buildPool();
        if (pool.isEmpty()) {
            Toast.makeText(requireContext(), "Select at least one option", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Character> chars = new ArrayList<>();

        if (cbUpper.isChecked()) chars.add(randomCharFrom(UPPER));
        if (cbLower.isChecked()) chars.add(randomCharFrom(LOWER));
        if (cbNumbers.isChecked()) chars.add(randomCharFrom(NUMS));
        if (cbSymbols.isChecked()) chars.add(randomCharFrom(SYMS));

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
        ClipboardManager clipboard =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Generated Password", text);
        clipboard.setPrimaryClip(clip);
    }

    private void updateStrengthLabel(int length) {
        int categories = 0;
        if (cbUpper.isChecked()) categories++;
        if (cbLower.isChecked()) categories++;
        if (cbNumbers.isChecked()) categories++;
        if (cbSymbols.isChecked()) categories++;

        int score = 0;

        if (length >= 8) score += 20;
        if (length >= 12) score += 20;
        if (length >= 16) score += 20;
        if (length >= 20) score += 20;
        if (length >= 24) score += 10;

        score += categories * 10;

        if (score > 100) score = 100;

        String label;
        int color;

        if (categories <= 1 || length < 10 || score < 40) {
            label = "Weak";
            color = Color.parseColor("#E53935");
        } else if (score < 70) {
            label = "Medium";
            color = Color.parseColor("#FB8C00");
        } else {
            label = "Strong";
            color = Color.parseColor("#43A047");
        }

        tvStrength.setText("Strength: " + label);
        tvStrengthHint.setText("Score: " + score + "/100");
        strengthBar.setProgress(score);

        strengthBar.setProgressTintList(ColorStateList.valueOf(color));
    }

}
