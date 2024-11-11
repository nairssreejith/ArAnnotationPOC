package com.nav.arannotationpoc.common.viewmodel;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScreenshotViewModel extends AndroidViewModel {

    private final MutableLiveData<String> saveStatusLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    public ScreenshotViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<String> getSaveStatusLiveData() {
        return saveStatusLiveData;
    }

    public void takeScreenshot(Activity activity, GLSurfaceView glSurfaceView, String folderName) {
        glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                int width = glSurfaceView.getWidth();
                int height = glSurfaceView.getHeight();
                Log.d("SHOWREADER", "Width: " + width + " Height: " + height);
                int size = width * height;
                IntBuffer buffer = IntBuffer.allocate(size);
                buffer.order();
                // Read pixels from the OpenGL buffer
                GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);

                int[] pixels = buffer.array();

                // Create a bitmap directly from the RGBA data
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixels));

                // Convert to RGBA_F16 format
                Bitmap rgbaF16Bitmap = bitmap.copy(Bitmap.Config.RGBA_F16, true);

                // Rotate the bitmap if needed
                Bitmap rotatedBitmap = rotateBitmapIfNeeded(rgbaF16Bitmap);

                // Save the bitmap
                int error = GLES20.glGetError();
                if (error != GLES20.GL_NO_ERROR) {
                    Log.e("SHOWREADER", "Error during glReadPixels: " + error);
                }
                new Thread(() -> {
                    new Handler(Looper.getMainLooper()).post(() -> saveBitmap(rotatedBitmap, folderName, activity));
                }).start();
                //saveBitmap(rotatedBitmap, folderName, activity);
            }
        });
    }

    private Bitmap rotateBitmapIfNeeded(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(180); // Rotate by 180 degrees if needed
        matrix.postScale(-1, 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void saveBitmap(Bitmap bitmap, String folderName, Activity activity) {
        // Create a unique file name with the current timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "Screenshot_" + timestamp + ".png";

        // Save directory
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), folderName);

        if (!storageDir.exists() && !storageDir.mkdirs()) {
            saveStatusLiveData.postValue("Failed to create directory");
            return;
        }

        File imageFile = new File(storageDir, fileName);

        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            // Compress the bitmap to PNG and write to output stream
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            saveStatusLiveData.postValue("Screenshot saved");
        } catch (IOException e) {
            e.printStackTrace();
            saveStatusLiveData.postValue("Failed to save screenshot");
        }
    }
}
