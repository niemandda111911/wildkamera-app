package com.wildkamera.app;

import androidx.camera.core.ImageProxy;
import java.nio.ByteBuffer;

public class MotionDetector {

    private byte[] previousFrame = null;
    private float sensitivity = 50f;

    // Aus Sensibilität berechnete Werte
    private int threshold;      // Helligkeitsunterschied pro Pixel (3-25)
    private float minChanged;   // Mindestanteil geänderter Pixel (0.01% - 3%)

    public void setSensitivity(float percent) {
        this.sensitivity = percent;
        // 100 = ultra empfindlich: threshold=3,  minChanged=0.01%
        // 50  = mittel:            threshold=12, minChanged=0.3%
        // 1   = wenig empfindlich: threshold=25, minChanged=3%
        float t = 1f - (percent / 100f);
        this.threshold  = (int)(3 + t * 22);       // 3..25
        this.minChanged = (float)(0.01 + t * 2.99); // 0.01..3.0 %
    }

    public boolean analyze(ImageProxy image) {
        // Nur jeden 2. Frame analysieren (Performance)
        byte[] current = extractYPlane(image);
        if (current == null) return false;

        if (previousFrame == null || previousFrame.length != current.length) {
            previousFrame = current;
            return false;
        }

        // Analysiere nur jeden 4. Pixel (schneller, trotzdem präzise)
        int total = 0;
        int changed = 0;
        for (int i = 0; i < current.length; i += 4) {
            total++;
            int diff = Math.abs((current[i] & 0xFF) - (previousFrame[i] & 0xFF));
            if (diff > threshold) changed++;
        }

        previousFrame = current;

        if (total == 0) return false;
        float changePercent = (changed * 100.0f) / total;
        return changePercent >= minChanged;
    }

    private byte[] extractYPlane(ImageProxy image) {
        try {
            ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
            ByteBuffer buf = yPlane.getBuffer();
            int rowStride = yPlane.getRowStride();
            int pixStride = yPlane.getPixelStride();
            int w = image.getWidth();
            int h = image.getHeight();
            byte[] data = new byte[w * h];
            int idx = 0;
            for (int row = 0; row < h; row++)
                for (int col = 0; col < w; col++)
                    data[idx++] = buf.get(row * rowStride + col * pixStride);
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    public void reset() {
        previousFrame = null;
    }
}