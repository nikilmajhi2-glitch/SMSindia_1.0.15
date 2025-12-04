package com.smsindia.app.workers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smsindia.app.MainActivity;
import com.smsindia.app.R;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class SmsWorker extends Worker {

    private static final String CHANNEL_ID = "sms_worker_channel";
    private static final String TAG = "SmsWorker";
    private final Context context;
    private final FirebaseFirestore db;
    private final String uid;

    public SmsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = context.getSharedPreferences("SMSINDIA_USER", Context.MODE_PRIVATE);
        this.uid = prefs.getString("mobile", "");
    }

    @NonNull
    @Override
    public Result doWork() {
        if (uid.isEmpty()) return Result.failure();

        // Get Selected SIM ID from Input Data
        int subId = getInputData().getInt("subId", -1);

        setForegroundAsync(createForegroundInfo("Processing Task..."));

        try {
            // Fetch 1 pending task synchronously
            Map<String, Object> task = fetchOneTaskSync();

            if (task == null) {
                return Result.success(); // No tasks available
            }

            String phone = (String) task.get("phone");
            String msg = (String) task.get("message");
            String docId = (String) task.get("id");

            if (phone != null && msg != null) {
                sendSmsOnSim(subId, phone, msg, docId);
            }

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Worker Error", e);
            return Result.retry();
        }
    }

    private void sendSmsOnSim(int subId, String phone, String message, String docId) {
        try {
            // 1. Get Correct SMS Manager (Dual SIM Support)
            SmsManager smsManager;
            if (subId != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    smsManager = context.getSystemService(SmsManager.class).createForSubscriptionId(subId);
                } else {
                    smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
                }
            } else {
                smsManager = SmsManager.getDefault();
            }

            // 2. Prepare Intents (Must match SmsDeliveryReceiver)
            ArrayList<String> parts = smsManager.divideMessage(message);
            ArrayList<PendingIntent> sentIntents = new ArrayList<>();

            for (int i = 0; i < parts.size(); i++) {
                Intent sent = new Intent("com.smsindia.SMS_SENT");
                sent.setClass(context, com.smsindia.app.receivers.SmsDeliveryReceiver.class);
                sent.putExtra("userId", uid);
                sent.putExtra("docId", docId);
                sent.putExtra("phone", phone);
                
                // Important: Android 12+ requires FLAG_IMMUTABLE or MUTABLE
                int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
                PendingIntent pi = PendingIntent.getBroadcast(context, (docId+i).hashCode(), sent, flags);
                sentIntents.add(pi);
            }

            // 3. Send Multipart
            smsManager.sendMultipartTextMessage(phone, null, parts, sentIntents, null);
            Log.d(TAG, "Sent to " + phone + " via SubID: " + subId);

        } catch (Exception e) {
            Log.e(TAG, "Send Failed", e);
        }
    }

    // Helper to fetch data synchronously in a Worker
    private Map<String, Object> fetchOneTaskSync() throws InterruptedException {
        final Map<String, Object>[] result = new Map[1];
        final CountDownLatch latch = new CountDownLatch(1);

        db.collection("sms_tasks").limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        result[0] = doc.getData();
                        if (result[0] != null) result[0].put("id", doc.getId());
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(); // Wait for Firebase
        return result[0];
    }

    private ForegroundInfo createForegroundInfo(String content) {
        createChannel();
        Intent i = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("SMS Background Task")
                .setContentText(content)
                // ✅ FIXED: Using ic_sim_card instead of ic_launcher_foreground
                .setSmallIcon(R.drawable.ic_sim_card) 
                .setContentIntent(pi)
                .setOngoing(true)
                .build();

        return new ForegroundInfo(1, n);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "SMS Worker", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
