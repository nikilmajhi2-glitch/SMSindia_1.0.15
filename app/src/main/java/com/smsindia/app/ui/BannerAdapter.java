package com.smsindia.app.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smsindia.app.R;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private List<String> colorCodes; // Using colors for now as placeholders

    public BannerAdapter(List<String> colorCodes) {
        this.colorCodes = colorCodes;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a simple view for the slide
        View view = new View(parent.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        holder.itemView.setBackgroundColor(Color.parseColor(colorCodes.get(position)));
    }

    @Override
    public int getItemCount() {
        return colorCodes.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
