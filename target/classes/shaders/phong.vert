// ==========================================================
// FILE: src/main/resources/shaders/phong.vert (Vertex Shader)
// Praktikum 1  : Phong / Blinn-Phong per-pixel lighting
// Tugas Mandiri: + FragPosLightSpace untuk Shadow Mapping
// ==========================================================
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;

out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoords;
out vec4 FragPosLightSpace;   // [TAMBAHAN] posisi fragment di Light Space (Shadow Mapping)

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix; // [TAMBAHAN] P_light * V_light

void main() {
    FragPos = vec3(model * vec4(aPos, 1.0));
    Normal = mat3(transpose(inverse(model))) * aNormal; // Normal Matrix
    TexCoords = aTexCoords;
    FragPosLightSpace = lightSpaceMatrix * vec4(FragPos, 1.0);
    gl_Position = projection * view * vec4(FragPos, 1.0);
}
