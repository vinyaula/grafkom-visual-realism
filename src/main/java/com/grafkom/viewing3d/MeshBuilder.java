package com.grafkom.viewing3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pembangun mesh segitiga yang TIDAK bergantung pada OpenGL.
 *
 * Dua jenis vektor normal dihasilkan (Indikator 2):
 *  - Flat   : satu normal tunggal per face  -> N_face = normalize((B-A) x (C-A))
 *  - Smooth : Vertex Normal Averaging       -> N_vertex = (sum N_face_i) / ||sum N_face_i||
 *             (rata-rata dilakukan atas semua face yang bertemu di POSISI yang sama, sehingga
 *              vertex duplikat di seam UV / kutub bola tetap mendapat normal yang identik)
 */
public class MeshBuilder {

    /** Kunci posisi terkuantisasi untuk "welding" vertex yang berada di titik yang sama. */
    private record PosKey(int x, int y, int z) {
        static PosKey of(float x, float y, float z) {
            return new PosKey(Math.round(x * 1e4f), Math.round(y * 1e4f), Math.round(z * 1e4f));
        }
    }

    /** Tiap elemen: 3 vertex x (x, y, z, u, v) = 15 float. */
    private final List<float[]> triangles = new ArrayList<>();

    /** Tambah satu segitiga (urutan CCW dilihat dari sisi luar). Segitiga degenerate dibuang. */
    public void addTriangle(float[] a, float[] b, float[] c) {
        if (faceNormal(a, b, c) == null) return;
        float[] t = new float[15];
        System.arraycopy(a, 0, t, 0, 5);
        System.arraycopy(b, 0, t, 5, 5);
        System.arraycopy(c, 0, t, 10, 5);
        triangles.add(t);
    }

    /**
     * Tambah grid parametrik (uSeg x vSeg) dari titik p[i][j] = {x,y,z} dan koordinat tekstur t[i][j] = {u,v}.
     * Konvensi orientasi: (dP/dv) x (dP/du) mengarah ke LUAR  =>  segitiga CCW dari luar.
     */
    public void addGrid(float[][][] p, float[][][] t) {
        int uSeg = p.length - 1;
        int vSeg = p[0].length - 1;
        for (int i = 0; i < uSeg; i++) {
            for (int j = 0; j < vSeg; j++) {
                float[] v00 = vertex(p, t, i, j);
                float[] v01 = vertex(p, t, i, j + 1);
                float[] v10 = vertex(p, t, i + 1, j);
                float[] v11 = vertex(p, t, i + 1, j + 1);
                addTriangle(v00, v01, v10);
                addTriangle(v01, v11, v10);
            }
        }
    }

    private static float[] vertex(float[][][] p, float[][][] t, int i, int j) {
        return new float[]{p[i][j][0], p[i][j][1], p[i][j][2], t[i][j][0], t[i][j][1]};
    }

    /** Normal face satuan (arah CCW), atau null bila segitiga degenerate (luas ~ 0). */
    static float[] faceNormal(float[] a, float[] b, float[] c) {
        double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        double nx = uy * vz - uz * vy;
        double ny = uz * vx - ux * vz;
        double nz = ux * vy - uy * vx;
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-9) return null;
        return new float[]{(float) (nx / len), (float) (ny / len), (float) (nz / len)};
    }

    public MeshData build() {
        int triCount = triangles.size();
        int vCount = triCount * 3;
        float[] pos = new float[vCount * 3];
        float[] uv = new float[vCount * 2];
        float[] flat = new float[vCount * 3];
        float[] smooth = new float[vCount * 3];

        // Pass 1: salin data, normal flat, dan akumulasi normal face per posisi (untuk averaging)
        Map<PosKey, float[]> accum = new HashMap<>();
        PosKey[] keys = new PosKey[vCount];
        for (int t = 0; t < triCount; t++) {
            float[] tri = triangles.get(t);
            float[] fn = faceNormal(
                    new float[]{tri[0], tri[1], tri[2]},
                    new float[]{tri[5], tri[6], tri[7]},
                    new float[]{tri[10], tri[11], tri[12]});
            for (int k = 0; k < 3; k++) {
                int vi = t * 3 + k;
                float x = tri[k * 5], y = tri[k * 5 + 1], z = tri[k * 5 + 2];
                pos[vi * 3] = x; pos[vi * 3 + 1] = y; pos[vi * 3 + 2] = z;
                uv[vi * 2] = tri[k * 5 + 3]; uv[vi * 2 + 1] = tri[k * 5 + 4];
                flat[vi * 3] = fn[0]; flat[vi * 3 + 1] = fn[1]; flat[vi * 3 + 2] = fn[2];

                PosKey key = PosKey.of(x, y, z);
                keys[vi] = key;
                float[] sum = accum.computeIfAbsent(key, kk -> new float[3]);
                sum[0] += fn[0]; sum[1] += fn[1]; sum[2] += fn[2];
            }
        }

        // Pass 2: N_vertex = (sum N_face) / ||sum N_face||
        for (int vi = 0; vi < vCount; vi++) {
            float[] s = accum.get(keys[vi]);
            double len = Math.sqrt(s[0] * s[0] + s[1] * s[1] + s[2] * s[2]);
            if (len < 1e-9) { // normal saling meniadakan -> pakai normal face
                smooth[vi * 3] = flat[vi * 3]; smooth[vi * 3 + 1] = flat[vi * 3 + 1]; smooth[vi * 3 + 2] = flat[vi * 3 + 2];
            } else {
                smooth[vi * 3] = (float) (s[0] / len);
                smooth[vi * 3 + 1] = (float) (s[1] / len);
                smooth[vi * 3 + 2] = (float) (s[2] / len);
            }
        }
        return new MeshData(pos, uv, flat, smooth);
    }
}
