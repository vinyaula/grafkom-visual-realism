# 3D Visual Realism Suite - Grafika Komputer Minggu 11

Rendering Faces pada Visual Realism (Java 17 + LWJGL 3 + JOML + GLSL 330 core).
Memenuhi Praktikum 1-4 dan Spesifikasi Tugas Mandiri (a-f) pada modul.

## Menjalankan
Syarat: JDK 17+, Maven, GPU/driver OpenGL 3.3+.

**IntelliJ IDEA**: File > Open > pilih `pom.xml` > Open as Project > tunggu Maven import > run `Main`.
(macOS: Run > Edit Configurations > VM options: `-XstartOnFirstThread`)

**Terminal**: `mvn compile exec:exec`  (VM option macOS sudah diatur otomatis oleh profile di pom.xml)

## Kontrol keyboard
| Tombol | Fungsi | Indikator |
|---|---|---|
| **B** | Phong <-> Blinn-Phong | 1 |
| **F** | Flat (per-face normal) <-> Smooth (vertex normal averaging) | 2 |
| **Z** | Z-Buffer Depth Test ON/OFF | 3 |
| **C** | Back-Face Culling ON/OFF | 3 |
| **T** | Tekstur ON/OFF | 4 |
| **N** | Filter Nearest <-> Bilinear + Mipmap | 4 |
| **S** | Shadow Mapping ON/OFF | 5 |
| **[ / ]** | Kurangi / tambah Shadow Bias (0 = tanpa bias -> Shadow Acne, besar -> Peter Panning) | 5 |
| 1 / 2 / 3 | Sphere / Torus / Torus Knot | a |
| Spasi | Jeda animasi (objek berputar + cahaya mengorbit) | - |
| Mouse kiri (drag), scroll, panah, +/- | Kamera orbit & zoom | - |
| V | VSync ON/OFF (default OFF supaya FPS bisa dibandingkan) | - |
| P | Cetak baris benchmark (FPS + konfigurasi) ke console | laporan #5 |
| F2 | Simpan screenshot ke folder `screenshots/` (nama file memuat konfigurasi) | laporan #4 |

Status semua toggle + FPS tampil di judul jendela.

## Struktur kode (package `com.grafkom.viewing3d`)
| File | Peran |
|---|---|
| `Main`, `VisualRealismApp` | entry point, game loop, input, Render Pass 1 (depth) & 2 (lighting) |
| `HSRManager` | **Praktikum 2** - Z-Buffer & Back-Face Culling |
| `Texture2D` | **Praktikum 3** - STBImage loader, UV, filter Nearest / Bilinear+Mipmap |
| `ShadowMapFBO` | **Praktikum 4** - FBO + depth texture 2048x2048 |
| `resources/shaders/phong.vert/.frag` | **Praktikum 1** - Phong/Blinn-Phong per-pixel + shadow factor |
| `resources/shaders/depth.vert/.frag` | depth pass shadow mapping |
| `MeshBuilder`, `MeshData`, `MeshFactory`, `Mesh` | mesh (Sphere, Torus, Torus Knot, lantai) dengan normal + UV; normal flat & smooth |
| `ShaderProgram`, `Window`, `Camera`, `RenderSettings`, `FpsCounter`, `ScreenshotUtil` | pendukung |

## Skenario uji untuk screenshot laporan
1. **Phong vs Blinn-Phong**: matikan tekstur (T), jeda (Spasi), tekan B bergantian; amati bentuk highlight.
2. **Flat vs Smooth**: tekan F pada Sphere (tekstur off). Facet terlihat pada Flat.
3. **HSR**: (a) Z ON + C ON = normal; (b) Z OFF + C ON; (c) Z OFF + C OFF = tampak tumpang-tindih;
   (d) putar kamera ke bawah lantai (panah bawah) dengan C ON -> lantai hilang (di-cull), C OFF -> lantai terlihat.
4. **Tekstur**: turunkan sudut kamera ke hampir sejajar lantai, bandingkan N (Nearest = moire/aliasing vs Mipmap = halus).
5. **Shadow**: S ON; lalu `[` sampai bias 0 (Shadow Acne) dan `]` sampai besar (Peter Panning).

## Catatan dibanding kode di modul
- `Texture2D` pada modul hanya meng-import `GL11.*` sehingga `glActiveTexture` / `GL_TEXTURE0` (ada di `GL13`) tidak ditemukan;
  di sini ditambahkan `import static org.lwjgl.opengl.GL13.*`.
- `specularStrength` dijadikan uniform (lantai dibuat lebih redup), dan specular tidak dihitung pada sisi yang membelakangi cahaya.
- Normal Smooth dihitung dengan "welding" per posisi sehingga seam UV dan kutub bola mendapat normal identik.
