package com.smsindia.app.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.smsindia.app.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeliveryLogActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private List<LogModel> logList;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_logs);

        // Header Back Button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_logs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        logList = new ArrayList<>();
        adapter = new LogAdapter(logList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("SMSINDIA_USER", MODE_PRIVATE);
        uid = prefs.getString("mobile", "");

        if (uid.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadLogs();
    }

    private void loadLogs() {
        db.collection("sent_logs")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    logList.clear();
                    if (snapshot.isEmpty()) {
                        Toast.makeText(this, "No logs found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String phone = doc.getString("phone");
                        // Handle both Timestamp object or Long (milliseconds)
                        Long timestamp = 0L;
                        try {
                            timestamp = doc.getLong("timestamp"); // If stored as numbers
                            if(timestamp == null) timestamp = doc.getTimestamp("timestamp").toDate().getTime();
                        } catch (Exception e) {
                            timestamp = System.currentTimeMillis();
                        }

                        if (phone != null) {
                            logList.add(new LogModel(phone, timestamp));
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // --- Data Model ---
    public static class LogModel {
        String phone;
        Long timestamp;

        public LogModel(String phone, Long timestamp) {
            this.phone = phone;
            this.timestamp = timestamp;
        }
    }

    // --- Adapter ---
    public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        List<LogModel> mList;
        public LogAdapter(List<LogModel> list) { mList = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_delivery_log, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LogModel model = mList.get(position);
            
            holder.phone.setText(model.phone);
            
            if(model.timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault());
                holder.date.setText(sdf.format(new Date(model.timestamp)));
            }
        }

        @Override
        public int getItemCount() { return mList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView phone, date;
            ViewHolder(View v) {
                super(v);
                phone = v.findViewById(R.id.tv_log_phone);
                date = v.findViewById(R.id.tv_log_date);
            }
        }
    }
}
