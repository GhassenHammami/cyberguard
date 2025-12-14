package com.example.cyberguard.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cyberguard.R;
import com.example.cyberguard.quiz.QuizStatsRepository;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CyberQuizFragment extends Fragment {

    private TextView tvProgress, tvStats, tvCategory, tvQuestion, tvFeedbackTitle, tvFeedbackExplain;
    private ProgressBar progressBar;
    private RecyclerView rvOptions;
    private View feedbackCard;
    private Button btnNext, btnRestart;

    private QuizStatsRepository statsRepo;
    private int bestScore = 0;
    private int lastScore = 0;
    private long attempts = 0;

    private final List<Question> questionPool = new ArrayList<>();
    private final List<Question> runQuestions = new ArrayList<>();

    private int index = 0;
    private int score = 0;
    private boolean answered = false;

    private OptionsAdapter adapter;

    private static final int RUN_SIZE = 5;

    private static class Question {
        String id;
        String category;
        String question;
        List<String> options;
        int answerIndex;
        String explanation;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_cyber_quiz, container, false);

        tvProgress = root.findViewById(R.id.cq_tv_progress);
        tvStats = root.findViewById(R.id.cq_tv_stats);
        tvCategory = root.findViewById(R.id.cq_tv_category);
        tvQuestion = root.findViewById(R.id.cq_tv_question);
        progressBar = root.findViewById(R.id.cq_progress_bar);

        rvOptions = root.findViewById(R.id.cq_rv_options);
        rvOptions.setLayoutManager(new LinearLayoutManager(getContext()));

        feedbackCard = root.findViewById(R.id.cq_card_feedback);
        tvFeedbackTitle = root.findViewById(R.id.cq_tv_feedback_title);
        tvFeedbackExplain = root.findViewById(R.id.cq_tv_feedback_explain);

        btnNext = root.findViewById(R.id.cq_btn_next);
        btnRestart = root.findViewById(R.id.cq_btn_restart);

        adapter = new OptionsAdapter(this::onOptionSelected);
        rvOptions.setAdapter(adapter);

        btnNext.setOnClickListener(v -> next());
        btnRestart.setOnClickListener(v -> restart());

        statsRepo = new QuizStatsRepository();
        loadFirebaseStats();

        loadQuestionsFromRaw();
        restart();

        return root;
    }


    private void loadFirebaseStats() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            tvStats.setText("Best: — • Last: — • Attempts: — (login required)");
            return;
        }

        statsRepo.loadStats(new QuizStatsRepository.Callback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                Object b = data.get("quizBestScore");
                Object l = data.get("quizLastScore");
                Object a = data.get("quizAttempts");

                bestScore = (b instanceof Number) ? ((Number) b).intValue() : 0;
                lastScore = (l instanceof Number) ? ((Number) l).intValue() : 0;
                attempts = (a instanceof Number) ? ((Number) a).longValue() : 0;

                updateStatsText();
            }

            @Override
            public void onError(Exception e) {
                tvStats.setText("Best: — • Last: — • Attempts: — (failed to load)");
            }
        });
    }

    private void updateStatsText() {
        tvStats.setText("Best: " + bestScore + " • Last: " + lastScore + " • Attempts: " + attempts);
    }

    private void loadQuestionsFromRaw() {
        questionPool.clear();
        try {
            InputStream is = requireContext().getResources().openRawResource(R.raw.cyber_quiz_questions);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Question q = new Question();
                q.id = o.optString("id");
                q.category = o.optString("category");
                q.question = o.optString("question");
                q.answerIndex = o.optInt("answerIndex", 0);
                q.explanation = o.optString("explanation");

                JSONArray opts = o.getJSONArray("options");
                q.options = new ArrayList<>();
                for (int j = 0; j < opts.length(); j++) q.options.add(opts.getString(j));

                if (q.options.size() >= 2 && q.answerIndex >= 0 && q.answerIndex < q.options.size()) {
                    questionPool.add(q);
                }
            }
        } catch (Exception ignored) {
        }
    }


    private void restart() {
        score = 0;
        index = 0;
        answered = false;

        runQuestions.clear();

        if (questionPool.isEmpty()) {
            showEmptyState();
            return;
        }

        List<Question> tmp = new ArrayList<>(questionPool);
        Collections.shuffle(tmp);

        int take = Math.min(RUN_SIZE, tmp.size());
        for (int i = 0; i < take; i++) runQuestions.add(tmp.get(i));

        showCurrent();
    }

    private void showEmptyState() {
        tvCategory.setText("Setup needed");
        tvQuestion.setText("No questions loaded. Add them to res/raw/cyber_quiz_questions.json");
        tvProgress.setText("—");
        progressBar.setProgress(0);
        adapter.setOptions(new ArrayList<>(), -1, false);
        btnNext.setEnabled(false);
        feedbackCard.setVisibility(View.GONE);
    }

    private void showCurrent() {
        if (runQuestions.isEmpty()) {
            showEmptyState();
            return;
        }

        if (index < 0) index = 0;
        if (index >= runQuestions.size()) index = runQuestions.size() - 1;

        Question q = runQuestions.get(index);
        answered = false;

        tvCategory.setText(q.category == null ? "" : q.category);
        tvQuestion.setText(q.question);

        int total = runQuestions.size();
        int shownIndex = index + 1;

        tvProgress.setText("Question " + shownIndex + "/" + total + " • Score " + score);
        int pct = (int) ((shownIndex * 100.0f) / total);
        progressBar.setProgress(pct);

        feedbackCard.setVisibility(View.GONE);
        btnNext.setEnabled(false);

        adapter.setOptions(q.options, -1, false);
    }

    private void onOptionSelected(int selectedIndex) {
        if (answered) return;

        Question q = runQuestions.get(index);
        answered = true;

        boolean correct = selectedIndex == q.answerIndex;
        if (correct) score++;

        feedbackCard.setVisibility(View.VISIBLE);
        tvFeedbackTitle.setText(correct ? "Correct ✅" : "Incorrect ❌");

        String correctAnswer = q.options.get(q.answerIndex);
        String explain = (q.explanation == null || q.explanation.trim().isEmpty())
                ? ("Correct answer: " + correctAnswer)
                : (q.explanation + "\n\nCorrect answer: " + correctAnswer);

        tvFeedbackExplain.setText(explain);

        adapter.setOptions(q.options, selectedIndex, true);
        btnNext.setEnabled(true);
    }

    private void next() {
        int total = runQuestions.size();

        if (index + 1 >= total) {
            feedbackCard.setVisibility(View.VISIBLE);
            tvFeedbackTitle.setText("Quiz completed 🎉");
            tvFeedbackExplain.setText("Final score: " + score + "/" + total + "\n\nTap Restart for another 5 questions.");

            btnNext.setEnabled(false);
            adapter.setOptions(new ArrayList<>(), -1, false);

            saveScoreToFirebase(score, total);
            return;
        }

        index++;
        showCurrent();
    }

    private void saveScoreToFirebase(int score, int total) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        statsRepo.recordAttempt(score, total, new QuizStatsRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                lastScore = score;
                attempts += 1;
                if (score > bestScore) bestScore = score;
                updateStatsText();
            }

            @Override
            public void onError(Exception ignored) {
            }
        });
    }

    private static class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.VH> {

        interface OnClick { void onClick(int index); }

        private final OnClick onClick;
        private List<String> options = new ArrayList<>();
        private int selectedIndex = -1;
        private boolean showResult = false;

        OptionsAdapter(OnClick onClick) {
            this.onClick = onClick;
        }

        void setOptions(List<String> options, int selectedIndex, boolean showResult) {
            this.options = (options == null) ? new ArrayList<>() : options;
            this.selectedIndex = selectedIndex;
            this.showResult = showResult;
            notifyDataSetChanged();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView text;
            VH(@NonNull View itemView) {
                super(itemView);
                text = itemView.findViewById(R.id.cq_option_text);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_option, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String opt = options.get(position);

            if (showResult && position == selectedIndex) holder.text.setText(opt + "  ← your choice");
            else holder.text.setText(opt);

            holder.itemView.setOnClickListener(v -> onClick.onClick(position));
        }

        @Override
        public int getItemCount() {
            return options.size();
        }
    }
}
