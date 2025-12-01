package com.example.cyberguard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LandingActivity.this, DrawerActivity.class));
            finish();
            return;
        }

        Button btnLogin = findViewById(R.id.landing_btnLogin);
        Button btnRegister = findViewById(R.id.landing_btnRegister);

        btnLogin.setOnClickListener(view ->
                startActivity(new Intent(LandingActivity.this, LoginActivity.class))
        );

        btnRegister.setOnClickListener(view ->
                startActivity(new Intent(LandingActivity.this, RegisterActivity.class))
        );

        setupVersion();
    }

    private void setupVersion() {
        TextView tvVersion = findViewById(R.id.landing_tvVersion);
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText(getString(R.string.app_version) + " " + versionName);
        } catch (Exception e) {
            tvVersion.setText(getString(R.string.app_version));
        }
    }
}
