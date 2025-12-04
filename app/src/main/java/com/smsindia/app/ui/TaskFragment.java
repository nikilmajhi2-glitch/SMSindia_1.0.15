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
import android.widget.ImageView;
import android.widget.ScrollView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskFragment extends Fragment {

    private static final int PERMISSION_REQ_CODE = 101;

    // UI Elements
    private MaterialCardView cardSim1, cardSim2;
    private TextView tvSim1Name, tvSim2Name;
    private TextView tvTimer, tvStatus, tvLogs;
    private TextView tvSessionSuccess, tvSessionFailed;
    private CircularProgressIndicator progressTimer;
    private SwitchMaterial switchAuto;
    private Button btnAction;
    private ScrollView scrollLogs;

    // Logic
    private int selectedSubId = -1;
    private boolean isAutoMode = false;
    private boolean isRunning = false;
    private CountDownTimer waitTimer;
    private FirebaseFirestore db;
    private String userId;

    // Session Stats
    private int sessionSuccessCount = 0;
    private int sessionFailedCount = 0;

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
        scrollLogs = v.findViewById(R.id.scroll_logs);
        
        tvSessionSuccess = v.findViewById(R.id.tv_session_success);
        tvSessionFailed = v.findViewById(R.id.tv_session_failed);
        
        progressTimer = v.findViewById(R.id.progress_timer_circle);
        switchAuto = v.findViewById(R.id.switch_auto_mode);
        btnAction = v.findViewById(R.id.btn_action_main);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = requireActivity().getSharedPreferences("SMSINDIA_USER", 0);
        userId = prefs.getString("mobile", "unknown");

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, PERMISSION_REQ_CODE);
        } else {
            loadSimCards();
        }

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
            SubscriptionInfo info1 = subs.get(0);
            subId1 = info1.getSubscriptionId();
            tvSim1Name.setText(info1.getCarrierName());
            selectSim(1);

            if (subs.size() > 1) {
                SubscriptionInfo info2 = subs.get(1);
                subId2 = info2.getSubscriptionId();
                tvSim2Name.setText(info2.getCarrierName());
                cardSim2.setEnabled(true);
                cardSim2.setAlpha(1.0f);
            } else {
                cardSim2.setEnabled(false);
                cardSim2.setAlpha(0.5f); 
                tvSim2Name.setText("No SIM");
            }
        } else {
            tvStatus.setText("No SIM Found");
            btnAction.setEnabled(false);
        }
    }

    private void selectSim(int simIndex) {
        // Get Colors from XML Resources
        int colorPrimary = ContextCompat.getColor(getContext(), R.color.app_primary);
        int colorGray = Color.LTGRAY;
        int bgSelected = Color.parseColor("#E3F2FD"); // Very light blue
        int bgUnselected = Color.WHITE;

        if (simIndex == 1) {
            selectedSubId = subId1;
            
            updateSimCardUI(cardSim1, tvSim1Name, true, colorPrimary, bgSelected);
            updateSimCardUI(cardSim2, tvSim2Name, false, colorGray, bgUnselected);
        } else if (simIndex == 2 && subId2 != -1) {
            selectedSubId = subId2;

            updateSimCardUI(cardSim2, tvSim2Name, true, colorPrimary, bgSelected);
            updateSimCardUI(cardSim1, tvSim1Name, false, colorGray, bgUnselected);
        }
    }

    private void updateSimCardUI(MaterialCardView card, TextView text, boolean isSelected, int color, int bgColor) {
        card.setStrokeColor(color);
        card.setStrokeWidth(isSelected ? 4 : 1);
        card.setCardBackgroundColor(bgColor);
        
        // Find the ImageView inside the card (child index 0 -> linear layout -> child index 0 -> image view)
        if(card.getChildCount() > 0 && card.getChildAt(0) instanceof ViewGroup) {
            ViewGroup layout = (ViewGroup) card.getChildAt(0);
            if(layout.getChildCount() > 0 && layout.getChildAt(0) instanceof ImageView) {
                ((ImageView) layout.getChildAt(0)).setColorFilter(color);
            }
        }
        text.setTextColor(color);
    }

    private void startProcess() {
        if(selectedSubId == -1) {
            Toast.makeText(getContext(), "Please select a SIM", Toast.LENGTH_SHORT).show();
            return;
        }
        isRunning = true;
        btnAction.setText("STOP TASK");
        
        // Use Red for Stop
        btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F"))); 
        
        log("Process Started...", true);
        fetchAndSend();
    }

    private void stopProcess(String reason) {
        isRunning = false;
        if (waitTimer != null) waitTimer.cancel();
        
        tvTimer.setText("00");
        progressTimer.setProgress(100);
        tvStatus.setText("Idle: " + reason);
        
        btnAction.setText(isAutoMode ? "START AUTO LOOP" : "SEND SINGLE TASK");
        // Back to Brand Blue
        int primaryColor = ContextCompat.getColor(getContext(), R.color.app_primary);
        btnAction.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        
        log("Process Stopped: " + reason, true);
    }

    private void fetchAndSend() {
        if (!isRunning) return;
        tvStatus.setText("Fetching Task...");
        
        db.collection("sms_tasks").limit(1).get()
            .addOnSuccessListener(snapshot -> {
                if (!snapshot.isEmpty()) {
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    String phone = doc.getString("phone");
                    String msg = doc.getString("message");
                    
                    if (phone != null && msg != null) {
                        sendSMS(phone, msg, doc.getId());
                    } else {
                        sessionFailedCount++;
                        updateStatsUI();
                        stopProcess("Bad Data");
                    }
                } else {
                    stopProcess("No Tasks in DB");
                }
            })
            .addOnFailureListener(e -> stopProcess("Network Error"));
    }

    private void sendSMS(String phone, String message, String docId) {
        tvStatus.setText("Sending...");
        log("Target: " + phone, false);

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
            log("SMS Sent -> Waiting Delivery", false);
            
            sessionSuccessCount++;
            updateStatsUI();

            if (isAutoMode) {
                startCooldownTimer();
            } else {
                stopProcess("Task Complete");
            }

        } catch (Exception e) {
            log("Error: " + e.getMessage(), true);
            sessionFailedCount++;
            updateStatsUI();
            stopProcess("Send Failed");
        }
    }
    
    private void updateStatsUI() {
        tvSessionSuccess.setText(String.valueOf(sessionSuccessCount));
        tvSessionFailed.setText(String.valueOf(sessionFailedCount));
    }

    private void startCooldownTimer() {
        tvStatus.setText("Cooling Down...");
        
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

    private void log(String msg, boolean isImportant) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String prefix = isImportant ? "█ " : "> ";
        String newLog = prefix + "[" + time + "] " + msg + "\n";
        
        tvLogs.append(newLog);
        
        // Auto Scroll
        scrollLogs.post(() -> scrollLogs.fullScroll(View.FOCUS_DOWN));
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
