package com.example.cyberguard.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cyberguard.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public class BreachCheckFragment extends Fragment {

    private RadioGroup modeGroup;
    private LinearLayout emailSection, passwordSection;

    private EditText etEmail, etPassword;
    private Button btnEmail, btnPassword;

    private ProgressBar progress;
    private LinearLayout resultCard;
    private TextView tvTitle, tvMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_breach_check, container, false);

        modeGroup = root.findViewById(R.id.bc_mode_group);
        emailSection = root.findViewById(R.id.bc_section_email);
        passwordSection = root.findViewById(R.id.bc_section_password);

        etEmail = root.findViewById(R.id.bc_et_email);
        etPassword = root.findViewById(R.id.bc_et_password);

        btnEmail = root.findViewById(R.id.bc_btn_check_email);
        btnPassword = root.findViewById(R.id.bc_btn_check_password);

        progress = root.findViewById(R.id.bc_progress);
        resultCard = root.findViewById(R.id.bc_result_card);
        tvTitle = root.findViewById(R.id.bc_tv_title);
        tvMessage = root.findViewById(R.id.bc_tv_message);

        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean emailMode = checkedId == R.id.bc_mode_email;
            emailSection.setVisibility(emailMode ? View.VISIBLE : View.GONE);
            passwordSection.setVisibility(emailMode ? View.GONE : View.VISIBLE);
            resultCard.setVisibility(View.GONE);
        });

        btnEmail.setOnClickListener(v -> onCheckEmail());
        btnPassword.setOnClickListener(v -> onCheckPassword());

        return root;
    }

    private void onCheckEmail() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email.");
            return;
        }

        setLoading(true);
        resultCard.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                String url = "https://api.xposedornot.com/v1/check-email/" + encodePath(email);
                JSONObject json = httpGetJson(url);

                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showEmailResult(email, json);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
            }
        }).start();
    }

    private void showEmailResult(String email, JSONObject json) {
        resultCard.setVisibility(View.VISIBLE);

        if (json.has("Error")) {
            tvTitle.setText("Looks OK ✅");
            tvMessage.setText("No breaches found for:\n" + email);
            return;
        }

        JSONArray breachesOuter = json.optJSONArray("breaches");
        JSONArray breaches = (breachesOuter != null && breachesOuter.length() > 0) ? breachesOuter.optJSONArray(0) : null;

        int count = (breaches == null) ? 0 : breaches.length();
        if (count == 0) {
            tvTitle.setText("Looks OK ✅");
            tvMessage.setText("No breaches found for:\n" + email);
            return;
        }

        tvTitle.setText("Breaches found ❌ (" + count + ")");
        StringBuilder sb = new StringBuilder();
        sb.append("Email: ").append(email).append("\n\nAffected sites:\n");
        for (int i = 0; i < breaches.length(); i++) {
            sb.append("• ").append(breaches.optString(i)).append("\n");
        }
        sb.append("\nRecommendations:\n• Change passwords on affected sites\n• Enable 2FA\n• Don’t reuse passwords");
        tvMessage.setText(sb.toString());
    }

    private void onCheckPassword() {
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(password) || password.length() < 4) {
            Toast.makeText(getContext(), "Enter a password (min 4 chars).", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        resultCard.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                long count = checkPwnedPasswordCount(password);

                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showPasswordResult(count);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
            }
        }).start();
    }

    private long checkPwnedPasswordCount(String password) throws Exception {
        String sha1 = sha1Hex(password).toUpperCase(Locale.US);
        String prefix = sha1.substring(0, 5);
        String suffix = sha1.substring(5);

        URL url = new URL("https://api.pwnedpasswords.com/range/" + prefix);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int code = conn.getResponseCode();
        if (code != 200) throw new Exception("Password API error: HTTP " + code);

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String apiSuffix = line.substring(0, colonIndex).trim();
                if (apiSuffix.equalsIgnoreCase(suffix)) {
                    String countStr = line.substring(colonIndex + 1).trim();
                    try {
                        return Long.parseLong(countStr);
                    } catch (NumberFormatException ignored) {
                        return 1;
                    }
                }
            }
        }
        return 0;
    }

    private void showPasswordResult(long count) {
        resultCard.setVisibility(View.VISIBLE);

        if (count > 0) {
            tvTitle.setText("Exposed ❌");
            tvMessage.setText("This password appears in breach data " + count + " times.\n\n" + "Recommendation: change it immediately, don’t reuse it, and enable 2FA.");
        } else {
            tvTitle.setText("Looks OK ✅");
            tvMessage.setText("This password was not found in known breach data.\n\n" + "Still: use a unique password + 2FA for best safety.");
        }
    }

    private void showError(String msg) {
        resultCard.setVisibility(View.VISIBLE);
        tvTitle.setText("Error");
        tvMessage.setText(msg == null ? "Something went wrong." : msg);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnEmail.setEnabled(!loading);
        btnPassword.setEnabled(!loading);
    }

    private static JSONObject httpGetJson(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int code = conn.getResponseCode();

        BufferedReader reader = new BufferedReader(new InputStreamReader((code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        return new JSONObject(sb.toString());
    }

    private static String sha1Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String encodePath(String s) {
        return s.replace("@", "%40").replace("+", "%2B");
    }
}
