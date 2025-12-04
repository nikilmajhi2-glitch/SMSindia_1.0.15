package com.smsindia.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smsindia.app.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShareFragment extends Fragment {

    private TextView tvTotalRefs, tvEarnings, tvCoins, tvCode;
    private RecyclerView recyclerMilestones;
    private FirebaseFirestore db;
    private String uid;

    // User Stats
    private long userSmsCount = 0;
    private long userReferralCount = 0;
    private Map<String, Object> claimedMilestones = new HashMap<>();
    
    // Adapter
    private MilestoneAdapter adapter;
    private List<Milestone> milestoneList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_share, container, false);

        // Init Views
        tvTotalRefs = v.findViewById(R.id.tv_total_referrals);
        tvEarnings = v.findViewById(R.id.tv_referral_earnings);
        tvCoins = v.findViewById(R.id.tv_total_coins);
        tvCode = v.findViewById(R.id.tv_share_code);
        Button btnShare = v.findViewById(R.id.btn_share_app);
        recyclerMilestones = v.findViewById(R.id.recycler_milestones);

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = requireActivity().getSharedPreferences("SMSINDIA_USER", 0);
        uid = prefs.getString("mobile", "");
        tvCode.setText(uid); // Set code immediately

        setupMilestoneList(); // Define rewards
        fetchUserData(); // Get counts from DB

        btnShare.setOnClickListener(view -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, 
                "Install SMSIndia: The Best Earning App!\n\nUse my Code: *" + uid + "*\nto get a signup bonus.\n\nDownload: https://play.google.com/store/apps/details?id=com.smsindia.app");
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Share via"));
        });

        return v;
    }

    private void setupMilestoneList() {
        milestoneList = new ArrayList<>();
        // Define Rules: Type (0=SMS, 1=Referral), Target, RewardCoins, ID
        milestoneList.add(new Milestone("ms_sms_20", "Send first 20 SMS", 20, 1, 0));
        milestoneList.add(new Milestone("ms_sms_100", "Send first 100 SMS", 100, 1, 0));
        milestoneList.add(new Milestone("ms_ref_1", "Invite 1st Friend", 1, 2, 1));
        milestoneList.add(new Milestone("ms_ref_5", "Invite 5 Friends", 5, 5, 1));
        milestoneList.add(new Milestone("ms_sms_500", "Send 500 SMS", 500, 5, 0));

        adapter = new MilestoneAdapter(milestoneList);
        recyclerMilestones.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerMilestones.setAdapter(adapter);
    }

    private void fetchUserData() {
        if (uid.isEmpty()) return;

        db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;

            // 1. Stats Dash
            Double earnings = snapshot.getDouble("referral_earnings");
            Long refs = snapshot.getLong("referral_count");
            Long coins = snapshot.getLong("coins"); 
            Long sms = snapshot.getLong("sms_count"); 

            tvEarnings.setText(String.format("₹%.1f", (earnings != null ? earnings : 0.0)));
            tvTotalRefs.setText(String.valueOf(refs != null ? refs : 0));
            tvCoins.setText(String.valueOf(coins != null ? coins : 0));

            // 2. Update local variables for milestones
            userReferralCount = (refs != null) ? refs : 0;
            userSmsCount = (sms != null) ? sms : 0;

            // 3. Get Claimed History
            if (snapshot.contains("claimed_milestones")) {
                claimedMilestones = (Map<String, Object>) snapshot.get("claimed_milestones");
            } else {
                claimedMilestones = new HashMap<>();
            }

            // 4. Refresh List
            adapter.notifyDataSetChanged();
        });
    }

    private void claimReward(Milestone m) {
        // Optimistic UI update prevention
        if (claimedMilestones.containsKey(m.id) && (boolean) claimedMilestones.get(m.id)) return;

        // Database Update
        db.collection("users").document(uid)
                .update("coins", FieldValue.increment(m.reward), 
                        "claimed_milestones." + m.id, true)
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), "Claimed " + m.reward + " Coins!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error claiming", Toast.LENGTH_SHORT).show());
    }

    // --- INNER CLASS: MODEL ---
    class Milestone {
        String id;
        String title;
        int target;
        int reward;
        int type; // 0 = SMS, 1 = Referral

        public Milestone(String id, String title, int target, int reward, int type) {
            this.id = id;
            this.title = title;
            this.target = target;
            this.reward = reward;
            this.type = type;
        }
    }

    // --- INNER CLASS: ADAPTER ---
    class MilestoneAdapter extends RecyclerView.Adapter<MilestoneAdapter.ViewHolder> {
        List<Milestone> list;

        public MilestoneAdapter(List<Milestone> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_milestone, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Milestone m = list.get(position);
            
            holder.title.setText(m.title);
            holder.desc.setText("Reward: " + m.reward + " Spin Coins");

            // Determine Progress
            long current = (m.type == 0) ? userSmsCount : userReferralCount;
            int progress = (int) ((current * 100) / m.target);
            if (progress > 100) progress = 100;
            
            holder.progressBar.setProgress(progress);

            // Determine Button State
            boolean isClaimed = claimedMilestones.containsKey(m.id);
            boolean isCompleted = current >= m.target;

            if (isClaimed) {
                holder.btnClaim.setText("CLAIMED");
                holder.btnClaim.setEnabled(false);
                holder.btnClaim.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
                holder.imgIcon.setColorFilter(Color.GRAY);
            } else if (isCompleted) {
                holder.btnClaim.setText("CLAIM");
                holder.btnClaim.setEnabled(true);
                
                // ✅ FIXED COLOR ERROR HERE
                holder.btnClaim.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6200EE")));
                holder.imgIcon.setColorFilter(Color.parseColor("#FFC107")); // Gold
                
                holder.btnClaim.setOnClickListener(v -> claimReward(m));
            } else {
                // In Progress
                holder.btnClaim.setText(current + " / " + m.target);
                holder.btnClaim.setEnabled(false);
                holder.btnClaim.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
                holder.imgIcon.setColorFilter(Color.GRAY);
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, desc;
            Button btnClaim;
            ImageView imgIcon;
            ProgressBar progressBar;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tv_milestone_title);
                desc = itemView.findViewById(R.id.tv_milestone_desc);
                btnClaim = itemView.findViewById(R.id.btn_claim_milestone);
                imgIcon = itemView.findViewById(R.id.img_milestone_icon);
                progressBar = itemView.findViewById(R.id.progress_milestone);
            }
        }
    }
}
