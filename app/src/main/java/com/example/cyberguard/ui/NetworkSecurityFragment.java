package com.example.cyberguard.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.ProxyInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cyberguard.R;

import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkSecurityFragment extends Fragment {

    private TextView tvRiskPill, tvRiskSummary, tvConnection, tvSignals;
    private Button btnRefresh;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_network_security, container, false);

        tvRiskPill = root.findViewById(R.id.ns_tv_risk_pill);
        tvRiskSummary = root.findViewById(R.id.ns_tv_risk_summary);
        tvConnection = root.findViewById(R.id.ns_tv_connection);
        tvSignals = root.findViewById(R.id.ns_tv_signals);

        btnRefresh = root.findViewById(R.id.ns_btn_refresh);


        btnRefresh.setOnClickListener(v -> refreshSignals());

        refreshSignals();
        return root;
    }

    private void refreshSignals() {
        setLoading(true);

        new Thread(() -> {
            try {
                SecurityReport report = buildSecurityReport(requireContext());
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    renderReport(report);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    tvRiskPill.setText("—");
                    tvRiskSummary.setText("Failed to refresh: " + e.getMessage());
                });
            }
        }).start();
    }

    private static class SecurityReport {
        String connectionText;
        String signalsText;
        int riskScore;
        String riskSummary;
    }

    private void renderReport(SecurityReport r) {
        tvConnection.setText(r.connectionText);
        tvSignals.setText(r.signalsText);

        tvRiskPill.setText(r.riskScore + "/100");

        if (r.riskScore <= 25) tvRiskSummary.setText("Low risk. Good baseline security signals.");
        else if (r.riskScore <= 55) tvRiskSummary.setText("Medium risk. Some signals could be improved.");
        else tvRiskSummary.setText("High risk. Avoid sensitive actions on this network.");

        if (!TextUtils.isEmpty(r.riskSummary)) tvRiskSummary.setText(r.riskSummary);
    }

    private static SecurityReport buildSecurityReport(Context ctx) {
        SecurityReport r = new SecurityReport();
        int risk = 0;

        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network active = cm.getActiveNetwork();
        NetworkCapabilities caps = active != null ? cm.getNetworkCapabilities(active) : null;

        boolean hasInternet = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        boolean validated = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        boolean wifi = caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean cell = caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        boolean vpn = caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);

        StringBuilder conn = new StringBuilder();
        if (caps == null) {
            conn.append("Status: Offline\n");
            risk += 10;
        } else {
            conn.append("Transport: ")
                    .append(wifi ? "Wi-Fi" : cell ? "Cellular" : vpn ? "VPN" : "Other")
                    .append("\n");
            conn.append("Internet: ").append(hasInternet ? "Yes" : "No").append("\n");
            conn.append("Validated: ").append(validated ? "Yes" : "No").append("\n");

            if (!validated) risk += 20;
        }

        StringBuilder sig = new StringBuilder();

        if (wifi) {
            sig.append("Wi-Fi: Connected\n");
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wi = wm != null ? wm.getConnectionInfo() : null;
            if (wi != null) {
                String ssid = wi.getSSID();
                if (ssid != null) sig.append("SSID: ").append(ssid).append("\n");
                sig.append("Signal: ").append(WifiManager.calculateSignalLevel(wi.getRssi(), 5)).append("/4").append("\n");
            }


            sig.append("Wi-Fi encryption: (depends on device/OS permissions)\n");
            risk += 5;
        } else if (cell) {
            sig.append("Cellular data: typically safer than random public Wi-Fi\n");
            risk += 5;
        }

        sig.append("VPN: ").append(vpn ? "On" : "Off").append("\n");
        if (!vpn && wifi) risk += 10;

        LinkProperties lp = active != null ? cm.getLinkProperties(active) : null;
        ProxyInfo proxy = lp != null ? lp.getHttpProxy() : null;
        boolean proxyOn = proxy != null && !TextUtils.isEmpty(proxy.getHost());
        sig.append("Proxy: ").append(proxyOn ? ("On (" + proxy.getHost() + ")") : "Off").append("\n");
        if (proxyOn) risk += 10;

        String privateDnsMode = Settings.Global.getString(ctx.getContentResolver(), "private_dns_mode");
        sig.append("Private DNS: ").append(privateDnsMode == null ? "Unknown" : privateDnsMode).append("\n");
        if ("off".equalsIgnoreCase(privateDnsMode)) risk += 10;

        boolean captiveLikely = false;
        if (hasInternet) {
            try {
                captiveLikely = isCaptivePortalLikely();
            } catch (Exception ignored) {}
        }
        sig.append("Captive portal: ").append(captiveLikely ? "Likely" : "No").append("\n");
        if (captiveLikely) risk += 20;

        if (risk < 0) risk = 0;
        if (risk > 100) risk = 100;

        r.connectionText = conn.toString().trim();
        r.signalsText = sig.toString().trim();
        r.riskScore = risk;

        if (wifi && !vpn && (proxyOn || "off".equalsIgnoreCase(privateDnsMode) || captiveLikely)) {
            r.riskSummary = "Higher risk on Wi-Fi. Consider enabling VPN and Private DNS; avoid logging into sensitive accounts here.";
        }

        return r;
    }

    private static boolean isCaptivePortalLikely() throws Exception {
        URL url = new URL("http://connectivitycheck.gstatic.com/generate_204");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        return code != 204;
    }

    private void setLoading(boolean loading) {
        btnRefresh.setEnabled(!loading);
    }
}
