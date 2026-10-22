package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlertDetail extends Fragment {

    private AlertCase alertCase;
    private CaseStateStore caseStateStore;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_alert_detail,
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

        String eventId = null;

        if (getArguments() != null) {
            eventId = getArguments()
                    .getString("event_id");
        }

        alertCase = loadCaseFromJson(eventId);

        TextView backButton =
                view.findViewById(R.id.back_to_cases);

        Button openCaseButton =
                view.findViewById(R.id.open_case_button);

        TextView caseId =
                view.findViewById(R.id.detail_case_id);

        TextView status =
                view.findViewById(R.id.detail_status);

        TextView title =
                view.findViewById(R.id.detail_title);

        TextView device =
                view.findViewById(R.id.detail_device);

        TextView caseStatus =
                view.findViewById(R.id.detail_case_status);

        TextView created =
                view.findViewById(R.id.detail_created);

        TextView severity =
                view.findViewById(R.id.detail_severity);

        TextView syncStatus =
                view.findViewById(R.id.detail_sync_status);

        TextView assignedTo =
                view.findViewById(R.id.detail_assigned_to);

        TextView message =
                view.findViewById(R.id.detail_message);

        caseId.setText(
                safeValue(
                        alertCase.getCaseId(),
                        "No case ID"
                )
        );

        status.setText(
                formatStatus(
                        alertCase.getCaseStatus()
                )
        );

        title.setText(
                safeValue(
                        alertCase.getTitle(),
                        "Alert"
                )
        );

        device.setText(
                safeValue(
                        alertCase.getDeviceDisplayName(),
                        alertCase.getDeviceId()
                )
        );

        caseStatus.setText(
                formatStatus(
                        alertCase.getCaseStatus()
                )
        );

        created.setText(
                safeValue(
                        alertCase.getOccurredAt(),
                        "Unknown"
                )
        );

        syncStatus.setText(
                safeValue(
                        alertCase.getSyncStatus(),
                        "Not synced"
                )
        );

        assignedTo.setText(
                safeValue(
                        alertCase.getAssignedTo(),
                        "Unassigned"
                )
        );

        message.setText(
                safeValue(
                        alertCase.getMessage(),
                        "No description available."
                )
        );

        styleSeverity(
                severity,
                alertCase.getSeverity()
        );

        backButton.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );

        openCaseButton.setOnClickListener(v ->
                openCase()
        );
    }

    private void openCase() {
        String username = "Kagiso M";

        alertCase.setCaseStatus("IN_PROGRESS");
        alertCase.setAssignedTo(username);

        caseStateStore.saveCase(
                alertCase.getEventId(),
                "IN_PROGRESS",
                username,
                alertCase.getNotes(),
                "SYNCED"
        );

        Bundle bundle = new Bundle();

        bundle.putString(
                "event_id",
                alertCase.getEventId()
        );

        CaseDetailNotes notesFragment =
                new CaseDetailNotes();

        notesFragment.setArguments(bundle);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        notesFragment
                )
                .addToBackStack("case_detail_notes")
                .commit();
    }

    private void styleSeverity(
            TextView severityView,
            String severity
    ) {
        if (severity == null) {
            severity = "LOW";
        }

        severityView.setText(
                capitalize(severity)
        );

        if ("HIGH".equalsIgnoreCase(severity)) {
            severityView.setBackgroundResource(
                    R.drawable.bg_severity_high
            );
            severityView.setTextColor(
                    getResources().getColor(
                            android.R.color.holo_red_dark
                    )
            );
        } else {
            severityView.setBackgroundResource(
                    R.drawable.bg_severity_low
            );
        }
    }

    private String formatStatus(String status) {
        if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            return "In Progress...";
        }

        if ("RESOLVED".equalsIgnoreCase(status)) {
            return "Resolved";
        }

        return "Active";
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        return value.substring(0, 1).toUpperCase()
                + value.substring(1).toLowerCase();
    }

    private String safeValue(
            String value,
            String fallback
    ) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value;
    }

    private AlertCase loadCaseFromJson(String eventId) {
        String json =
                readAssetFile("security_events.json");

        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            JSONArray jsonArray =
                    new JSONArray(json);

            for (int i = 0;
                 i < jsonArray.length();
                 i++) {

                JSONObject object =
                        jsonArray.getJSONObject(i);

                String currentEventId =
                        object.optString(
                                "eventId",
                                ""
                        );

                if (currentEventId.equals(eventId)) {
                    return convertJsonObjectToCase(
                            object
                    );
                }
            }

        } catch (JSONException e) {
            Log.e(
                    "AlertDetail",
                    "Could not parse security_events.json",
                    e
            );
        }

        return null;
    }

    private AlertCase convertJsonObjectToCase(
            JSONObject object
    ) {
        AlertCase alertCase =
                new AlertCase();

        alertCase.setEventId(
                object.optString("eventId", "")
        );

        alertCase.setEventType(
                object.optString("eventType", "")
        );

        alertCase.setDeviceId(
                object.optString("deviceId", "")
        );

        alertCase.setDeviceDisplayName(
                object.optString(
                        "deviceDisplayName",
                        ""
                )
        );

        alertCase.setSeverity(
                object.optString(
                        "severity",
                        "LOW"
                )
        );

        alertCase.setTitle(
                object.optString(
                        "title",
                        "Alert"
                )
        );

        alertCase.setMessage(
                object.optString(
                        "message",
                        ""
                )
        );

        alertCase.setOccurredAt(
                object.optString(
                        "occurredAt",
                        ""
                )
        );

        alertCase.setCaseId(
                readNullableString(
                        object,
                        "caseId"
                )
        );

        alertCase.setDataSource(
                object.optString(
                        "dataSource",
                        ""
                )
        );

        alertCase.setTechnicalDescription(
                object.optString(
                        "technicalDescription",
                        ""
                )
        );

        alertCase.setAction(
                object.optString(
                        "action",
                        ""
                )
        );

        alertCase.setCaseStatus("ACTIVE");
        alertCase.setSyncStatus("Synced");
        alertCase.setAssignedTo("Unassigned");
        alertCase.setNotes("");

        return alertCase;
    }

    private String readAssetFile(String fileName) {
        try (InputStream inputStream =
                     requireContext()
                             .getAssets()
                             .open(fileName)) {

            byte[] bytes =
                    new byte[inputStream.available()];

            int bytesRead =
                    inputStream.read(bytes);

            if (bytesRead <= 0) {
                return "";
            }

            return new String(
                    bytes,
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {
            Log.e(
                    "AlertDetail",
                    "Could not read " + fileName,
                    e
            );

            return null;
        }
    }

    private String readNullableString(
            JSONObject object,
            String key
    ) {
        if (!object.has(key)
                || object.isNull(key)) {
            return null;
        }

        return object.optString(key, null);
    }
}