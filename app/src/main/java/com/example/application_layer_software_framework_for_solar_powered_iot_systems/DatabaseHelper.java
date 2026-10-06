package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SolarFlare.db";
    private static final int DATABASE_VERSION = 1;

    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE users (" +
                    "user_id TEXT PRIMARY KEY NOT NULL, " +
                    "username TEXT NOT NULL, " +
                    "email TEXT UNIQUE NOT NULL, " +
                    "password_hash TEXT, " +
                    "created_at TEXT NOT NULL);";

    private static final String CREATE_TABLE_DEVICES =
            "CREATE TABLE devices (" +
                    "device_id TEXT PRIMARY KEY NOT NULL, " +
                    "device_name TEXT NOT NULL, " +
                    "user_id TEXT, " +
                    "is_online INTEGER DEFAULT 0, " +
                    "last_seen TEXT, " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE);";

    private static final String CREATE_TABLE_TELEMETRY =
            "CREATE TABLE telemetry_readings (" +
                    "reading_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "device_id TEXT NOT NULL, " +
                    "timestamp TEXT NOT NULL, " +
                    "temperature REAL, " +
                    "humidity REAL, " +
                    "battery_percentage REAL, " +
                    "event_type TEXT DEFAULT 'telemetry', " +
                    "sequence_number INTEGER, " +
                    "sync_status TEXT DEFAULT 'PENDING_SYNC', " +
                    "sync_error_message TEXT, " +
                    "FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE);";

    private static final String CREATE_TABLE_EFFICIENCY_NOTES =
            "CREATE TABLE efficiency_notes (" +
                    "note_id TEXT PRIMARY KEY NOT NULL, " +
                    "device_id TEXT NOT NULL, " +
                    "user_id TEXT NOT NULL, " +
                    "title TEXT, " +
                    "content TEXT NOT NULL, " +
                    "created_at TEXT NOT NULL, " +
                    "updated_at TEXT NOT NULL, " +
                    "sync_status TEXT DEFAULT 'PENDING_SYNC', " +
                    "FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE);";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_DEVICES);
        db.execSQL(CREATE_TABLE_TELEMETRY);
        db.execSQL(CREATE_TABLE_EFFICIENCY_NOTES);

        db.execSQL("CREATE INDEX idx_telemetry_sync ON telemetry_readings(sync_status);");
        db.execSQL("CREATE INDEX idx_telemetry_device_time ON telemetry_readings(device_id, timestamp);");
        db.execSQL("CREATE INDEX idx_notes_device ON efficiency_notes(device_id);");
        db.execSQL("CREATE INDEX idx_notes_sync ON efficiency_notes(sync_status);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS efficiency_notes;");
        db.execSQL("DROP TABLE IF EXISTS telemetry_readings;");
        db.execSQL("DROP TABLE IF EXISTS devices;");
        db.execSQL("DROP TABLE IF EXISTS users;");
        onCreate(db);
    }
}