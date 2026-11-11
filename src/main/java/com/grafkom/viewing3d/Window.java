package com.grafkom.viewing3d;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/** Pembungkus jendela GLFW + konteks OpenGL 3.3 Core. */
public class Window {
    private final long handle;
    private int framebufferWidth, framebufferHeight;

    public Window(String title, int width, int height) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Tidak dapat menginisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);   // wajib di macOS
        glfwWindowHint(GLFW_DEPTH_BITS, 24);                      // presisi Z-Buffer 24-bit (mitigasi Z-Fighting)
        glfwWindowHint(GLFW_SAMPLES, 4);                          // MSAA 4x untuk tepi siluet

        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) throw new RuntimeException("Gagal membuat jendela GLFW (butuh OpenGL 3.3+)");

        glfwMakeContextCurrent(handle);
        GL.createCapabilities();
        glfwSwapInterval(0);

        updateFramebufferSize();
        glfwSetFramebufferSizeCallback(handle, (win, w, h) -> {
            framebufferWidth = w;
            framebufferHeight = h;
        });
        glfwShowWindow(handle);

        System.out.println("OpenGL  : " + glGetString(GL_VERSION));
        System.out.println("Renderer: " + glGetString(GL_RENDERER));
    }

    private void updateFramebufferSize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetFramebufferSize(handle, w, h);
            framebufferWidth = w.get(0);
            framebufferHeight = h.get(0);
        }
    }

    public long getHandle() { return handle; }
    public int getFramebufferWidth() { return framebufferWidth; }
    public int getFramebufferHeight() { return framebufferHeight; }

    public boolean shouldClose() { return glfwWindowShouldClose(handle); }
    public void close() { glfwSetWindowShouldClose(handle, true); }
    public void swapBuffers() { glfwSwapBuffers(handle); }
    public void setTitle(String title) { glfwSetWindowTitle(handle, title); }
    public void setVSync(boolean on) { glfwSwapInterval(on ? 1 : 0); }

    public void destroy() {
        glfwDestroyWindow(handle);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
