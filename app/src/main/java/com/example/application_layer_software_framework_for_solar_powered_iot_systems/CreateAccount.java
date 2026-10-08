package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CreateAccount extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etRepeatPassword;
    private Button btnCreate;
    private TextView tvLoginRedirect;

    private DatabaseHelper databaseHelper;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_account);

        databaseHelper = new DatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etUsername = findViewById(R.id.acc_username);
        etEmail = findViewById(R.id.acc_email);
        etPassword = findViewById(R.id.acc_password);
        etRepeatPassword = findViewById(R.id.acc_repeatPassword);
        btnCreate = findViewById(R.id.acc_createBtn);
        tvLoginRedirect = findViewById(R.id.acc_login_redirect);

        btnCreate.setOnClickListener(v -> createAccount());

        tvLoginRedirect.setOnClickListener(v -> {
            Intent intent = new Intent(CreateAccount.this, SignIn.class);
            startActivity(intent);
            finish();
        });
    }

    private void createAccount() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String repeatPassword = etRepeatPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(repeatPassword)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(repeatPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate BCrypt hash for local and cloud database storage
        String passwordHash = HashSecurity.hashPassword(password);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        // 1. Online registration via Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    String userId = (user != null) ? user.getUid() : UUID.randomUUID().toString();

                    // 2. Save all fields to local SQLite
                    databaseHelper.insertUser(userId, username, email, passwordHash, timestamp);

                    // 3. Save all fields to Cloud Firestore
                    Map<String, Object> firestoreUser = new HashMap<>();
                    firestoreUser.put("user_id", userId);
                    firestoreUser.put("username", username);
                    firestoreUser.put("email", email);
                    firestoreUser.put("password_hash", passwordHash);
                    firestoreUser.put("created_at", timestamp);

                    firestore.collection("users").document(userId).set(firestoreUser)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(CreateAccount.this, "Account created and synced successfully!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(CreateAccount.this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(CreateAccount.this, "Account saved locally (Firestore sync pending)", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(CreateAccount.this, MainActivity.class));
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    // 4. Offline Fallback: If offline or Firebase Auth fails, store in local SQLite
                    if (databaseHelper.checkEmailExists(email)) {
                        Toast.makeText(this, "Email is already registered locally", Toast.LENGTH_SHORT).show();
                    } else {
                        String localUid = "offline_" + UUID.randomUUID().toString();
                        boolean inserted = databaseHelper.insertUser(localUid, username, email, passwordHash, timestamp);
                        if (inserted) {
                            Toast.makeText(this, "Offline account created locally", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(CreateAccount.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}