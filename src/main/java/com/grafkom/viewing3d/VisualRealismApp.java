package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * 3D Visual Realism Suite - menggabungkan seluruh praktikum:
 *  Praktikum 1 : GLSL Phong / Blinn-Phong per-pixel         (phong.vert / phong.frag)     [B]
 *  Praktikum 1 : Flat vs Smooth (vertex normal averaging)   (MeshBuilder / Mesh)          [F]
 *  Praktikum 2 : HSRManager - Z-Buffer & Back-Face Culling                                [Z] [C]
 *  Praktikum 3 : Texture2D  - UV mapping, Nearest vs Bilinear+Mipmap                      [T] [N]
 *  Praktikum 4 : ShadowMapFBO - Shadow Mapping 2-Pass                                     [S] [ [ ] ]
 */
public class VisualRealismApp {

    private static final int WIDTH = 1280, HEIGHT = 720;
    private static final String TITLE = "3D Visual Realism Suite";

    // Scene
    private static final float GROUND_HALF = 6f;
    private static final float GROUND_UV_REPEAT = 6f;       // checker.png (8x8 kotak) diulang 6x per sisi
    private static final float LIGHT_RADIUS = 7f, LIGHT_HEIGHT = 6f;
    private static final float LIGHT_ORTHO = 9f, LIGHT_NEAR = 2f, LIGHT_FAR = 16f;

    private final RenderSettings settings = new RenderSettings();
    private final Camera camera = new Camera();
    private final FpsCounter fps = new FpsCounter();

    private Window window;
    private ShaderProgram phongShader, depthShader;
    private ShadowMapFBO shadowMap;

    private Mesh[] meshes;
    private final String[] meshNames = {"Sphere", "Torus", "Torus Knot"};
    private final float[] meshHeight = {1.7f, 1.8f, 2.0f};   // tinggi pusat mesh di atas lantai
    private final float[] meshTilt = {0f, 35f, 0f};          // kemiringan awal (derajat, sumbu X)
    private Mesh groundMesh;
    private Texture2D objectTexture, groundTexture;

    private float objectAngle = 0f;   // derajat
    private float lightAngle = 40f;   // derajat

    private boolean dragging = false;
    private double lastX, lastY;
    private boolean screenshotRequested = false;

    public void run() {
        init();
        loop();
        cleanup();
    }

    // ------------------------------------------------------------------ init
    private void init() {
        window = new Window(TITLE, WIDTH, HEIGHT);

        phongShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        shadowMap = new ShadowMapFBO();

        // Mesh 3D bermateri (Vektor Normal Vertex + Koordinat UV)
        meshes = new Mesh[]{
                new Mesh(MeshFactory.createSphere(1.2f, 20, 32)),
                new Mesh(MeshFactory.createTorus(1.2f, 0.45f, 40, 20)),
                new Mesh(MeshFactory.createTorusKnot(0.42f, 240, 20, 0.55f))
        };
        groundMesh = new Mesh(MeshFactory.createPlane(GROUND_HALF, GROUND_UV_REPEAT));

        objectTexture = new Texture2D("textures/brick.png");
        groundTexture = new Texture2D("textures/checker.png");

        setupInput();
        printControls();
    }

