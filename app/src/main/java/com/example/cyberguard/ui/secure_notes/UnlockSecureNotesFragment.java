package com.example.cyberguard.ui.secure_notes;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cyberguard.R;
import com.example.cyberguard.secure.NotesCrypto;
import com.example.cyberguard.secure.SecureNotesRepository;
import com.example.cyberguard.secure.SecureNotesSessionViewModel;

import java.util.Map;

import javax.crypto.SecretKey;

public class UnlockSecureNotesFragment extends Fragment {

    private static final String VAULT_CHECK_PLAINTEXT = "CYBERGUARD_SECURE_NOTES_OK";

    private EditText etMaster;
    private EditText etConfirm;
    private Button btnUnlock;
    private ProgressBar progress;
    private TextView tvError;

    private SecureNotesRepository repo;
    private SecureNotesSessionViewModel session;

    private boolean isFirstTimeSetup = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_unlock_secure_notes, container, false);

        etMaster = root.findViewById(R.id.sn_et_master_password);
        etConfirm = root.findViewById(R.id.sn_et_master_password_confirm);
        btnUnlock = root.findViewById(R.id.sn_btn_unlock);
        progress = root.findViewById(R.id.sn_unlock_progress);
        tvError = root.findViewById(R.id.sn_tv_unlock_error);

        repo = new SecureNotesRepository();
        session = new ViewModelProvider(requireActivity()).get(SecureNotesSessionViewModel.class);

        // Determine if this is first time (no verifier stored)
        setLoading(true);
        repo.getVaultCheck(new SecureNotesRepository.Callback<Map<String, String>>() {
            @Override
            public void onSuccess(Map<String, String> v) {
                setLoading(false);
                String cipher = v.get("cipher");
                String iv = v.get("iv");

                isFirstTimeSetup = (cipher == null || iv == null);
                etConfirm.setVisibility(isFirstTimeSetup ? View.VISIBLE : View.GONE);
                btnUnlock.setText(isFirstTimeSetup ? "Create Master Password" : "Unlock");
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                showError("Failed to load vault status: " + e.getMessage());
            }
        });

        btnUnlock.setOnClickListener(v -> unlockOrSetup());

        return root;
    }

    private void unlockOrSetup() {
        tvError.setVisibility(View.GONE);

        String master = etMaster.getText().toString();
        if (TextUtils.isEmpty(master) || master.length() < 6) {
            showError("Master password must be at least 6 characters.");
            return;
        }

        if (isFirstTimeSetup) {
            String confirm = etConfirm.getText().toString();
            if (!master.equals(confirm)) {
                showError("Passwords do not match.");
                return;
            }
        }

        setLoading(true);

        // Always get/create salt first
        repo.getOrCreateSalt(new SecureNotesRepository.Callback<String>() {
            @Override
            public void onSuccess(String saltB64) {
                new Thread(() -> {
                    try {
                        SecretKey key = NotesCrypto.deriveKey(master, saltB64);

                        if (isFirstTimeSetup) {
                            // Create verifier and store it
                            NotesCrypto.EncResult enc = NotesCrypto.encrypt(VAULT_CHECK_PLAINTEXT, key);

                            requireActivity().runOnUiThread(() -> {
                                repo.setVaultCheck(enc.cipherTextB64, enc.ivB64, new SecureNotesRepository.Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void value) {
                                        setLoading(false);
                                        session.setSession(key, saltB64);
                                        NavHostFragment.findNavController(UnlockSecureNotesFragment.this)
                                                .navigate(R.id.action_unlockSecureNotesFragment_to_secureNotesFragment);
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        setLoading(false);
                                        showError("Failed to set vault password: " + e.getMessage());
                                    }
                                });
                            });

                        } else {
                            // Verify password by decrypting stored verifier
                            repo.getVaultCheck(new SecureNotesRepository.Callback<Map<String, String>>() {
                                @Override
                                public void onSuccess(Map<String, String> v) {
                                    String cipher = v.get("cipher");
                                    String iv = v.get("iv");

                                    new Thread(() -> {
                                        try {
                                            String plain = NotesCrypto.decrypt(cipher, iv, key);
                                            if (!VAULT_CHECK_PLAINTEXT.equals(plain)) throw new Exception("Bad check");

                                            requireActivity().runOnUiThread(() -> {
                                                setLoading(false);
                                                session.setSession(key, saltB64);
                                                NavHostFragment.findNavController(UnlockSecureNotesFragment.this)
                                                        .navigate(R.id.action_unlockSecureNotesFragment_to_secureNotesFragment);
                                            });

                                        } catch (Exception ex) {
                                            requireActivity().runOnUiThread(() -> {
                                                setLoading(false);
                                                showError("Wrong master password.");
                                            });
                                        }
                                    }).start();
                                }

                                @Override
                                public void onError(Exception e) {
                                    requireActivity().runOnUiThread(() -> {
                                        setLoading(false);
                                        showError("Failed to verify: " + e.getMessage());
                                    });
                                }
                            });
                        }

                    } catch (Exception e) {
                        requireActivity().runOnUiThread(() -> {
                            setLoading(false);
                            showError("Failed: " + e.getMessage());
                        });
                    }
                }).start();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                showError("Failed to fetch salt: " + e.getMessage());
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnUnlock.setEnabled(!loading);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
