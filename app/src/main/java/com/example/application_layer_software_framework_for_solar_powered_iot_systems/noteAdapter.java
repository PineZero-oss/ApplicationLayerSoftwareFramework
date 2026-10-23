package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


class Note{
    private String title;
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Note(String title, String description) {
        this.title = title;
        this.description = description;
    }

    // Added fields & constructor for SQLite / Firestore note tracking
    private String noteId;
    private String userId;

    public Note(String noteId, String userId, String title, String description) {
        this.noteId = noteId;
        this.userId = userId;
        this.title = title;
        this.description = description;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
public class noteAdapter extends RecyclerView.Adapter<noteAdapter.ViewHolder> {

    private final List<Note> notes;


    public noteAdapter(List<Note> notes) {
        this.notes = notes;
    }

    @NonNull
    @Override
    public noteAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_items, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull noteAdapter.ViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.noteTitle.setText(note.getTitle());
        holder.noteDescription.setText(note.getDescription());

    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView noteTitle;
        public TextView noteDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.noteTitle);
            noteDescription = itemView.findViewById(R.id.noteDescription);

            ImageButton editNoteBtn = itemView.findViewById(R.id.editNoteBtn);

            //implementation of edit note button
            editNoteBtn.setOnClickListener(v -> {
                Fragment editNoteFragment = new editNote();

                int position = getAdapterPosition();
                noteAdapter adapter = findParentAdapter(itemView);
                if (position != RecyclerView.NO_POSITION && adapter != null) {
                    Note selectedNote = adapter.notes.get(position);
                    android.os.Bundle args = new android.os.Bundle();
                    args.putString("note_id", selectedNote.getNoteId());
                    args.putString("note_title", selectedNote.getTitle());
                    args.putString("note_description", selectedNote.getDescription());
                    editNoteFragment.setArguments(args);
                }

                FragmentActivity activity = (FragmentActivity) itemView.getContext();
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, editNoteFragment)
                        .addToBackStack(null)
                        .commit();
            });

            // Added implementation of remove (delete) note button
            ImageButton removeNoteBtn = itemView.findViewById(R.id.removeNoteBtn);
            if (removeNoteBtn != null) {
                removeNoteBtn.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    noteAdapter adapter = findParentAdapter(itemView);
                    if (position != RecyclerView.NO_POSITION && adapter != null) {
                        Note targetNote = adapter.notes.get(position);
                        if (targetNote.getNoteId() != null) {
                            DatabaseHelper dbHelper = new DatabaseHelper(itemView.getContext());
                            dbHelper.markNoteForDeletion(targetNote.getNoteId());
                        }
                        adapter.notes.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, adapter.notes.size());

                        android.widget.Toast.makeText(itemView.getContext(), "Note deleted", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }

        private static noteAdapter findParentAdapter(View itemView) {
            ViewParent parent = itemView.getParent();
            while (parent != null && !(parent instanceof RecyclerView)) {
                parent = parent.getParent();
            }
            if (parent instanceof RecyclerView) {
                RecyclerView rv = (RecyclerView) parent;
                if (rv.getAdapter() instanceof noteAdapter) {
                    return (noteAdapter) rv.getAdapter();
                }
            }
            return null;
        }
    }
}