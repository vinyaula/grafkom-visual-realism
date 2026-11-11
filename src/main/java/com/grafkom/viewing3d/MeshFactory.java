package com.grafkom.viewing3d;

/**
 * Generator mesh 3D bermateri: setiap vertex punya Vektor Normal dan Koordinat Tekstur UV.
 * Semua mesh berorientasi CCW dari sisi luar (cocok dengan glFrontFace(GL_CCW) di HSRManager).
 */
public final class MeshFactory {

    private MeshFactory() {}

    /** UV Sphere. u = sektor (bujur), v = 1 di kutub utara -> 0 di kutub selatan. */
    public static MeshData createSphere(float radius, int stacks, int sectors) {
        float[][][] p = new float[stacks + 1][sectors + 1][3];
        float[][][] t = new float[stacks + 1][sectors + 1][2];
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * i / stacks;              // 0 (utara) .. PI (selatan)
            for (int j = 0; j <= sectors; j++) {
                double theta = 2.0 * Math.PI * j / sectors;
                p[i][j][0] = (float) (radius * Math.sin(phi) * Math.cos(theta));
                p[i][j][1] = (float) (radius * Math.cos(phi));
                p[i][j][2] = (float) (radius * Math.sin(phi) * Math.sin(theta));
                t[i][j][0] = (float) j / sectors;
                t[i][j][1] = 1f - (float) i / stacks;
            }
        }
        // Koreksi U di kutub: vertex kutub dipakai segitiga yang u-nya di tengah sektor -> hindari texture shear
        for (int j = 1; j <= sectors; j++) t[0][j][0] = (j - 0.5f) / sectors;
        for (int j = 0; j < sectors; j++) t[stacks][j][0] = (j + 0.5f) / sectors;

        MeshBuilder mb = new MeshBuilder();
        mb.addGrid(p, t);
        return mb.build();
    }

    /** Torus (sumbu Y). R = jari-jari utama, r = jari-jari tabung. */
    public static MeshData createTorus(float R, float r, int rings, int sides) {
        float[][][] p = new float[rings + 1][sides + 1][3];
        float[][][] t = new float[rings + 1][sides + 1][2];
        for (int a = 0; a <= rings; a++) {
            double theta = 2.0 * Math.PI * a / rings;
            for (int b = 0; b <= sides; b++) {
                double phi = 2.0 * Math.PI * b / sides;
                double ring = R + r * Math.cos(phi);
                p[a][b][0] = (float) (ring * Math.cos(theta));
                p[a][b][1] = (float) (r * Math.sin(phi));
                p[a][b][2] = (float) (ring * Math.sin(theta));
                t[a][b][0] = 3f * a / rings;   // 3x pengulangan agar bata tidak melar
                t[a][b][1] = (float) b / sides;
            }
        }
        MeshBuilder mb = new MeshBuilder();
        mb.addGrid(p, t);
        return mb.build();
    }

    /** Torus Knot (trefoil, p=2, q=3) - mesh kompleks dengan self-shadowing. */
    public static MeshData createTorusKnot(float tubeRadius, int segments, int sides, float scale) {
        final double dt = 1e-3;
        float[][][] p = new float[segments + 1][sides + 1][3];
        float[][][] t = new float[segments + 1][sides + 1][2];

        // panjang kurva -> jumlah pengulangan tekstur sepanjang tabung
        double length = 0;
        double[] prev = knotCurve(0);
        for (int k = 1; k <= 2000; k++) {
            double[] cur = knotCurve(2.0 * Math.PI * k / 2000);
            length += dist(prev, cur);
            prev = cur;
        }
        float tiles = Math.max(1f, Math.round(length / (2.0 * Math.PI * tubeRadius)));

        for (int a = 0; a <= segments; a++) {
            double u = 2.0 * Math.PI * a / segments;
            double[] c = knotCurve(u);
            double[] c1 = knotCurve(u + dt);
            double[] c0 = knotCurve(u - dt);

            // Frame: T = tangen, N = arah kelengkungan (turunan kedua), B = T x N
            double[] T = normalize(new double[]{c1[0] - c0[0], c1[1] - c0[1], c1[2] - c0[2]});
            double[] N = {c1[0] + c0[0] - 2 * c[0], c1[1] + c0[1] - 2 * c[1], c1[2] + c0[2] - 2 * c[2]};
            double nt = N[0] * T[0] + N[1] * T[1] + N[2] * T[2];
            N = normalize(new double[]{N[0] - nt * T[0], N[1] - nt * T[1], N[2] - nt * T[2]});
            double[] B = {T[1] * N[2] - T[2] * N[1], T[2] * N[0] - T[0] * N[2], T[0] * N[1] - T[1] * N[0]};

            for (int b = 0; b <= sides; b++) {
                double phi = 2.0 * Math.PI * b / sides;
                double cx = Math.cos(phi), sx = Math.sin(phi);
                for (int d = 0; d < 3; d++) {
                    p[a][b][d] = (float) ((c[d] + tubeRadius * (cx * N[d] + sx * B[d])) * scale);
                }
                t[a][b][0] = tiles * a / segments;
                t[a][b][1] = (float) b / sides;
            }
        }
        MeshBuilder mb = new MeshBuilder();
        mb.addGrid(p, t);
        return mb.build();
    }

    /** Kurva trefoil dalam koordinat dunia (Y = atas). */
    private static double[] knotCurve(double t) {
        double r = 2.0 + Math.cos(3.0 * t);
        return new double[]{r * Math.cos(2.0 * t), Math.sin(3.0 * t), r * Math.sin(2.0 * t)};
    }

    /** Bidang lantai (ground plane) pada y = 0, normal +Y, ubin tekstur diulang uvRepeat kali. */
    public static MeshData createPlane(float half, float uvRepeat) {
        MeshBuilder mb = new MeshBuilder();
        float[] a = {-half, 0, -half, 0, 0};
        float[] b = {-half, 0, half, 0, uvRepeat};
        float[] c = {half, 0, half, uvRepeat, uvRepeat};
        float[] d = {half, 0, -half, uvRepeat, 0};
        mb.addTriangle(a, b, c);
        mb.addTriangle(a, c, d);
        return mb.build();
    }

    private static double dist(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double[] normalize(double[] v) {
        double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[]{v[0] / l, v[1] / l, v[2] / l};
    }
}
