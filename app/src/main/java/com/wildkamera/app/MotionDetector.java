package com.wildkamera.app;

import androidx.camera.core.ImageProxy;
import java.nio.ByteBuffer;

public class MotionDetector {

    private byte[] previousFrame = null;
    private float sensitivityPercent = 1.0f;
    private int pixelDiffThreshold = 10;

    /**
     * Sensibilität 1-100%
     * Intern wird SOWOHL der Prozentsatz geänderter Pixel
     * ALS AUCH der Helligkeits-Schwellenwert angepasst:
     *
     * Slider 100% = ultra-empfindlich (threshold=3,  minPixel=0.05%)
     * Slider 50%  = mittel            (threshold=12, minPixel=1%)
     * Slider 1%   = wenig empfindlich (threshold=25, minPixel=5%)
     */
    public void setSensitivity(float percent) {
        // Prozentsatz geänderter Pixel (niedriger = empfindlicher)
        // 100% Slider → 0.05%, 1% Slider → 5%
        this.sensitivityPercent = 5.0f - (percent / 100f * 4.95f);
        this.sensitivityPercent = Math.max(0.05f, this.sensitivityPercent);

        // Pixel-Helligkeits-Schwellenwert (niedriger = empfindlicher)
        // 100% Slider → 3, 1% Slider → 28
        this.pixelDiffThreshold = (int)(28 - (percent / 100f * 25));
        this.pixelDiffThreshold = Math.max(3, this.pixelDiffThreshold);
    }

    public boolean analyze(ImageProxy image) {
        byte[] current = extractYPlane(image);
        if (current == null) return false;
        boolean motion = false;
        if (previousFrame != null && previousFrame.length == current.length) {
            int total = current.length;
            int changed = 0;
            for (int i = 0; i < total; i++) {
                if (Math.abs((current[i] & 0xFF) - (previousFrame[i] & 0xFF))
                        > pixelDiffThreshold)
                    changed++;
            }
            float changePercent = (changed * 100.0f) / total;
            motion = changePercent >= sensitivityPercent;
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