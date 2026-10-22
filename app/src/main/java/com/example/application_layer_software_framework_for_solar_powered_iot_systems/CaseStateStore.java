package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.content.Context;
import android.content.SharedPreferences;

public class CaseStateStore {

    private static final String PREFS_NAME = "case_states";

    private final SharedPreferences preferences;

    public CaseStateStore(Context context) {
        preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void saveCase(
            String eventId,
            String status,
            String assignedTo,
            String notes,
            String syncStatus
    ) {
        preferences.edit()
                .putString(eventId + "_status", status)
                .putString(eventId + "_assigned_to", assignedTo)
                .putString(eventId + "_notes", notes)
                .putString(eventId + "_sync_status", syncStatus)
                .apply();
    }

    public String getStatus(
            String eventId,
            String fallback
    ) {
        return preferences.getString(
                eventId + "_status",
                fallback
        );
    }

    public String getAssignedTo(
            String eventId,
            String fallback
    ) {
        return preferences.getString(
                eventId + "_assigned_to",
                fallback
        );
    }

    public String getNotes(String eventId) {
        return preferences.getString(
                eventId + "_notes",
                ""
        );
    }

    public String getSyncStatus(
            String eventId,
            String fallback
    ) {
        return preferences.getString(
                eventId + "_sync_status",
                fallback
        );
    }
}