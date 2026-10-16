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
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link weekly_usage#newInstance} factory method to
 * create an instance of this fragment.
 */
public class weekly_usage extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public weekly_usage() {
        // Required empty public constructor
    }

    public static class weeklyUsage {
        private String deviceId;
        private String periodStart;
        private String periodEnd;
        private String dataSource;
        private String unit;
        private String bestDay;
        private double energyKwh;
        private double totalEnergyKwh;
        private double averageDailyEnergyKwh;

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getPeriodStart() { return periodStart; }
        public void setPeriodStart(String periodStart) { this.periodStart = periodStart; }

        public String getPeriodEnd() { return periodEnd; }
        public void setPeriodEnd(String periodEnd) { this.periodEnd = periodEnd; }

        public String getDataSource() { return dataSource; }
        public void setDataSource(String dataSource) { this.dataSource = dataSource; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public String getBestDay() { return bestDay; }
        public void setBestDay(String bestDay) { this.bestDay = bestDay; }

        public double getEnergyKwh() { return energyKwh; }
        public void setEnergyKwh(double energyKwh) { this.energyKwh = energyKwh; }

        public double getTotalEnergyKwh() { return totalEnergyKwh; }
        public void setTotalEnergyKwh(double totalEnergyKwh) { this.totalEnergyKwh = totalEnergyKwh; }

        public double getAverageDailyEnergyKwh() { return averageDailyEnergyKwh; }
        public void setAverageDailyEnergyKwh(double averageDailyEnergyKwh) { this.averageDailyEnergyKwh = averageDailyEnergyKwh; }
    }

    public static weekly_usage newInstance(String param1, String param2) {
        weekly_usage fragment = new weekly_usage();
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
        return inflater.inflate(R.layout.fragment_weekly_usage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        drawWeeklyUsageChart(view);
    }

    // Draws weekly usage chart
    private void drawWeeklyUsageChart(View view) {
        BarChart weeklyUsageChart = view.findViewById(R.id.weeklyUsageChart);
        if (weeklyUsageChart == null) return;
        List<weeklyUsage> allReadings = loadReadings(requireContext(), "weeklydata.json");
        if (allReadings.isEmpty()) {
            return;
        }

        int readingLimit = Math.min(allReadings.size() , 7) ;
        List<weeklyUsage> readings = allReadings.subList(0, readingLimit);

        List<String> labels = new ArrayList<>();
        ArrayList<BarEntry> weeklyUsageEntries = new ArrayList<>();
        String[] days = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        for (int i = 0; i < readings.size(); i++) {
            weeklyUsage reading = readings.get(i);
            String dayLabel = days[i % days.length];
            labels.add(dayLabel);
            weeklyUsageEntries.add(new BarEntry((float) i, (float) reading.getEnergyKwh(), null, null));
        }

        weeklyUsageChart.getLegend().setEnabled(true);
        weeklyUsageChart.getAxisRight().setEnabled(false);
        weeklyUsageChart.getDescription().setEnabled(false);
        weeklyUsageChart.setExtraOffsets(6f, 12f, 6f, 4f);

        //Y-axis
        YAxis yAxis = weeklyUsageChart.getAxisLeft();
        yAxis.setAxisLineColor(Color.parseColor("#E2E8F0"));
        yAxis.setGridColor(Color.parseColor("#E2E8F0"));
        yAxis.setTextColor(Color.parseColor("#1E3A8A"));

        //X-axis
        XAxis xAxis = weeklyUsageChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLinesEnabled(false);

        BarDataSet weeklyUsageDataSet = new BarDataSet(weeklyUsageEntries, "Weekly Energy (kWh)");
        weeklyUsageDataSet.setValueTextSize(8f);

        BarData barData = new BarData(weeklyUsageDataSet);
        weeklyUsageChart.setData(barData);
        weeklyUsageChart.invalidate();
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

    private List<weeklyUsage> loadReadings(Context context, String fileName) {
        List<weeklyUsage> readings = new ArrayList<>();
        String json = loadJSONFromAsset(context, fileName);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                if (jsonArray.length() > 0) {
                    JSONObject weeklyObj = jsonArray.getJSONObject(0);
                    JSONArray dailyTotals = weeklyObj.optJSONArray("dailyTotals");
                    if (dailyTotals != null) {
                        for (int i = 0; i < dailyTotals.length(); i++) {
                            JSONObject dayObj = dailyTotals.getJSONObject(i);
                            weeklyUsage reading = new weeklyUsage();
                            reading.setPeriodStart(dayObj.optString("date"));
                            reading.setEnergyKwh(dayObj.optDouble("energyKwh", 0.0));
                            readings.add(reading);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return readings;
    }
}
