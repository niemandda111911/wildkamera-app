package com.wildkamera.app;

import androidx.camera.core.ImageProxy;
import java.nio.ByteBuffer;

public class MotionDetector {

    private byte[] previousFrame = null;
    private float sensitivityPercent = 2.0f;
    private int pixelDiffThreshold = 15;

    public void setSensitivity(float percent) {
        this.sensitivityPercent = Math.max(0.1f, percent);
        this.pixelDiffThreshold = Math.max(5, 30 - (int)(percent * 0.25f));
    }

    public boolean analyze(ImageProxy image) {
        byte[] current = extractYPlane(image);
        if (current == null) return false;
        boolean motion = false;
        if (previousFrame != null && previousFrame.length == current.length) {
            int total = current.length;
            int changed = 0;
            for (int i = 0; i < total; i++) {
                if (Math.abs((current[i] & 0xFF) - (previousFrame[i] & 0xFF)) > pixelDiffThreshold)
                    changed++;
            }
            motion = ((changed * 100.0f) / total) >= sensitivityPercent;
        }
        previousFrame = current;
        return motion;
    }

    private byte[] extractYPlane(ImageProxy image) {
        try {
            ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
            ByteBuffer buf = yPlane.getBuffer();
            int rowStride = yPlane.getRowStride();
            int pixStride = yPlane.getPixelStride();
            int w = image.getWidth(), h = image.getHeight();
            byte[] data = new byte[w * h];
            int idx = 0;
            for (int row = 0; row < h; row++)
                for (int col = 0; col < w; col++)
                    data[idx++] = buf.get(row * rowStride + col * pixStride);
            return data;
        } catch (Exception e) { return null; }
    }

    public void reset() { previousFrame = null; }
}