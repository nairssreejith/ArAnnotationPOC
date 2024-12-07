package com.nav.arannotationpoc.common.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nav.arannotationpoc.common.helpers.ScreenRecordService;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScreenRecordViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isRecording = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsRecording() {
        return isRecording;
    }

    public void toggleRecordingState() {
        isRecording.setValue(isRecording.getValue() == null || !isRecording.getValue());
    }
}