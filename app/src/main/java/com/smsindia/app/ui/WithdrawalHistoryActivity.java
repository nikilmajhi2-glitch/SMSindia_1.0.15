package com.smsindia.app.ui;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.smsindia.app.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WithdrawalHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WithdrawalAdapter adapter;
    private List<WithdrawModel> list;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdrawal_history);

        recyclerView = findViewById(R.id.recycler_withdrawals);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();
        adapter = new WithdrawalAdapter(list);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("SMSINDIA_USER", 0);
        String uid = prefs.getString("mobile", "");

        if(!uid.isEmpty()) {
            loadHistory(uid);
        }
    }

    private void loadHistory(String uid) {
        db.collection("users").document(uid).collection("withdrawals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    list.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Double amount = doc.getDouble("amount");
                        String status = doc.getString("status");
                        Timestamp ts = doc.getTimestamp("timestamp");
                        list.add(new WithdrawModel(amount, status, ts));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // --- Data Model ---
    public static class WithdrawModel {
        Double amount;
        String status;
        Timestamp timestamp;

        public WithdrawModel(Double amount, String status, Timestamp timestamp) {
            this.amount = amount;
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    // --- Adapter ---
    public class WithdrawalAdapter extends RecyclerView.Adapter<WithdrawalAdapter.ViewHolder> {
        List<WithdrawModel> mList;
        public WithdrawalAdapter(List<WithdrawModel> list) { mList = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_withdrawal, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WithdrawModel model = mList.get(position);
            
            holder.amount.setText("₹ " + model.amount);
            
            if(model.timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                holder.date.setText(sdf.format(model.timestamp.toDate()));
            }

            // STATUS LOGIC (Reviewing -> Processing -> Completed)
            holder.status.setText(model.status);

            int color;
            if ("Completed".equalsIgnoreCase(model.status)) {
                color = Color.parseColor("#4CAF50"); // Green
            } else if ("Processing".equalsIgnoreCase(model.status)) {
                color = Color.parseColor("#2196F3"); // Blue
            } else {
                color = Color.parseColor("#FF9800"); // Orange (Reviewing)
            }
            holder.status.setBackgroundTintList(ColorStateList.valueOf(color));
        }

        @Override
        public int getItemCount() { return mList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView date, amount, status;
            ViewHolder(View v) {
                super(v);
                date = v.findViewById(R.id.tv_date);
                amount = v.findViewById(R.id.tv_amount);
                status = v.findViewById(R.id.tv_status);
            }
        }
    }
}
