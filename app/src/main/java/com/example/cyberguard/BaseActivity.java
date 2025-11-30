package com.example.cyberguard;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected FirebaseAuth mAuth;
    protected FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    protected void setupDrawer(int menuItemId) {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navView);

        if (drawerLayout == null || navigationView == null) {
            // Drawer not available in this activity
            return;
        }

        // Set up navigation listener
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            boolean handled = handleNavigationItemSelected(itemId);
            if (handled) {
                drawerLayout.closeDrawer(navigationView);
            }
            return handled;
        });

        // Load user info in drawer header
        loadUserInfoInDrawer();
    }

    protected void setupHamburgerMenu(int menuButtonId) {
        if (drawerLayout == null || navigationView == null) {
            return;
        }
        
        ImageView ivMenu = findViewById(menuButtonId);
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(navigationView));
        }
    }

    protected boolean handleNavigationItemSelected(int itemId) {
        if (itemId == R.id.nav_dashboard) {
            if (!(this instanceof DashboardActivity)) {
                startActivity(new Intent(this, DashboardActivity.class));
                if (!(this instanceof ProfileActivity)) {
                    finish();
                }
            }
            return true;
        } else if (itemId == R.id.nav_profile) {
            if (!(this instanceof ProfileActivity)) {
                startActivity(new Intent(this, ProfileActivity.class));
                if (!(this instanceof DashboardActivity)) {
                    finish();
                }
            }
            return true;
        } else if (itemId == R.id.nav_about) {
            showAboutDialog();
            return true;
        } else if (itemId == R.id.nav_help) {
            showHelpDialog();
            return true;
        } else if (itemId == R.id.nav_privacy) {
            showPrivacyDialog();
            return true;
        } else if (itemId == R.id.nav_share) {
            shareApp();
            return true;
        } else if (itemId == R.id.nav_rate) {
            rateApp();
            return true;
        } else if (itemId == R.id.nav_logout) {
            showLogoutDialog();
            return true;
        }
        return false;
    }

    protected void loadUserInfoInDrawer() {
        if (navigationView == null) return;

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView == null) return;

            TextView tvFullName = headerView.findViewById(R.id.drawer_tvFullName);
            TextView tvEmail = headerView.findViewById(R.id.drawer_tvEmail);
            TextView tvSecurityStatus = headerView.findViewById(R.id.drawer_tvSecurityStatus);

            if (tvEmail != null) {
                tvEmail.setText(currentUser.getEmail());
            }
            if (tvSecurityStatus != null) {
                tvSecurityStatus.setText(getString(R.string.security_status_good));
            }

            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String fullName = document.getString("fullName");
                                if (tvFullName != null) {
                                    if (fullName != null && !fullName.isEmpty()) {
                                        tvFullName.setText(fullName);
                                    } else {
                                        tvFullName.setText("User");
                                    }
                                }
                                loadStatistics(document);
                            } else {
                                if (tvFullName != null) {
                                    tvFullName.setText("User");
                                }
                                loadStatistics(null);
                            }
                        } else {
                            if (tvFullName != null) {
                                tvFullName.setText("User");
                            }
                            loadStatistics(null);
                        }
                    });
        }
    }

    protected void loadStatistics(DocumentSnapshot document) {
        if (navigationView == null) return;

        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;

        TextView tvBreachesChecked = headerView.findViewById(R.id.drawer_tvBreachesChecked);
        TextView tvPasswordsGenerated = headerView.findViewById(R.id.drawer_tvPasswordsGenerated);
        TextView tvQuizzesCompleted = headerView.findViewById(R.id.drawer_tvQuizzesCompleted);

        if (document != null && document.exists()) {
            Long breachesChecked = document.getLong("breachesChecked");
            Long passwordsGenerated = document.getLong("passwordsGenerated");
            Long quizzesCompleted = document.getLong("quizzesCompleted");

            if (tvBreachesChecked != null) {
                tvBreachesChecked.setText(String.valueOf(breachesChecked != null ? breachesChecked : 0));
            }
            if (tvPasswordsGenerated != null) {
                tvPasswordsGenerated.setText(String.valueOf(passwordsGenerated != null ? passwordsGenerated : 0));
            }
            if (tvQuizzesCompleted != null) {
                tvQuizzesCompleted.setText(String.valueOf(quizzesCompleted != null ? quizzesCompleted : 0));
            }
        } else {
            if (tvBreachesChecked != null) {
                tvBreachesChecked.setText("0");
            }
            if (tvPasswordsGenerated != null) {
                tvPasswordsGenerated.setText("0");
            }
            if (tvQuizzesCompleted != null) {
                tvQuizzesCompleted.setText("0");
            }
        }
    }

    protected void showFeatureComingSoon(String featureName) {
        android.widget.Toast.makeText(this, featureName + " feature coming soon!", android.widget.Toast.LENGTH_SHORT).show();
    }

    protected void showAboutDialog() {
        String versionName = "1.0.0";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        new AlertDialog.Builder(this)
                .setTitle("About CyberGuard")
                .setMessage("CyberGuard v" + versionName + "\n\n" +
                        "Your comprehensive cybersecurity companion.\n\n" +
                        "Protect yourself with:\n" +
                        "• Breach checking\n" +
                        "• Secure notes encryption\n" +
                        "• Cybersecurity news\n" +
                        "• Phishing training\n" +
                        "• Network security monitoring\n" +
                        "• Security quizzes\n" +
                        "• Daily security checklist\n" +
                        "• Password generator\n\n" +
                        "Stay safe, stay secure!")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    protected void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Help & Support")
                .setMessage("Need help? We're here for you!\n\n" +
                        "Common Questions:\n\n" +
                        "• How do I check for breaches?\n" +
                        "  Go to Dashboard > Breach Check\n\n" +
                        "• How do I generate a password?\n" +
                        "  Use the Password Generator feature\n\n" +
                        "• Where can I learn about phishing?\n" +
                        "  Check out Phishing Training\n\n" +
                        "For more support, contact us at:\n" +
                        "support@cyberguard.app")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    protected void showPrivacyDialog() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        new AlertDialog.Builder(this)
                .setTitle("Privacy Policy")
                .setMessage("Privacy Policy\n\n" +
                        "Last Updated: " + currentDate + "\n\n" +
                        "CyberGuard is committed to protecting your privacy.\n\n" +
                        "Data Collection:\n" +
                        "• We collect only necessary information for account creation\n" +
                        "• Your security data is stored securely\n" +
                        "• We do not share your data with third parties\n\n" +
                        "Security:\n" +
                        "• All data is encrypted in transit and at rest\n" +
                        "• We use industry-standard security practices\n\n" +
                        "For the full privacy policy, visit:\n" +
                        "www.cyberguard.app/privacy")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    protected void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "CyberGuard - Cyber Security Companion");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text));
        startActivity(Intent.createChooser(shareIntent, "Share CyberGuard"));
    }

    protected void rateApp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + getPackageName())));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    protected void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout))
                .setMessage(getString(R.string.logout_confirmation))
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    protected void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, LandingActivity.class));
        finish();
    }

}