    private void setupInput() {
        long h = window.getHandle();

        glfwSetKeyCallback(h, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) onKeyPressed(key);
        });
        glfwSetMouseButtonCallback(h, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
                if (dragging) {
                    double[] x = new double[1], y = new double[1];
                    glfwGetCursorPos(h, x, y);
                    lastX = x[0];
                    lastY = y[0];
                }
            }
        });
        glfwSetCursorPosCallback(h, (win, x, y) -> {
            if (dragging) camera.rotate((float) (-(x - lastX) * 0.4), (float) ((y - lastY) * 0.4));
            lastX = x;
            lastY = y;
        });
        glfwSetScrollCallback(h, (win, dx, dy) -> camera.zoom((float) (-dy * 0.8)));
    }

    private void onKeyPressed(int key) {
        switch (key) {
            case GLFW_KEY_B -> settings.blinnPhong = !settings.blinnPhong;
            case GLFW_KEY_F -> settings.smoothShading = !settings.smoothShading;
            case GLFW_KEY_Z -> settings.depthTest = !settings.depthTest;
            case GLFW_KEY_C -> settings.backFaceCulling = !settings.backFaceCulling;
            case GLFW_KEY_T -> settings.useTexture = !settings.useTexture;
            case GLFW_KEY_N -> toggleFilterMode();
            case GLFW_KEY_S -> settings.shadowEnabled = !settings.shadowEnabled;
            case GLFW_KEY_LEFT_BRACKET -> settings.biasScale = Math.max(0f, settings.biasScale - 0.25f);
            case GLFW_KEY_RIGHT_BRACKET -> settings.biasScale = Math.min(8f, settings.biasScale + 0.25f);
            case GLFW_KEY_1 -> settings.modelIndex = 0;
            case GLFW_KEY_2 -> settings.modelIndex = 1;
            case GLFW_KEY_3 -> settings.modelIndex = 2;
            case GLFW_KEY_SPACE -> settings.animate = !settings.animate;
            case GLFW_KEY_V -> {
                settings.vsync = !settings.vsync;
                window.setVSync(settings.vsync);
            }
            case GLFW_KEY_P -> printBenchmark();
            case GLFW_KEY_F2 -> screenshotRequested = true;
            case GLFW_KEY_ESCAPE -> window.close();
            default -> { }
        }
        updateTitle();
    }

    private void toggleFilterMode() {
        settings.filterMode = (settings.filterMode == Texture2D.FilterMode.NEAREST)
                ? Texture2D.FilterMode.BILINEAR_MIPMAP
                : Texture2D.FilterMode.NEAREST;
        objectTexture.setFilterMode(settings.filterMode);
        groundTexture.setFilterMode(settings.filterMode);
    }

    // ------------------------------------------------------------------ loop
    private void loop() {
        double last = glfwGetTime();
        while (!window.shouldClose()) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            glfwPollEvents();
            update(dt);
            render();

            if (screenshotRequested) {
                screenshotRequested = false;
                String saved = ScreenshotUtil.save(window.getFramebufferWidth(), window.getFramebufferHeight(), settings.fileTag());
                System.out.println(saved != null ? "[Screenshot] " + saved : "[Screenshot] gagal disimpan");
            }
            window.swapBuffers();

            if (fps.tick(now)) updateTitle();
        }
    }

    private void update(float dt) {
        long h = window.getHandle();
        float rot = 70f * dt;
        if (glfwGetKey(h, GLFW_KEY_LEFT) == GLFW_PRESS) camera.rotate(rot, 0f);
        if (glfwGetKey(h, GLFW_KEY_RIGHT) == GLFW_PRESS) camera.rotate(-rot, 0f);
        if (glfwGetKey(h, GLFW_KEY_UP) == GLFW_PRESS) camera.rotate(0f, rot);
        if (glfwGetKey(h, GLFW_KEY_DOWN) == GLFW_PRESS) camera.rotate(0f, -rot);
        if (glfwGetKey(h, GLFW_KEY_EQUAL) == GLFW_PRESS || glfwGetKey(h, GLFW_KEY_KP_ADD) == GLFW_PRESS) camera.zoom(-8f * dt);
        if (glfwGetKey(h, GLFW_KEY_MINUS) == GLFW_PRESS || glfwGetKey(h, GLFW_KEY_KP_SUBTRACT) == GLFW_PRESS) camera.zoom(8f * dt);

        if (settings.animate) {
            objectAngle = (objectAngle + 18f * dt) % 360f;   // objek berputar
            lightAngle = (lightAngle + 22f * dt) % 360f;     // cahaya mengorbit -> bayangan real-time bergerak
        }
    }

    // ---------------------------------------------------------------- render
    private void render() {
        int fbW = window.getFramebufferWidth(), fbH = window.getFramebufferHeight();
        if (fbW == 0 || fbH == 0) return; // jendela diminimalkan

        int idx = settings.modelIndex;
        Mesh object = meshes[idx];

        // Matriks model objek: translate * Ry(spin) * Rx(tilt)
        Matrix4f objectModel = new Matrix4f()
                .translate(0f, meshHeight[idx], 0f)
                .rotateY((float) Math.toRadians(objectAngle))
                .rotateX((float) Math.toRadians(meshTilt[idx]));
        Matrix4f groundModel = new Matrix4f();

        // Cahaya + Light Space Matrix (proyeksi ORTOGRAFIK untuk Directional Light)
        Vector3f lightPos = lightPosition();
        Matrix4f lightProjection = new Matrix4f().ortho(-LIGHT_ORTHO, LIGHT_ORTHO, -LIGHT_ORTHO, LIGHT_ORTHO, LIGHT_NEAR, LIGHT_FAR);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 1f, 0f));
        Matrix4f lightSpaceMatrix = new Matrix4f(lightProjection).mul(lightView);

        // ===== RENDER PASS 1 : Depth Pass (sudut pandang cahaya -> Shadow Map) =====
        if (settings.shadowEnabled) {
            shadowMap.bindForWriting();
            HSRManager.setupDepthTest(true);          // depth test wajib aktif pada pass 1, apa pun status tombol Z
            HSRManager.setupBackFaceCulling(false);
            depthShader.use();
            depthShader.setUniform("lightSpaceMatrix", lightSpaceMatrix);
            depthShader.setUniform("model", objectModel);
            object.draw(settings.smoothShading);      // hanya objek yang melempar bayangan; lantai hanya menerima
            shadowMap.unbind(fbW, fbH);
        }

        // ===== RENDER PASS 2 : Lighting Pass (sudut pandang kamera) =====
        glViewport(0, 0, fbW, fbH);
        glClearColor(0.07f, 0.08f, 0.11f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Hidden Surface Removal sesuai tombol Z dan C (HSRManager)
        HSRManager.setupDepthTest(settings.depthTest);
        HSRManager.setupBackFaceCulling(settings.backFaceCulling);

        Matrix4f view = camera.getViewMatrix();
        Matrix4f projection = camera.getProjectionMatrix((float) fbW / fbH);

        phongShader.use();
        phongShader.setUniform("view", view);
        phongShader.setUniform("projection", projection);
        phongShader.setUniform("lightSpaceMatrix", lightSpaceMatrix);
        phongShader.setUniform("lightPos", lightPos);
        phongShader.setUniform("viewPos", camera.getPosition());
        phongShader.setUniform("lightColor", new Vector3f(1f, 1f, 1f));
        phongShader.setUniform("useBlinnPhong", settings.blinnPhong);
        phongShader.setUniform("useTexture", settings.useTexture);
        phongShader.setUniform("useShadow", settings.shadowEnabled);
        phongShader.setUniform("biasScale", settings.biasScale);
        phongShader.setUniform("textureSampler", 0);   // GL_TEXTURE0 = tekstur permukaan
        phongShader.setUniform("shadowMap", 1);        // GL_TEXTURE1 = Shadow (Depth) Map
        shadowMap.bindDepthTexture(1);

        // Lantai dulu (agar urutan gambar tetap benar saat Z-Buffer dimatikan)
        groundTexture.bind(0);
        phongShader.setUniform("model", groundModel);
        phongShader.setUniform("objectColor", new Vector3f(0.55f, 0.57f, 0.60f));
        phongShader.setUniform("specularStrength", 0.15f);
        groundMesh.draw(settings.smoothShading);

        // Objek 3D
        objectTexture.bind(0);
        phongShader.setUniform("model", objectModel);
        phongShader.setUniform("objectColor", new Vector3f(0.85f, 0.35f, 0.25f));
        phongShader.setUniform("specularStrength", 0.8f);
        object.draw(settings.smoothShading);
    }

    private Vector3f lightPosition() {
        double a = Math.toRadians(lightAngle);
        return new Vector3f((float) (LIGHT_RADIUS * Math.cos(a)), LIGHT_HEIGHT, (float) (LIGHT_RADIUS * Math.sin(a)));
    }

    // ------------------------------------------------------------------- HUD
    private void updateTitle() {
        window.setTitle(TITLE + " [" + meshNames[settings.modelIndex] + "] | " + settings.summary()
                + String.format(" | %.0f FPS (%.2f ms)", fps.getFps(), fps.getFrameTimeMs())
                + (settings.vsync ? " | VSync" : ""));
    }

    private void printBenchmark() {
        System.out.printf("[BENCH] %s | model=%s | %.1f FPS (%.3f ms/frame) | vsync=%s%n",
                settings.summary(), meshNames[settings.modelIndex], fps.getFps(), fps.getFrameTimeMs(), settings.vsync);
    }

    private void printControls() {
        System.out.println("""

                ============ 3D Visual Realism Suite - KONTROL ============
                 B        Phong <-> Blinn-Phong        (Indikator 1)
                 F        Flat  <-> Smooth shading     (Indikator 2)
                 Z        Z-Buffer Depth Test ON/OFF   (Indikator 3)
                 C        Back-Face Culling ON/OFF     (Indikator 3)
                 T        Tekstur ON/OFF               (Indikator 4)
                 N        Filter: Nearest <-> Bilinear+Mipmap
                 S        Shadow Mapping ON/OFF        (Indikator 5)
                 [  ]     Kurangi / tambah Shadow Bias (0 = tanpa bias -> Shadow Acne)
                 1 2 3    Sphere / Torus / Torus Knot
                 Spasi    Jeda / lanjut animasi (objek berputar + cahaya mengorbit)
                 Mouse kiri drag, scroll, panah, + / -   : kamera orbit & zoom
                 V        VSync ON/OFF (default OFF agar FPS bisa dibandingkan)
                 P        Cetak baris benchmark (FPS + konfigurasi) ke console
                 F2       Simpan screenshot ke folder screenshots/
                 Esc      Keluar
                ===========================================================
                """);
    }

    // --------------------------------------------------------------- cleanup
    private void cleanup() {
        for (Mesh m : meshes) m.cleanup();
        groundMesh.cleanup();
        objectTexture.cleanup();
        groundTexture.cleanup();
        shadowMap.cleanup();
        phongShader.cleanup();
        depthShader.cleanup();
        window.destroy();
    }
}
