package com.nav.arannotationpoc.common.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.nav.arannotationpoc.R;

import java.io.File;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private final List<File> imageFiles;
    private final Context context;
    private final OnImageClickListener onImageClickListener;
    private int selectedPosition = 0; // Default selection

    public interface OnImageClickListener {
        void onImageClick(File imageFile, int position);
    }

    public ImageAdapter(Context context, List<File> imageFiles, OnImageClickListener listener) {
        this.context = context;
        this.imageFiles = imageFiles;
        this.onImageClickListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        File imageFile = imageFiles.get(position);

        Glide.with(context)
                .load(imageFile)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache for faster subsequent loads
                .placeholder(R.drawable.image_preview_placeholder) // Optional: Show a placeholder while loading
                .error(R.drawable.image_preview_error) // Optional: Show an error image if loading fails
                .into(holder.imageView);

        // Highlight selected image
        if (selectedPosition == position) {
            holder.borderOverlay.setVisibility(View.VISIBLE);
        } else {
            holder.borderOverlay.setVisibility(View.GONE);
        }


        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);  // Update the previous selected
            notifyItemChanged(selectedPosition);  // Update the newly selected

            // Trigger the callback
            onImageClickListener.onImageClick(imageFile, selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return imageFiles.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        View borderOverlay;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.previewImageView);
            borderOverlay = itemView.findViewById(R.id.borderOverlay);
        }
    }
}
