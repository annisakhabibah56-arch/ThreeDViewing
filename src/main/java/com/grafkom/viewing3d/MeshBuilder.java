package com.grafkom.viewing3d;

import org.joml.Vector3f;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Pembuat geometri prosedural (Sphere, Torus, Rumah 3D, Lantai) berupa triangle soup
 * dengan koordinat UV, serta penghitung vektor normal:
 *  - flatNormals   : satu normal per face (Flat Shading)
 *  - smoothNormals : rata-rata normal face pada tiap vertex (Smooth Shading)
 * Semua segitiga otomatis diorientasikan CCW menghadap keluar (aman untuk back-face culling).
 */
public final class MeshBuilder {
    private MeshBuilder() {}

    /** Hasil geometri: tiga vertex berurutan = satu segitiga. */
    public static final class Geometry {
        public final float[] positions; // x,y,z per vertex
        public final float[] uvs;       // u,v per vertex
        Geometry(float[] positions, float[] uvs) { this.positions = positions; this.uvs = uvs; }
        public int vertexCount() { return positions.length / 3; }
    }

    private static final class Builder {
        private float[] pos = new float[256];
        private float[] uv = new float[170];
        private int vertices = 0;

        /** Menambah segitiga; urutan vertex dibalik bila normalnya mengarah ke dalam. */
        void tri(Vector3f a, float[] ta, Vector3f b, float[] tb, Vector3f c, float[] tc, Vector3f insideRef) {
            Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
            if (normal.lengthSquared() < 1E-12f) return;   // segitiga degenerate (kutub bola)
            Vector3f centroid = new Vector3f(a).add(b).add(c).div(3f);
            if (normal.dot(new Vector3f(centroid).sub(insideRef)) < 0f) {
                Vector3f tmp = b; b = c; c = tmp;
                float[] t = tb; tb = tc; tc = t;
            }
            push(a, ta); push(b, tb); push(c, tc);
        }

        private void push(Vector3f p, float[] t) {
            if (vertices * 3 + 3 > pos.length) pos = Arrays.copyOf(pos, pos.length * 2);
            if (vertices * 2 + 2 > uv.length) uv = Arrays.copyOf(uv, uv.length * 2);
            pos[vertices * 3] = p.x; pos[vertices * 3 + 1] = p.y; pos[vertices * 3 + 2] = p.z;
            uv[vertices * 2] = t[0]; uv[vertices * 2 + 1] = t[1];
            vertices++;
        }

        Geometry build() {
            return new Geometry(Arrays.copyOf(pos, vertices * 3), Arrays.copyOf(uv, vertices * 2));
        }
    }

