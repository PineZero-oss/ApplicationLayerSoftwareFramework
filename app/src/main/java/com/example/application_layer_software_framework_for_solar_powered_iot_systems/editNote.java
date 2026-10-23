package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link editNote#newInstance} factory method to
 * create an instance of this fragment.
 */
public class editNote extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public editNote() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment editNote.
     */
    // TODO: Rename and change types and number of parameters
    public static editNote newInstance(String param1, String param2) {
        editNote fragment = new editNote();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_note, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        android.widget.EditText titleTxt = view.findViewById(R.id.titleTxt);
        android.widget.EditText descriptionTxt = view.findViewById(R.id.descriptionTxt);
        android.widget.Button btnSaveEdit = view.findViewById(R.id.btnAddNote);

        String noteId = null;
        if (getArguments() != null) {
            noteId = getArguments().getString("note_id", null);
            titleTxt.setText(getArguments().getString("note_title", ""));
            descriptionTxt.setText(getArguments().getString("note_description", ""));
        }

        final String finalNoteId = noteId;
        btnSaveEdit.setOnClickListener(v -> {
            String updatedTitle = titleTxt.getText().toString().trim();
            String updatedDescription = descriptionTxt.getText().toString().trim();

            if (updatedTitle.isEmpty()) {
                titleTxt.setError("Please enter a note title");
                titleTxt.requestFocus();
                return;
            }

            if (updatedDescription.isEmpty()) {
                descriptionTxt.setError("Please enter a note description");
                descriptionTxt.requestFocus();
                return;
            }

            if (finalNoteId != null) {
                DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
                dbHelper.updateNote(finalNoteId, updatedTitle, updatedDescription);
            }

            android.widget.Toast.makeText(requireContext(), "Note updated", android.widget.Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        });
    }
}