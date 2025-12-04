package com.smsindia.app.ui;

import android.content.SharedPreferences;
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

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<TransactionModel> list;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();
        adapter = new HistoryAdapter(list);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("SMSINDIA_USER", 0);
        String uid = prefs.getString("mobile", "");

        if(!uid.isEmpty()) {
            loadHistory(uid);
        }
    }

    private void loadHistory(String uid) {
        db.collection("users").document(uid).collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    list.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String title = doc.getString("title");
                        Double amount = doc.getDouble("amount");
                        String type = doc.getString("type");
                        Timestamp ts = doc.getTimestamp("timestamp");
                        list.add(new TransactionModel(title, amount, type, ts));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // --- Inner Classes for Simplicity ---

    public static class TransactionModel {
        String title, type;
        Double amount;
        Timestamp timestamp;

        public TransactionModel(String title, Double amount, String type, Timestamp timestamp) {
            this.title = title;
            this.amount = amount;
            this.type = type;
            this.timestamp = timestamp;
        }
    }

    public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        List<TransactionModel> mList;
        public HistoryAdapter(List<TransactionModel> list) { mList = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TransactionModel model = mList.get(position);
            holder.title.setText(model.title);
            
            // Format Date
            if(model.timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                holder.date.setText(sdf.format(model.timestamp.toDate()));
            }

            // Format Amount and Color
            if ("DEBIT".equals(model.type)) {
                holder.amount.setText("- ₹" + model.amount);
                holder.amount.setTextColor(android.graphics.Color.RED);
            } else {
                holder.amount.setText("+ ₹" + model.amount);
                holder.amount.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Green
            }
        }

        @Override
        public int getItemCount() { return mList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, date, amount;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.tv_tx_title);
                date = v.findViewById(R.id.tv_tx_date);
                amount = v.findViewById(R.id.tv_tx_amount);
            }
        }
    }
}
