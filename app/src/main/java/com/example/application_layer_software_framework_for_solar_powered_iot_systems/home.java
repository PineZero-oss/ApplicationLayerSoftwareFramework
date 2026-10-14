package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

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
        drawSummaryChart(view);
    }

    private void drawSummaryChart(View view) {
        LineChart summaryChart = view.findViewById(R.id.summaryChart);
        if (summaryChart == null) return;

        // Load readings and limit to 30 entries
        List<Reading> allReadings = loadReadings(requireContext(), "monthlydata.json");
        int readingLimit = Math.min(allReadings.size(), 30);
        List<Reading> readings = allReadings.subList(0, readingLimit);

        // Configuring basic chart properties
        summaryChart.getLegend().setEnabled(false);
        summaryChart.getAxisRight().setEnabled(false);
        summaryChart.getDescription().setEnabled(false);
        summaryChart.setExtraOffsets(12f, 16f, 12f, 8f);

        List<String> labels = new ArrayList<>();
        ArrayList<Entry> entries = new ArrayList<>();


        // Populate labels and entries with data
        for (int i = 0; i < readings.size(); i++) {
            Reading reading = readings.get(i);
            String ts = reading.getTimestamp();
            String formattedTime = (ts != null && ts.length() >= 16) ? ts.substring(11, 16) : (ts != null ? ts : ""); // Extract time part
            labels.add(formattedTime);

            entries.add(new Entry((float) i, reading.getPowerKw(), null, null));
        }

        // Chart X-axis with timestamp labels
        XAxis xAxis = summaryChart.getXAxis();
        xAxis.setDrawGridLinesEnabled(false);
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        // Chart Y-axis
        YAxis ytAxis = summaryChart.getAxisLeft();
        ytAxis.setAxisLineColor(Color.parseColor("#f1f1f1"));

        // Chart data & styling
        LineDataSet dataSet = new LineDataSet(entries, "Power Consumption");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValuesEnabled(false);
        dataSet.setDrawFilledEnabled(true);
        dataSet.setFillDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.summarychart));
        dataSet.setDrawCirclesEnabled(true);
        dataSet.setLineWidth(2.5f);

        LineData lineData = new LineData(dataSet);
        summaryChart.setData(lineData);
        summaryChart.invalidate();
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
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return readings;
    }
}
