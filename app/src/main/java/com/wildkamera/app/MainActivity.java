package com.wildkamera.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private boolean isActive = false;
    private Button btnToggle;
    private TextView tvRecordingStatus;
    private PreviewView previewView;

    public static MainActivity instance;

    private final ActivityResultLauncher<String[]> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean allGranted = true;
                        for (Boolean v : result.values()) if (!v) { allGranted = false; break; }
                        if (allGranted) startWildkamera();
                        else Toast.makeText(this,
                                getString(R.string.perm_required), Toast.LENGTH_LONG).show();
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        btnToggle         = findViewById(R.id.btnToggle);
        previewView       = findViewById(R.id.previewView);
        tvRecordingStatus = findViewById(R.id.tvRecordingStatus);

        WildkameraService.sharedPreviewView = previewView;

        btnToggle.setOnClickListener(v -> {
            if (!isActive) {
                if (hasAllPermissions()) startWildkamera();
                else permLauncher.launch(requiredPermissions());
            } else {
                stopWildkamera();
            }
        });

        findViewById(R.id.btnSettings).setOnClickListener(v ->
            new SettingsDialog().show(getSupportFragmentManager(), "settings")
        );
    }

    /** Called from WildkameraService to update recording indicator */
    public void setRecordingStatus(boolean recording, String filename) {
        runOnUiThread(() -> {
            if (tvRecordingStatus == null) return;
            if (recording) {
                tvRecordingStatus.setVisibility(View.VISIBLE);
                tvRecordingStatus.setText("⏺ Aufnahme: " + filename);
            } else {
                tvRecordingStatus.setVisibility(View.GONE);
            }
        });
    }

    private void startWildkamera() {
        isActive = true;
        btnToggle.setText(R.string.btn_on);
        btnToggle.setBackgroundTintList(
                ContextCompat.getColorStateList(this, R.color.green_active));
        Intent svc = new Intent(this, WildkameraService.class);
        svc.setAction(WildkameraService.ACTION_START);
        ContextCompat.startForegroundService(this, svc);
    }

    private void stopWildkamera() {
        isActive = false;
        btnToggle.setText(R.string.btn_off);
        btnToggle.setBackgroundTintList(
                ContextCompat.getColorStateList(this, R.color.gray_inactive));
        if (tvRecordingStatus != null)
            tvRecordingStatus.setVisibility(View.GONE);
        Intent svc = new Intent(this, WildkameraService.class);
        svc.setAction(WildkameraService.ACTION_STOP);
        startService(svc);
    }

    private boolean hasAllPermissions() {
        for (String perm : requiredPermissions())
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }

    private String[] requiredPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);
        perms.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
            perms.add(Manifest.permission.READ_MEDIA_VIDEO);
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        return perms.toArray(new String[0]);
    }

    @Override
    protected void onDestroy() {
        instance = null;
        WildkameraService.sharedPreviewView = null;
        super.onDestroy();
    }
}