package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Alerts extends Fragment
        implements AlertAdapter.OnAlertClickListener {

    private AlertAdapter adapter;

    private TextView filterAll;
    private TextView filterCritical;
    private TextView filterHigh;
    private TextView filterMedium;
    private TextView filterLow;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_alerts,
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

        RecyclerView recyclerView =
                view.findViewById(
                        R.id.alerts_recycler_view
                );

        filterAll =
                view.findViewById(R.id.filter_all);

        filterCritical =
                view.findViewById(R.id.filter_critical);

        filterHigh =
                view.findViewById(R.id.filter_high);

        filterMedium =
                view.findViewById(R.id.filter_medium);

        filterLow =
                view.findViewById(R.id.filter_low);

        /*
         * Configure the RecyclerView once.
         */
        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        requireContext()
                )
        );

        recyclerView.setNestedScrollingEnabled(true);
        recyclerView.setHasFixedSize(false);

        /*
         * Load the real JSON data.
         */
        List<AlertCase> cases =
                loadCasesFromJson();


        if (cases.isEmpty()) {
            AlertCase testCase =
                    new AlertCase();

            testCase.setEventId("test-event");
            testCase.setCaseId("TEST-001");
            testCase.setCaseStatus("ACTIVE");
            testCase.setTitle("RecyclerView Test Card");
            testCase.setDeviceDisplayName("Test Device");
            testCase.setMessage(
                    "If this card appears, the RecyclerView and item XML work."
            );
            testCase.setSeverity("HIGH");
            testCase.setDataSource("TEST");
            testCase.setOccurredAt("Now");

            cases.add(testCase);
        }



        adapter = new AlertAdapter(
                cases,
                this
        );

        recyclerView.setAdapter(adapter);

        /*

         * the adapter initially contains all cases.
         */
        filterAll.setSelected(true);

        filterAll.setOnClickListener(v ->
                selectFilter(
                        filterAll,
                        "ALL"
                )
        );

        filterCritical.setOnClickListener(v ->
                selectFilter(
                        filterCritical,
                        "CRITICAL"
                )
        );

        filterHigh.setOnClickListener(v ->
                selectFilter(
                        filterHigh,
                        "HIGH"
                )
        );

        filterMedium.setOnClickListener(v ->
                selectFilter(
                        filterMedium,
                        "MEDIUM"
                )
        );

        filterLow.setOnClickListener(v ->
                selectFilter(
                        filterLow,
                        "LOW"
                )
        );

        Log.d(
                "Alerts",
                "RecyclerView adapter count: "
                        + adapter.getItemCount()
        );
    }

    private void selectFilter(
            TextView selectedView,
            String severity
    ) {
        adapter.filterBySeverity(severity);

        filterAll.setSelected(false);
        filterCritical.setSelected(false);
        filterHigh.setSelected(false);
        filterMedium.setSelected(false);
        filterLow.setSelected(false);

        selectedView.setSelected(true);
    }

    @Override
    public void onAlertClicked(
            AlertCase alertCase
    ) {
        Bundle bundle =
                new Bundle();

        bundle.putString(
                "event_id",
                alertCase.getEventId()
        );

        AlertDetail detailFragment =
                new AlertDetail();

        detailFragment.setArguments(bundle);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        detailFragment
                )
                .addToBackStack("alert_detail")
                .commit();
    }

    private List<AlertCase> loadCasesFromJson() {
        List<AlertCase> cases =
                new ArrayList<>();

        String json =
                readAssetFile("security_events.json");

        if (json == null
                || json.trim().isEmpty()) {

            Log.e(
                    "Alerts",
                    "JSON file is empty or could not be read"
            );

            return cases;
        }

        json = json.trim();

        try {
            JSONArray jsonArray;

            if (json.startsWith("[")) {
                jsonArray =
                        new JSONArray(json);

            } else if (json.startsWith("{")) {
                JSONObject root =
                        new JSONObject(json);

                if (root.has("events")) {
                    jsonArray =
                            root.getJSONArray("events");

                } else if (root.has("securityEvents")) {
                    jsonArray =
                            root.getJSONArray("securityEvents");

                } else if (root.has("data")) {
                    jsonArray =
                            root.getJSONArray("data");

                } else {
                    /*
                     * If the file contains one event object
                     * instead of an array, treat it as one event.
                     */
                    jsonArray =
                            new JSONArray();

                    jsonArray.put(root);
                }

            } else {
                Log.e(
                        "Alerts",
                        "JSON must start with [ or {"
                );

                return cases;
            }

            Log.d(
                    "Alerts",
                    "JSON event count: "
                            + jsonArray.length()
            );

            for (int i = 0;
                 i < jsonArray.length();
                 i++) {

                JSONObject object =
                        jsonArray.getJSONObject(i);

                AlertCase alertCase =
                        new AlertCase();

                alertCase.setEventId(
                        object.optString(
                                "eventId",
                                ""
                        )
                );

                alertCase.setEventType(
                        object.optString(
                                "eventType",
                                ""
                        )
                );

                alertCase.setDeviceId(
                        object.optString(
                                "deviceId",
                                ""
                        )
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

                cases.add(alertCase);
            }

        } catch (JSONException e) {
            Log.e(
                    "Alerts",
                    "JSON parsing failed",
                    e
            );
        }

        Log.d(
                "Alerts",
                "Cases loaded: "
                        + cases.size()
        );

        return cases;
    }

    private String readNullableString(
            JSONObject object,
            String key
    ) {
        if (!object.has(key)
                || object.isNull(key)) {
            return null;
        }

        return object.optString(
                key,
                null
        );
    }

    private String readAssetFile(
            String fileName
    ) {
        try (InputStream inputStream =
                     requireContext()
                             .getAssets()
                             .open(fileName)) {

            byte[] bytes =
                    new byte[inputStream.available()];

            int bytesRead =
                    inputStream.read(bytes);

            if (bytesRead <= 0) {
                Log.e(
                        "Alerts",
                        "Asset was opened but contains no bytes"
                );

                return "";
            }

            String json =
                    new String(
                            bytes,
                            StandardCharsets.UTF_8
                    );

            Log.d(
                    "Alerts",
                    "Read JSON characters: "
                            + json.length()
            );

            Log.d(
                    "Alerts",
                    "JSON starts with: "
                            + json.trim().substring(
                            0,
                            Math.min(
                                    20,
                                    json.trim().length()
                            )
                    )
            );

            return json;

        } catch (IOException e) {
            Log.e(
                    "Alerts",
                    "Could not open asset: "
                            + fileName,
                    e
            );

            return null;
        }
    }
}