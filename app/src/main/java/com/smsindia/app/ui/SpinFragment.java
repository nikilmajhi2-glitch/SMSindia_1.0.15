package com.smsindia.app.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smsindia.app.R;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SpinFragment extends Fragment {

    private LuckyWheelView wheelView;
    private Button btnSpin;
    private TextView tvTokens;
    private FirebaseFirestore db;
    private String uid;
    
    private long spinTokens = 0;
    private boolean isSpinning = false;

    // Rewards configuration
    // Index: 0=0.6, 1=0.8, 2=10, 3=0, 4=100, 5=0.6
    private final Double[] rewardsValue = {0.6, 0.8, 10.0, 0.0, 100.0, 0.6};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_spin, container, false);

        wheelView = v.findViewById(R.id.wheel_view);
        btnSpin = v.findViewById(R.id.btn_spin_now);
        tvTokens = v.findViewById(R.id.tv_spin_tokens);
        
        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = requireActivity().getSharedPreferences("SMSINDIA_USER", 0);
        uid = prefs.getString("mobile", "");

        fetchSpinTokens();

        btnSpin.setOnClickListener(view -> {
            if (isSpinning) return;
            if (spinTokens <= 0) {
                Toast.makeText(getContext(), "No Spin Coins left! Refer friends to earn more.", Toast.LENGTH_LONG).show();
                return;
            }
            startRiggedSpin();
        });

        return v;
    }

    private void fetchSpinTokens() {
        if(uid.isEmpty()) return;
        db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;
            Long tokens = snapshot.getLong("coins"); 
            spinTokens = (tokens != null) ? tokens : 0;
            tvTokens.setText(String.valueOf(spinTokens));
        });
    }

    private void startRiggedSpin() {
        isSpinning = true;
        btnSpin.setEnabled(false);
        btnSpin.setText("Spinning...");

        // Deduct 1 Coin immediately
        db.collection("users").document(uid).update("coins", FieldValue.increment(-1));

        // --- RIGGED LOGIC ---
        int targetIndex;
        int rand = new Random().nextInt(100); // 0 to 99

        if (rand < 90) {
            // 90% Chance: Small Win (0.6)
            targetIndex = (new Random().nextBoolean()) ? 0 : 5;
        } else if (rand < 98) {
             // 8% Chance: Medium (0.8 or 0)
             targetIndex = (new Random().nextBoolean()) ? 1 : 3;
        } else {
            // 2% Chance: Jackpot (10 or 100)
            targetIndex = (new Random().nextBoolean()) ? 2 : 4;
        }
        
        // Calculate Rotation
        float sectorAngle = 360f / 6f;
        float finalAngle = (360 - (targetIndex * sectorAngle)) + (360 * 10); // 10 full spins
        finalAngle -= (sectorAngle / 2); // Center adjustment

        ObjectAnimator animator = ObjectAnimator.ofFloat(wheelView, "rotation", 0f, finalAngle);
        animator.setDuration(4000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();

        Double reward = rewardsValue[targetIndex];

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isSpinning = false;
                if (isAdded()) { // Check if user is still on this screen
                    btnSpin.setEnabled(true);
                    btnSpin.setText("SPIN NOW");
                    handleWin(reward);
                }
            }
        });
    }

    private void handleWin(Double reward) {
        if (reward > 0) {
            // 1. Update Balance
            db.collection("users").document(uid).update("balance", FieldValue.increment(reward));
            
            // 2. Record History (Added this!)
            Map<String, Object> tx = new HashMap<>();
            tx.put("title", "Spin Reward");
            tx.put("amount", reward);
            tx.put("type", "CREDIT");
            tx.put("timestamp", FieldValue.serverTimestamp());
            
            db.collection("users").document(uid)
                    .collection("transactions").add(tx);

            Toast.makeText(getContext(), "You Won ₹" + reward + "!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Better Luck Next Time!", Toast.LENGTH_SHORT).show();
        }
    }
}
