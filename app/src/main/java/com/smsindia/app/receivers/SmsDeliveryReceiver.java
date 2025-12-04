package com.smsindia.app.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SmsDeliveryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if ("com.smsindia.SMS_SENT".equals(action)) {
            int resultCode = getResultCode();
            String userId = intent.getStringExtra("userId");
            String docId = intent.getStringExtra("docId");
            String phone = intent.getStringExtra("phone");

            if (userId == null || docId == null) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (resultCode == Activity.RESULT_OK) {
                // 1. Success: Update Balance & Logs
                Map<String, Object> log = new HashMap<>();
                log.put("phone", phone);
                log.put("status", "DELIVERED");
                log.put("timestamp", FieldValue.serverTimestamp());
                log.put("amount", 0.16);

                db.collection("users").document(userId)
                        .collection("delivery_logs").add(log);

                db.collection("users").document(userId)
                        .update(
                                "balance", FieldValue.increment(0.16),
                                "sms_count", FieldValue.increment(1)
                        );

                // Mark task as done globally (optional, depending on your logic)
                // db.collection("sms_tasks").document(docId).delete();

                Toast.makeText(context, "SMS Sent! ₹0.16 added.", Toast.LENGTH_SHORT).show();

            } else {
                // 2. Failed
                Map<String, Object> log = new HashMap<>();
                log.put("phone", phone);
                log.put("status", "FAILED");
                log.put("errorCode", resultCode);
                log.put("timestamp", FieldValue.serverTimestamp());

                db.collection("users").document(userId)
                        .collection("delivery_logs").add(log);

                Toast.makeText(context, "SMS Failed. Check SIM/Balance.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
