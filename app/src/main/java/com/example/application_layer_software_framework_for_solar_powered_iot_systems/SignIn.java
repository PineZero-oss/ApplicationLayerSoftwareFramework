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

public class SignIn extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignUpRedirect, tvForgotPasswordRedirect;

    private DatabaseHelper databaseHelper;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin);

        databaseHelper = new DatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.acc_email);
        etPassword = findViewById(R.id.acc_password);
        btnLogin = findViewById(R.id.acc_createBtn);
        tvSignUpRedirect = findViewById(R.id.acc_signup_redirect);
        tvForgotPasswordRedirect = findViewById(R.id.acc_forgotPassword_redirect);

        btnLogin.setOnClickListener(v -> loginUser());

        tvSignUpRedirect.setOnClickListener(v -> {
            Intent intent = new Intent(SignIn.this, CreateAccount.class);
            startActivity(intent);
            finish();
        });

        tvForgotPasswordRedirect.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> Toast.makeText(this, "Reset email sent to " + email, Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter your email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Try Firebase Authentication online
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(SignIn.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignIn.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    // 2. Offline fallback: verify against local SQLite using BCrypt
                    boolean isLocalValid = databaseHelper.verifyLocalCredentials(email, password);
                    if (isLocalValid) {
                        Toast.makeText(SignIn.this, "Offline login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignIn.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(SignIn
                                .this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}