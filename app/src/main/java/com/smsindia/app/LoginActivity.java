package com.smsindia.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smsindia.app.MainActivity;
import com.smsindia.app.R;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText phoneInput, passwordInput, referInput;
    private Button loginBtn, signupBtn;
    private TextView deviceIdText;

    private FirebaseFirestore db;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();

        // Check if already logged in
        SharedPreferences prefs = getSharedPreferences("SMSINDIA_USER", MODE_PRIVATE);
        if (prefs.contains("mobile")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        referInput = findViewById(R.id.referInput);
        loginBtn = findViewById(R.id.loginBtn);
        signupBtn = findViewById(R.id.signupBtn);
        deviceIdText = findViewById(R.id.deviceIdText);

        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        deviceIdText.setText("Device ID: " + deviceId);

        loginBtn.setOnClickListener(v -> loginUser());
        signupBtn.setOnClickListener(v -> registerUser());
    }

    private void loginUser() {
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter phone and password", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading("Verifying...");

        db.collection("users").document(phone).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                hideLoading();
                Toast.makeText(this, "User not found! Please Register.", Toast.LENGTH_SHORT).show();
                return;
            }

            String storedPass = snapshot.getString("password");
            String storedDevice = snapshot.getString("deviceId");

            if (storedPass != null && storedPass.equals(password)) {
                if (storedDevice != null && !storedDevice.equals(deviceId)) {
                    hideLoading();
                    Toast.makeText(this, "Login denied: Account linked to another device.", Toast.LENGTH_LONG).show();
                } else {
                    saveLoginAndRedirect(phone);
                }
            } else {
                hideLoading();
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            hideLoading();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void registerUser() {
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String referCode = referInput.getText().toString().trim();

        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter phone and password", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading("Creating Account...");

        // 1. Check Device ID Lock
        db.collection("users")
                .whereEqualTo("deviceId", deviceId)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        hideLoading();
                        Toast.makeText(this, "Device already registered! Please Login.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 2. Check if Phone Exists
                    db.collection("users").document(phone).get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot.exists()) {
                                    hideLoading();
                                    Toast.makeText(this, "Phone already registered! Use Login.", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                // 3. Create User Data
                                Map<String, Object> user = new HashMap<>();
                                user.put("phone", phone);
                                user.put("password", password);
                                user.put("deviceId", deviceId);
                                user.put("createdAt", FieldValue.serverTimestamp());
                                user.put("balance", 0.0);
                                user.put("coins", 0);
                                user.put("sms_count", 0);
                                user.put("referral_count", 0);
                                user.put("referral_earnings", 0.0);

                                if (!TextUtils.isEmpty(referCode)) {
                                    if (referCode.equals(phone)) {
                                        hideLoading();
                                        Toast.makeText(this, "You cannot refer yourself!", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    user.put("referredBy", referCode);
                                    updateReferrer(referCode);
                                }

                                db.collection("users").document(phone).set(user)
                                        .addOnSuccessListener(unused -> saveLoginAndRedirect(phone))
                                        .addOnFailureListener(e -> {
                                            hideLoading();
                                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            });
                }).addOnFailureListener(e -> hideLoading());
    }

    private void updateReferrer(String referrerPhone) {
        db.collection("users").document(referrerPhone)
                .update("referral_count", FieldValue.increment(1));
    }

    private void saveLoginAndRedirect(String phone) {
        SharedPreferences prefs = getSharedPreferences("SMSINDIA_USER", MODE_PRIVATE);
        prefs.edit()
                .putString("mobile", phone)
                .putString("deviceId", deviceId)
                .apply();

        // Delay slightly for UX
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            hideLoading();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }, 1000);
    }

    // --- Loading Dialog Logic ---
    private AlertDialog loadingDialog;

    private void showLoading(String message) {
        if(loadingDialog != null && loadingDialog.isShowing()) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        TextView tvMessage = dialogView.findViewById(R.id.tv_loading_message);
        tvMessage.setText(message);
        builder.setView(dialogView);
        builder.setCancelable(false);
        loadingDialog = builder.create();
        if(loadingDialog.getWindow() != null) 
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
