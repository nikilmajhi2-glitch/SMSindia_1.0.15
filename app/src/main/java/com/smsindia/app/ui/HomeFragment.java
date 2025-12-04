package com.smsindia.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable; // Added
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat; // Added
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.smsindia.app.R;
import com.smsindia.app.adapters.BannerAdapter; // Make sure you have this adapter

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView tvBalanceAmount, tvUserMobile;
    private ViewPager2 bannerViewPager;
    private FirebaseFirestore db;
    private String uid;
    
    private final int[] DAILY_REWARDS = {2, 5, 2, 2, 5, 2, 10, 5, 5, 20};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Views
        tvBalanceAmount = v.findViewById(R.id.tv_balance_amount);
        tvUserMobile = v.findViewById(R.id.tv_user_mobile);
        bannerViewPager = v.findViewById(R.id.banner_viewpager);
        Button btnHistory = v.findViewById(R.id.btn_history);
        View dailyCheckinCard = v.findViewById(R.id.card_daily_checkin);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = requireActivity().getSharedPreferences("SMSINDIA_USER", 0);
        uid = prefs.getString("mobile", ""); 

        setupBannerSlider();
        fetchUserBalance();

        dailyCheckinCard.setOnClickListener(view -> showDailyCheckInDialog());
        
        // Note: Make sure HistoryActivity exists or change this
        btnHistory.setOnClickListener(view -> {
            // Intent intent = new Intent(getActivity(), HistoryActivity.class);
            // startActivity(intent);
            Toast.makeText(getContext(), "History Clicked", Toast.LENGTH_SHORT).show();
        });
        
        return v;
    }

    private void showDailyCheckInDialog() {
        if (uid == null || uid.isEmpty()) return;
        
        db.collection("users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) return;

                String lastDate = documentSnapshot.getString("last_checkin_date");
                Long streakLong = documentSnapshot.getLong("streak");
                int currentStreak = (streakLong != null) ? streakLong.intValue() : 0;

                String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                int streakToDisplay = 1;
                boolean canClaim = true;
                
                if (todayDate.equals(lastDate)) {
                    streakToDisplay = currentStreak;
                    canClaim = false;
                } else {
                    streakToDisplay = currentStreak + 1; 
                    if(streakToDisplay > 10) streakToDisplay = 1; 
                }

                launchDialogUI(streakToDisplay, canClaim, todayDate);
            });
    }

    private void launchDialogUI(int currentDay, boolean canClaim, String todayDate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_daily_checkin, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if(dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvStreak = view.findViewById(R.id.tv_streak_status);
        Button btnClaim = view.findViewById(R.id.btn_claim_reward);
        View btnClose = view.findViewById(R.id.btn_close_dialog); 

        tvStreak.setText("Current Streak: Day " + currentDay);

        // Get Brand Colors safely
        int primaryColor = ContextCompat.getColor(getContext(), R.color.app_primary);
        int grayColor = ContextCompat.getColor(getContext(), R.color.gray_text);

        int[] viewIds = {
            R.id.day1, R.id.day2, R.id.day3, R.id.day4, R.id.day5,
            R.id.day6, R.id.day7, R.id.day8, R.id.day9, R.id.day10
        };

        for (int i = 0; i < viewIds.length; i++) {
            int dayNum = i + 1;
            View dayView = view.findViewById(viewIds[i]);
            
            TextView lblDay = dayView.findViewById(R.id.lbl_day);
            TextView lblAmount = dayView.findViewById(R.id.lbl_amount);
            
            // Parent of Amount is the circular background
            View bgCircle = (View) dayView.findViewById(R.id.lbl_amount).getParent(); 
            
            lblDay.setText("Day " + dayNum);
            lblAmount.setText("₹" + DAILY_REWARDS[i]);

            if (dayNum < currentDay) {
                // PAST DAYS (Completed)
                dayView.setAlpha(0.5f);
                // Set circle to gray
                bgCircle.setBackgroundResource(R.drawable.bg_circle_gray); 
            } else if (dayNum == currentDay) {
                // TODAY (Active) - Use Brand Color
                bgCircle.setBackgroundResource(R.drawable.bg_circle_brand);
                lblDay.setTextColor(primaryColor);
                lblDay.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                // FUTURE DAYS
                bgCircle.setBackgroundResource(R.drawable.bg_circle_gray);
            }
        }

        if (!canClaim) {
            btnClaim.setText("COME BACK TOMORROW");
            btnClaim.setEnabled(false);
            btnClaim.setBackgroundTintList(getContext().getColorStateList(android.R.color.darker_gray));
        } else {
            int rewardAmount = DAILY_REWARDS[currentDay - 1];
            btnClaim.setText("CLAIM ₹" + rewardAmount);
            // Button uses Brand Color via XML, or set here:
            btnClaim.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.app_primary));
            
            btnClaim.setOnClickListener(v -> {
                claimReward(currentDay, rewardAmount, todayDate, dialog);
            });
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void claimReward(int day, int amount, String todayDate, AlertDialog dialog) {
        WriteBatch batch = db.batch();
        DocumentReference userRef = db.collection("users").document(uid);
        DocumentReference historyRef = db.collection("users").document(uid).collection("transactions").document();

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("balance", FieldValue.increment(amount));
        userUpdates.put("last_checkin_date", todayDate);
        userUpdates.put("streak", day);
        batch.update(userRef, userUpdates);

        Map<String, Object> txData = new HashMap<>();
        txData.put("title", "Daily Check-in (Day " + day + ")");
        txData.put("amount", amount);
        txData.put("type", "CREDIT");
        txData.put("timestamp", FieldValue.serverTimestamp());
        batch.set(historyRef, txData);

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Claimed ₹" + amount + " successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

     private void setupBannerSlider() {
        List<BannerAdapter.BannerModel> banners = new ArrayList<>();

        // Banner 1: Referral (Gold/Orange)
        banners.add(new BannerAdapter.BannerModel(
                "Refer & Earn",
                "Get ₹50 instantly for every friend you invite.",
                "#FF9800" 
        ));

        // Banner 2: Task (Royal Blue)
        banners.add(new BannerAdapter.BannerModel(
                "SMS Tasks Live",
                "Start the auto-sender now and earn passively.",
                "#2962FF" 
        ));

        // Banner 3: Daily Bonus (Green)
        banners.add(new BannerAdapter.BannerModel(
                "Daily Check-in",
                "Don't break your streak! Claim free coins today.",
                "#4CAF50" 
        ));

        BannerAdapter adapter = new BannerAdapter(banners);
        bannerViewPager.setAdapter(adapter);
    }

    private void fetchUserBalance() {
        if (uid == null || uid.isEmpty()) return;
        db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (e == null && snapshot != null && snapshot.exists()) {
                Double bal = snapshot.getDouble("balance");
                if (bal != null) tvBalanceAmount.setText(String.format("₹ %.2f", bal));
                String name = snapshot.getString("name");
                if(name != null) tvUserMobile.setText(name);
            }
        });
    }
}
