// ==========================================================
// FILE: src/main/resources/shaders/phong.frag (Fragment Shader)
// Praktikum 1  : Ambient + Diffuse + Specular (Phong vs Blinn-Phong), per-pixel
// Tugas Mandiri: + Shadow Mapping (perbandingan depth di Light Space)
// ==========================================================
#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;
uniform bool useTexture;
uniform bool useBlinnPhong;

// ---- [TAMBAHAN] ----
uniform float specularStrength;  // modul: konstanta 0.8 -> dijadikan uniform (lantai dibuat kurang mengkilap)
uniform sampler2D shadowMap;     // Depth Map hasil Render Pass 1 (texture unit 1)
uniform bool useShadow;          // toggle bayangan (tombol S)
uniform float biasScale;         // pengali Shadow Bias (tombol [ dan ])

// Render Pass 2 - Penentuan Bayangan
// d_current > d_shadow + bias  ->  BAYANGAN (0.2),  selain itu TERANGI CAHAYA (1.0)
float calculateShadow(vec4 fragPosLightSpace, vec3 norm, vec3 lightDir) {
    // perspective divide -> NDC [-1,1] -> [0,1]
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;

    // di luar far-plane cahaya -> tidak mungkin terhalang
    if (projCoords.z > 1.0) return 1.0;

    float d_shadow  = texture(shadowMap, projCoords.xy).r; // depth terdekat dari sisi cahaya
    float d_current = projCoords.z;                        // depth fragment saat ini

    // Shadow Bias dinamis (modul): bias = max(0.05 * (1.0 - N.L), 0.005)
    float bias = max(0.05 * (1.0 - dot(norm, lightDir)), 0.005) * biasScale;

    return (d_current > d_shadow + bias) ? 0.2 : 1.0;
}

void main() {
    // 1. Ambient Lighting
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse Lighting (Lambertian)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular Lighting (Phong vs Blinn-Phong)
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (diff > 0.0) { // tidak ada kilauan pada sisi yang membelakangi cahaya
        if (useBlinnPhong) {
            vec3 halfwayDir = normalize(lightDir + viewDir);
            spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0); // Blinn-Phong
        } else {
            vec3 reflectDir = reflect(-lightDir, norm);
            spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Standard Phong
        }
    }
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow Factor (ambient tidak ikut diredam bayangan)
    float shadow = useShadow ? calculateShadow(FragPosLightSpace, norm, lightDir) : 1.0;

    // Combine Texture & Lighting
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + shadow * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
