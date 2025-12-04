package com.smsindia.app.ui;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smsindia.app.R;

import java.util.ArrayList;
import java.util.List;

public class TaskFragment extends Fragment {

    private static final int PERMISSION_REQ_CODE = 101;

    // UI Elements
    private MaterialCardView cardSim1, cardSim2;
    private TextView tvSim1Name, tvSim2Name;
    private TextView tvTimer, tvStatus, tvLogs;
    private CircularProgressIndicator progressTimer;
    private SwitchMaterial switchAuto;
    private Button btnAction;

    // Logic
    private int selectedSubId = -1;
    private boolean isAutoMode = false;
    private boolean isRunning = false;
    private CountDownTimer waitTimer;
    private FirebaseFirestore db;
    private String userId;

    // Sim IDs
    private int subId1 = -1;
    private int subId2 = -1;

    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task, container, false);

        // Init Views
        cardSim1 = v.findViewById(R.id.card_sim_1);
        cardSim2 = v.findViewById(R.id.card_sim_2);
        tvSim1Name = v.findViewById(R.id.tv_sim1_name);
        tvSim2Name = v.findViewById(R.id.tv_sim2_name);
        tvTimer = v.findViewById(R.id.tv_timer);
        tvStatus = v.findViewById(R.id.status_message);
        tvLogs = v.findViewById(R.id.tv_logs);
        progressTimer = v.findViewById(R.id.progress_timer_circle);
        switchAuto = v.findViewById(R.id.switch_auto_mode);
        btnAction = v.findViewById(R.id.btn_action_main);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = requireActivity().getSharedPreferences("SMSINDIA_USER", 0);
        userId = prefs.getString("mobile", "unknown");

        // Check Perms
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, PERMISSION_REQ_CODE);
        } else {
            loadSimCards();
        }

        // Listeners
        cardSim1.setOnClickListener(view -> selectSim(1));
        cardSim2.setOnClickListener(view -> selectSim(2));

        switchAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAutoMode = isChecked;
            if(!isRunning) btnAction.setText(isChecked ? "START AUTO LOOP" : "SEND SINGLE TASK");
        });

        btnAction.setOnClickListener(view -> {
            if (isRunning) {
                stopProcess("Stopped by user");
            } else {
                startProcess();
            }
        });

        return v;
    }

    private void loadSimCards() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        SubscriptionManager sm = (SubscriptionManager) requireContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        List<SubscriptionInfo> subs = sm.getActiveSubscriptionInfoList();

        if (subs != null && !subs.isEmpty()) {
            // SIM 1
            SubscriptionInfo info1 = subs.get(0);
            subId1 = info1.getSubscriptionId();
            tvSim1Name.setText(info1.getCarrierName());
            
            // Default select SIM 1
            selectSim(1);

            // SIM 2
            if (subs.size() > 1) {
                SubscriptionInfo info2 = subs.get(1);
                subId2 = info2.getSubscriptionId();
                tvSim2Name.setText(info2.getCarrierName());
                cardSim2.setEnabled(true);
                cardSim2.setAlpha(1.0f);
            } else {
                cardSim2.setEnabled(false);
                cardSim2.setAlpha(0.5f); // Dim if no SIM 2
                tvSim2Name.setText("No SIM");
            }
        } else {
            tvStatus.setText("No SIM Found");
            btnAction.setEnabled(false);
        }
    }

    private void selectSim(int simIndex) {
        // UI Update Logic
        int colorSelected = Color.parseColor("#6200EE");
        int colorUnselected = Color.parseColor("#E0E0E0");
        int bgSelected = Color.parseColor("#E8EAF6"); // Light Indigo
        int bgUnselected = Color.WHITE;

        if (simIndex == 1) {
            selectedSubId = subId1;
            
            cardSim1.setStrokeColor(colorSelected);
            cardSim1.setStrokeWidth(5); // Thick border
            cardSim1.setCardBackgroundColor(bgSelected);
            tvSim1Name.setTextColor(colorSelected);

            cardSim2.setStrokeColor(colorUnselected);
            cardSim2.setStrokeWidth(2);
            cardSim2.setCardBackgroundColor(bgUnselected);
            tvSim2Name.setTextColor(Color.GRAY);
        } else if (simIndex == 2 && subId2 != -1) {
            selectedSubId = subId2;

            cardSim2.setStrokeColor(colorSelected);
            cardSim2.setStrokeWidth(5);
            cardSim2.setCardBackgroundColor(bgSelected);
            tvSim2Name.setTextColor(colorSelected);

            cardSim1.setStrokeColor(colorUnselected);
            cardSim1.setStrokeWidth(2);
            cardSim1.setCardBackgroundColor(bgUnselected);
            tvSim1Name.setTextColor(Color.GRAY);
        }
    }

    // --- LOGIC ---

    private void startProcess() {
        if(selectedSubId == -1) {
            Toast.makeText(getContext(), "Please select a SIM", Toast.LENGTH_SHORT).show();
            return;
        }
        isRunning = true;
        btnAction.setText("STOP TASK");
        // ✅ FIXED COLOR HERE
        btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F"))); // Red
        
        log("Process Started...");
        fetchAndSend();
    }

    private void stopProcess(String reason) {
        isRunning = false;
        if (waitTimer != null) waitTimer.cancel();
        
        tvTimer.setText("00");
        progressTimer.setProgress(100);
        tvStatus.setText("Idle: " + reason);
        
        btnAction.setText(isAutoMode ? "START AUTO LOOP" : "SEND SINGLE TASK");
        // ✅ FIXED COLOR HERE
        btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6200EE"))); // Purple
        log("Process Stopped: " + reason);
    }

    private void fetchAndSend() {
        if (!isRunning) return;
        tvStatus.setText("Fetching Data...");
        
        // Fetch random task (Limit 1)
        db.collection("sms_tasks").limit(1).get()
            .addOnSuccessListener(snapshot -> {
                if (!snapshot.isEmpty()) {
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    String phone = doc.getString("phone");
                    String msg = doc.getString("message");
                    
                    if (phone != null && msg != null) {
                        sendSMS(phone, msg, doc.getId());
                    } else {
                        stopProcess("Bad Data");
                    }
                } else {
                    stopProcess("No Tasks Found in DB");
                }
            })
            .addOnFailureListener(e -> stopProcess("Network Error"));
    }

    private void sendSMS(String phone, String message, String docId) {
        tvStatus.setText("Sending to " + phone);
        log("Target: " + phone);

        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                 smsManager = requireContext().getSystemService(SmsManager.class).createForSubscriptionId(selectedSubId);
            } else {
                 smsManager = SmsManager.getSmsManagerForSubscriptionId(selectedSubId);
            }

            ArrayList<String> parts = smsManager.divideMessage(message);
            ArrayList<PendingIntent> sentIntents = new ArrayList<>();

            for (int i = 0; i < parts.size(); i++) {
                Intent sent = new Intent("com.smsindia.SMS_SENT");
                sent.setClass(requireContext(), com.smsindia.app.receivers.SmsDeliveryReceiver.class);
                sent.putExtra("userId", userId);
                sent.putExtra("docId", docId);
                sent.putExtra("phone", phone);
                
                int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
                PendingIntent pi = PendingIntent.getBroadcast(requireContext(), i, sent, flags);
                sentIntents.add(pi);
            }

            smsManager.sendMultipartTextMessage(phone, null, parts, sentIntents, null);
            log("SMS Sent -> Waiting for Delivery");

            if (isAutoMode) {
                startCooldownTimer();
            } else {
                stopProcess("Single Task Done");
            }

        } catch (Exception e) {
            log("Error: " + e.getMessage());
            stopProcess("SMS Send Failed");
        }
    }

    private void startCooldownTimer() {
        tvStatus.setText("Cooling down...");
        
        // 15 Seconds Timer
        waitTimer = new CountDownTimer(15000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                int progress = (int) ((millisUntilFinished * 100) / 15000);
                
                tvTimer.setText(String.format("%02d", seconds));
                progressTimer.setProgress(progress);
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00");
                progressTimer.setProgress(0);
                if (isRunning) {
                    fetchAndSend();
                }
            }
        }.start();
    }

    private void log(String msg) {
        String prev = tvLogs.getText().toString();
        // Keep only last 10 lines
        if(prev.length() > 500) prev = prev.substring(0, 500) + "...";
        tvLogs.setText("> " + msg + "\n" + prev);
    }

    private boolean hasPermissions() {
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQ_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSimCards();
        }
    }
}
