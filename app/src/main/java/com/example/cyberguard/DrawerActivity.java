package com.example.cyberguard;

import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

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
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_dashboard, R.id.nav_gallery, R.id.nav_slideshow).setOpenableLayout(drawer).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_drawer);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        loadUserInfoInDrawer();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_drawer);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    protected void loadUserInfoInDrawer() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
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
}