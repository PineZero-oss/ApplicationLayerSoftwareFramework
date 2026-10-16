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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link monthly_usage#newInstance} factory method to
 * create an instance of this fragment.
 */
public class monthly_usage extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public monthly_usage() {
        // Required empty public constructor
    }

    public static class monthlyUsage {
        private String month;
        private double energyKwh;

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }

        public double getEnergyKwh() { return energyKwh; }
        public void setEnergyKwh(double energyKwh) { this.energyKwh = energyKwh; }
    }

    public static monthly_usage newInstance(String param1, String param2) {
        monthly_usage fragment = new monthly_usage();
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
        return inflater.inflate(R.layout.fragment_monthly_usage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        drawMonthlyUsageChart(view);
    }

    private void drawMonthlyUsageChart(View view) {
        BarChart monthlyUsageChart = view.findViewById(R.id.dailyUsageChart);
        if (monthlyUsageChart == null) return;

        List<monthlyUsage> readings = loadReadings(requireContext(), "monthlydata.json");
        if (readings.isEmpty()) {
            return;
        }

        List<String> labels = new ArrayList<>();
        ArrayList<BarEntry> monthlyUsageEntries = new ArrayList<>();

        for (int i = 0; i < readings.size(); i++) {
            monthlyUsage reading = readings.get(i);
            String m = reading.getMonth();
            String monthLabel = m;
            if (m != null && m.length() >= 7) {
                String y = m.substring(2, 4);
                String mo = m.substring(5, 7);
                switch (mo) {
                    case "01": monthLabel = "Jan "; break;
                    case "02": monthLabel = "Feb "; break;
                    case "03": monthLabel = "Mar "; break;
                    case "04": monthLabel = "Apr "; break;
                    case "05": monthLabel = "May "; break;
                    case "06": monthLabel = "Jun "; break;
                    case "07": monthLabel = "Jul "; break;
                    case "08": monthLabel = "Aug "; break;
                    case "09": monthLabel = "Sep "; break;
                    case "10": monthLabel = "Oct "; break;
                    case "11": monthLabel = "Nov "; break;
                    case "12": monthLabel = "Dec "; break;
                }
            }
            labels.add(monthLabel);
            monthlyUsageEntries.add(new BarEntry((float) i, (float) reading.getEnergyKwh(), null, null));
        }

        monthlyUsageChart.getLegend().setEnabled(true);
        monthlyUsageChart.getAxisRight().setEnabled(false);
        monthlyUsageChart.getDescription().setEnabled(false);
        monthlyUsageChart.setExtraOffsets(6f, 12f, 6f, 4f);

        // Y-axis
        YAxis yAxis = monthlyUsageChart.getAxisLeft();
        yAxis.setAxisLineColor(Color.parseColor("#E2E8F0"));
        yAxis.setGridColor(Color.parseColor("#E2E8F0"));
        yAxis.setTextColor(Color.parseColor("#1E3A8A"));

        // X-axis
        XAxis xAxis = monthlyUsageChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLinesEnabled(false);

        BarDataSet monthlyUsageDataSet = new BarDataSet(monthlyUsageEntries, "Monthly Energy (kWh)");
        monthlyUsageDataSet.setValueTextSize(8f);

        BarData barData = new BarData(monthlyUsageDataSet);
        monthlyUsageChart.setData(barData);
        monthlyUsageChart.invalidate();
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

    private List<monthlyUsage> loadReadings(Context context, String fileName) {
        List<monthlyUsage> readings = new ArrayList<>();
        String json = loadJSONFromAsset(context, fileName);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                Map<String, Double> monthlyTotalsMap = new LinkedHashMap<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String timestamp = obj.optString("timestamp");
                    double powerKw = obj.optDouble("powerKw", 0.0);

                    if (timestamp != null && timestamp.length() >= 7) {
                        String monthKey = timestamp.substring(0, 7); // e.g. "2025-11"
                        double currentTotal = monthlyTotalsMap.containsKey(monthKey) ? monthlyTotalsMap.get(monthKey) : 0.0;
                        monthlyTotalsMap.put(monthKey, currentTotal + powerKw);
                    }
                }

                for (Map.Entry<String, Double> entry : monthlyTotalsMap.entrySet()) {
                    monthlyUsage reading = new monthlyUsage();
                    reading.setMonth(entry.getKey());
                    reading.setEnergyKwh(entry.getValue());
                    readings.add(reading);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return readings;
    }
}
