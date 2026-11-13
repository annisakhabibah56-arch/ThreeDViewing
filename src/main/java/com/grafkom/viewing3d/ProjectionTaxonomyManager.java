package com.grafkom.viewing3d;

import org.joml.Matrix4f;

/** Enam mode sesuai enum dan contoh tombol 1 sampai 6 dalam modul. */
public final class ProjectionTaxonomyManager {
    public enum ProjectionType {
        ORTHOGRAPHIC,
        ISOMETRIC,
        OBLIQUE_CAVALIER,
        PERSPECTIVE_1POINT,
        PERSPECTIVE_2POINT,
        PERSPECTIVE_3POINT;

        public boolean isPerspective() {
            return switch (this) {
                case PERSPECTIVE_1POINT, PERSPECTIVE_2POINT, PERSPECTIVE_3POINT -> true;
                default -> false;
            };
        }
    }

    private ProjectionType type = ProjectionType.PERSPECTIVE_3POINT;

    public ProjectionType getType() { return type; }

    public void select(ProjectionType type) {
        this.type = java.util.Objects.requireNonNull(type);
    }

    public Matrix4f calculateProjection(PerspectiveProjection perspective, float radius) {
        return switch (type) {
            case ORTHOGRAPHIC, ISOMETRIC -> orthographic(perspective, radius);
            case OBLIQUE_CAVALIER -> {
                // Faktor kedalaman 1, sudut shear 45 derajat.
                float k = (float) Math.sqrt(0.5);
                Matrix4f shear = new Matrix4f()
                        .m20(-k).m21(-k)
                        .m30(-k * radius).m31(-k * radius);
                // Translasi shear menjaga bidang target z=-radius tidak bergeser.
                yield orthographic(perspective, radius).mul(shear);
            }
            case PERSPECTIVE_1POINT, PERSPECTIVE_2POINT, PERSPECTIVE_3POINT ->
                    perspective.calculateProjectionMatrix();
        };
    }

    private Matrix4f orthographic(PerspectiveProjection perspective, float radius) {
        float halfHeight = radius * 0.45f;
        float halfWidth = halfHeight * perspective.getAspectRatio();
        return new Matrix4f().ortho(-halfWidth, halfWidth, -halfHeight, halfHeight,
                perspective.getNear(), perspective.getFar());
    }

    // Jumlah titik hilang ditentukan oleh orientasi kamera terhadap sumbu objek.
    public float defaultAzimuth() {
        return switch (type) {
            case ISOMETRIC, PERSPECTIVE_2POINT, PERSPECTIVE_3POINT -> 45f;
            default -> 0f;
        };
    }

    public float defaultElevation() {
        return switch (type) {
            case ISOMETRIC -> 35.26439f;
            case PERSPECTIVE_3POINT -> 25f;
            default -> 0f;
        };
    }
}

