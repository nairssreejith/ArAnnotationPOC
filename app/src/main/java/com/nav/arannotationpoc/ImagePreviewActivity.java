package com.nav.arannotationpoc;

import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nav.arannotationpoc.common.adapter.ImageAdapter;
import com.nav.arannotationpoc.common.viewmodel.ImagePreviewViewModel;

import java.io.File;

public class ImagePreviewActivity extends AppCompatActivity {

    private ImageView fullScreenImageView;
    private RecyclerView imageRecyclerView;
    private ImageAdapter adapter;
    private ImagePreviewViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_preview);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fullScreenImageView = findViewById(R.id.fullScreenImageView);
        imageRecyclerView = findViewById(R.id.imageRecyclerView);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ImagePreviewViewModel.class);

        // Observe the images list from ViewModel
        viewModel.getImagesList().observe(this, imageFiles -> {
            if (imageFiles != null && !imageFiles.isEmpty()) {
                // Initialize RecyclerView Adapter
                adapter = new ImageAdapter(this, imageFiles, this::previewImage);
                imageRecyclerView.setAdapter(adapter);

                // Preview the first image by default
                previewImage(imageFiles.get(0), 0);
            }
        });

        // Load images from folder
        File imageFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots");
        viewModel.loadImages(imageFolder);

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        imageRecyclerView.setLayoutManager(layoutManager);
    }

    private void previewImage(File imageFile, int position) {
        if (imageFile.exists()) {
            Glide.with(this)
                    .load(imageFile)
                    .error(R.drawable.image_preview_error)
                    .into(fullScreenImageView);
        }
    }
}
