package com.smsindia.app.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smsindia.app.R;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvBalanceAmount, tvUserMobile;
    private ViewPager2 bannerViewPager;
    private FirebaseFirestore db;
    private String uid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Initialize Views based on your NEW XML IDs
        tvBalanceAmount = v.findViewById(R.id.tv_balance_amount);
        tvUserMobile = v.findViewById(R.id.tv_user_mobile);
        bannerViewPager = v.findViewById(R.id.banner_viewpager);
        
        Button btnAddMoney = v.findViewById(R.id.btn_add_money);
        Button btnHistory = v.findViewById(R.id.btn_history);
        View dailyCheckinCard = v.findViewById(R.id.card_daily_checkin);

        // 2. Initialize Firebase
        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = requireActivity().getSharedPreferences("SMSINDIA_USER", 0);
        uid = prefs.getString("mobile", ""); 

        // 3. Setup Logic
        setupBannerSlider();
        fetchUserBalance();

        // 4. Click Listeners
        dailyCheckinCard.setOnClickListener(view -> 
            Toast.makeText(getContext(), "Daily Check-in Clicked", Toast.LENGTH_SHORT).show()
        );
        
        btnAddMoney.setOnClickListener(view -> 
            Toast.makeText(getContext(), "Add Money Clicked", Toast.LENGTH_SHORT).show()
        );

        return v;
    }

    private void setupBannerSlider() {
        // Since we are using ViewPager2, we need a simple adapter.
        // For now, let's just put placeholder logic or leave it empty to prevent crashes.
        // If you want images here, you need a RecyclerView Adapter.
        // See step 3 below for the Adapter code.
        
        List<String> colors = new ArrayList<>();
        colors.add("#FF5733"); // Dummy data
        colors.add("#33FF57");
        colors.add("#3357FF");

        BannerAdapter adapter = new BannerAdapter(colors);
        bannerViewPager.setAdapter(adapter);
    }

    private void fetchUserBalance() {
        if (uid == null || uid.isEmpty()) return;

        db.collection("users").document(uid)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) return;
                if (snapshot != null && snapshot.exists()) {
                    Double balance = snapshot.getDouble("balance");
                    if (balance != null) {
                        tvBalanceAmount.setText(String.format("₹ %.2f", balance));
                    }
                    String name = snapshot.getString("name");
                    if(name != null) tvUserMobile.setText(name);
                }
            });
    }
}
