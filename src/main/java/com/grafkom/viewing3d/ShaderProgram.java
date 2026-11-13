package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static org.lwjgl.opengl.GL20.*;

/** Membungkus vertex + fragment shader GLSL 330 core: kompilasi, link, dan uniform. */
public final class ShaderProgram {
    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public ShaderProgram(String vertexResource, String fragmentResource) {
        String vertexSource;
        String fragmentSource;
        try {
            vertexSource = Resources.readText(vertexResource);
            fragmentSource = Resources.readText(fragmentResource);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca shader: " + e.getMessage(), e);
        }
        int vertex = compile(GL_VERTEX_SHADER, vertexSource, vertexResource);
        int fragment = compile(GL_FRAGMENT_SHADER, fragmentSource, fragmentResource);

        programId = glCreateProgram();
        glAttachShader(programId, vertex);
        glAttachShader(programId, fragment);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            String log = glGetProgramInfoLog(programId);
            throw new IllegalStateException("Link shader gagal (" + vertexResource + " + "
                    + fragmentResource + "):\n" + log);
        }
        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    private static int compile(int type, String source, String name) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String log = glGetShaderInfoLog(shader);
            throw new IllegalStateException("Kompilasi shader gagal (" + name + "):\n" + log);
        }
        return shader;
    }

    public void use() { glUseProgram(programId); }

    private int location(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void setInt(String name, int value) { glUniform1i(location(name), value); }
    public void setBool(String name, boolean value) { glUniform1i(location(name), value ? 1 : 0); }
    public void setFloat(String name, float value) { glUniform1f(location(name), value); }
    public void setVec3(String name, Vector3f v) { glUniform3f(location(name), v.x, v.y, v.z); }
    public void setVec3(String name, float x, float y, float z) { glUniform3f(location(name), x, y, z); }
    public void setMat4(String name, Matrix4f m) { glUniformMatrix4fv(location(name), false, m.get(new float[16])); }

    public void dispose() { glDeleteProgram(programId); }
}
