package com.nav.arannotationpoc;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;

import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.nav.arannotationpoc.common.helpers.AppSettings;
import com.nav.arannotationpoc.common.helpers.BiquadFilter;
import com.nav.arannotationpoc.common.helpers.CameraPermissionHelper;
import com.nav.arannotationpoc.common.helpers.DisplayRotationHelper;
import com.nav.arannotationpoc.common.helpers.ScreenRecordService;
import com.nav.arannotationpoc.common.viewmodel.ScreenRecordViewModel;
import com.nav.arannotationpoc.common.viewmodel.ScreenshotViewModel;
import com.nav.arannotationpoc.common.rendering.BackgroundRenderer;
import com.nav.arannotationpoc.common.rendering.LineShaderRenderer;
import com.nav.arannotationpoc.common.rendering.LineUtils;


import android.util.DisplayMetrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer, GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener{
    private static final String TAG = MainActivity.class.getSimpleName();

    private GLSurfaceView mSurfaceView;
    private final List<Plane> trackedPlanes = new ArrayList<>();

    private ScreenshotViewModel screenshotViewModel;

    private Config mDefaultConfig;
    private Session mSession;
    private BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private LineShaderRenderer mLineShaderRenderer = new LineShaderRenderer();
    private Frame mFrame;

    private float[] projmtx = new float[16];
    private float[] viewmtx = new float[16];
    private float[] mZeroMatrix = new float[16];

    private boolean mPaused = false;

    private float mScreenWidth = 0;
    private float mScreenHeight = 0;

    Vector2f touchPoint = new Vector2f(100, 200); // Example screen touch point

    // Define the background plane
    Vector3f planePosition = new Vector3f(0, 0, 0); // Position of the plane at z=0
    Vector3f planeNormal = new Vector3f(0, 0, 1);   // Normal vector of the plane

    private BiquadFilter biquadFilter;
    private Vector3f mLastPoint;
    private AtomicReference<Vector2f> lastTouch = new AtomicReference<>();

    private GestureDetector mDetector;

    private LinearLayout mSettingsUI;
    private LinearLayout mButtonBar;

    private SeekBar mLineWidthBar;
    private SeekBar mLineDistanceScaleBar;
    private SeekBar mSmoothingBar;


    private float mLineWidthMax = 0.13f;
    private float mDistanceScale = 0.0f;
    private float mLineSmoothing = 0.1f;

    private float[] mLastFramePosition;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    private AtomicBoolean bIsTracking = new AtomicBoolean(true);
    private AtomicBoolean bReCenterView = new AtomicBoolean(false);
    private AtomicBoolean bTouchDown = new AtomicBoolean(false);
    private AtomicBoolean bClearDrawing = new AtomicBoolean(false);
    private AtomicBoolean bLineParameters = new AtomicBoolean(false);
    private AtomicBoolean bUndo = new AtomicBoolean(false);
    private AtomicBoolean bNewStroke = new AtomicBoolean(false);

    private ArrayList<ArrayList<Vector3f>> mStrokes;

    private DisplayRotationHelper mDisplayRotationHelper;
    private Snackbar mMessageSnackbar;

    private boolean bInstallRequested;

    private TrackingState mState;


    private static final int REQUEST_CODE_MEDIA_PROJECTION = 1000;
    private MediaProjectionManager mediaProjectionManager;
    private Intent mediaProjectionData;
    private int mediaProjectionResultCode = -1;
    private ScreenRecordViewModel viewModel;
    private DisplayMetrics displayMetrics;

    /**
     * Setup the app when main activity is created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.surfaceView);
        mButtonBar = findViewById(R.id.button_bar);

        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        viewModel = new ViewModelProvider(this).get(ScreenRecordViewModel.class);

        ImageButton btnRecord = findViewById(R.id.recordButton);
        viewModel.getIsRecording().observe(this, isRecording -> {
            if (isRecording) {
                btnRecord.setImageResource(R.drawable.baseline_stop_circle_24);
                startMediaProjectionRequest();
            } else {
                btnRecord.setImageResource(R.drawable.baseline_video_camera_24);
                stopScreenRecording();
            }
        });
        btnRecord.setOnClickListener(v -> viewModel.toggleRecordingState());

        displayMetrics = getResources().getDisplayMetrics();

        /*mLineDistanceScaleBar.setProgress(sharedPref.getInt("mLineDistanceScale", 1));
        mLineWidthBar.setProgress(sharedPref.getInt("mLineWidth", 10));
        mSmoothingBar.setProgress(sharedPref.getInt("mSmoothing", 50));*/

        /*mDistanceScale = LineUtils.map((float) mLineDistanceScaleBar.getProgress(), 0, 100, 1, 200, true);
        mLineWidthMax = LineUtils.map((float) mLineWidthBar.getProgress(), 0f, 100f, 0.1f, 5f, true);
        mLineSmoothing = LineUtils.map((float) mSmoothingBar.getProgress(), 0, 100, 0.01f, 0.2f, true);
*/
        /*SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            *//**
             * Listen for seekbar changes, and update the settings
             *//*
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SharedPreferences.Editor editor = sharedPref.edit();

                if (seekBar == mLineDistanceScaleBar) {
                    editor.putInt("mLineDistanceScale", progress);
                    mDistanceScale = LineUtils.map((float) progress, 0f, 100f, 1f, 200f, true);
                } else if (seekBar == mLineWidthBar) {
                    editor.putInt("mLineWidth", progress);
                    mLineWidthMax = LineUtils.map((float) progress, 0f, 100f, 0.1f, 5f, true);
                } else if (seekBar == mSmoothingBar) {
                    editor.putInt("mSmoothing", progress);
                    mLineSmoothing = LineUtils.map((float) progress, 0, 100, 0.01f, 0.2f, true);
                }
                mLineShaderRenderer.bNeedsUpdate.set(true);

                editor.apply();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
*/
        /*mLineDistanceScaleBar.setOnSeekBarChangeListener(seekBarChangeListener);
        mLineWidthBar.setOnSeekBarChangeListener(seekBarChangeListener);
        mSmoothingBar.setOnSeekBarChangeListener(seekBarChangeListener);

        // Hide the settings ui
        mSettingsUI.setVisibility(View.GONE);*/

        mDisplayRotationHelper = new DisplayRotationHelper(/*context=*/ this);
        // Reset the zero matrix
        Matrix.setIdentityM(mZeroMatrix, 0);

        mLastPoint = new Vector3f(0, 0, 0);

        bInstallRequested = false;


        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Setup touch detector
        mDetector = new GestureDetector(this, this);
        mDetector.setOnDoubleTapListener(this);
        mStrokes = new ArrayList<>();

        // Initialize ViewModel
        screenshotViewModel = new ViewModelProvider(this).get(ScreenshotViewModel.class);

        // Observe LiveData for save status
        screenshotViewModel.getSaveStatusLiveData().observe(this, status -> {
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        });


    }


    /**
     * addStroke adds a new stroke to the scene
     *
     * @param touchPoint a 2D point in screen space and is projected into 3D world space
     */
    private void addStroke(Vector2f touchPoint) {
        /*List<Vector3f> newPoints = LineUtils.GetWorldCoords2(touchPoint, mScreenWidth, mScreenHeight, projmtx, viewmtx, planePosition, planeNormal);
        if (newPoints != null) {
            for (Vector3f point : newPoints) {
                // Convert Vector3f points to OpenGL vertices and render as line segments
                addStroke(point);
            }
        }*/

        Vector3f newPoint = LineUtils.GetWorldCoords(touchPoint, mScreenWidth, mScreenHeight, projmtx, viewmtx);
        addStroke(newPoint);


    }


    /**
     * addPoint adds a point to the current stroke
     *
     * @param touchPoint a 2D point in screen space and is projected into 3D world space
     */
    private void addPoint(Vector2f touchPoint) {
        /*List<Vector3f> newPoints = LineUtils.GetWorldCoords2(touchPoint, mScreenWidth, mScreenHeight, projmtx, viewmtx, planePosition,planeNormal);
        if (newPoints != null) {
            for (Vector3f point : newPoints) {
                // Convert Vector3f points to OpenGL vertices and render as line segments
                addPoint(point);
            }
        }*/

        Vector3f newPoint = LineUtils.GetWorldCoords(touchPoint, mScreenWidth, mScreenHeight, projmtx, viewmtx);
        addPoint(newPoint);

    }


    /**
     * addStroke creates a new stroke
     *
     * @param newPoint a 3D point in world space
     */
    private void addStroke(Vector3f newPoint) {
        biquadFilter = new BiquadFilter(mLineSmoothing);
        for (int i = 0; i < 1500; i++) {
            biquadFilter.update(newPoint);
        }
        Vector3f p = biquadFilter.update(newPoint);
        mLastPoint = new Vector3f(p);
        mStrokes.add(new ArrayList<Vector3f>());
        mStrokes.get(mStrokes.size() - 1).add(mLastPoint);
    }

    /**
     * addPoint adds a point to the current stroke
     *
     * @param newPoint a 3D point in world space
     */
    private void addPoint(Vector3f newPoint) {
        if (LineUtils.distanceCheck(newPoint, mLastPoint)) {
            Vector3f p = biquadFilter.update(newPoint);
            mLastPoint = new Vector3f(p);
            mStrokes.get(mStrokes.size() - 1).add(mLastPoint);
        }
    }


    /**
     * onResume part of the Android Activity Lifecycle
     */
    @Override
    protected void onResume() {
        super.onResume();



        if (mSession == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !bInstallRequested)) {
                    case INSTALL_REQUESTED:
                        bInstallRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                mSession = new Session(/* context= */ this);
            } catch (UnavailableArcoreNotInstalledException
                     | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            // Create default config and check if supported.
            Config config = new Config(mSession);
            if (!mSession.isSupported(config)) {
                Log.e(TAG, "Exception creating session Device Does Not Support ARCore", exception);
            }
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
            config.setFocusMode(Config.FocusMode.AUTO);
            mSession.configure(config);
        }
        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            throw new RuntimeException(e);
        }
        mSurfaceView.onResume();
        mDisplayRotationHelper.onResume();
        mPaused = false;
    }

    /**
     * onPause part of the Android Activity Lifecycle
     */
    @Override
    public void onPause() {
        super.onPause();
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.

        if (mSession != null) {
            mDisplayRotationHelper.onPause();
            mSurfaceView.onPause();
            mSession.pause();
        }

        mPaused = false;

        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mScreenHeight = displayMetrics.heightPixels;
        mScreenWidth = displayMetrics.widthPixels;

        stopScreenRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    /**
     * Create renderers after the Surface is Created and on the GL Thread
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        if (mSession == null) {
            return;
        }

        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(/*context=*/this);

        try {

            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());
            mLineShaderRenderer.createOnGlThread(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        mScreenWidth = width;
        mScreenHeight = height;
    }


    /**
     * update() is executed on the GL Thread.
     * The method handles all operations that need to take place before drawing to the screen.
     * The method :
     * extracts the current projection matrix and view matrix from the AR Pose
     * handles adding stroke and points to the data collections
     * updates the ZeroMatrix and performs the matrix multiplication needed to re-center the drawing
     * updates the Line Renderer with the current strokes, color, distance scale, line width etc
     */
    private void update() {

        if (mSession == null) {
            return;
        }

        mDisplayRotationHelper.updateSessionIfNeeded(mSession);

        try {

            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());

            mFrame = mSession.update();

            // Retrieve only the updated planes for this frame
            Collection<Plane> updatedPlanes = mFrame.getUpdatedTrackables(Plane.class);

            for (Plane plane : updatedPlanes) {
                // Process only actively tracked planes
                if (plane.getTrackingState() == TrackingState.TRACKING) {
                    Log.d("PLANETRACK", "Plane is being tracked");
                    // Check if the plane is already in the tracked list
                    if (!trackedPlanes.contains(plane)) {
                        // New plane detected, add it to trackedPlanes
                        trackedPlanes.add(plane);

                        Log.d("PLANETRACK", "Plane tracked is added for processing");
                        processPlane(plane); // Perform any processing for this new plane
                    }
                } else if (plane.getTrackingState() == TrackingState.STOPPED) {
                    // Plane is no longer tracked, remove it from the tracked list
                    trackedPlanes.remove(plane);
                }
            }
            Camera camera = mFrame.getCamera();

            mState = camera.getTrackingState();

            // Update tracking states
            if (mState == TrackingState.TRACKING && !bIsTracking.get()) {
                bIsTracking.set(true);
            } else if (mState== TrackingState.STOPPED && bIsTracking.get()) {
                bIsTracking.set(false);
                bTouchDown.set(false);
            }

            // Get projection matrix.
            camera.getProjectionMatrix(projmtx, 0, AppSettings.getNearClip(), AppSettings.getFarClip());
            camera.getViewMatrix(viewmtx, 0);

            float[] position = new float[3];
            camera.getPose().getTranslation(position, 0);

            // Check if camera has moved much, if thats the case, stop touchDown events
            // (stop drawing lines abruptly through the air)
            if (mLastFramePosition != null) {
                Vector3f distance = new Vector3f(position[0], position[1], position[2]);
                distance.sub(new Vector3f(mLastFramePosition[0], mLastFramePosition[1], mLastFramePosition[2]));

                if (distance.length() > 0.15) {
                    bTouchDown.set(false);
                }
            }
            mLastFramePosition = position;

            // Multiply the zero matrix
            Matrix.multiplyMM(viewmtx, 0, viewmtx, 0, mZeroMatrix, 0);


            if (bNewStroke.get()) {
                bNewStroke.set(false);
                addStroke(lastTouch.get());
                mLineShaderRenderer.bNeedsUpdate.set(true);
            } else if (bTouchDown.get()) {
                addPoint(lastTouch.get());
                mLineShaderRenderer.bNeedsUpdate.set(true);
            }

            if (bReCenterView.get()) {
                bReCenterView.set(false);
                mZeroMatrix = getCalibrationMatrix();
            }

            if (bClearDrawing.get()) {
                bClearDrawing.set(false);
                clearDrawing();
                mLineShaderRenderer.bNeedsUpdate.set(true);
            }

            if (bUndo.get()) {
                bUndo.set(false);
                if (mStrokes.size() > 0) {
                    mStrokes.remove(mStrokes.size() - 1);
                    mLineShaderRenderer.bNeedsUpdate.set(true);
                }
            }
            mLineShaderRenderer.setDrawDebug(bLineParameters.get());
            if (mLineShaderRenderer.bNeedsUpdate.get()) {
                mLineShaderRenderer.setColor(AppSettings.getColor());
                mLineShaderRenderer.mDrawDistance = AppSettings.getStrokeDrawDistance();
                mLineShaderRenderer.setDistanceScale(mDistanceScale);
                mLineShaderRenderer.setLineWidth(mLineWidthMax);
                mLineShaderRenderer.clear();
                mLineShaderRenderer.updateStrokes(mStrokes);
                mLineShaderRenderer.upload();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to handle any plane-specific processing (e.g., placing objects on the plane)
    private void processPlane(Plane plane) {
        float[] planePositionArray = new float[3];
        plane.getCenterPose().getTranslation();
        planePosition = new Vector3f(planePositionArray[0], planePositionArray[1], planePositionArray[2]);

        float[] planeNormalArray = new float[3];
        plane.getCenterPose().getZAxis();
        planeNormal = new Vector3f(planeNormalArray[0], planeNormalArray[1], planeNormalArray[2]);

        // Use planePosition and planeNormal as needed, e.g., for placing annotations or drawing lines on the plane.
    }




    /**
     * GL Thread Loop
     * clears the Color Buffer and Depth Buffer, draws the current texture from the camera
     * and draws the Line Renderer if ARCore is tracking the world around it
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        if (mPaused) return;

        update();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mFrame == null) {
            return;
        }

        // Draw background.
        mBackgroundRenderer.draw(mFrame);

        // Draw Lines
        if (mFrame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            mLineShaderRenderer.draw(viewmtx, projmtx, mScreenWidth, mScreenHeight, AppSettings.getNearClip(), AppSettings.getFarClip());
        }
    }


    /**
     * Get a matrix usable for zero calibration (only position and compass direction)
     */
    public float[] getCalibrationMatrix() {
        float[] t = new float[3];
        float[] m = new float[16];

        mFrame.getCamera().getPose().getTranslation(t, 0);
        float[] z = mFrame.getCamera().getPose().getZAxis();
        Vector3f zAxis = new Vector3f(z[0], z[1], z[2]);
        zAxis.y = 0;
        zAxis.normalize();

        double rotate = Math.atan2(zAxis.x, zAxis.z);

        Matrix.setIdentityM(m, 0);
        Matrix.translateM(m, 0, t[0], t[1], t[2]);
        Matrix.rotateM(m, 0, (float) Math.toDegrees(rotate), 0, 1, 0);
        return m;
    }


    /**
     * Clears the Datacollection of Strokes and sets the Line Renderer to clear and update itself
     * Designed to be executed on the GL Thread
     */
    public void clearDrawing() {
        mStrokes.clear();
        mLineShaderRenderer.clear();
    }


    /**
     * onClickUndo handles the touch input on the GUI and sets the AtomicBoolean bUndo to be true
     * the actual undo functionality is executed in the GL Thread
     */
    public void onClickUndo(View button) {
        bUndo.set(true);
    }

    /**
     * onClickLineDebug toggles the Line Renderer's Debug View on and off. The line renderer will
     * highlight the lines on the same depth plane to allow users to draw things more coherently
     */
    public void onClickLineDebug(View button) {
        bLineParameters.set(!bLineParameters.get());
    }


    /**
     * onClickClear handle showing an AlertDialog to clear the drawing
     */
    public void onClickClear(View button) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage("Sure you want to clear?");

        // Set up the buttons
        builder.setPositiveButton("Clear ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bClearDrawing.set(true);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }


    /**
     * onClickPreview function opens the ImagePreviewActivity for previewing all the saved screenshots
     */
    public void onClickPreview(View button) {

        Intent intent = new Intent(MainActivity.this, ImagePreviewActivity.class);

        // Pass any necessary data to the ImagePreviewActivity if required
        //intent.putExtra("key", "value"); // Replace "key" and "value" with actual data if needed

        // Start the ImagePreviewActivity
        startActivity(intent);
    }

    private void startMediaProjectionRequest() {
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        Log.d("TRACK_RECORD", "Hitting startMediaProjectionRequest.");
        startActivityForResult(captureIntent, REQUEST_CODE_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK && data != null) {
            mediaProjectionResultCode = resultCode;
            mediaProjectionData = data;
            Log.d("TRACK_RECORD", "Hitting onActivityResult with result code:: " + resultCode);
            startScreenRecording();
        }
    }

    private void startScreenRecording() {
        Intent serviceIntent = new Intent(this, ScreenRecordService.class);
        serviceIntent.putExtra("RESULT_CODE", mediaProjectionResultCode);
        serviceIntent.putExtra("DATA_INTENT", mediaProjectionData);

        Log.d("TRACK_RECORD", "Hitting startScreenRecording.");

        startForegroundService(serviceIntent); // Start the foreground service
    }

    private void stopScreenRecording() {
        Intent serviceIntent = new Intent(this, ScreenRecordService.class);
        Log.d("TRACK_RECORD", "Hitting stopScreenRecording.");
        stopService(serviceIntent); // Stop the foreground service
    }


    public void onClickCapture(View view) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            mSurfaceView.queueEvent(() -> screenshotViewModel.takeScreenshot(this, mSurfaceView, "Screenshots"));
        }, 100);
       //screenshotViewModel.takeScreenshot(this, mSurfaceView, "Screenshots");
    }

    // ------- Touch events

    /**
     * onTouchEvent handles saving the lastTouch screen position and setting bTouchDown and bNewStroke
     * AtomicBooleans to trigger addPoint and addStroke on the GL Thread to be called
     */
    @Override
    public boolean onTouchEvent(MotionEvent tap) {
        this.mDetector.onTouchEvent(tap);

        if (tap.getAction() == MotionEvent.ACTION_DOWN ) {
            lastTouch.set(new Vector2f(tap.getX(), tap.getY()));
            bTouchDown.set(true);
            bNewStroke.set(true);
            return true;
        } else if (tap.getAction() == MotionEvent.ACTION_MOVE || tap.getAction() == MotionEvent.ACTION_POINTER_DOWN) {
            lastTouch.set(new Vector2f(tap.getX(), tap.getY()));
            bTouchDown.set(true);
            return true;
        } else if (tap.getAction() == MotionEvent.ACTION_UP || tap.getAction() == MotionEvent.ACTION_CANCEL) {
            bTouchDown.set(false);
            lastTouch.set(new Vector2f(tap.getX(), tap.getY()));
            return true;
        }

        return super.onTouchEvent(tap);
    }


    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    /**
     * onDoubleTap shows and hides the Button Bar at the Top of the View
     */
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (mButtonBar.getVisibility() == View.GONE) {
            mButtonBar.setVisibility(View.VISIBLE);
        } else {
            mButtonBar.setVisibility(View.GONE);
        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent tap) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

}