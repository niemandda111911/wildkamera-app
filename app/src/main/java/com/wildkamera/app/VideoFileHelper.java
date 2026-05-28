package com.wildkamera.app;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VideoFileHelper {
    private final Context context;
    private final AppSettings settings;

    public VideoFileHelper(Context context, AppSettings settings) {
        this.context = context;
        this.settings = settings;
    }

    public File nextVideoFile() {
        File dir = resolveDirectory();
        if (!dir.exists()) dir.mkdirs();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        int num = nextDailyIndex(dir, dateStr);
        String filename = String.format("Wildkamera_%s_%03d.mp4", dateStr, num);
        return new File(dir, filename);
    }

    public File resolveDirectory() {
        String custom = settings.getSavePath();
        if (custom != null && !custom.isEmpty()) return new File(custom);
        File movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        return new File(movies, "Wildkamera");
    }

    private int nextDailyIndex(File dir, String dateStr) {
        int max = 0;
        File[] files = dir.listFiles();
        if (files == null) return 1;
        String prefix = "Wildkamera_" + dateStr + "_";
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith(prefix) && name.endsWith(".mp4")) {
                try {
                    int n = Integer.parseInt(name.substring(prefix.length(), name.length() - 4));
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) {}
            }
        }
        return max + 1;
    }

    public void registerInMediaStore(File videoFile) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.getName());
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + "/Wildkamera");
        } else {
            values.put(MediaStore.Video.Media.DATA, videoFile.getAbsolutePath());
        }
        Uri col = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        try { context.getContentResolver().insert(col, values); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
