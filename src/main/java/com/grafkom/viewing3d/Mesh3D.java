package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh 3D bermateri di GPU: posisi + vektor normal vertex + koordinat tekstur UV.
 * Menyimpan dua VAO: normal per-face (Flat) dan normal rata-rata per-vertex (Smooth).
 * Layout interleaved: location 0 = posisi, 1 = normal, 2 = UV (sesuai phong.vert).
 */
public final class Mesh3D {
    private static final int FLOATS_PER_VERTEX = 8;
    private final int vaoFlat, vaoSmooth, vboFlat, vboSmooth;
    private final int vertexCount;

    public Mesh3D(MeshBuilder.Geometry geometry) {
        vertexCount = geometry.vertexCount();
        float[] flat = MeshBuilder.flatNormals(geometry.positions);
        float[] smooth = MeshBuilder.smoothNormals(geometry.positions);
        vaoFlat = glGenVertexArrays();
        vboFlat = glGenBuffers();
        upload(vaoFlat, vboFlat, geometry, flat);
        vaoSmooth = glGenVertexArrays();
        vboSmooth = glGenBuffers();
        upload(vaoSmooth, vboSmooth, geometry, smooth);
        glBindVertexArray(0);
    }

    private void upload(int vao, int vbo, MeshBuilder.Geometry g, float[] normals) {
        float[] data = new float[vertexCount * FLOATS_PER_VERTEX];
        for (int i = 0; i < vertexCount; i++) {
            int o = i * FLOATS_PER_VERTEX;
            data[o] = g.positions[i * 3];
            data[o + 1] = g.positions[i * 3 + 1];
            data[o + 2] = g.positions[i * 3 + 2];
            data[o + 3] = normals[i * 3];
            data[o + 4] = normals[i * 3 + 1];
            data[o + 5] = normals[i * 3 + 2];
            data[o + 6] = g.uvs[i * 2];
            data[o + 7] = g.uvs[i * 2 + 1];
        }
        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
    }

    public void draw(boolean smooth) {
        glBindVertexArray(smooth ? vaoSmooth : vaoFlat);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void dispose() {
        glDeleteVertexArrays(vaoFlat);
        glDeleteVertexArrays(vaoSmooth);
        glDeleteBuffers(vboFlat);
        glDeleteBuffers(vboSmooth);
    }
}
