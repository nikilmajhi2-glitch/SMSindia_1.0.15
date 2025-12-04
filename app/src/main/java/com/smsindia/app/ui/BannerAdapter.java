package com.smsindia.app.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smsindia.app.R;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private List<BannerModel> bannerList;

    public BannerAdapter(List<BannerModel> bannerList) {
        this.bannerList = bannerList;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the new nice layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        BannerModel banner = bannerList.get(position);

        holder.tvTitle.setText(banner.title);
        holder.tvDesc.setText(banner.description);
        
        // Parse color string safely
        try {
            holder.layout.setBackgroundColor(Color.parseColor(banner.backgroundColor));
        } catch (Exception e) {
            holder.layout.setBackgroundColor(Color.parseColor("#2962FF")); // Default Blue
        }

        // Set Icons based on context (Optional)
        if(banner.title.contains("Refer")) {
            holder.icon.setImageResource(android.R.drawable.ic_menu_share);
        } else if(banner.title.contains("Daily")) {
            holder.icon.setImageResource(android.R.drawable.ic_menu_my_calendar);
        } else {
            holder.icon.setImageResource(android.R.drawable.stat_sys_upload);
        }
    }

    @Override
    public int getItemCount() {
        return bannerList.size();
    }

    // --- Data Model ---
    public static class BannerModel {
        String title;
        String description;
        String backgroundColor;

        public BannerModel(String title, String description, String backgroundColor) {
            this.title = title;
            this.description = description;
            this.backgroundColor = backgroundColor;
        }
    }

    // --- ViewHolder ---
    static class BannerViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc;
        ImageView icon;
        RelativeLayout layout;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_banner_title);
            tvDesc = itemView.findViewById(R.id.tv_banner_desc);
            icon = itemView.findViewById(R.id.img_banner_icon);
            layout = itemView.findViewById(R.id.banner_layout);
        }
    }
}
