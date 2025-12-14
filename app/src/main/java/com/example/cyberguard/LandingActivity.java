package com.example.cyberguard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

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
            if (!com.example.cyberguard.util.NetworkUtils.hasInternetConnection(this)) {
                showInternetRequiredAndClose();
                return;
            }

            startActivity(new Intent(LandingActivity.this, DrawerActivity.class));
            finish();
            return;
        }

        Button btnLogin = findViewById(R.id.landing_btnLogin);
        Button btnRegister = findViewById(R.id.landing_btnRegister);

        btnLogin.setOnClickListener(view -> startActivity(new Intent(LandingActivity.this, LoginActivity.class)));

        btnRegister.setOnClickListener(view -> startActivity(new Intent(LandingActivity.this, RegisterActivity.class)));
    }

    private void showInternetRequiredAndClose() {
        new android.app.AlertDialog.Builder(this).setTitle("Internet connection required").setMessage("Please enable Wi-Fi or mobile data to continue.").setCancelable(false).setPositiveButton("Close", (dialog, which) -> {
            dialog.dismiss();
            finishAffinity();
        }).show();
    }
}
