package com.grafkom.viewing3d;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Pembaca file teks (shader GLSL) dari classpath: src/main/resources atau di dalam JAR. */
public final class Resources {
    private Resources() {}

    public static String readText(String path) throws IOException {
        try (InputStream in = Resources.class.getResourceAsStream(path)) {
            if (in == null) throw new FileNotFoundException("Resource tidak ditemukan: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
