package com.wildkamera.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WildkameraService extends LifecycleService {

    private static final String TAG = "WildkameraService";
    public static final String CHANNEL_ID = "wildkamera_channel";
    public static final String CHANNEL_MOTION = "wildkamera_motion";
    public static final int NOTIF_ID_RUNNING = 1;
    public static final int NOTIF_ID_MOTION = 2;
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP  = "ACTION_STOP";
    public static PreviewView sharedPreviewView = null;

    private boolean isRecording = false;
    private long lastMotionTime = 0;
    private long recordingStartTime = 0;

    private AppSettings settings;
    private VideoFileHelper fileHelper;
    private MotionDetector motionDetector;
    private ExecutorService analysisExecutor;
    private PowerManager.WakeLock wakeLock;
    private Handler mainHandler;
    private Runnable recordingCheckRunnable;
    private ProcessCameraProvider cameraProvider;
    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording;

    @Override
    public void onCreate() {
        super.onCreate();
        settings = new AppSettings(this);
        fileHelper = new VideoFileHelper(this, settings);
        motionDetector = new MotionDetector();
        analysisExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannels();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent == null) return START_STICKY;
        if (ACTION_START.equals(intent.getAction())) {
            startForeground(NOTIF_ID_RUNNING, buildRunningNotification());
            motionDetector.setSensitivity(settings.getSensitivity());
            startCamera();
            startRecordingCheckLoop();
        } else if (ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
        }
        return START_STICKY;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCamera();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "CameraProvider error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera() {
        if (cameraProvider == null) return;
        cameraProvider.unbindAll();
        Preview preview = new Preview.Builder().build();
        if (sharedPreviewView != null)
            preview.setSurfaceProvider(sharedPreviewView.getSurfaceProvider());
        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(analysisExecutor, image -> {
            boolean motion = motionDetector.analyze(image);
            image.close();
            if (motion) onMotionDetected();
        });
        try {
            cameraProvider.bindToLifecycle(this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview, videoCapture, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Camera bind error", e);
        }
    }

    private void onMotionDetected() {
        lastMotionTime = System.currentTimeMillis();
        sendMotionNotification();
        mainHandler.post(()
-> { if (!isRecording) startNewRecording(); });
    }

    private void startNewRecording() {
        if (videoCapture == null) return;
        File outFile = fileHelper.nextVideoFile();
        FileOutputOptions opts = new FileOutputOptions.Builder(outFile).build();
        try {
            currentRecording = videoCapture.getOutput()
                    .prepareRecording(this, opts)
                    .start(ContextCompat.getMainExecutor(this), event -> {
                        if (event instanceof VideoRecordEvent.Finalize) {
                            VideoRecordEvent.Finalize fin = (VideoRecordEvent.Finalize) event;
                            if (!fin.hasError()) fileHelper.registerInMediaStore(outFile);
                            isRecording = false;
                            currentRecording = null;
                        }
                    });
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();
        } catch (Exception e) {
            Log.e(TAG, "Recording error", e);
        }
    }

    private void stopCurrentRecording() {
        if (currentRecording != null) currentRecording.stop();
    }

    private void startRecordingCheckLoop() {
        recordingCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    long now = System.currentTimeMillis();
                    long trailMs = settings.getTrailTime() * 1000L;
                    long maxMs = settings.getMaxVideoMin() * 60 * 1000L;
                    if ((now - recordingStartTime) >= maxMs) {
                        stopCurrentRecording();
                        mainHandler.postDelayed(() -> startNewRecording(), 300);
                    } else if ((now - lastMotionTime) >= trailMs) {
                        stopCurrentRecording();
                    }
                }
                mainHandler.postDelayed(this, 1000);
            }
        };
        mainHandler.postDelayed(recordingCheckRunnable, 1000);
    }

    private void createNotificationChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID, "Wildkamera Status", NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_MOTION, "Bewegungserkennung", NotificationManager.IMPORTANCE_HIGH));
    }

    private Notification buildRunningNotification() {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Wildkamera aktiv")
                .setContentText("Bewegungserkennung läuft im Hintergrund")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void sendMotionNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIF_ID_MOTION, new NotificationCompat.Builder(this, CHANNEL_MOTION)
                .setContentTitle("Wildkamera")
                .setContentText("Bewegung erkannt!")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .build());
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Wildkamera::RecordingLock");
        wakeLock.acquire();
    }

    @Override
    public void onDestroy() {
        if (recordingCheckRunnable != null) mainHandler.removeCallbacks(recordingCheckRunnable);
        stopCurrentRecording();
        if (cameraProvider != null) cameraProvider.unbindAll();
        if (analysisExecutor != null) analysisExecutor.shutdown();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }
}
