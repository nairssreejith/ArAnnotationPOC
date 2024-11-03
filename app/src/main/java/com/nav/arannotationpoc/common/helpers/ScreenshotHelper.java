package com.nav.arannotationpoc.common.helpers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotHelper {

    public static void takeScreenshot(Activity activity, GLSurfaceView glSurfaceView, View overlayView, String folderName) {
        // Create bitmap for GLSurfaceView
        Bitmap glBitmap = Bitmap.createBitmap(glSurfaceView.getWidth(), glSurfaceView.getHeight(), Bitmap.Config.ARGB_8888);

        // Use PixelCopy to capture GLSurfaceView content
        PixelCopy.request(activity.getWindow(), new Rect(0, 0, glSurfaceView.getWidth(), glSurfaceView.getHeight()), glBitmap,
                copyResult -> {
                    if (copyResult == PixelCopy.SUCCESS) {
                        // Combine GLSurfaceView bitmap with overlay views
                        Bitmap finalBitmap = combineBitmaps(glBitmap, overlayView);
                        // Save the final bitmap
                        saveBitmap(glBitmap, folderName, activity);
                    } else {
                        Toast.makeText(activity, "Screenshot failed", Toast.LENGTH_SHORT).show();
                    }
                }, new android.os.Handler());
    }

    private static Bitmap combineBitmaps(Bitmap glBitmap, View overlayView) {
        // Create a bitmap with the same size as the GLSurfaceView
        Bitmap resultBitmap = Bitmap.createBitmap(glBitmap.getWidth(), glBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);

        // Draw GLSurfaceView content first
        canvas.drawBitmap(glBitmap, 0, 0, null);

        // Draw overlay view (e.g., button bar) on top of it
        overlayView.draw(canvas);

        return resultBitmap;
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
