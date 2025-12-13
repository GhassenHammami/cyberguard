package com.example.cyberguard.ui.secure_notes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cyberguard.R;
import com.example.cyberguard.secure.SecureNotesRepository;
import com.example.cyberguard.secure.SecureNotesSessionViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SecureNotesFragment extends Fragment {

    private RecyclerView rv;
    private ProgressBar progress;
    private TextView tvEmpty;
    private FloatingActionButton fab;

    private SecureNotesRepository repo;
    private SecureNotesSessionViewModel session;
    private ListenerRegistration reg;

    private NotesAdapter adapter;

    static class NoteRow {
        String id;
        String title;
        Date updatedAt;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_secure_notes, container, false);

        rv = root.findViewById(R.id.sn_rv);
        progress = root.findViewById(R.id.sn_list_progress);
        tvEmpty = root.findViewById(R.id.sn_tv_empty);
        fab = root.findViewById(R.id.sn_fab_add);

        repo = new SecureNotesRepository();
        session = new ViewModelProvider(requireActivity()).get(SecureNotesSessionViewModel.class);

        adapter = new NotesAdapter(new ArrayList<>(), row -> openEditor(row.id));
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        fab.setOnClickListener(v -> openEditor(null));

        // If user enters directly without unlocking
        if (!session.isUnlocked()) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_secureNotesFragment_to_unlockSecureNotesFragment);
            return root;
        }

        listenNotes();
        return root;
    }

    private void listenNotes() {
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        reg = repo.listenNotes((QuerySnapshot snaps, com.google.firebase.firestore.FirebaseFirestoreException e) -> {
            progress.setVisibility(View.GONE);

            if (e != null || snaps == null) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Failed to load notes.");
                return;
            }

            List<NoteRow> rows = new ArrayList<>();
            for (DocumentSnapshot doc : snaps.getDocuments()) {
                NoteRow r = new NoteRow();
                r.id = doc.getId();
                r.title = doc.getString("title");
                if (doc.getTimestamp("updatedAt") != null) {
                    r.updatedAt = doc.getTimestamp("updatedAt").toDate();
                }
                rows.add(r);
            }

            adapter.setItems(rows);
            tvEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void openEditor(@Nullable String noteId) {
        Bundle args = new Bundle();
        if (noteId != null) args.putString("noteId", noteId);
        NavHostFragment.findNavController(this).navigate(R.id.action_secureNotesFragment_to_editSecureNoteFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) reg.remove();
    }

    // ---------------- Adapter ----------------
    static class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.VH> {

        interface OnClick { void onClick(NoteRow row); }

        private final List<NoteRow> items;
        private final OnClick onClick;

        NotesAdapter(List<NoteRow> items, OnClick onClick) {
            this.items = items;
            this.onClick = onClick;
        }

        void setItems(List<NoteRow> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, date;
            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.sn_item_title);
                date = itemView.findViewById(R.id.sn_item_date);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_secure_note, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            NoteRow row = items.get(position);
            holder.title.setText(row.title == null || row.title.trim().isEmpty() ? "(Untitled)" : row.title);

            if (row.updatedAt != null) {
                holder.date.setText("Updated: " + DateFormat.getDateTimeInstance().format(row.updatedAt));
            } else {
                holder.date.setText("Updated: -");
            }

            holder.itemView.setOnClickListener(v -> onClick.onClick(row));
        }

        @Override
        public int getItemCount() { return items.size(); }
    }
}
