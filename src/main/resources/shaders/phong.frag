#version 330 core
// ==========================================================
// FILE: src/main/resources/shaders/phong.frag (Fragment Shader)
// ==========================================================
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;       // [Indikator 5]

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;
uniform sampler2D shadowMap;     // [Indikator 5] Depth Texture dari ShadowMapFBO
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadows;         // [Indikator 5] bayangan ON/OFF
uniform bool grayscaleMode;      // mode stereo anaglyph (warisan Minggu 10)

// [Indikator 5] Pass 2 Shadow Mapping: bandingkan kedalaman fragment dengan Shadow Map.
// Mengembalikan 0.0 (terang) sampai 1.0 (bayangan penuh); PCF 3x3 menghaluskan tepi.
float calculateShadow(vec4 fragPosLightSpace, vec3 norm, vec3 lightDir) {
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    if (projCoords.z > 1.0)
        return 0.0;

    // Shadow bias dinamis berdasarkan kemiringan normal (mencegah Shadow Acne)
    float bias = max(0.05 * (1.0 - dot(norm, lightDir)), 0.005);

    float shadow = 0.0;
    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float closestDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += (projCoords.z - bias > closestDepth) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
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
    float specularStrength = 0.8;
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0); // Blinn-Phong
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Standard Phong
    }
    vec3 specular = specularStrength * spec * lightColor;

    // [Indikator 5] Faktor bayangan: 1.0 terang, 0.2 bayangan penuh
    float shadow = useShadows ? calculateShadow(FragPosLightSpace, norm, lightDir) : 0.0;
    float shadowFactor = 1.0 - 0.8 * shadow;

    // Combine Texture & Lighting
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + shadowFactor * (diffuse + specular)) * baseColor;
    if (grayscaleMode)
        result = vec3(dot(result, vec3(0.299, 0.587, 0.114)));
    FragColor = vec4(result, 1.0);
}
