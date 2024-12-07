package com.nav.arannotationpoc.common.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImagePreviewViewModel extends ViewModel {
    private final MutableLiveData<List<File>> imagesList = new MutableLiveData<>();
    private final Executor executor = Executors.newSingleThreadExecutor();

    public LiveData<List<File>> getImagesList() {
        return imagesList;
    }

    public void loadImages(File folder) {
        executor.execute(() -> {
            List<File> filteredImages = new ArrayList<>();
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.length() > 1024 * 100 && isImageFile(file)) {
                            filteredImages.add(file);
                        }
                    }
                }
            }
            imagesList.postValue(filteredImages);
        });
    }

    private boolean isImageFile(File file) {
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
        String fileName = file.getName().toLowerCase();
        for (String extension : imageExtensions) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }
}
