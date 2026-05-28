package com.wildkamera.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.view.LayoutInflater;
import android.view.View;

public class SettingsDialog extends DialogFragment {

    private AppSettings settings;
    private String chosenPath = "";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        settings = new AppSettings(requireContext());

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_settings, null);

        SeekBar seekSensitivity = view.findViewById(R.id.seekSensitivity);
        TextView tvSensVal      = view.findViewById(R.id.tvSensitivityValue);
        EditText etPreBuffer    = view.findViewById(R.id.etPreBuffer);
        EditText etTrailTime    = view.findViewById(R.id.etTrailTime);
        EditText etMaxVideo     = view.findViewById(R.id.etMaxVideo);
        TextView tvSavePath     = view.findViewById(R.id.tvSavePath);
        Button btnChooseFolder  = view.findViewById(R.id.btnChooseFolder);
        Button btnSave          = view.findViewById(R.id.btnSaveSettings);

        int sensInt = (int) settings.getSensitivity();
        seekSensitivity.setProgress(sensInt);
        tvSensVal.setText(sensInt + "%");
        etPreBuffer.setText(String.valueOf(settings.getPreBuffer()));
        etTrailTime.setText(String.valueOf(settings.getTrailTime()));
        etMaxVideo.setText(String.valueOf(settings.getMaxVideoMin()));
        String sp = settings.getSavePath();
        tvSavePath.setText(sp.isEmpty() ? "Movies/Wildkamera (Standard)" : sp);
        chosenPath = sp;

        seekSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                tvSensVal.setText(Math.max(1, p) + "%");
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        btnChooseFolder.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(i, 99);
        });

        btnSave.setOnClickListener(v -> {
            int sens = Math.max(1, seekSensitivity.getProgress());
            settings.setSensitivity(sens);
            try { settings.setPreBuffer(Integer.parseInt(etPreBuffer.getText().toString())); }
            catch (NumberFormatException e) { settings.setPreBuffer(AppSettings.DEFAULT_PRE_BUFFER); }
            try { settings.setTrailTime(Integer.parseInt(etTrailTime.getText().toString())); }
            catch (NumberFormatException e) { settings.setTrailTime(AppSettings.DEFAULT_TRAIL_TIME); }
            try { settings.setMaxVideoMin(Integer.parseInt(etMaxVideo.getText().toString())); }
            catch (NumberFormatException e) { settings.setMaxVideoMin(AppSettings.DEFAULT_MAX_VIDEO_MIN); }
            settings.setSavePath(chosenPath);
            dismiss();
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 99 && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                chosenPath = uri.getPath() != null ? uri.getPath() : uri.toString();
                View v = getView();
                if (v != null) {
                    TextView tv = v.findViewById(R.id.tvSavePath);
                    if (tv != null) tv.setText(chosenPath);
                }
            }
        }
    }
}
