package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SolarFlare.db";
    private static final int DATABASE_VERSION = 1;

    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE users (" +
                    "user_id TEXT PRIMARY KEY NOT NULL, " +
                    "username TEXT NOT NULL, " +
                    "email TEXT UNIQUE NOT NULL, " +
                    "password_hash TEXT, " +
                    "user_type TEXT DEFAULT 'farmer', " +
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
                    "author_email TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
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

    // Insert user into SQLite
    public boolean insertUser(String userId, String username, String email, String passwordHash, String createdAt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("username", username);
        values.put("email", email);
        values.put("password_hash", passwordHash);
        values.put("user_type", ROLE_FARMER);
        values.put("created_at", createdAt);

        long result = db.insert("users", null, values);
        return result != -1;
    }

    // Check if an email is already present locally
    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT user_id FROM users WHERE email = ?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Verify password hash using BCrypt for offline authentication
    public boolean verifyLocalCredentials(String email, String plainPassword) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT password_hash FROM users WHERE email = ?",
                new String[]{email}
        );

        boolean isValid = false;
        if (cursor.moveToFirst()) {
            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"));
            isValid = HashSecurity.checkPassword(plainPassword, storedHash);
        }
        cursor.close();
        return isValid;
    }

    // Retrieve user details by email
    public Cursor getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT user_id, username, email, created_at FROM users WHERE email = ?", new String[]{email});
    }

    // ==================== ADDED: ROLES, NOTES CRUD & CLOUD SYNC ====================

    public static final String ROLE_FARMER = "farmer";
    public static final String ROLE_MANAGER = "manager";

    public interface SyncCallback {
        void onComplete(boolean success, String message);
    }

    public interface RoleSyncCallback {
        void onRoleSynced(boolean isManager);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            ensureColumnsExist(db);
        }
    }

    // Safely adds new columns if the database was already created prior to this update
    private void ensureColumnsExist(SQLiteDatabase db) {
        if (!hasColumn(db, "users", "user_type")) {
            db.execSQL("ALTER TABLE users ADD COLUMN user_type TEXT DEFAULT 'farmer';");
        }
        if (!hasColumn(db, "efficiency_notes", "author_email")) {
            db.execSQL("ALTER TABLE efficiency_notes ADD COLUMN author_email TEXT;");
        }
        if (!hasColumn(db, "efficiency_notes", "description")) {
            db.execSQL("ALTER TABLE efficiency_notes ADD COLUMN description TEXT;");
        }
    }

    private boolean hasColumn(SQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        boolean found = false;
        if (cursor != null) {
            int nameIdx = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (nameIdx != -1 && columnName.equalsIgnoreCase(cursor.getString(nameIdx))) {
                    found = true;
                    break;
                }
            }
            cursor.close();
        }
        return found;
    }

    public static String normalizeUserType(String rawType) {
        if (rawType != null) {
            String cleaned = rawType.trim().toLowerCase(Locale.US);
            if ("manager".equals(cleaned) || "managers".equals(cleaned)) {
                return ROLE_MANAGER;
            }
        }
        return ROLE_FARMER;
    }

    public void upsertUserFromCloud(String userId, String username, String email, String rawUserType) {
        if (email == null || email.trim().isEmpty()) return;

        String normalizedRole = normalizeUserType(rawUserType);
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT user_id FROM users WHERE user_id = ? OR email = ?",
                new String[]{userId != null ? userId : "", email}
        );
        boolean exists = cursor.moveToFirst();
        cursor.close();

        ContentValues values = new ContentValues();
        if (username != null && !username.trim().isEmpty()) {
            values.put("username", username);
        }
        values.put("email", email);
        values.put("user_type", normalizedRole);

        if (exists) {
            db.update(
                    "users",
                    values,
                    "user_id = ? OR email = ?",
                    new String[]{userId != null ? userId : "", email}
            );
        } else if (userId != null) {
            values.put("user_id", userId);
            if (!values.containsKey("username")) {
                values.put("username", email.split("@")[0]);
            }
            values.put("created_at", nowIsoTimestamp());
            db.insert("users", null, values);
        }
    }

    public String getCurrentUserEmail(Context context) {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null && fbUser.getEmail() != null) {
            return fbUser.getEmail();
        }
        SharedPreferences prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        return prefs.getString("logged_in_email", null);
    }

    public String getCurrentUserId(Context context) {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            return fbUser.getUid();
        }
        String email = getCurrentUserEmail(context);
        if (email != null) {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT user_id FROM users WHERE email = ?", new String[]{email});
            String uid = null;
            if (c.moveToFirst()) {
                uid = c.getString(0);
            }
            c.close();
            if (uid != null) return uid;
            return email;
        }
        return "offline_user";
    }

    public String getCurrentUserType(Context context) {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = fbUser != null ? fbUser.getUid() : null;
        String email = getCurrentUserEmail(context);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        if (uid != null && email != null) {
            cursor = db.rawQuery(
                    "SELECT user_type FROM users WHERE user_id = ? OR email = ? LIMIT 1",
                    new String[]{uid, email}
            );
        } else if (email != null) {
            cursor = db.rawQuery(
                    "SELECT user_type FROM users WHERE email = ? LIMIT 1",
                    new String[]{email}
            );
        }

        String role = ROLE_FARMER;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                role = normalizeUserType(cursor.getString(0));
            }
            cursor.close();
        }
        return role;
    }

    public boolean isCurrentUserManager(Context context) {
        return ROLE_MANAGER.equals(getCurrentUserType(context));
    }

    public boolean insertNote(String noteId, String userId, String authorEmail, String title, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        String now = nowIsoTimestamp();

        ContentValues values = new ContentValues();
        values.put("note_id", noteId);
        values.put("device_id", "general");
        values.put("user_id", userId);
        values.put("author_email", authorEmail);
        values.put("title", title);
        values.put("description", description);
        values.put("content", description);
        values.put("created_at", now);
        values.put("updated_at", now);
        values.put("sync_status", "PENDING_SYNC");

        long res = db.insertWithOnConflict("efficiency_notes", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return res != -1;
    }

    public boolean updateNote(String noteId, String newTitle, String newDescription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", newTitle);
        values.put("description", newDescription);
        values.put("content", newDescription);
        values.put("updated_at", nowIsoTimestamp());
        values.put("sync_status", "PENDING_SYNC");

        int rows = db.update("efficiency_notes", values, "note_id = ?", new String[]{noteId});
        return rows > 0;
    }

    public void markNoteForDeletion(String noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sync_status", "PENDING_DELETE");
        values.put("updated_at", nowIsoTimestamp());
        db.update("efficiency_notes", values, "note_id = ?", new String[]{noteId});
    }

    public void hardDeleteNote(String noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("efficiency_notes", "note_id = ?", new String[]{noteId});
    }

    // Managers see ALL notes; Farmers ONLY see their own notes
    public List<Note> getVisibleNotesForCurrentUser(Context context) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        boolean isManager = isCurrentUserManager(context);
        String userId = getCurrentUserId(context);
        String email = getCurrentUserEmail(context);

        Cursor cursor;
        if (isManager) {
            cursor = db.rawQuery(
                    "SELECT note_id, user_id, title, COALESCE(description, content) FROM efficiency_notes " +
                            "WHERE sync_status != 'PENDING_DELETE' ORDER BY updated_at DESC",
                    null
            );
        } else {
            cursor = db.rawQuery(
                    "SELECT note_id, user_id, title, COALESCE(description, content) FROM efficiency_notes " +
                            "WHERE (user_id = ? OR (author_email IS NOT NULL AND author_email = ?)) " +
                            "AND sync_status != 'PENDING_DELETE' ORDER BY updated_at DESC",
                    new String[]{userId, email != null ? email : ""}
            );
        }

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String nId = cursor.getString(0);
                String uId = cursor.getString(1);
                String title = cursor.getString(2);
                String desc = cursor.getString(3);
                list.add(new Note(nId, uId, title != null ? title : "", desc != null ? desc : ""));
            }
            cursor.close();
        }
        return list;
    }

    public void syncUserRoleFromFirestore(Context context, @Nullable RoleSyncCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (callback != null) callback.onRoleSynced(isCurrentUserManager(context));
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String role = ROLE_FARMER;
                    String username = null;
                    String email = currentUser.getEmail();

                    if (doc.exists()) {
                        username = doc.getString("username");
                        String cloudRole = doc.getString("user_type");
                        if (cloudRole == null) cloudRole = doc.getString("userType");

                        if (cloudRole == null) {
                            Map<String, Object> initRole = new HashMap<>();
                            initRole.put("user_type", ROLE_FARMER);
                            firestore.collection("users").document(currentUser.getUid())
                                    .set(initRole, SetOptions.merge());
                        }
                        role = normalizeUserType(cloudRole);
                    }

                    upsertUserFromCloud(currentUser.getUid(), username, email, role);

                    if (!ROLE_MANAGER.equals(role)) {
                        removeOtherUsersNotes(currentUser.getUid(), email);
                    }

                    if (callback != null) {
                        callback.onRoleSynced(ROLE_MANAGER.equals(role));
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onRoleSynced(isCurrentUserManager(context));
                });
    }

    public void syncDatabaseWithFirestore(Context context, @Nullable SyncCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (callback != null) {
                callback.onComplete(false, "Please sign in online to sync with cloud.");
            }
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String uid = currentUser.getUid();
        String email = currentUser.getEmail();

        // 1. Sync user profile & user_type between Firestore and SQLite
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    String cloudUsername = userDoc.exists() ? userDoc.getString("username") : null;
                    String rawRole = null;
                    if (userDoc.exists()) {
                        rawRole = userDoc.getString("user_type");
                        if (rawRole == null) rawRole = userDoc.getString("userType");
                    }

                    if (!userDoc.exists() || rawRole == null) {
                        Map<String, Object> defaultProfile = new HashMap<>();
                        defaultProfile.put("user_type", ROLE_FARMER);
                        if (email != null) defaultProfile.put("email", email);
                        firestore.collection("users").document(uid).set(defaultProfile, SetOptions.merge());
                    }

                    String syncedRole = normalizeUserType(rawRole);
                    upsertUserFromCloud(uid, cloudUsername, email, syncedRole);
                    boolean isManager = ROLE_MANAGER.equals(syncedRole);

                    // 2. Push local SQLite changes (deletions, notes, telemetry) to Firestore
                    List<Task<?>> pushTasks = new ArrayList<>();
                    SQLiteDatabase sqlite = this.getWritableDatabase();

                    Cursor delCursor = sqlite.rawQuery(
                            "SELECT note_id FROM efficiency_notes WHERE sync_status = 'PENDING_DELETE'",
                            null
                    );
                    while (delCursor.moveToNext()) {
                        String delNoteId = delCursor.getString(0);
                        Task<Void> delTask = firestore.collection("efficiency_notes")
                                .document(delNoteId)
                                .delete()
                                .addOnSuccessListener(unused -> hardDeleteNote(delNoteId));
                        pushTasks.add(delTask);
                    }
                    delCursor.close();

                    Cursor pendingCursor = sqlite.rawQuery(
                            "SELECT note_id, device_id, user_id, author_email, title, COALESCE(description, content), created_at, updated_at " +
                                    "FROM efficiency_notes WHERE sync_status = 'PENDING_SYNC'",
                            null
                    );
                    while (pendingCursor.moveToNext()) {
                        String nId = pendingCursor.getString(0);
                        String nDeviceId = pendingCursor.getString(1);
                        String nUserId = pendingCursor.getString(2);
                        String nEmail = pendingCursor.getString(3);
                        String nTitle = pendingCursor.getString(4);
                        String nDesc = pendingCursor.getString(5);
                        String nCreated = pendingCursor.getString(6);
                        String nUpdated = pendingCursor.getString(7);

                        if ("offline_user".equals(nUserId) || (email != null && email.equals(nUserId))) {
                            nUserId = uid;
                        }

                        Map<String, Object> noteMap = new HashMap<>();
                        noteMap.put("note_id", nId);
                        noteMap.put("device_id", nDeviceId != null ? nDeviceId : "general");
                        noteMap.put("user_id", nUserId);
                        noteMap.put("author_email", nEmail != null ? nEmail : email);
                        noteMap.put("title", nTitle != null ? nTitle : "");
                        noteMap.put("description", nDesc != null ? nDesc : "");
                        noteMap.put("content", nDesc != null ? nDesc : "");
                        noteMap.put("created_at", nCreated);
                        noteMap.put("updated_at", nUpdated);

                        final String finalUserId = nUserId;
                        Task<Void> putTask = firestore.collection("efficiency_notes")
                                .document(nId)
                                .set(noteMap, SetOptions.merge())
                                .addOnSuccessListener(unused -> markNoteSynced(nId, finalUserId));
                        pushTasks.add(putTask);
                    }
                    pendingCursor.close();

                    Cursor telCursor = sqlite.rawQuery(
                            "SELECT reading_id, device_id, timestamp, temperature, humidity, battery_percentage, event_type, sequence_number " +
                                    "FROM telemetry_readings WHERE sync_status = 'PENDING_SYNC'",
                            null
                    );
                    while (telCursor.moveToNext()) {
                        int readingId = telCursor.getInt(0);
                        Map<String, Object> telMap = new HashMap<>();
                        telMap.put("deviceId", telCursor.getString(1));
                        telMap.put("timestamp", telCursor.getString(2));
                        telMap.put("temperature", telCursor.getDouble(3));
                        telMap.put("humidity", telCursor.getDouble(4));
                        telMap.put("batteryPercentage", telCursor.getDouble(5));
                        telMap.put("eventType", telCursor.getString(6));
                        telMap.put("sequenceNumber", telCursor.getInt(7));

                        Task<?> telTask = firestore.collection("telemetry_readings")
                                .add(telMap)
                                .addOnSuccessListener(docRef -> markTelemetrySynced(readingId));
                        pushTasks.add(telTask);
                    }
                    telCursor.close();

                    // 3. Pull notes from Firestore -> SQLite according to role
                    Tasks.whenAllComplete(pushTasks).addOnCompleteListener(done -> {
                        if (!isManager) {
                            removeOtherUsersNotes(uid, email);
                        }

                        Task<QuerySnapshot> pullQuery = isManager
                                ? firestore.collection("efficiency_notes").get()
                                : firestore.collection("efficiency_notes").whereEqualTo("user_id", uid).get();

                        pullQuery.addOnSuccessListener(querySnapshot -> {
                            Set<String> remoteIds = new HashSet<>();
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                String rNoteId = doc.getId();
                                remoteIds.add(rNoteId);

                                String rDeviceId = doc.getString("device_id");
                                String rUserId = doc.getString("user_id");
                                String rEmail = doc.getString("author_email");
                                String rTitle = doc.getString("title");
                                String rDesc = doc.getString("description");
                                if (rDesc == null) rDesc = doc.getString("content");
                                String rCreated = doc.getString("created_at");
                                String rUpdated = doc.getString("updated_at");

                                upsertSyncedNoteFromCloud(
                                        rNoteId,
                                        rDeviceId != null ? rDeviceId : "general",
                                        rUserId != null ? rUserId : uid,
                                        rEmail,
                                        rTitle != null ? rTitle : "",
                                        rDesc != null ? rDesc : "",
                                        rCreated != null ? rCreated : nowIsoTimestamp(),
                                        rUpdated != null ? rUpdated : nowIsoTimestamp()
                                );
                            }

                            reconcileDeletedRemoteNotes(remoteIds);

                            if (callback != null) {
                                callback.onComplete(true, "Sync complete (" + syncedRole + ")");
                            }
                        }).addOnFailureListener(e -> {
                            if (callback != null) {
                                callback.onComplete(false, "Failed to pull notes: " + e.getMessage());
                            }
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onComplete(false, "Sync failed: " + e.getMessage());
                    }
                });
    }

    private void markNoteSynced(String noteId, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sync_status", "SYNCED");
        values.put("user_id", userId);
        db.update("efficiency_notes", values, "note_id = ?", new String[]{noteId});
    }

    private void markTelemetrySynced(int readingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sync_status", "SYNCED");
        db.update("telemetry_readings", values, "reading_id = ?", new String[]{String.valueOf(readingId)});
    }

    private void upsertSyncedNoteFromCloud(String noteId, String deviceId, String userId, String authorEmail,
                                           String title, String description, String createdAt, String updatedAt) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery(
                "SELECT sync_status FROM efficiency_notes WHERE note_id = ?",
                new String[]{noteId}
        );
        if (c.moveToFirst()) {
            String status = c.getString(0);
            if ("PENDING_SYNC".equals(status) || "PENDING_DELETE".equals(status)) {
                c.close();
                return;
            }
        }
        c.close();

        ContentValues values = new ContentValues();
        values.put("note_id", noteId);
        values.put("device_id", deviceId);
        values.put("user_id", userId);
        values.put("author_email", authorEmail);
        values.put("title", title);
        values.put("description", description);
        values.put("content", description);
        values.put("created_at", createdAt);
        values.put("updated_at", updatedAt);
        values.put("sync_status", "SYNCED");

        db.insertWithOnConflict("efficiency_notes", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void reconcileDeletedRemoteNotes(Set<String> remoteIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery(
                "SELECT note_id FROM efficiency_notes WHERE sync_status = 'SYNCED'",
                null
        );
        List<String> toRemove = new ArrayList<>();
        while (c.moveToNext()) {
            String localId = c.getString(0);
            if (!remoteIds.contains(localId)) {
                toRemove.add(localId);
            }
        }
        c.close();

        for (String id : toRemove) {
            db.delete("efficiency_notes", "note_id = ?", new String[]{id});
        }
    }

    private void removeOtherUsersNotes(String currentUid, String currentEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(
                "efficiency_notes",
                "user_id != ? AND (author_email IS NULL OR author_email != ?)",
                new String[]{currentUid, currentEmail != null ? currentEmail : ""}
        );
    }

    private static String nowIsoTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
}