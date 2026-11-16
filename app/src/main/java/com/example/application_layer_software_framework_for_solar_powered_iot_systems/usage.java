package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link usage#newInstance} factory method to
 * create an instance of this fragment.
 */
public class usage extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private TextView filterDay, filterWeek, filterMonth;

    public usage() {
        // Required empty public constructor
    }

    public static usage newInstance(String param1, String param2) {
        usage fragment = new usage();
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
        return inflater.inflate(R.layout.fragment_usage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        filterDay = view.findViewById(R.id.filter_day);
        filterWeek = view.findViewById(R.id.filter_week);
        filterMonth = view.findViewById(R.id.filter_month);

        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction().replace(R.id.usage_container, new daily_usage()).commit();
            updateSelectedFilter(filterDay);
        }

        if (filterDay != null) {
            filterDay.setOnClickListener(v -> {
                getChildFragmentManager().beginTransaction().replace(R.id.usage_container, new daily_usage()).commit();
                updateSelectedFilter(filterDay);
            });
        }

        if (filterWeek != null) {
            filterWeek.setOnClickListener(v -> {
                getChildFragmentManager().beginTransaction().replace(R.id.usage_container, new weekly_usage()).commit();
                updateSelectedFilter(filterWeek);
            });
        }

        if (filterMonth != null) {
            filterMonth.setOnClickListener(v -> {
                getChildFragmentManager().beginTransaction().replace(R.id.usage_container, new monthly_usage()).commit();
                updateSelectedFilter(filterMonth);
            });
        }
    }

    private void updateSelectedFilter(TextView selected) {
        if (filterDay != null) filterDay.setSelected(false);
        if (filterWeek != null) filterWeek.setSelected(false);
        if (filterMonth != null) filterMonth.setSelected(false);
        if (selected != null) selected.setSelected(true);
    }
}
