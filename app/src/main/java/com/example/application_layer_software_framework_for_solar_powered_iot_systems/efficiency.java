package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 - A simple {
@link Fragment} subclass.
 - Use the {
@link efficiency#newInstance} factory method to
 - create an instance of this fragment.

 */
public class efficiency extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public static class Reading {
        private String deviceId;
        private String timestamp;
        private float batteryPercentage, humidity, temperature;

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public float getBatteryPercentage() { return batteryPercentage; }
        public void setBatteryPercentage(float batteryPercentage) { this.batteryPercentage = batteryPercentage; }

        public float getHumidity() { return humidity; }
        public void setHumidity(float humidity) { this.humidity = humidity; }

        public float getTemperature() { return temperature; }
        public void setTemperature(float temperature) { this.temperature = temperature; }
    }

    public efficiency() {
        // Required empty public constructor
    }

    /**
     - Use this factory method to create a new instance of
     - this fragment using the provided parameters.
     *
     -
     @param param1 Parameter 1.
     -
     @param param2 Parameter 2.
     -
     @return A new instance of fragment efficiency.
     */
    // TODO: Rename and change types and number of parameters
    public static efficiency newInstance(String param1, String param2) {
        efficiency fragment = new efficiency();
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
        return inflater.inflate(R.layout.fragment_efficiency, container, false);
    }

    private void drawTemperatureChart(View view) {
        LineChart temperatureChart = view.findViewById(R.id.temperatureChart);
        LineChart humidityChart = view.findViewById(R.id.humidityChart);

        // Load readings from jsonl file
        List<Reading> allReadings = loadReadings(requireContext(), "messages.jsonl");
        if (allReadings.isEmpty()) {
            return;
        }

        int readingLimit = Math.min(allReadings.size(), 30);
        List<Reading> readings = allReadings.subList(0, readingLimit);

        // Update Energy Efficiency card with latest values
        updateEfficiencySummaries(view, allReadings, readings);

        List<String> labels = new ArrayList<>();
        ArrayList<Entry> tempEntries = new ArrayList<>();
        ArrayList<Entry> humidityEntries = new ArrayList<>();

        for (int i = 0; i < readings.size(); i++) {
            Reading reading = readings.get(i);
            String ts = reading.getTimestamp();
            String formattedTime = (ts != null && ts.length() >= 16)
                    ? ts.substring(11, 16)
                    : String.valueOf(i + 1);
            labels.add(formattedTime);

            tempEntries.add(new Entry((float) i, reading.getTemperature(), null, null));
            humidityEntries.add(new Entry((float) i, reading.getHumidity(), null, null));
        }

        // 1. Configure Temperature Chart
        if (temperatureChart != null) {
            temperatureChart.getLegend().setEnabled(false);
            temperatureChart.getAxisRight().setEnabled(false);
            temperatureChart.getDescription().setEnabled(false);
            temperatureChart.setExtraOffsets(12f, 16f, 12f, 8f);

            XAxis xAxis = temperatureChart.getXAxis();
            xAxis.setDrawGridLinesEnabled(false);
            xAxis.setGranularity(1f);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextColor(Color.parseColor("#1E3A8A"));
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

            YAxis yAxis = temperatureChart.getAxisLeft();
            yAxis.setAxisLineColor(Color.parseColor("#E2E8F0"));
            yAxis.setGridColor(Color.parseColor("#E2E8F0"));
            yAxis.setTextColor(Color.parseColor("#1E3A8A"));

            LineDataSet tempDataSet = new LineDataSet(tempEntries, "Temperature (°C)");
            tempDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            tempDataSet.setDrawValuesEnabled(false);
            tempDataSet.setDrawFilledEnabled(true);
            tempDataSet.setDrawCirclesEnabled(true);
            tempDataSet.setColor(Color.parseColor("#EA580C"));
            tempDataSet.setCircleColor(Color.parseColor("#EA580C"));
            tempDataSet.setCircleRadius(3.5f);
            tempDataSet.setLineWidth(2.5f);

            android.graphics.drawable.GradientDrawable tempGradient = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            Color.parseColor("#CCEA580C"),
                            Color.parseColor("#CC4CAF50")
                    }
            );
            tempDataSet.setFillDrawable(tempGradient);

            temperatureChart.setData(new LineData(tempDataSet));
            temperatureChart.invalidate();
        }


        // 2. Configure Humidity Chart
        if (humidityChart != null) {
            humidityChart.getLegend().setEnabled(false);
            humidityChart.getAxisRight().setEnabled(false);
            humidityChart.getDescription().setEnabled(false);
            humidityChart.setExtraOffsets(12f, 16f, 12f, 8f);

            XAxis xAxisHum = humidityChart.getXAxis();
            xAxisHum.setDrawGridLinesEnabled(false);
            xAxisHum.setGranularity(1f);
            xAxisHum.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxisHum.setTextColor(Color.parseColor("#1E3A8A"));
            xAxisHum.setValueFormatter(new IndexAxisValueFormatter(labels));

            YAxis yAxisHum = humidityChart.getAxisLeft();
            yAxisHum.setAxisLineColor(Color.parseColor("#E2E8F0"));
            yAxisHum.setGridColor(Color.parseColor("#E2E8F0"));
            yAxisHum.setTextColor(Color.parseColor("#1E3A8A"));

            LineDataSet humDataSet = new LineDataSet(humidityEntries, "Humidity (%)");
            humDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            humDataSet.setDrawValuesEnabled(false);
            humDataSet.setDrawFilledEnabled(true);
            humDataSet.setDrawCirclesEnabled(true);
            humDataSet.setColor(Color.parseColor("#0284C7"));
            humDataSet.setCircleColor(Color.parseColor("#0284C7"));
            humDataSet.setCircleRadius(3.5f);
            humDataSet.setLineWidth(2.5f);

            android.graphics.drawable.GradientDrawable humGradient = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            Color.parseColor("#990284C7"),
                            Color.parseColor("#3338BDF8")
                    }
            );
            humDataSet.setFillDrawable(humGradient);

            humidityChart.setData(new LineData(humDataSet));
            humidityChart.invalidate();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        drawTemperatureChart(view);

        android.widget.Button btnAddNote = view.findViewById(R.id.btnAddNote);
        if (btnAddNote != null) {
            btnAddNote.setOnClickListener(v ->
                    android.widget.Toast.makeText(requireContext(), "Add note clicked", android.widget.Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void updateEfficiencySummaries(View view, List<Reading> allReadings, List<Reading> chartReadings) {
        float latestSolarBattery = 0f;
        float latestIncubatorBattery = 0f;

        // Find the latest readings for solar battery and incubator
        for (Reading r : allReadings) {
            if ("solar-battery-01".equalsIgnoreCase(r.getDeviceId())) {
                latestSolarBattery = r.getBatteryPercentage();
            } else if ("incubator-01".equalsIgnoreCase(r.getDeviceId())) {
                latestIncubatorBattery = r.getBatteryPercentage();
            }
        }

        int solarPct = Math.round(latestSolarBattery);
        int incubatorPct = Math.round(latestIncubatorBattery);

        // Energy Efficiency Card UI
        TextView tvSolarPowerValue = view.findViewById(R.id.tvSolarPowerValue);
        android.widget.ProgressBar pbSolarPower = view.findViewById(R.id.pbSolarPower);
        TextView tvIncubatorValue = view.findViewById(R.id.tvIncubatorValue);
        android.widget.ProgressBar pbIncubator = view.findViewById(R.id.pbIncubator);

        if (tvSolarPowerValue != null) tvSolarPowerValue.setText(solarPct + "%");
        if (pbSolarPower != null) pbSolarPower.setProgress(solarPct);
        if (tvIncubatorValue != null) tvIncubatorValue.setText(incubatorPct + "%");
        if (pbIncubator != null) pbIncubator.setProgress(incubatorPct);

        // Calculate Min, Max, and Current stats for Temperature and Humidity
        float minTemp = Float.MAX_VALUE;
        float maxTemp = -Float.MAX_VALUE;
        float minHum = Float.MAX_VALUE;
        float maxHum = -Float.MAX_VALUE;

        for (Reading r : chartReadings) {
            minTemp = Math.min(minTemp, r.getTemperature());
            maxTemp = Math.max(maxTemp, r.getTemperature());
            minHum = Math.min(minHum, r.getHumidity());
            maxHum = Math.max(maxHum, r.getHumidity());
        }

        Reading latest = chartReadings.get(chartReadings.size() - 1);

        // Temperature Overview Summary UI
        TextView tvCurrentTemp = view.findViewById(R.id.tvCurrentTemp);
        TextView tvMinTemp = view.findViewById(R.id.tvMinTemp);
        TextView tvMaxTemp = view.findViewById(R.id.tvMaxTemp);

        if (tvCurrentTemp != null) {
            tvCurrentTemp.setText(String.format(java.util.Locale.US, "%.1f°C", latest.getTemperature()));
        }
        if (tvMinTemp != null) {
            tvMinTemp.setText(String.format(java.util.Locale.US, "%.1f°C", minTemp));
        }
        if (tvMaxTemp != null) {
            tvMaxTemp.setText(String.format(java.util.Locale.US, "%.1f°C", maxTemp));
        }

        // Humidity Overview Summary UI
        TextView tvCurrentHumidity = view.findViewById(R.id.tvCurrentHumidity);
        TextView tvMinHumidity = view.findViewById(R.id.tvMinHumidity);
        TextView tvMaxHumidity = view.findViewById(R.id.tvMaxHumidity);

        if (tvCurrentHumidity != null) {
            tvCurrentHumidity.setText(String.format(java.util.Locale.US, "%.0f%%", latest.getHumidity()));
        }
        if (tvMinHumidity != null) {
            tvMinHumidity.setText(String.format(java.util.Locale.US, "%.0f%%", minHum));
        }
        if (tvMaxHumidity != null) {
            tvMaxHumidity.setText(String.format(java.util.Locale.US, "%.0f%%", maxHum));
        }
    }

    private List<Reading> loadReadings(Context context, String fileName) {
        List<Reading> readings = new ArrayList<>();
        String rawData = loadJSONFromAsset(context, fileName);
        if (rawData != null) {
            String trimmed = rawData.trim();
            try {
                if (trimmed.startsWith("[")) {
                    JSONArray jsonArray = new JSONArray(trimmed);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        readings.add(parseReadingObject(jsonArray.getJSONObject(i)));
                    }
                } else {
                    String[] lines = trimmed.split("\\r?\\n");
                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            JSONObject obj = new JSONObject(line.trim());
                            readings.add(parseReadingObject(obj));
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return readings;
    }

    private Reading parseReadingObject(JSONObject obj) {
        Reading reading = new Reading();
        reading.setDeviceId(obj.optString("deviceId"));
        reading.setTimestamp(obj.optString("timestamp"));
        reading.setTemperature((float) obj.optDouble("temperature", 0.0));
        reading.setHumidity((float) obj.optDouble("humidity", 0.0));
        reading.setBatteryPercentage((float) obj.optDouble("batteryPercentage", 0.0));
        return reading;
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
}