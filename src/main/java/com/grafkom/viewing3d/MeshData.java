package com.grafkom.viewing3d;

/**
 * Data mesh mentah (tanpa OpenGL): triangle list non-indexed dengan winding CCW.
 * Menyimpan DUA set vektor normal sekaligus agar toggle Flat vs Smooth (tombol F)
 * cukup berpindah VAO tanpa menghitung ulang.
 */
public final class MeshData {

    public final float[] positions;     // 3 float per vertex (x,y,z)
    public final float[] uvs;           // 2 float per vertex (u,v)
    public final float[] flatNormals;   // 3 float per vertex - normal PER FACE (Flat Shading)
    public final float[] smoothNormals; // 3 float per vertex - normal rata-rata vertex (Smooth Shading)

    public MeshData(float[] positions, float[] uvs, float[] flatNormals, float[] smoothNormals) {
        this.positions = positions;
        this.uvs = uvs;
        this.flatNormals = flatNormals;
        this.smoothNormals = smoothNormals;
    }

    public int vertexCount() {
        return positions.length / 3;
    }

    /** Layout interleaved sesuai shader: location 0 = pos(3), 1 = normal(3), 2 = uv(2). */
    public float[] interleaved(boolean smooth) {
        float[] normals = smooth ? smoothNormals : flatNormals;
        int n = vertexCount();
        float[] out = new float[n * 8];
        for (int i = 0; i < n; i++) {
            out[i * 8]     = positions[i * 3];
            out[i * 8 + 1] = positions[i * 3 + 1];
            out[i * 8 + 2] = positions[i * 3 + 2];
            out[i * 8 + 3] = normals[i * 3];
            out[i * 8 + 4] = normals[i * 3 + 1];
            out[i * 8 + 5] = normals[i * 3 + 2];
            out[i * 8 + 6] = uvs[i * 2];
            out[i * 8 + 7] = uvs[i * 2 + 1];
        }
        return out;
    }
}
