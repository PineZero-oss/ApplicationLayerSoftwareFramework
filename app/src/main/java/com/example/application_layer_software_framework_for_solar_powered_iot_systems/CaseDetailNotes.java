package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CaseDetailNotes extends Fragment {

    private String eventId;
    private CaseStateStore caseStateStore;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_case_detail_notes,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        caseStateStore =
                new CaseStateStore(requireContext());

        if (getArguments() != null) {
            eventId = getArguments()
                    .getString("event_id");
        }

        if (eventId == null) {
            eventId = "demo-event-1";
        }

        TextView backButton =
                view.findViewById(
                        R.id.notes_back_to_cases
                );

        TextView caseId =
                view.findViewById(
                        R.id.notes_case_id
                );

        TextView status =
                view.findViewById(
                        R.id.notes_status
                );

        EditText notesInput =
                view.findViewById(
                        R.id.investigation_notes_input
                );

        Button saveButton =
                view.findViewById(
                        R.id.save_notes_button
                );

        Button closeButton =
                view.findViewById(
                        R.id.close_case_button
                );

        caseId.setText("CASE-1048");

        status.setText(
                caseStateStore.getStatus(
                        eventId,
                        "IN_PROGRESS"
                )
        );

        notesInput.setText(
                caseStateStore.getNotes(eventId)
        );

        backButton.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );

        saveButton.setOnClickListener(v -> {
            String notes = notesInput
                    .getText()
                    .toString()
                    .trim();

            if (notes.isEmpty()) {
                notesInput.setError(
                        "Enter investigation notes"
                );
                return;
            }

            caseStateStore.saveCase(
                    eventId,
                    "IN_PROGRESS",
                    "Kagiso M",
                    notes,
                    "SYNCED"
            );

            status.setText("In Progress...");

            Toast.makeText(
                    requireContext(),
                    "Notes saved",
                    Toast.LENGTH_SHORT
            ).show();
        });

        closeButton.setOnClickListener(v -> {
            String notes = notesInput
                    .getText()
                    .toString()
                    .trim();

            caseStateStore.saveCase(
                    eventId,
                    "RESOLVED",
                    "Kagiso M",
                    notes,
                    "SYNCED"
            );

            Toast.makeText(
                    requireContext(),
                    "Case closed",
                    Toast.LENGTH_SHORT
            ).show();

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(
                            R.id.fragment_container,
                            new Alerts()
                    )
                    .commit();
        });
    }
}