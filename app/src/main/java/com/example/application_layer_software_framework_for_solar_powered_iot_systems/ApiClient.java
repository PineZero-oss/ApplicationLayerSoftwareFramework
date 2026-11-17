package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {

    public static final String BASE_URL = "http://10.0.2.2:8000";

    private static final ExecutorService executor =
            Executors.newFixedThreadPool(2);
    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(String json);
        void onError(Exception e);
    }

    public static void get(String path, Callback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + path);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new RuntimeException("HTTP " + code);
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                }

                String body = sb.toString();
                mainHandler.post(() -> callback.onSuccess(body));

            } catch (Exception e) {
                Log.e("ApiClient", "GET " + path + " failed", e);
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }
}