package com.wildkamera.app;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
    private static final String PREFS_NAME = "wildkamera_settings";
    private static final String KEY_SENSITIVITY   = "sensitivity";
    private static final String KEY_PRE_BUFFER    = "pre_buffer_sec";
    private static final String KEY_TRAIL_TIME    = "trail_time_sec";
    private static final String KEY_MAX_VIDEO_MIN = "max_video_min";
    private static final String KEY_SAVE_PATH     = "save_path";
    private static final String KEY_QUALITY       = "video_quality";
    private static final String KEY_AUDIO         = "audio_enabled";

    public static final float  DEFAULT_SENSITIVITY   = 1.0f;
    public static final int    DEFAULT_PRE_BUFFER    = 20;
    public static final int    DEFAULT_TRAIL_TIME    = 30;
    public static final int    DEFAULT_MAX_VIDEO_MIN = 10;
    public static final String DEFAULT_QUALITY       = "SD";
    public static final boolean DEFAULT_AUDIO        = false;

    private final SharedPreferences prefs;

    public AppSettings(Context ctx) {
        prefs = ctx.getApplicationContext()
                   .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public float   getSensitivity()  { return prefs.getFloat(KEY_SENSITIVITY, DEFAULT_SENSITIVITY); }
    public int     getPreBuffer()    { return prefs.getInt(KEY_PRE_BUFFER, DEFAULT_PRE_BUFFER); }
    public int     getTrailTime()    { return prefs.getInt(KEY_TRAIL_TIME, DEFAULT_TRAIL_TIME); }
    public int     getMaxVideoMin()  { return prefs.getInt(KEY_MAX_VIDEO_MIN, DEFAULT_MAX_VIDEO_MIN); }
    public String  getSavePath()     { return prefs.getString(KEY_SAVE_PATH, ""); }
    public String  getQuality()      { return prefs.getString(KEY_QUALITY, DEFAULT_QUALITY); }
    public boolean getAudioEnabled() { return prefs.getBoolean(KEY_AUDIO, DEFAULT_AUDIO); }

    public void setSensitivity(float v)   { prefs.edit().putFloat(KEY_SENSITIVITY, v).apply(); }
    public void setPreBuffer(int v)       { prefs.edit().putInt(KEY_PRE_BUFFER, v).apply(); }
    public void setTrailTime(int v)       { prefs.edit().putInt(KEY_TRAIL_TIME, v).apply(); }
    public void setMaxVideoMin(int v)     { prefs.edit().putInt(KEY_MAX_VIDEO_MIN, v).apply(); }
    public void setSavePath(String v)     { prefs.edit().putString(KEY_SAVE_PATH, v).apply(); }
    public void setQuality(String v)      { prefs.edit().putString(KEY_QUALITY, v).apply(); }
    public void setAudioEnabled(boolean v){ prefs.edit().putBoolean(KEY_AUDIO, v).apply(); }
}