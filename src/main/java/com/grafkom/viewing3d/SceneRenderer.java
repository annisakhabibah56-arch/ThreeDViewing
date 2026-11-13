package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.lwjgl.opengl.GL13.*;

/**
 * 3D Visual Realism Suite: shading model, flat/smooth, HSR, texture mapping, dan shadow mapping.
 * Menyimpan seluruh resource GPU dan status toggle yang diubah dari keyboard.
 */
public final class SceneRenderer {
    public enum MeshType {
        SPHERE("Sphere"), TORUS("Torus"), HOUSE("Rumah 3D");
        private final String label;
        MeshType(String label) { this.label = label; }
        public String label() { return label; }
    }

    private static final float GROUND_Y = -0.002f;   // sedikit di bawah alas objek agar tidak z-fighting

    private final ShaderProgram phong = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
    private final ShaderProgram depth = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
    private final Texture2D brick = new Texture2D(texturePath("brick.png"));
    private final Texture2D floor = new Texture2D(texturePath("floor.png"));
    private final ShadowMapFBO shadowMap = new ShadowMapFBO();

    private final Mesh3D ground = new Mesh3D(MeshBuilder.ground(10f, 16f));
    private final Mesh3D[] objects = {
            new Mesh3D(MeshBuilder.sphere(1f, 10, 16, 2f)),
            new Mesh3D(MeshBuilder.torus(1.1f, 0.45f, 24, 12, 3f, 1f)),
            new Mesh3D(MeshBuilder.house())
    };
    private final Matrix4f[] objectModels = {
            new Matrix4f().translation(0, 1.0f, 0),
            new Matrix4f().translation(0, 0.45f, 0),
            new Matrix4f()
    };
    private final Matrix4f groundModel = new Matrix4f().translation(0, GROUND_Y, 0);

    // status toggle
    private boolean blinnPhong = true, smooth = true, depthTest = true, culling = true;
    private boolean textures = true, shadows = true, animateLight = true, mipmapped = true;
    private int meshIndex = 0;

    // cahaya
    private float lightAngle = 0.8f;
    private final Vector3f lightPos = new Vector3f();
    private final Matrix4f lightSpace = new Matrix4f();

    public SceneRenderer() {
        phong.use();
        phong.setInt("textureSampler", 0);   // texture unit 0 = tekstur permukaan
        phong.setInt("shadowMap", 1);        // texture unit 1 = shadow map
        phong.setVec3("lightColor", 1f, 1f, 1f);
        updateLight(0f);
    }

    /** stbi_load membaca file dari disk, jadi path dicari relatif terhadap folder project. */
    private static String texturePath(String name) {
        String[] candidates = {"src/main/resources/textures/" + name, "build/textures/" + name, "textures/" + name};
        for (String c : candidates)
            if (Files.isRegularFile(Paths.get(c))) return c;
        System.err.println("File tekstur tidak ditemukan: " + candidates[0]
                + " (jalankan program dari folder project ThreeDViewing)");
        return candidates[0];
    }

    // ---------------------------------------------------------------- toggle
    public void toggleBlinnPhong() { blinnPhong = !blinnPhong; }
    public void toggleSmooth() { smooth = !smooth; }
    public void toggleDepthTest() { depthTest = !depthTest; }
    public void toggleCulling() { culling = !culling; }
    public void toggleTexture() { textures = !textures; }
    public void toggleShadows() { shadows = !shadows; }
    public void toggleLightAnimation() { animateLight = !animateLight; }
    public void nextMesh() { meshIndex = (meshIndex + 1) % objects.length; }
    public void toggleTextureFilter() {
        mipmapped = !mipmapped;
        brick.setMipmapFiltering(mipmapped);
        floor.setMipmapFiltering(mipmapped);
    }

    public void resetDefaults() {
        blinnPhong = smooth = depthTest = culling = textures = shadows = animateLight = true;
        meshIndex = 0;
        lightAngle = 0.8f;
        if (!mipmapped) toggleTextureFilter();
        updateLight(0f);
    }

