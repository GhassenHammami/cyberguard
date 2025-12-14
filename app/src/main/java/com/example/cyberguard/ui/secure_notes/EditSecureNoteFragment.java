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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import javax.crypto.SecretKey;

public class EditSecureNoteFragment extends Fragment {

    private EditText etTitle, etBody;
    private Button btnSave, btnDelete;
    private ProgressBar progress;
    private TextView tvError;

    private SecureNotesRepository repo;
    private SecureNotesSessionViewModel session;

    private @Nullable String noteId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_edit_secure_note, container, false);

        etTitle = root.findViewById(R.id.sn_et_title);
        etBody = root.findViewById(R.id.sn_et_body);
        btnSave = root.findViewById(R.id.sn_btn_save);
        btnDelete = root.findViewById(R.id.sn_btn_delete);
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
        btnDelete.setVisibility(noteId != null ? View.VISIBLE : View.GONE);

        btnSave.setOnClickListener(v -> save());
        btnDelete.setOnClickListener(v -> delete());

        if (noteId != null) loadNote(noteId);

        return root;
    }

    private void loadNote(String id) {
        setLoading(true);
        tvError.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("users").document(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("secure_notes").document(id)
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
        etTitle.setText(doc.getString("title"));

        String cipher = doc.getString("cipherTextB64");
        String iv = doc.getString("ivB64");

        if (cipher == null || iv == null) return;

        SecretKey key = session.getKey();
        new Thread(() -> {
            try {
                String plain = NotesCrypto.decrypt(cipher, iv, key);
                requireActivity().runOnUiThread(() -> etBody.setText(plain));
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> showError("Failed to decrypt note. Wrong master password?"));
            }
        }).start();
    }

    private void save() {
        tvError.setVisibility(View.GONE);

        String title = etTitle.getText().toString().trim();
        String body = etBody.getText().toString();

        if (TextUtils.isEmpty(body)) {
            showError("Note body can’t be empty.");
            return;
        }

        setLoading(true);

        SecretKey key = session.getKey();

        if (noteId == null) {
            repo.createNote(title, body, key, new SecureNotesRepository.Callback<com.google.firebase.firestore.DocumentReference>() {
                @Override
                public void onSuccess(com.google.firebase.firestore.DocumentReference value) {
                    setLoading(false);
                    NavHostFragment.findNavController(EditSecureNoteFragment.this).popBackStack();
                }

                @Override
                public void onError(Exception e) {
                    setLoading(false);
                    showError(e.getMessage());
                }
            });
        } else {
            repo.updateNote(noteId, title, body, key, new SecureNotesRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void value) {
                    setLoading(false);
                    NavHostFragment.findNavController(EditSecureNoteFragment.this).popBackStack();
                }

                @Override
                public void onError(Exception e) {
                    setLoading(false);
                    showError(e.getMessage());
                }
            });
        }
    }

    private void delete() {
        if (noteId == null) return;

        setLoading(true);
        repo.deleteNote(noteId, new SecureNotesRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                setLoading(false);
                NavHostFragment.findNavController(EditSecureNoteFragment.this).popBackStack();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                showError(e.getMessage());
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnDelete.setEnabled(!loading);
    }

    private void showError(String msg) {
        tvError.setText(msg == null ? "Something went wrong." : msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
