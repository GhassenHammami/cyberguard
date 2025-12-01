package com.example.cyberguard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cyberguard.databinding.ActivityDrawerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DrawerActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityDrawerBinding binding;
    private NavigationView navigationView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDrawerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarDrawer.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        navigationView = binding.navView;


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_dashboard, R.id.nav_profile, R.id.nav_slideshow).setOpenableLayout(drawer).build();
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
                //
                return true;
            } else if (id == R.id.nav_rate) {
                //
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

        if (tvEmail != null) {
            tvEmail.setText(currentUser.getEmail());
        }

        db.collection("users").document(currentUser.getUid()).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                return;
            }
            DocumentSnapshot document = task.getResult();
            if (document != null && document.exists()) {
                String fullName = document.getString("fullName");

                if (tvFullName != null && fullName != null && !fullName.isEmpty()) {
                    tvFullName.setText(fullName);
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

}