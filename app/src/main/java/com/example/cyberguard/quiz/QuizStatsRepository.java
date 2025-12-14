package com.example.cyberguard.quiz;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class QuizStatsRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(Exception e);
    }

    private DocumentReference userDoc() {
        return db.collection("users").document(auth.getCurrentUser().getUid());
    }

    public void loadStats(Callback<Map<String, Object>> cb) {
        userDoc().get()
                .addOnSuccessListener(snap -> cb.onSuccess(snap.getData() == null ? new HashMap<>() : snap.getData()))
                .addOnFailureListener(cb::onError);
    }

    public void recordAttempt(int score, int total, Callback<Void> cb) {
        DocumentReference user = userDoc();
        CollectionReference history = user.collection("quiz_attempts");

        db.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(user);

                    long best = snap.contains("quizBestScore") ? (snap.getLong("quizBestScore") == null ? 0 : snap.getLong("quizBestScore")) : 0;
                    long attempts = snap.contains("quizAttempts") ? (snap.getLong("quizAttempts") == null ? 0 : snap.getLong("quizAttempts")) : 0;

                    boolean isNewBest = score > best;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("quizLastScore", score);
                    updates.put("quizLastAt", FieldValue.serverTimestamp());
                    updates.put("quizAttempts", attempts + 1);

                    if (isNewBest) {
                        updates.put("quizBestScore", score);
                        updates.put("quizBestAt", FieldValue.serverTimestamp());
                    }

                    transaction.set(user, updates, SetOptions.merge());

                    DocumentReference attemptDoc = history.document();
                    Map<String, Object> attemptData = new HashMap<>();
                    attemptData.put("score", score);
                    attemptData.put("total", total);
                    attemptData.put("createdAt", FieldValue.serverTimestamp());
                    transaction.set(attemptDoc, attemptData);

                    return null;
                }).addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }
}
