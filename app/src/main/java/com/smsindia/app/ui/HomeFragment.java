package com.smsindia.app.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.smsindia.app.R;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvBalance, tvUsername;
    private ImageSlider imageSlider;
    private LinearLayout btnDailyCheckIn;
    private FirebaseFirestore db;
    private String uid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Views
        tvBalance = v.findViewById(R.id.tv_balance);
        tvUsername = v.findViewById(R.id.tv_username);
        imageSlider = v.findViewById(R.id.image_slider);
        btnDailyCheckIn = v.findViewById(R.id.btn_daily_checkin);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get User ID from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("SMSINDIA_USER", 0);
        uid = prefs.getString("mobile", ""); // Assuming mobile is the Doc ID, or use a specific UID

        // Setup UI Components
        setupBanners();
        fetchUserBalance();
        setupClickListeners();

        return v;
    }

    private void setupBanners() {
        // Add dummy banners or fetch from server
        List<SlideModel> slideModels = new ArrayList<>();
        slideModels.add(new SlideModel("https://via.placeholder.com/800x400/FF5733/ffffff?text=Welcome+Offer", ScaleTypes.FIT));
        slideModels.add(new SlideModel("https://via.placeholder.com/800x400/33FF57/ffffff?text=Refer+Earn", ScaleTypes.FIT));
        slideModels.add(new SlideModel("https://via.placeholder.com/800x400/3357FF/ffffff?text=Daily+Bonus", ScaleTypes.FIT));
        
        imageSlider.setImageList(slideModels);
    }

    private void fetchUserBalance() {
        if (uid == null || uid.isEmpty()) {
            tvBalance.setText("₹ 0.00");
            return;
        }

        // Listening to real-time updates
        db.collection("users").document(uid)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("HomeFragment", "Listen failed.", e);
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            // Retrieve balance field. 
                            // Make sure the field name matches your Firestore (e.g., "balance" or "wallet_balance")
                            Double balance = snapshot.getDouble("balance");
                            String name = snapshot.getString("name");

                            if (balance != null) {
                                tvBalance.setText(String.format("₹ %.2f", balance));
                            } else {
                                tvBalance.setText("₹ 0.00");
                            }
                            
                            if(name != null){
                                tvUsername.setText(name);
                            }
                        } else {
                            Log.d("HomeFragment", "Current data: null");
                        }
                    }
                });
    }

    private void setupClickListeners() {
        btnDailyCheckIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performDailyCheckIn();
            }
        });
    }

    private void performDailyCheckIn() {
        // Logic for daily check-in
        // 1. Check if user already checked in today
        // 2. Update Firestore balance
        Toast.makeText(getContext(), "Checking in... (Add Logic Here)", Toast.LENGTH_SHORT).show();
        
        // Example of adding money (WARNING: Better done via Cloud Functions for security)
        /*
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.update("balance", FieldValue.increment(1.0))
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Received ₹1 Daily Bonus!", Toast.LENGTH_SHORT).show());
        */
    }
}
