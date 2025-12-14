package com.example.cyberguard.ui.secure_notes;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cyberguard.R;
import com.example.cyberguard.secure.NotesCrypto;
import com.example.cyberguard.secure.SecureNotesRepository;
import com.example.cyberguard.secure.SecureNotesSessionViewModel;
import com.example.cyberguard.util.NetworkUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import javax.crypto.SecretKey;

public class EditSecureNoteFragment extends Fragment {

    private TextView tvHeader;
    private TextInputEditText etTitle, etBody;
    private ImageButton btnSave, btnDeleteCancel;
    private ProgressBar progress;
    private TextView tvError;

    private SecureNotesRepository repo;
    private SecureNotesSessionViewModel session;

    private @Nullable String noteId;

    // Keep originals for Cancel + dirty check
    private String originalTitle = "";
    private String originalBody = "";

    private boolean suppressWatchers = false;
    private boolean isDirty = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_edit_secure_note, container, false);

        tvHeader = root.findViewById(R.id.sn_tv_header);
        etTitle = root.findViewById(R.id.sn_et_title);
        etBody = root.findViewById(R.id.sn_et_body);
        btnSave = root.findViewById(R.id.sn_btn_edit_save);          // will act as SAVE only
        btnDeleteCancel = root.findViewById(R.id.sn_btn_delete_cancel);
        progress = root.findViewById(R.id.sn_edit_progress);
        tvError = root.findViewById(R.id.sn_tv_edit_error);

        repo = new SecureNotesRepository();
        session = new ViewModelProvider(requireActivity()).get(SecureNotesSessionViewModel.class);

        if (session.isUnlocked()) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_editSecureNoteFragment_to_unlockSecureNotesFragment);
            return root;
        }

        noteId = getArguments() != null ? getArguments().getString("noteId") : null;

        // Editable by default
        etTitle.setEnabled(true);
        etBody.setEnabled(true);

        // Start clean state: hide save, show delete
        setCleanState();

        // Watch changes to toggle Save/Cancel UI
        attachDirtyWatchers();

        if (noteId == null) {
            tvHeader.setText("New Note");
            // Originals are empty initially
            originalTitle = "";
            originalBody = "";
        } else {
            tvHeader.setText("Secure Note");
            loadNote(noteId);
        }

        btnSave.setOnClickListener(v -> {
            // Save is only visible when dirty, but still safe to call
            save();
        });

        btnDeleteCancel.setOnClickListener(v -> {
            if (!isDirty) {
                // Delete (clean mode)
                confirmDelete();
            } else {
                // Cancel (dirty mode)
                cancelEdit();
            }
        });

        return root;
    }

    private void attachDirtyWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressWatchers) return;
                updateDirtyStateFromFields();
            }
        };

        etTitle.addTextChangedListener(watcher);
        etBody.addTextChangedListener(watcher);
    }

    private void updateDirtyStateFromFields() {
        String t = safe(etTitle.getText());
        String b = safe(etBody.getText());

        boolean dirtyNow = !t.equals(originalTitle) || !b.equals(originalBody);
        if (dirtyNow == isDirty) return;

        isDirty = dirtyNow;
        if (isDirty) setDirtyState();
        else setCleanState();
    }

    private void setDirtyState() {
        // Show save button + make delete become cancel
        btnSave.setVisibility(View.VISIBLE);
        btnSave.setImageResource(R.drawable.ic_save); // 💾
        btnDeleteCancel.setImageResource(R.drawable.ic_close); // ✖
    }

    private void setCleanState() {
        isDirty = false;

        // Hide save button + restore delete
        btnSave.setVisibility(View.GONE);
        btnDeleteCancel.setImageResource(R.drawable.ic_delete); // 🗑

        // Optional: remove focus for nicer "read" feel
        etTitle.clearFocus();
        etBody.clearFocus();
    }

    private void loadNote(String id) {
        setLoading(true);
        tvError.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("secure_notes")
                .document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    if (!doc.exists()) {
                        showError("Note not found.");
                        return;
                    }
                    fillFromDoc(doc);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
    }

    private void fillFromDoc(DocumentSnapshot doc) {
        String title = doc.getString("title");
        if (title == null) title = "";

        // set title immediately
        suppressWatchers = true;
        etTitle.setText(title);
        suppressWatchers = false;

        String cipher = doc.getString("cipherTextB64");
        String iv = doc.getString("ivB64");
        if (cipher == null || iv == null) {
            // no body stored
            originalTitle = title;
            originalBody = safe(etBody.getText());
            setCleanState();
            return;
        }

        SecretKey key = session.getKey();
        new Thread(() -> {
            try {
                String plain = NotesCrypto.decrypt(cipher, iv, key);
                requireActivity().runOnUiThread(() -> {
                    suppressWatchers = true;
                    etBody.setText(plain);
                    suppressWatchers = false;

                    // Save originals for cancel + dirty check
                    originalTitle = safe(etTitle.getText());
                    originalBody = safe(etBody.getText());
                    setCleanState();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> showError("Failed to decrypt note. Wrong master password?"));
            }
        }).start();
    }

    private void cancelEdit() {
        tvError.setVisibility(View.GONE);

        // New note -> cancel just exits
        if (noteId == null) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        // Restore original values without re-triggering dirty state
        suppressWatchers = true;
        etTitle.setText(originalTitle);
        etBody.setText(originalBody);
        suppressWatchers = false;

        setCleanState();
    }

    private void save() {
        tvError.setVisibility(View.GONE);

        if (!NetworkUtils.hasInternetConnection(requireContext())) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("No internet connection")
                    .setMessage("Please check your connection and try again.")
                    .setPositiveButton("OK", (d, w) -> d.dismiss())
                    .show();
            return;
        }

        String title = safe(etTitle.getText()).trim();
        String body = safe(etBody.getText());

        if (TextUtils.isEmpty(body)) {
            showError("Note body can’t be empty.");
            Toast.makeText(requireContext(), "Save failed: note body can’t be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        SecretKey key = session.getKey();

        if (noteId == null) {
            repo.createNote(title, body, key, new SecureNotesRepository.Callback<com.google.firebase.firestore.DocumentReference>() {
                @Override
                public void onSuccess(com.google.firebase.firestore.DocumentReference value) {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Note saved.", Toast.LENGTH_SHORT).show();
                    // After creating, go back to list
                    NavHostFragment.findNavController(EditSecureNoteFragment.this).popBackStack();
                }

                @Override
                public void onError(Exception e) {
                    setLoading(false);
                    showError(e.getMessage());
                    Toast.makeText(requireContext(),
                            "Save failed: " + (e.getMessage() == null ? "Something went wrong." : e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            repo.updateNote(noteId, title, body, key, new SecureNotesRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void value) {
                    setLoading(false);

                    // Update originals (so cancel returns to latest saved)
                    originalTitle = title;
                    originalBody = body;

                    setCleanState();

                    Toast.makeText(requireContext(), "Note saved.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    setLoading(false);
                    showError(e.getMessage());
                    Toast.makeText(requireContext(),
                            "Save failed: " + (e.getMessage() == null ? "Something went wrong." : e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void confirmDelete() {
        if (noteId == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete note?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> delete())
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    private void delete() {
        if (noteId == null) return;

        setLoading(true);
        repo.deleteNote(noteId, new SecureNotesRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                setLoading(false);
                Toast.makeText(requireContext(), "Note deleted.", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(EditSecureNoteFragment.this).popBackStack();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                showError(e.getMessage());
                Toast.makeText(requireContext(),
                        "Delete failed: " + (e.getMessage() == null ? "Something went wrong." : e.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnDeleteCancel.setEnabled(!loading);
        etTitle.setEnabled(!loading);
        etBody.setEnabled(!loading);
    }

    private void showError(String msg) {
        tvError.setText(msg == null ? "Something went wrong." : msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private String safe(@Nullable CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }
}
