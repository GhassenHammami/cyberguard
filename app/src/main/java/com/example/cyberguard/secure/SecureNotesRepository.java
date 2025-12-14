package com.example.cyberguard.secure;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

public class SecureNotesRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(Exception e);
    }

    private DocumentReference userDoc() {
        assert auth.getCurrentUser() != null;
        return db.collection("users").document(auth.getCurrentUser().getUid());
    }

    private CollectionReference notesCol() {
        return userDoc().collection("secure_notes");
    }

    /** Ensure user has a salt stored in Firestore (generated once). */
    public void getOrCreateSalt(Callback<String> cb) {
        userDoc().get().addOnSuccessListener(snap -> {
            String salt = snap.getString("secureNotesSaltB64");
            if (salt != null && !salt.isEmpty()) {
                cb.onSuccess(salt);
                return;
            }
            String newSalt = NotesCrypto.generateSaltB64();
            userDoc().set(
                            new HashMap<String, Object>() {{
                                put("secureNotesSaltB64", newSalt);
                            }},
                            SetOptions.merge()
                    ).addOnSuccessListener(v -> cb.onSuccess(newSalt))
                    .addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }

    /** Create a note (encrypted content) */
    public void createNote(String title, String plainBody, SecretKey key, Callback<DocumentReference> cb) {
        try {
            NotesCrypto.EncResult enc = NotesCrypto.encrypt(plainBody, key);

            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("cipherTextB64", enc.cipherTextB64);
            data.put("ivB64", enc.ivB64);
            data.put("updatedAt", FieldValue.serverTimestamp());

            notesCol().add(data)
                    .addOnSuccessListener(cb::onSuccess)
                    .addOnFailureListener(cb::onError);

        } catch (Exception e) {
            cb.onError(e);
        }
    }

    /** Update a note */
    public void updateNote(String noteId, String title, String plainBody, SecretKey key, Callback<Void> cb) {
        try {
            NotesCrypto.EncResult enc = NotesCrypto.encrypt(plainBody, key);

            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("cipherTextB64", enc.cipherTextB64);
            data.put("ivB64", enc.ivB64);
            data.put("updatedAt", FieldValue.serverTimestamp());

            notesCol().document(noteId).update(data)
                    .addOnSuccessListener(v -> cb.onSuccess(null))
                    .addOnFailureListener(cb::onError);

        } catch (Exception e) {
            cb.onError(e);
        }
    }

    /** Listen to notes list (titles + encrypted blobs) */
    public ListenerRegistration listenNotes(EventListener<QuerySnapshot> listener) {
        return notesCol()
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public void deleteNote(String noteId, Callback<Void> cb) {
        notesCol().document(noteId).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void getVaultCheck(Callback<Map<String, String>> cb) {
        userDoc().get().addOnSuccessListener(snap -> {
            String cipher = snap.getString("secureNotesCheckCipherB64");
            String iv = snap.getString("secureNotesCheckIvB64");

            Map<String, String> res = new HashMap<>();
            res.put("cipher", cipher);
            res.put("iv", iv);
            cb.onSuccess(res);
        }).addOnFailureListener(cb::onError);
    }

    public void setVaultCheck(String cipherB64, String ivB64, Callback<Void> cb) {
        Map<String, Object> data = new HashMap<>();
        data.put("secureNotesCheckCipherB64", cipherB64);
        data.put("secureNotesCheckIvB64", ivB64);

        userDoc().set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }


}
