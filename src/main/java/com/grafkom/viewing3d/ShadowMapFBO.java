package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;   // GL_CLAMP_TO_BORDER, glActiveTexture
import static org.lwjgl.opengl.GL30.*;

/**
 * Praktikum 4 - Shadow Mapping Depth FBO Manager (Indikator 5)
 * Framebuffer Object yang menampung Shadow Map 2D Depth Texture (hanya depth, tanpa color attachment).
 */
public class ShadowMapFBO {
    private int fboID;
    private int depthTextureID;
    private final int SHADOW_WIDTH = 2048, SHADOW_HEIGHT = 2048;

    public ShadowMapFBO() {
        fboID = glGenFramebuffers();
        depthTextureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTextureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, SHADOW_WIDTH, SHADOW_HEIGHT, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (java.nio.ByteBuffer) null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        float[] borderColor = {1.0f, 1.0f, 1.0f, 1.0f};   // di luar frustum cahaya: depth = 1.0 (tidak bayangan)
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureID, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Shadow Map FBO tidak lengkap (incomplete framebuffer)");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Render Pass 1: aktifkan FBO, set viewport ke ukuran shadow map, bersihkan depth. */
    public void bindForWriting() {
        glViewport(0, 0, SHADOW_WIDTH, SHADOW_HEIGHT);
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    /** Kembali ke default framebuffer (layar) dan pulihkan viewport. */
    public void unbind(int screenWidth, int screenHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenWidth, screenHeight);
    }

    /** Render Pass 2: pasang Depth Map pada texture unit tertentu agar dapat di-sample fragment shader. */
    public void bindDepthTexture(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, depthTextureID);
    }

    public int getDepthTextureID() { return depthTextureID; }
    public int getWidth() { return SHADOW_WIDTH; }
    public int getHeight() { return SHADOW_HEIGHT; }

    public void cleanup() {
        glDeleteFramebuffers(fboID);
        glDeleteTextures(depthTextureID);
    }
}
