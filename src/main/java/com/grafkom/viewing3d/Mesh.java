package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh di GPU. Menyimpan dua VAO dari geometri yang sama:
 *   - VAO Flat   (normal per-face)
 *   - VAO Smooth (normal rata-rata per-vertex)
 * sehingga toggle Flat/Smooth (tombol F) hanya memilih VAO yang di-draw.
 * Layout vertex: location 0 = aPos (vec3), 1 = aNormal (vec3), 2 = aTexCoords (vec2).
 */
public class Mesh {
    private static final int STRIDE = 8 * Float.BYTES;

    private final int vaoFlat, vboFlat, vaoSmooth, vboSmooth;
    private final int vertexCount;

    public Mesh(MeshData data) {
        this.vertexCount = data.vertexCount();
        int[] flat = createBuffers(data.interleaved(false));
        int[] smooth = createBuffers(data.interleaved(true));
        vaoFlat = flat[0];   vboFlat = flat[1];
        vaoSmooth = smooth[0]; vboSmooth = smooth[1];
    }

    private static int[] createBuffers(float[] interleaved) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, interleaved, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, STRIDE, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, STRIDE, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
        return new int[]{vao, vbo};
    }

    public void draw(boolean smooth) {
        glBindVertexArray(smooth ? vaoSmooth : vaoFlat);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public int getTriangleCount() { return vertexCount / 3; }

    public void cleanup() {
        glDeleteVertexArrays(vaoFlat);   glDeleteBuffers(vboFlat);
        glDeleteVertexArrays(vaoSmooth); glDeleteBuffers(vboSmooth);
    }
}
