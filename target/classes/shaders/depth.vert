// ==========================================================
// FILE: depth.vert - Shadow Mapping, Render Pass 1 (Depth Pass)
// Scene dirender dari sudut pandang SUMBER CAHAYA (Light Space)
// ==========================================================
#version 330 core
layout (location = 0) in vec3 aPos;

uniform mat4 lightSpaceMatrix; // P_light * V_light
uniform mat4 model;

void main() {
    gl_Position = lightSpaceMatrix * model * vec4(aPos, 1.0);
}