    // ------------------------------------------------------------------ Sphere
    public static Geometry sphere(float radius, int stacks, int slices, float uRepeat) {
        Builder b = new Builder();
        Vector3f center = new Vector3f();
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                Vector3f p00 = spherePoint(radius, stacks, slices, i, j);
                Vector3f p10 = spherePoint(radius, stacks, slices, i + 1, j);
                Vector3f p11 = spherePoint(radius, stacks, slices, i + 1, j + 1);
                Vector3f p01 = spherePoint(radius, stacks, slices, i, j + 1);
                float u0 = (float) j / slices * uRepeat, u1 = (float) (j + 1) / slices * uRepeat;
                float v0 = 1f - (float) i / stacks, v1 = 1f - (float) (i + 1) / stacks;
                b.tri(p00, new float[]{u0, v0}, p10, new float[]{u0, v1}, p11, new float[]{u1, v1}, center);
                b.tri(p00, new float[]{u0, v0}, p11, new float[]{u1, v1}, p01, new float[]{u1, v0}, center);
            }
        }
        return b.build();
    }

    private static Vector3f spherePoint(float r, int stacks, int slices, int i, int j) {
        double phi = Math.PI * i / stacks;
        double theta = 2 * Math.PI * (j % slices) / slices;   // j % slices: posisi seam identik
        return new Vector3f((float) (r * Math.sin(phi) * Math.cos(theta)),
                (float) (r * Math.cos(phi)),
                (float) (r * Math.sin(phi) * Math.sin(theta)));
    }

    // ------------------------------------------------------------------ Torus
    public static Geometry torus(float major, float minor, int majorSeg, int minorSeg,
                                 float uRepeat, float vRepeat) {
        Builder b = new Builder();
        for (int i = 0; i < majorSeg; i++) {
            for (int j = 0; j < minorSeg; j++) {
                Vector3f p00 = torusPoint(major, minor, majorSeg, minorSeg, i, j);
                Vector3f p10 = torusPoint(major, minor, majorSeg, minorSeg, i + 1, j);
                Vector3f p11 = torusPoint(major, minor, majorSeg, minorSeg, i + 1, j + 1);
                Vector3f p01 = torusPoint(major, minor, majorSeg, minorSeg, i, j + 1);
                float u0 = (float) i / majorSeg * uRepeat, u1 = (float) (i + 1) / majorSeg * uRepeat;
                float v0 = (float) j / minorSeg * vRepeat, v1 = (float) (j + 1) / minorSeg * vRepeat;
                float[] t00 = {u0, v0}, t10 = {u1, v0}, t11 = {u1, v1}, t01 = {u0, v1};
                b.tri(p00, t00, p10, t10, p11, t11, tubeCenter(major, p00, p10, p11));
                b.tri(p00, t00, p11, t11, p01, t01, tubeCenter(major, p00, p11, p01));
            }
        }
        return b.build();
    }

    private static Vector3f torusPoint(float R, float r, int majorSeg, int minorSeg, int i, int j) {
        double theta = 2 * Math.PI * (i % majorSeg) / majorSeg;
        double phi = 2 * Math.PI * (j % minorSeg) / minorSeg;
        double ring = R + r * Math.cos(phi);
        return new Vector3f((float) (ring * Math.cos(theta)), (float) (r * Math.sin(phi)),
                (float) (ring * Math.sin(theta)));
    }

    /** Titik pusat tabung terdekat dari titik berat segitiga = acuan "bagian dalam" torus. */
    private static Vector3f tubeCenter(float R, Vector3f a, Vector3f b, Vector3f c) {
        float cx = (a.x + b.x + c.x) / 3f, cz = (a.z + b.z + c.z) / 3f;
        double angle = Math.atan2(cz, cx);
        return new Vector3f((float) (R * Math.cos(angle)), 0f, (float) (R * Math.sin(angle)));
    }

    // ------------------------------------------------------------------ Rumah 3D
    /** Rumah: prisma lima sisi (dinding 2 x 1,2 x 2, atap pelana hingga y = 2). Cembung. */
    public static Geometry house() {
        Builder b = new Builder();
        Vector3f inside = new Vector3f(0, 0.9f, 0);
        Vector3f X = new Vector3f(1, 0, 0), Y = new Vector3f(0, 1, 0), Z = new Vector3f(0, 0, 1);
        float h = 1.2f, ridge = 2.0f;

        // dinding depan (z=+1) dan belakang (z=-1)
        polygon(b, inside, X, Y, 1.2f, 1.2f,
                v(-1, 0, 1), v(1, 0, 1), v(1, h, 1), v(-1, h, 1));
        polygon(b, inside, X, Y, 1.2f, 1.2f,
                v(-1, 0, -1), v(1, 0, -1), v(1, h, -1), v(-1, h, -1));
        // dinding samping berbentuk segi lima (x=+1 dan x=-1)
        polygon(b, inside, Z, Y, 1.2f, 1.2f,
                v(1, 0, -1), v(1, 0, 1), v(1, h, 1), v(1, ridge, 0), v(1, h, -1));
        polygon(b, inside, Z, Y, 1.2f, 1.2f,
                v(-1, 0, -1), v(-1, 0, 1), v(-1, h, 1), v(-1, ridge, 0), v(-1, h, -1));
        // atap pelana
        polygon(b, inside, X, new Vector3f(0, 0.6247f, -0.7809f), 1.2f, 1.2f,
                v(-1, h, 1), v(1, h, 1), v(1, ridge, 0), v(-1, ridge, 0));
        polygon(b, inside, X, new Vector3f(0, 0.6247f, 0.7809f), 1.2f, 1.2f,
                v(-1, h, -1), v(1, h, -1), v(1, ridge, 0), v(-1, ridge, 0));
        // alas
        polygon(b, inside, X, Z, 1.2f, 1.2f,
                v(-1, 0, -1), v(1, 0, -1), v(1, 0, 1), v(-1, 0, 1));
        return b.build();
    }

    private static Vector3f v(float x, float y, float z) { return new Vector3f(x, y, z); }

    /** Poligon cembung dipecah kipas; UV = proyeksi planar titik pada sumbu uDir/vDir. */
    private static void polygon(Builder b, Vector3f inside, Vector3f uDir, Vector3f vDir,
                                float su, float sv, Vector3f... pts) {
        float[][] uv = new float[pts.length][];
        for (int k = 0; k < pts.length; k++)
            uv[k] = new float[]{(pts[k].dot(uDir) + 2f) * su, (pts[k].dot(vDir) + 2f) * sv};
        for (int k = 1; k < pts.length - 1; k++)
            b.tri(pts[0], uv[0], pts[k], uv[k], pts[k + 1], uv[k + 1], inside);
    }

    // ------------------------------------------------------------------ Lantai
    /** Bidang lantai persegi di y = 0, normal ke atas, UV berulang uvRepeat kali. */
    public static Geometry ground(float halfSize, float uvRepeat) {
        Builder b = new Builder();
        Vector3f below = new Vector3f(0, -100f, 0);
        Vector3f a = v(-halfSize, 0, halfSize), c = v(halfSize, 0, halfSize),
                d = v(halfSize, 0, -halfSize), e = v(-halfSize, 0, -halfSize);
        b.tri(a, new float[]{0, 0}, c, new float[]{uvRepeat, 0}, d, new float[]{uvRepeat, uvRepeat}, below);
        b.tri(a, new float[]{0, 0}, d, new float[]{uvRepeat, uvRepeat}, e, new float[]{0, uvRepeat}, below);
        return b.build();
    }

    // ------------------------------------------------------------------ Normal
    /** Flat Shading: satu vektor normal per segitiga (face normal). */
    public static float[] flatNormals(float[] pos) {
        float[] normals = new float[pos.length];
        for (int t = 0; t + 8 < pos.length; t += 9) {
            Vector3f n = faceNormal(pos, t);
            for (int k = 0; k < 3; k++) {
                normals[t + k * 3] = n.x; normals[t + k * 3 + 1] = n.y; normals[t + k * 3 + 2] = n.z;
            }
        }
        return normals;
    }

    private record Key(long x, long y, long z) {
        static Key of(float[] p, int o) {
            return new Key(Math.round(p[o] * 1E4f), Math.round(p[o + 1] * 1E4f), Math.round(p[o + 2] * 1E4f));
        }
    }

    /**
     * Smooth Shading: N_vertex = (sum N_face) / |sum N_face| untuk semua face yang berbagi
     * posisi vertex yang sama. Vertex di seam UV (posisi sama, UV beda) ikut digabung
     * sehingga tidak muncul garis jahitan pada pencahayaan.
     */
    public static float[] smoothNormals(float[] pos) {
        Map<Key, Vector3f> sum = new HashMap<>();
        for (int t = 0; t + 8 < pos.length; t += 9) {
            Vector3f n = faceNormal(pos, t);
            for (int k = 0; k < 3; k++)
                sum.computeIfAbsent(Key.of(pos, t + k * 3), key -> new Vector3f()).add(n);
        }
        float[] normals = new float[pos.length];
        for (int t = 0; t + 8 < pos.length; t += 9) {
            for (int k = 0; k < 3; k++) {
                Vector3f n = new Vector3f(sum.get(Key.of(pos, t + k * 3)));
                if (n.lengthSquared() < 1E-12f) n = faceNormal(pos, t);
                else n.normalize();
                normals[t + k * 3] = n.x; normals[t + k * 3 + 1] = n.y; normals[t + k * 3 + 2] = n.z;
            }
        }
        return normals;
    }

    private static Vector3f faceNormal(float[] p, int o) {
        Vector3f a = new Vector3f(p[o], p[o + 1], p[o + 2]);
        Vector3f b = new Vector3f(p[o + 3], p[o + 4], p[o + 5]);
        Vector3f c = new Vector3f(p[o + 6], p[o + 7], p[o + 8]);
        Vector3f n = b.sub(a).cross(c.sub(a));
        if (n.lengthSquared() < 1E-12f) return new Vector3f(0, 1, 0);
        return n.normalize();
    }
}
