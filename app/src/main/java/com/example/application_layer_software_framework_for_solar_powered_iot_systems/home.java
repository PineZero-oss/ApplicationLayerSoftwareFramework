package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class home extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public static class Reading {
        private String deviceId;
        private String timestamp;
        private float powerKw;

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public float getPowerKw() { return powerKw; }
        public void setPowerKw(float powerKw) { this.powerKw = powerKw; }
    }

    public home() {
        // Required empty public constructor
    }

    public static home newInstance(String param1, String param2) {
        home fragment = new home();
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
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button addNoteBtn = view.findViewById(R.id.btnAddNote);
        ImageButton editNoteBtn = view.findViewById(R.id.editNoteBtn);
        // Set up RecyclerView and adapter
        RecyclerView notesRecyclerView = view.findViewById(R.id.notes_recyclerview);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        notesRecyclerView.setNestedScrollingEnabled(false);

        // Load only user-saved notes
        List<Note> userNotes = addNote.loadNotes(requireContext());
        noteAdapter adapter = new noteAdapter(userNotes);
        notesRecyclerView.setAdapter(adapter);

        //add note button
        addNoteBtn.setOnClickListener(v -> {
            Fragment addNoteFragment = new addNote();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, addNoteFragment)
                    .addToBackStack(null)
                    .commit();
        });





        drawSummaryChart(view);
    }

    private void drawSummaryChart(View view) {
        // Load readings and limit to 30 entries
        List<Reading> allReadings = loadReadings(requireContext(), "monthlydata.json");
        int readingLimit = Math.min(allReadings.size(), 30);
        List<Reading> readings = allReadings.subList(0, readingLimit);

        List<String> labels = new ArrayList<>();
        ArrayList<Entry> entries = new ArrayList<>();

        // Populate labels and entries with data
        for (int i = 0; i < readings.size(); i++) {
            Reading reading = readings.get(i);
            entries.add(new Entry((float) i, reading.getPowerKw(), null, null));
        }
    }


    public void updatePowerGauge(CircularProgressIndicator powerGauge, TextView watts, TextView device, String deviceID, float powerKw, float maxPower){

        //converting our kilowatts into watts
        float pWatts = powerKw * 1000f;

        int progPercentage = (int)((powerKw / maxPower) * 100f);
        progPercentage = Math.min(Math.max(progPercentage, 0), 100);

        powerGauge.setProgress(progPercentage);
        watts.setText(String.format("%.2f W", pWatts));
        device.setText(deviceID);

    }

    private String loadJSONFromAsset(Context context, String fileName) {
        try (InputStream is = context.getAssets().open(fileName)) {
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            return new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Reading> loadReadings(Context context, String fileName) {
        List<Reading> readings = new ArrayList<>();
        String json = loadJSONFromAsset(context, fileName);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    Reading reading = new Reading();
                    reading.setDeviceId(obj.optString("deviceId"));
                    reading.setTimestamp(obj.optString("timestamp"));
                    reading.setPowerKw((float) obj.optDouble("powerKw", 0.0));
                    readings.add(reading);
                }

                if(jsonArray.length() > 0) {
                    JSONObject lastestjsonItem = jsonArray.getJSONObject(jsonArray.length() - 1);

                    String deviceId = lastestjsonItem.optString("deviceId");
                    float powerKw = (float) lastestjsonItem.optDouble("powerKw", 0.0);

                    CircularProgressIndicator progBar = getView().findViewById(R.id.progress_bar);
                    TextView pDeviceID = getView().findViewById(R.id.p_device_id);
                    TextView powerGenerationValue = getView().findViewById(R.id.power_generation_value);

                    updatePowerGauge(progBar, powerGenerationValue, pDeviceID, deviceId, powerKw, 5.0f);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return readings;
    }
}