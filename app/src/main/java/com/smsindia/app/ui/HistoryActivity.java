package com.smsindia.app.ui;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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

        // Header Back Button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

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
                        
                        if (title == null) title = "Unknown Transaction";
                        if (amount == null) amount = 0.0;
                        
                        list.add(new TransactionModel(title, amount, type, ts));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

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
            
            if(model.timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
                holder.date.setText(sdf.format(model.timestamp.toDate()));
            }

            // Format Amount and Color
            if ("DEBIT".equals(model.type)) {
                holder.amount.setText("- ₹" + model.amount);
                holder.amount.setTextColor(Color.RED);
                holder.icon.setImageResource(android.R.drawable.arrow_up_float); // Arrow Up (Sent)
                holder.icon.setColorFilter(Color.RED);
                holder.iconBg.setBackgroundResource(R.drawable.bg_circle_red_light); // Need to create
            } else {
                holder.amount.setText("+ ₹" + model.amount);
                // Use success green
                holder.amount.setTextColor(Color.parseColor("#00C853")); 
                holder.icon.setImageResource(android.R.drawable.arrow_down_float); // Arrow Down (Received)
                holder.icon.setColorFilter(Color.parseColor("#00C853"));
                holder.iconBg.setBackgroundResource(R.drawable.bg_circle_green_light); // Need to create
            }
        }

        @Override
        public int getItemCount() { return mList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, date, amount;
            ImageView icon;
            View iconBg;
            
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.tv_tx_title);
                date = v.findViewById(R.id.tv_tx_date);
                amount = v.findViewById(R.id.tv_tx_amount);
                icon = v.findViewById(R.id.img_tx_icon);
                iconBg = v.findViewById(R.id.layout_icon_bg);
            }
        }
    }
}
