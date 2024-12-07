package com.nav.arannotationpoc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nav.arannotationpoc.common.adapter.ImageAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImagePreviewActivity extends AppCompatActivity {

    private ImageView fullScreenImageView;
    private RecyclerView imageRecyclerView;

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

        // Load images from local folder
        List<File> imageFiles = loadImagesFromFolder(new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Screenshots"));

        if (imageFiles.isEmpty()) return; // Handle no images case

        // Set default preview
        previewImage(imageFiles.get(0), 0);

        // Set up RecyclerView
        ImageAdapter adapter = new ImageAdapter(this, imageFiles, this::previewImage);
        imageRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageRecyclerView.setAdapter(adapter);
    }

    private void previewImage(File imageFile, int i) {
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        fullScreenImageView.setImageBitmap(bitmap);
    }

    private List<File> loadImagesFromFolder(File folder) {
        List<File> imageFiles = new ArrayList<>();
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".jpg") || file.getName().endsWith(".png")) {
                        imageFiles.add(file);
                    }
                }
            }
        }
        return imageFiles;
    }
}