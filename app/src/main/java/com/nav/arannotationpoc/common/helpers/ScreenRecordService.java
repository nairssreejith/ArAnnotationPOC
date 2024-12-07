package com.nav.arannotationpoc.common.helpers;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenRecordService extends Service {

    public static final String CHANNEL_ID = "ScreenRecordChannel";
    private static final int NOTIFICATION_ID = 1;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;
    private int screenWidth, screenHeight, screenDensity;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Initialize MediaProjectionManager
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Log.d("TRACK_RECORD", "Hitting onCreate.");

        // Get screen dimensions
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        if (windowManager != null && windowManager.getDefaultDisplay() != null) {
            windowManager.getDefaultDisplay().getMetrics(metrics);
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
            screenDensity = metrics.densityDpi;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, getNotification());
        }
        Log.d("TRACK_RECORD", "Hitting onStartCommand.");

        int resultCode = intent.getIntExtra("RESULT_CODE", -1);
        Intent data = intent.getParcelableExtra("DATA_INTENT");

        if (resultCode == Activity.RESULT_OK && data != null) {
            mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);

            // Register the callback to handle the projection lifecycle
            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                    // Handle the stopping of the projection
                    Log.d("ScreenRecordService", "MediaProjection stopped.");
                    showToast("Recording stopped.");
                }
            }, null);

            showToast("Recording started.");
            try {
                Log.d("TRACK_RECORD", "Hitting in if case with resultCode and data not null.");
                startScreenRecording();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return START_STICKY;
    }

    private void startScreenRecording() throws IOException {
        setupMediaRecorder();

        Log.d("TRACK_RECORD", "Hitting in Service startScreenRecording.");

        Surface surface = mediaRecorder.getSurface();
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenRecordService",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
        );

        mediaRecorder.start();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void setupMediaRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();

        Log.d("TRACK_RECORD", "Hitting setUpMediaRecorder.");

        // Configure MediaRecorder
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        File videoFile = getOutputFile();
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mediaRecorder.setVideoSize(screenWidth, screenHeight);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoEncodingBitRate(8 * 1000 * 1000); // 8 Mbps for video quality

        mediaRecorder.prepare();
    }

    private File getOutputFile() {
        File videoDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Recordings");
        if (!videoDir.exists()) {
            videoDir.mkdirs();
        }

        Log.d("TRACK_RECORD", "Hitting getOutputFile.");

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(videoDir, "ScreenRecord_" + timeStamp + ".mp4");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }

        if (virtualDisplay != null) {
            virtualDisplay.release();
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screen Record Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Recording")
                .setContentText("Recording your screen...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();
    }
}