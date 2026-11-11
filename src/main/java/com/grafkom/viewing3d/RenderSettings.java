package com.grafkom.viewing3d;

/** Seluruh status yang dikendalikan lewat keyboard (satu sumber kebenaran untuk HUD, shader, dan screenshot). */
public class RenderSettings {
    public boolean blinnPhong = true;          // B : Phong <-> Blinn-Phong          (Indikator 1)
    public boolean smoothShading = true;       // F : Flat  <-> Smooth               (Indikator 2)
    public boolean depthTest = true;           // Z : Z-Buffer Depth Test ON/OFF     (Indikator 3)
    public boolean backFaceCulling = true;     // C : Back-Face Culling ON/OFF       (Indikator 3)
    public boolean useTexture = true;          // T : tekstur ON/OFF                 (Indikator 4)
    public Texture2D.FilterMode filterMode = Texture2D.FilterMode.BILINEAR_MIPMAP;  // N : Nearest <-> Bilinear+Mipmap
    public boolean shadowEnabled = true;       // S : Shadow Mapping ON/OFF          (Indikator 5)
    public float biasScale = 1.0f;             // [ ] : pengali Shadow Bias          (Indikator 5)
    public boolean animate = true;             // Spasi : jeda animasi
    public boolean vsync = false;              // V : VSync (OFF agar FPS dapat dibandingkan)
    public int modelIndex = 0;                 // 1 / 2 / 3 : pilih mesh

    public String summary() {
        return (blinnPhong ? "Blinn-Phong" : "Phong")
                + " | " + (smoothShading ? "Smooth" : "Flat")
                + " | Z:" + onOff(depthTest)
                + " | Cull:" + onOff(backFaceCulling)
                + " | Tex:" + (useTexture ? filterMode.label : "OFF")
                + " | Shadow:" + (shadowEnabled ? "ON bias x" + String.format("%.2f", biasScale) : "OFF");
    }

    /** Potongan nama file untuk screenshot. */
    public String fileTag() {
        return (blinnPhong ? "blinn" : "phong")
                + "_" + (smoothShading ? "smooth" : "flat")
                + "_z" + (depthTest ? 1 : 0)
                + "_cull" + (backFaceCulling ? 1 : 0)
                + "_tex" + (useTexture ? (filterMode == Texture2D.FilterMode.NEAREST ? "nearest" : "mip") : "off")
                + "_shadow" + (shadowEnabled ? String.format("%.2f", biasScale) : "off");
    }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }
}
