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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link home#newInstance} factory method to
 * create an instance of this fragment.
 */
public class home extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // Reading class to hold data from JSON
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

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment home.
     */
    // TODO: Rename and change types and number of parameters
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        drawSummaryChart(view);
    }

    // Drawing the summary chart
    private void drawSummaryChart(View view) {
        LineChart summaryChart = view.findViewById(R.id.summaryChart);
        if (summaryChart == null) return;

        // Configuring basic chart properties
        summaryChart.setBackgroundColor(Color.parseColor("#EDD2E0"));
        summaryChart.setDrawGridBackgroundEnabled(false);
        summaryChart.getDescription().setEnabled(false);

        //chart X-axis
        XAxis xAxis = summaryChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLinesEnabled(false);

        //chart Y-axis
        YAxis leftAxis = summaryChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLinesEnabled(true);
        summaryChart.getAxisRight().setEnabled(false);

        //chart Legend
        Legend legend = summaryChart.getLegend();
        legend.setTextColor(Color.BLACK);
        legend.setForm(Legend.LegendForm.LINE);



        List<Reading> readings = loadReadings(requireContext(), "monthlydata.json");
        ArrayList<Entry> entries = new ArrayList<>();

        int readingLimit = Math.min(readings.size(), 31); // Limit to 31 readings
        for (int i = 0; i < readingLimit; i++) {
            Reading reading = readings.get(i);
            Entry entry = new Entry((float) i, reading.getPowerKw(), null, null);
            entries.add(entry);
        }

        //chart data
        LineDataSet dataSet = new LineDataSet(entries, "Power Consumption");
        //chart styling
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(3f);
        dataSet.setDrawCirclesEnabled(false);
        dataSet.setFillColor(Color.parseColor("#DBABBE"));
        dataSet.setDrawValuesEnabled(false);
        dataSet.setDrawFilledEnabled(false);
        dataSet.setDrawCircleHoleEnabled(false);
        dataSet.setCircleRadius(5f);


        LineData lineData = new LineData(dataSet);
        summaryChart.setData(lineData);
        summaryChart.invalidate();

    }

    // Load JSON from asset
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

    // Load readings from JSON
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
