package com.example.cyberguard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cyberguard.databinding.ActivityDrawerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class DrawerActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private NavigationView navigationView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityDrawerBinding binding = ActivityDrawerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarDrawer.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        navigationView = binding.navView;


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dashboard,
                R.id.nav_profile,
                R.id.nav_about,
                R.id.nav_help,
                R.id.nav_privacy
        ).setOpenableLayout(drawer).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_drawer);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                showLogoutDialog();
                drawer.closeDrawers();
                return true;
            } else if (id == R.id.nav_share) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String text = "Check out CyberGuard! https://cybeguard.tn";
                shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
                drawer.closeDrawers();
                return true;
            } else if (id == R.id.nav_rate) {
                String packageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            android.net.Uri.parse("market://details?id=" + packageName)));
                } catch (android.content.ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
                }
                drawer.closeDrawers();
                return true;
            } else {
                boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                drawer.closeDrawers();
                return handled;
            }
        });
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserInfoInDrawer();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_drawer);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    protected void loadUserInfoInDrawer() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || navigationView == null) return;

        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;

        TextView tvFullName = headerView.findViewById(R.id.drawer_tvFullName);
        TextView tvEmail = headerView.findViewById(R.id.drawer_tvEmail);

        if (tvEmail != null && currentUser.getEmail() != null) {
            tvEmail.setText(currentUser.getEmail());
        }

        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }

        userListener = db.collection("users")
                .document(currentUser.getUid())
                .addSnapshotListener((document, e) -> {
                    if (e != null || document == null || !document.exists()) return;

                    String fullName = document.getString("fullName");
                    String email = document.getString("email");

                    if (tvFullName != null && fullName != null && !fullName.trim().isEmpty()) {
                        tvFullName.setText(fullName);
                    }

                    if (tvEmail != null) {
                        if (email != null && !email.trim().isEmpty()) {
                            tvEmail.setText(email);
                        } else if (currentUser.getEmail() != null) {
                            tvEmail.setText(currentUser.getEmail());
                        }
                    }
                });
    }

    protected void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.menu_logout)).setMessage(getString(R.string.menu_logout_confirmation)).setPositiveButton("Yes", (dialog, which) -> logout()).setNegativeButton("No", (dialog, which) -> dialog.dismiss()).show();
    }

    protected void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, LandingActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }

}