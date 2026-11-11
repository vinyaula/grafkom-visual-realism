package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;                  // glActiveTexture & GL_TEXTURE0 ada di GL13 (modul hanya import GL11)
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

/**
 * Praktikum 3 - Java Texture Loader & UV Mapping (Indikator 4)
 * Membaca PNG/JPG memakai STBImage dan memuatnya ke GPU Memory.
 * Mendukung dua mode filtering yang dapat ditukar saat runtime (tombol N).
 */
public final class Texture2D {

    public enum FilterMode {
        NEAREST("Nearest"),
        BILINEAR_MIPMAP("Bilinear+Mipmap");

        public final String label;
        FilterMode(String label) { this.label = label; }
    }

    private final int textureID;
    private FilterMode filterMode;

    public Texture2D(String filePath) {
        textureID = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Texture Wrap Parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // Citra: baris pertama = atas, sedangkan OpenGL v=0 = bawah -> flip agar arah V tidak terbalik
        stbi_set_flip_vertically_on_load(true);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer data = loadImage(filePath, width, height, channels);
            if (data != null) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
                glGenerateMipmap(GL_TEXTURE_2D);
                stbi_image_free(data);
            } else {
                System.err.println("Failed to load texture: " + filePath + " -> memakai tekstur papan catur pengganti");
                uploadFallbackChecker();
            }
        }
        setFilterMode(FilterMode.BILINEAR_MIPMAP);
    }

    /** Coba baca dari file di disk (stbi_load); bila tidak ada, baca dari classpath (src/main/resources). */
    private static ByteBuffer loadImage(String filePath, IntBuffer w, IntBuffer h, IntBuffer c) {
        if (Files.exists(Paths.get(filePath))) {
            return stbi_load(filePath, w, h, c, 4);
        }
        String resource = filePath.startsWith("/") ? filePath : "/" + filePath;
        try (InputStream in = Texture2D.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            ByteBuffer raw = MemoryUtil.memAlloc(bytes.length);
            try {
                raw.put(bytes).flip();
                return stbi_load_from_memory(raw, w, h, c, 4);
            } finally {
                MemoryUtil.memFree(raw);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private void uploadFallbackChecker() {
        int size = 64;
        ByteBuffer px = MemoryUtil.memAlloc(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean on = ((x / 8) + (y / 8)) % 2 == 0;
                px.put((byte) (on ? 255 : 30)).put((byte) (on ? 0 : 30)).put((byte) (on ? 255 : 30)).put((byte) 255);
            }
        }
        px.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, px);
        glGenerateMipmap(GL_TEXTURE_2D);
        MemoryUtil.memFree(px);
    }

    /** Ganti mode filtering: Nearest Neighbor (GL_NEAREST) atau Bilinear + Mipmap (GL_LINEAR_MIPMAP_LINEAR). */
    public void setFilterMode(FilterMode mode) {
        this.filterMode = mode;
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (mode == FilterMode.NEAREST) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }
    }

    public FilterMode getFilterMode() { return filterMode; }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void cleanup() {
        glDeleteTextures(textureID);
    }
}