    public String status() {
        return String.format(java.util.Locale.ROOT,
                "%s | %s | %s | Z-Buffer %s | Culling %s | Tekstur %s | Bayangan %s",
                MeshType.values()[meshIndex].label(),
                blinnPhong ? "Blinn-Phong" : "Phong",
                smooth ? "Smooth" : "Flat",
                depthTest ? "ON" : "OFF", culling ? "ON" : "OFF",
                textures ? (mipmapped ? "Bilinear+Mipmap" : "Nearest") : "OFF",
                shadows ? "ON" : "OFF");
    }

    // ---------------------------------------------------------------- cahaya
    public void update(float dt) { updateLight(animateLight ? dt : 0f); }

    private void updateLight(float dt) {
        lightAngle += dt * 0.5f;
        lightPos.set(5.5f * (float) Math.cos(lightAngle), 6.5f, 5.5f * (float) Math.sin(lightAngle));
        // Directional light: proyeksi ortografik dari sudut pandang cahaya.
        // Near/far (5 .. 11.5) dipilih agar rentang kedalaman 6.5 sehingga bias dinamis
        // max(0.05 * (1 - N.L), 0.005) dari modul bekerja tanpa Shadow Acne / Peter Panning.
        Matrix4f lightProjection = new Matrix4f().ortho(-6f, 6f, -6f, 6f, 5f, 11.5f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0, 0.5f, 0), new Vector3f(0, 1, 0));
        lightSpace.set(lightProjection).mul(lightView);
    }

    // ---------------------------------------------------------------- Pass 1: depth dari sudut cahaya
    public void renderShadowPass(int screenWidth, int screenHeight) {
        if (!shadows) return;
        depth.use();
        depth.setMat4("lightSpaceMatrix", lightSpace);
        shadowMap.bindForWriting();
        HSRManager.setupDepthTest(true);
        HSRManager.setupBackFaceCulling(false);   // seluruh sisi ikut ditulis; yang tersimpan = terdekat ke cahaya
        depth.setMat4("model", objectModels[meshIndex]);
        objects[meshIndex].draw(smooth);
        shadowMap.unbind(screenWidth, screenHeight);
    }

    // ---------------------------------------------------------------- Pass 2: render dari kamera
    public void renderScene(Matrix4f projection, Matrix4f view, Vector3f eye, boolean grayscale) {
        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(culling);

        phong.use();
        phong.setMat4("projection", projection);
        phong.setMat4("view", view);
        phong.setMat4("lightSpaceMatrix", lightSpace);
        phong.setVec3("lightPos", lightPos);
        phong.setVec3("viewPos", eye);
        phong.setBool("useBlinnPhong", blinnPhong);
        phong.setBool("useTexture", textures);
        phong.setBool("useShadows", shadows);
        phong.setBool("grayscaleMode", grayscale);

        glActiveTexture(GL_TEXTURE0 + 1);                         // unit 1 = Shadow Map
        glBindTexture(GL_TEXTURE_2D, shadowMap.getDepthTextureID());

        // lantai dahulu, objek sesudahnya (urutan ini membuat efek HSR OFF terlihat jelas)
        floor.bind(0);
        phong.setMat4("model", groundModel);
        phong.setVec3("objectColor", 0.55f, 0.6f, 0.68f);
        ground.draw(false);

        brick.bind(0);
        phong.setMat4("model", objectModels[meshIndex]);
        phong.setVec3("objectColor", 0.85f, 0.45f, 0.3f);
        objects[meshIndex].draw(smooth);

        glActiveTexture(GL_TEXTURE0);
    }

    public void dispose() {
        phong.dispose(); depth.dispose();
        brick.dispose(); floor.dispose();
        ground.dispose();
        for (Mesh3D m : objects) m.dispose();
    }
}
