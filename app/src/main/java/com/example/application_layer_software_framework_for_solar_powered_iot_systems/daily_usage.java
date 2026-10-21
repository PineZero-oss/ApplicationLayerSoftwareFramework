package com.example.application_layer_software_framework_for_solar_powered_iot_systems;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
/**
 * A simple {@link Fragment} subclass.
 * Use the {@link daily_usage#newInstance} factory method to
 * create an instance of this fragment.
 */
public class daily_usage extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    public static class dailyUsage {
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
    public daily_usage() {
        // Required empty public constructor
    }
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment daily_usage.
     */
    // TODO: Rename and change types and number of parameters
    public static daily_usage newInstance(String param1, String param2) {
        daily_usage fragment = new daily_usage();
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
        return inflater.inflate(R.layout.fragment_daily_usage, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        drawDailyUsageChart(view);

        List<dailyUsage> readings =
                loadReadings(requireContext(), "dailydata.json");
        updateDailyBreakdown(view, readings);

    }
    // Draws daily usage chart
    private void drawDailyUsageChart(View view){

        LineChart dailyUsageChart = view.findViewById(R.id.dailyUsageChart);
        if (dailyUsageChart == null) return;
        List<dailyUsage> allReadings = loadReadings(requireContext(), "dailydata.json");
        if (allReadings.isEmpty()) {
            return;
        }
        // Limit readings to the last 12hrs
        int readingLimit = Math.max(0,allReadings.size() - 13);
        List<dailyUsage> readings = allReadings.subList(readingLimit, allReadings.size());

        List<String> labels = new ArrayList<>();
        ArrayList<Entry> dailyUsageEntries = new ArrayList<>();

        // Update Energy Efficiency card with latest values
        for(int i = 0; i < readings.size(); i++){
            dailyUsage reading = readings.get(i);
            String ts = reading.getTimestamp();
            String formattedTime = (ts != null && ts.length() >= 16)
                    ? ts.substring(11, 16)
                    : String.valueOf(i + 1);
            labels.add(formattedTime);

            // Add daily usage data to the chart
            dailyUsageEntries.add(new Entry((float) i, reading.getPowerKw(), null, null));
        }

        //configure daily usage chart
        dailyUsageChart.getLegend().setEnabled(false);
        dailyUsageChart.getAxisRight().setEnabled(false);
        dailyUsageChart.getDescription().setEnabled(false);
        dailyUsageChart.setExtraOffsets(12f, 16f, 12f, 8f);

        //X-axis
        XAxis xAxis = dailyUsageChart.getXAxis();
        xAxis.setDrawGridLinesEnabled(false);
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        //Y-axis
        YAxis yAxis = dailyUsageChart.getAxisLeft();
        yAxis.setAxisLineColor(Color.parseColor("#E2E8F0"));
        yAxis.setGridColor(Color.parseColor("#E2E8F0"));
        yAxis.setTextColor(Color.parseColor("#1E3A8A"));

        //configure daily usage data
        LineDataSet dailyUsageDataSet = new LineDataSet(dailyUsageEntries, "Daily Usage");
        dailyUsageDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dailyUsageDataSet.setDrawValuesEnabled(false);
        dailyUsageDataSet.setDrawFilledEnabled(true);
        dailyUsageDataSet.setDrawCirclesEnabled(true);
        dailyUsageDataSet.setColor(Color.parseColor("#00CECB"));
        dailyUsageDataSet.setCircleColor(Color.parseColor("#00CECB"));
        dailyUsageDataSet.setCircleRadius(3.5f);
        dailyUsageDataSet.setLineWidth(2.5f);

        LineData lineData = new LineData(dailyUsageDataSet);
        dailyUsageChart.setData(lineData);
        dailyUsageChart.invalidate();
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

    // Helper method to format daily time
    private String formatDailyTime(String timestamp) {
        if (timestamp == null || timestamp.length() < 16) {
            return "";
        }
        return timestamp.substring(0, 10)
                + " at "
                + timestamp.substring(11, 16);
    }
    // Update daily breakdown
    private void updateDailyBreakdown(
            View view,
            List<dailyUsage> readings
    ) {
        TextView peakValue =
                view.findViewById(R.id.daily_peak_value);

        TextView peakTime =
                view.findViewById(R.id.daily_peak_time);

        TextView lowValue =
                view.findViewById(R.id.daily_low_value);

        TextView lowTime =
                view.findViewById(R.id.daily_low_time);
        if (readings == null || readings.isEmpty()) {
            peakValue.setText("No data");
            peakTime.setText("");
            lowValue.setText("No data");
            lowTime.setText("");
            return;
        }
        dailyUsage peak = readings.get(0);
        dailyUsage low = readings.get(0);
        int startingValue =  Math.max(0,readings.size() - 13);

        for (dailyUsage reading : readings.subList(startingValue, readings.size())) {
            if (reading.getPowerKw() > peak.getPowerKw()) {
                peak = reading;
            }

            if (reading.getPowerKw() < low.getPowerKw()) {
                low = reading;
            }
        }
        peakValue.setText(String.format(
                java.util.Locale.getDefault(),
                "%.2f kW",
                peak.getPowerKw()
        ));
        peakTime.setText(formatDailyTime(peak.getTimestamp()));

        lowValue.setText(String.format(
                java.util.Locale.getDefault(),
                "%.2f kW",
                low.getPowerKw()
        ));
        lowTime.setText(formatDailyTime(low.getTimestamp()));
    }
    private List<dailyUsage> loadReadings(Context context, String fileName) {

        List<dailyUsage> readings = new ArrayList<>();
        String json = loadJSONFromAsset(context, fileName);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    dailyUsage reading = new dailyUsage();
                    reading.setDeviceId(obj.optString("deviceId"));
                    reading.setTimestamp(obj.optString("timestamp"));
                    reading.setPowerKw((float) obj.optDouble("powerKw", 0.0));
                    readings.add(reading);
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return readings;
    }
}



