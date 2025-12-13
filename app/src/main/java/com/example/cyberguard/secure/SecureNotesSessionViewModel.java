package com.example.cyberguard.secure;

import androidx.lifecycle.ViewModel;

import javax.crypto.SecretKey;

public class SecureNotesSessionViewModel extends ViewModel {
    private SecretKey key;
    private String saltB64;

    public void setSession(SecretKey key, String saltB64) {
        this.key = key;
        this.saltB64 = saltB64;
    }

    public boolean isUnlocked() {
        return key != null && saltB64 != null;
    }

    public SecretKey getKey() { return key; }
    public String getSaltB64() { return saltB64; }

    public void lock() {
        key = null;
        saltB64 = null;
    }
}
