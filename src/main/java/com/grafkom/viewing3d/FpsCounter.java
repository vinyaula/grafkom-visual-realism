package com.grafkom.viewing3d;

/** Penghitung FPS (rata-rata per 0,5 detik) untuk analisis kinerja rendering di laporan. */
public class FpsCounter {
    private double windowStart = -1;
    private int frames = 0;
    private double fps = 0;

    /** Panggil sekali per frame. Mengembalikan true saat nilai FPS baru tersedia. */
    public boolean tick(double nowSeconds) {
        if (windowStart < 0) windowStart = nowSeconds;
        frames++;
        double elapsed = nowSeconds - windowStart;
        if (elapsed >= 0.5) {
            fps = frames / elapsed;
            frames = 0;
            windowStart = nowSeconds;
            return true;
        }
        return false;
    }

    public double getFps() { return fps; }
    public double getFrameTimeMs() { return fps > 0 ? 1000.0 / fps : 0; }
}
