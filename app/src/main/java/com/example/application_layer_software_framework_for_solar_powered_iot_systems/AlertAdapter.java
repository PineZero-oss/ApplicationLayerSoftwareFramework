package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AlertAdapter
        extends RecyclerView.Adapter<
        AlertAdapter.AlertViewHolder> {

    public interface OnAlertClickListener {
        void onAlertClicked(AlertCase alertCase);
    }

    private final List<AlertCase> allCases =
            new ArrayList<>();

    private final List<AlertCase> visibleCases =
            new ArrayList<>();

    private final OnAlertClickListener listener;

    public AlertAdapter(
            List<AlertCase> cases,
            OnAlertClickListener listener
    ) {
        this.listener = listener;

        if (cases != null) {
            allCases.addAll(cases);
            visibleCases.addAll(cases);
        }
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view =
                LayoutInflater.from(
                        parent.getContext()
                ).inflate(
                        R.layout.item_alert_case,
                        parent,
                        false
                );

        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull AlertViewHolder holder,
            int position
    ) {
        AlertCase alertCase =
                visibleCases.get(position);

        holder.caseId.setText(
                safeValue(
                        alertCase.getCaseId(),
                        "No case ID"
                )
        );

        holder.status.setText(
                formatStatus(
                        alertCase.getCaseStatus()
                )
        );

        holder.title.setText(
                safeValue(
                        alertCase.getTitle(),
                        "Alert"
                )
        );

        holder.device.setText(
                safeValue(
                        alertCase.getDeviceDisplayName(),
                        alertCase.getDeviceId()
                )
        );

        holder.message.setText(
                safeValue(
                        alertCase.getMessage(),
                        "No description available"
                )
        );

        holder.severity.setText(
                safeValue(
                        alertCase.getSeverity(),
                        "LOW"
                )
        );

        holder.dataSource.setText(
                safeValue(
                        alertCase.getDataSource(),
                        "Unknown source"
                )
        );

        holder.time.setText(
                formatOccurredAt(
                        alertCase.getOccurredAt()
                )
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAlertClicked(alertCase);
            }
        });
    }

    @Override
    public int getItemCount() {
        return visibleCases.size();
    }

    public void filterBySeverity(
            String severity
    ) {
        visibleCases.clear();

        if (severity == null
                || "ALL".equalsIgnoreCase(severity)) {

            visibleCases.addAll(allCases);

        } else {
            for (AlertCase alertCase : allCases) {
                if (severity.equalsIgnoreCase(
                        alertCase.getSeverity()
                )) {
                    visibleCases.add(alertCase);
                }
            }
        }

        notifyDataSetChanged();
    }

    private static String safeValue(
            String value,
            String fallback
    ) {
        if (value == null
                || value.trim().isEmpty()) {
            return fallback;
        }

        return value;
    }



    private static String formatStatus(
            String status
    ) {
        if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            return "In Progress...";
        }

        if ("RESOLVED".equalsIgnoreCase(status)) {
            return "Resolved";
        }

        return "Active";
    }

    private static String formatOccurredAt(
            String occurredAt
    ) {
        if (occurredAt == null
                || occurredAt.trim().isEmpty()) {
            return "Unknown time";
        }

        try {
            SimpleDateFormat inputFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                            Locale.getDefault()
                    );

            SimpleDateFormat outputFormat =
                    new SimpleDateFormat(
                            "dd MMM yyyy, HH:mm",
                            Locale.getDefault()
                    );

            Date date =
                    inputFormat.parse(
                            occurredAt
                    );

            if (date == null) {
                return occurredAt;
            }

            return outputFormat.format(date);

        } catch (ParseException e) {
            return occurredAt;
        }
    }

    public static class AlertViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView caseId;
        private final TextView status;
        private final TextView title;
        private final TextView device;
        private final TextView message;
        private final TextView severity;
        private final TextView dataSource;
        private final TextView time;

        public AlertViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            caseId =
                    itemView.findViewById(
                            R.id.alert_case_id
                    );

            status =
                    itemView.findViewById(
                            R.id.alert_status
                    );

            title =
                    itemView.findViewById(
                            R.id.alert_title
                    );

            device =
                    itemView.findViewById(
                            R.id.alert_device
                    );

            message =
                    itemView.findViewById(
                            R.id.alert_message
                    );

            severity =
                    itemView.findViewById(
                            R.id.alert_severity
                    );

            dataSource =
                    itemView.findViewById(
                            R.id.alert_data_source
                    );

            time =
                    itemView.findViewById(
                            R.id.alert_time
                    );
        }
    }
}