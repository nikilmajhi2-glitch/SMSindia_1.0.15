package com.smsindia.app.ui;

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
import java.util.Random;

public class SpinFragment extends Fragment {

    private LuckyWheelView wheelView;
    private Button btnSpin;
    private TextView tvTokens;
    private FirebaseFirestore db;
    private String uid;
    
    private long spinTokens = 0;
    private boolean isSpinning = false;

    // Wheel Data corresponding to the LuckyWheelView string array
    // {"₹0.6", "₹0.8", "₹10", "₹0", "₹100", "₹0.6"}
    // Indices:
    // 0: ₹0.6
    // 1: ₹0.8
    // 2: ₹10
    // 3: ₹0
    // 4: ₹100
    // 5: ₹0.6
    private Double[] rewardsValue = {0.6, 0.8, 10.0, 0.0, 100.0, 0.6};

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
                Toast.makeText(getContext(), "No Spin Tokens left! Refer friends to earn more.", Toast.LENGTH_LONG).show();
                return;
            }
            startRiggedSpin();
        });

        return v;
    }

    private void fetchSpinTokens() {
        if(uid.isEmpty()) return;
        db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            Long tokens = snapshot.getLong("coins"); // Assuming 'coins' is spin token
            spinTokens = (tokens != null) ? tokens : 0;
            tvTokens.setText(String.valueOf(spinTokens));
        });
    }

    private void startRiggedSpin() {
        isSpinning = true;
        btnSpin.setEnabled(false);

        // Deduct Token in Cloud
        db.collection("users").document(uid).update("coins", FieldValue.increment(-1));

        // --- PROBABILITY LOGIC (96% for 0.6) ---
        int targetIndex;
        int rand = new Random().nextInt(100); // 0 to 99

        if (rand < 96) {
            // 96% Chance: Land on Index 0 or 5 (Values 0.6)
            targetIndex = (new Random().nextBoolean()) ? 0 : 5;
        } else {
            // 4% Chance: Land on others (1, 2, 3, 4)
            // 1=0.8, 2=10, 3=0, 4=100
             int[] others = {1, 2, 3, 4};
             targetIndex = others[new Random().nextInt(others.length)];
        }
        
        // --- CALCULATE ROTATION ---
        // 6 sectors total. Each is 60 degrees.
        // We want the target sector to stop at the Right side (Indicator at 0 degrees usually, but let's assume Arrow is at 3 o'clock / 0 deg on unit circle)
        // Adjust depending on where your start angle in LuckyWheelView is.
        // In our View, index 0 starts at 0 deg (3 o'clock) going clockwise.
        
        float sectorAngle = 360f / 6f;
        
        // To land index i at the indicator (angle 0), we need to rotate negative i * sectorAngle.
        // Adding rotation pushes the slice defined at start away.
        // Final Rotation = (360 - (targetIndex * sectorAngle)) + (360 * rotations)
        
        float finalAngle = (360 - (targetIndex * sectorAngle)) + (360 * 10); // 10 spins
        
        // Add a little randomness inside the sector center to look real
        // Center of sector is sectorAngle/2.
        finalAngle -= (sectorAngle / 2); 

        ObjectAnimator animator = ObjectAnimator.ofFloat(wheelView, "rotation", 0f, finalAngle);
        animator.setDuration(4000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();

        Double reward = rewardsValue[targetIndex];

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                isSpinning = false;
                btnSpin.setEnabled(true);
                handleWin(reward);
            }
        });
    }

    private void handleWin(Double reward) {
        if (reward > 0) {
            Toast.makeText(getContext(), "You Won ₹" + reward + "!", Toast.LENGTH_LONG).show();
            db.collection("users").document(uid).update("balance", FieldValue.increment(reward));
            
            // Add Transaction History for tracking
            // Use HashMap and specific collection reference if you implemented history earlier
        } else {
            Toast.makeText(getContext(), "Better Luck Next Time!", Toast.LENGTH_SHORT).show();
        }
    }
}
