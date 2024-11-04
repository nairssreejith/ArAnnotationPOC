package com.nav.arannotationpoc.common.helpers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotHelper {

    public static void takeScreenshot(Activity activity, GLSurfaceView glSurfaceView, View overlayView, String folderName) {
        glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                int width = glSurfaceView.getWidth();
                int height = glSurfaceView.getHeight();
                int size = width * height;
                IntBuffer buffer = IntBuffer.allocate(size);

                // Read pixels from the OpenGL buffer
                GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);

                int[] pixels = buffer.array();

                // Create a bitmap directly from the RGBA data
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixels));

                /*int[] rgbaPixels = buffer.array();
                int[] argbPixels = new int[size];

                // Correct color format from RGBA to ARGB and flip vertically
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        int index = i * width + j;
                        int pixel = rgbaPixels[index];

                        int red = (pixel >> 24) & 0xFF;
                        int green = (pixel >> 16) & 0xFF;
                        int blue = (pixel >> 8) & 0xFF;
                        int alpha = pixel & 0xFF;

                        // Convert RGBA to ARGB for Bitmap
                        int argbPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;

                        // Flip vertically
                        int flippedIndex = (height - i - 1) * width + j;
                        argbPixels[flippedIndex] = argbPixel;
                    }
                }

                // Create bitmap from the ARGB pixel array
                Bitmap bitmap = Bitmap.createBitmap(argbPixels, width, height, Bitmap.Config.ARGB_8888);*/

                // Correct orientation (rotate if needed)
                Bitmap rotatedBitmap = rotateBitmapIfNeeded(bitmap);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Save or use the bitmap as needed here
                        // For example, saveBitmap(bitmap);
                        saveBitmap(rotatedBitmap, folderName, activity);
                    }
                });
            }
        });
    }

    private static Bitmap rotateBitmapIfNeeded(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(180);// Rotate by 180 degrees if needed (adjust based on device orientation)
        matrix.postScale(-1, 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static void saveBitmap(Bitmap bitmap, String folderName, Activity activity) {
        // Create a unique file name with the current timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "Screenshot_" + timestamp + ".png";

        // Save directory
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), folderName);

        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Toast.makeText(activity, "Failed to create directory", Toast.LENGTH_SHORT).show();
        }

        File imageFile = new File(storageDir, fileName);

        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            // Compress the bitmap to PNG and write to output stream
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            Toast.makeText(activity, "Screenshot saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Failed to save screenshot", Toast.LENGTH_SHORT).show();
        }
    }
}
