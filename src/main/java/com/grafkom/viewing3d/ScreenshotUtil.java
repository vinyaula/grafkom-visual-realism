package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImageWrite.*;

/** Menyimpan isi framebuffer ke screenshots/*.png (tombol F2) untuk bahan laporan bagian 4. */
public final class ScreenshotUtil {

    private ScreenshotUtil() {}

    public static String save(int width, int height, String tag) {
        ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 3);
        try {
            glPixelStorei(GL_PACK_ALIGNMENT, 1);
            glReadBuffer(GL_BACK);
            glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, pixels);

            Path dir = Paths.get("screenshots");
            Files.createDirectories(dir);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path file = dir.resolve("shot_" + stamp + "_" + tag + ".png");

            stbi_flip_vertically_on_write(true);   // OpenGL: baris pertama = bawah
            boolean ok = stbi_write_png(file.toString(), width, height, 3, pixels, width * 3);
            return ok ? file.toAbsolutePath().toString() : null;
        } catch (IOException e) {
            System.err.println("Gagal menyimpan screenshot: " + e.getMessage());
            return null;
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }
}
