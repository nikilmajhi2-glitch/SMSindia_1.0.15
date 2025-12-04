package com.smsindia.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.smsindia.app.R;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvMobile, tvBalance, tvBankName, tvBankAc;
    private ImageView imgProfile;
    private LinearLayout layoutSavedBank;
    private FirebaseFirestore db;
    private String uid;
    private double currentBalance = 0.0;
    private boolean hasBankDetails = false;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri selectedImage = result.getData().getData();
                try {
                    InputStream imageStream = requireActivity().getContentResolver().openInputStream(selectedImage);
                    Bitmap selectedBitmap = BitmapFactory.decodeStream(imageStream);
                    imgProfile.setImageBitmap(selectedBitmap);
                    Toast.makeText(getContext(), "Profile picture updated locally", Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        tvMobile = v.findViewById(R.id.tv_profile_mobile);
        tvBalance = v.findViewById(R.id.tv_profile_balance);
        imgProfile = v.findViewById(R.id.img_profile);
        Button btnWithdraw = v.findViewById(R.id.btn_withdraw);
        Button btnHistory = v.findViewById(R.id.btn_withdraw_history);
        TextView btnAddBank = v.findViewById(R.id.btn_add_bank);
        
        layoutSavedBank = v.findViewById(R.id.layout_saved_bank);
        tvBankName = v.findViewById(R.id.tv_bank_name);
        tvBankAc = v.findViewById(R.id.tv_bank_ac);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = requireActivity().getSharedPreferences("SMSINDIA_USER", 0);
        uid = prefs.getString("mobile", "");

        tvMobile.setText(uid);
        fetchUserData();

        imgProfile.setOnClickListener(view -> openGallery());
        btnAddBank.setOnClickListener(view -> showAddBankDialog());
        
        btnWithdraw.setOnClickListener(view -> requestWithdrawal());
        
        // Open History Page
        btnHistory.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), WithdrawalHistoryActivity.class);
            startActivity(intent);
        });

        return v;
    }

    private void fetchUserData() {
        if (uid.isEmpty()) return;
        db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;

            Double bal = snapshot.getDouble("balance");
            currentBalance = (bal != null) ? bal : 0.0;
            tvBalance.setText(String.format("₹ %.2f", currentBalance));

            String name = snapshot.getString("name");
            if (name != null && !name.isEmpty()) tvMobile.setText(name);

            if (snapshot.contains("bank_account")) {
                Map<String, Object> bankMap = (Map<String, Object>) snapshot.get("bank_account");
                if (bankMap != null) {
                    hasBankDetails = true;
                    layoutSavedBank.setVisibility(View.VISIBLE);
                    tvBankName.setText((String) bankMap.get("bank_name"));
                    tvBankAc.setText("AC: " + bankMap.get("account_no"));
                }
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void showAddBankDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_bank, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        EditText etName = view.findViewById(R.id.et_bank_name);
        EditText etAc = view.findViewById(R.id.et_bank_ac);
        EditText etIfsc = view.findViewById(R.id.et_bank_ifsc);
        Button btnSave = view.findViewById(R.id.btn_save_bank);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String ac = etAc.getText().toString().trim();
            String ifsc = etIfsc.getText().toString().trim();

            if (name.isEmpty() || ac.isEmpty() || ifsc.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            saveBankDetails(name, ac, ifsc);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void saveBankDetails(String name, String ac, String ifsc) {
        Map<String, Object> bankData = new HashMap<>();
        bankData.put("bank_name", name);
        bankData.put("account_no", ac);
        bankData.put("ifsc", ifsc);

        db.collection("users").document(uid)
            .update("bank_account", bankData)
            .addOnSuccessListener(a -> Toast.makeText(getContext(), "Bank details saved!", Toast.LENGTH_SHORT).show());
    }

    private void requestWithdrawal() {
        if (currentBalance < 100) {
            Toast.makeText(getContext(), "Minimum withdrawal is ₹100", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasBankDetails) {
            Toast.makeText(getContext(), "Please add bank details first", Toast.LENGTH_LONG).show();
            showAddBankDialog();
            return;
        }

        // --- Perform Withdrawal (Batch Write) ---
        WriteBatch batch = db.batch();
        DocumentReference userRef = db.collection("users").document(uid);
        DocumentReference withdrawRef = db.collection("users").document(uid).collection("withdrawals").document();
        DocumentReference historyRef = db.collection("users").document(uid).collection("transactions").document();

        // 1. Deduct Balance
        batch.update(userRef, "balance", FieldValue.increment(-currentBalance));

        // 2. Create Withdrawal Request
        Map<String, Object> req = new HashMap<>();
        req.put("amount", currentBalance);
        req.put("status", "Reviewing"); // Status: Reviewing -> Processing -> Completed
        req.put("timestamp", FieldValue.serverTimestamp());
        batch.set(withdrawRef, req);

        // 3. Add to Transaction History
        Map<String, Object> tx = new HashMap<>();
        tx.put("title", "Withdrawal Request");
        tx.put("amount", currentBalance);
        tx.put("type", "DEBIT");
        tx.put("timestamp", FieldValue.serverTimestamp());
        batch.set(historyRef, tx);

        batch.commit().addOnSuccessListener(a -> {
            Toast.makeText(getContext(), "Withdrawal Request Sent!", Toast.LENGTH_LONG).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
