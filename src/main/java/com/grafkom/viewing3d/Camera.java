package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Kamera orbit (drag mouse kiri = putar, scroll = zoom).
 * zNear/zFar dibuat proporsional (0.1 .. 100) agar presisi Z-Buffer terjaga dan Z-Fighting minimal.
 * Pitch boleh negatif (kamera di bawah lantai) untuk mengamati efek Back-Face Culling pada bidang lantai.
 */
public class Camera {
    public static final float Z_NEAR = 0.1f;
    public static final float Z_FAR = 100f;
    private static final float MIN_PITCH = -40f, MAX_PITCH = 89f;
    private static final float MIN_DISTANCE = 3f, MAX_DISTANCE = 30f;

    private float yaw = 35f;       // derajat, putaran horizontal
    private float pitch = 28f;     // derajat, putaran vertikal
    private float distance = 11f;
    private final Vector3f target = new Vector3f(0f, 1.2f, 0f);

    public void rotate(float dYaw, float dPitch) {
        yaw += dYaw;
        pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch + dPitch));
    }

    public void zoom(float delta) {
        distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance + delta));
    }

    public Vector3f getPosition() {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vector3f(
                target.x + (float) (distance * Math.cos(p) * Math.sin(y)),
                target.y + (float) (distance * Math.sin(p)),
                target.z + (float) (distance * Math.cos(p) * Math.cos(y)));
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), target, new Vector3f(0f, 1f, 0f));
    }

    public Matrix4f getProjectionMatrix(float aspect) {
        return new Matrix4f().perspective((float) Math.toRadians(45.0), aspect, Z_NEAR, Z_FAR);
    }
}
