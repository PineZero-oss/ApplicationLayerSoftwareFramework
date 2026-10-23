package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.util.List;
import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link addNote#newInstance} factory method to
 * create an instance of this fragment.
 */
public class addNote extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Button btnAddNote;
    private EditText titleTxt;
    private EditText descriptionTxt;

    public addNote() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment addNote.
     */
    // TODO: Rename and change types and number of parameters
    public static addNote newInstance(String param1, String param2) {
        addNote fragment = new addNote();
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
        return inflater.inflate(R.layout.fragment_add_note, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleTxt = view.findViewById(R.id.titleTxt);
        descriptionTxt = view.findViewById(R.id.descriptionTxt);
        btnAddNote = view.findViewById(R.id.btnAddNote);

        btnAddNote.setOnClickListener(v -> {
            String title = titleTxt.getText().toString().trim();
            String description = descriptionTxt.getText().toString().trim();

            if (title.isEmpty()) {
                titleTxt.setError("Please enter a note title");
                titleTxt.requestFocus();
                return;
            }

            if (description.isEmpty()) {
                descriptionTxt.setError("Please enter a note description");
                descriptionTxt.requestFocus();
                return;
            }

            DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
            String noteId = UUID.randomUUID().toString();
            String userId = dbHelper.getCurrentUserId(requireContext());
            String authorEmail = dbHelper.getCurrentUserEmail(requireContext());

            dbHelper.insertNote(noteId, userId, authorEmail, title, description);

            android.widget.Toast.makeText(requireContext(), "Note saved", android.widget.Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        });
    }

    public static List<Note> loadNotes(android.content.Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        return dbHelper.getVisibleNotesForCurrentUser(context);
    }
}