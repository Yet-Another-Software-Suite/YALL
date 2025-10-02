package limelight;

/**
 * Represents the physical specifications of a Limelight camera model,
 * including its name, sensor type, field of view, and projection type.
 */
public class LimelightModel {
        /**
         * Projection type defines how the camera projects 3D points into 2D:
         * - PLANAR: Standard pinhole/rolling shutter projection (rectilinear).
         * - SPHERICAL: Fisheye-like projection (common with global shutter + wide FOV).
         */
        public enum ProjectionType {
                PLANAR,
                SPHERICAL
        }

        /** Human-readable model name (e.g., "Limelight 3A"). */
        public final String modelName;

        /** Camera sensor model (e.g., "ov5647", "ov9281", "imx462"). */
        public final String cameraModel;

        /** Horizontal field of view in degrees. */
        public final double horizontalFov;

        /** Vertical field of view in degrees. */
        public final double verticalFov;

        /** Projection type (PLANAR or SPHERICAL). */
        public final ProjectionType projectionType;

        /**
         * Constructs a Limelight camera model description.
         *
         * @param modelName      Human-readable name
         * @param cameraModel    Sensor model name
         * @param horizontalFov  Horizontal field of view (degrees, > 0)
         * @param verticalFov    Vertical field of view (degrees, > 0)
         * @param projectionType Projection model (planar or spherical)
         */
        public LimelightModel(String modelName, String cameraModel,
                        double horizontalFov, double verticalFov,
                        ProjectionType projectionType) {
                assert horizontalFov > 0 : "Horizontal FOV must be positive";
                assert verticalFov > 0 : "Vertical FOV must be positive";

                this.modelName = modelName;
                this.cameraModel = cameraModel;
                this.horizontalFov = horizontalFov;
                this.verticalFov = verticalFov;
                this.projectionType = projectionType;
        }

        /** @return true if the camera uses a planar/pinhole projection model. */
        public boolean isPlanar() {
                return projectionType == ProjectionType.PLANAR;
        }

        /** @return true if the camera uses a spherical/fisheye projection model. */
        public boolean isSpherical() {
                return projectionType == ProjectionType.SPHERICAL;
        }

        /**
         * @return true if the camera likely uses a global shutter sensor
         *         (associated with spherical projection).
         */
        public boolean isGlobalShutter() {
                return isSpherical();
        }

        /**
         * @return true if the camera likely uses a rolling shutter sensor
         *         (associated with planar projection).
         */
        public boolean isRollingShutter() {
                return isPlanar();
        }

        // =====================
        // Predefined models
        // =====================

        /** Limelight 2 / 2+ (OV5647 rolling shutter → planar). */
        public static final LimelightModel Limelight2 = new LimelightModel(
                        "Limelight 2 / 2+", "ov5647", 62.5, 48.9, ProjectionType.PLANAR);

        /** Limelight 3 (OV5647 rolling shutter → planar). */
        public static final LimelightModel Limelight3 = new LimelightModel(
                        "Limelight 3", "ov5647", 62.5, 48.9, ProjectionType.PLANAR);

        /** Limelight 3A (OV5647 rolling shutter → planar). */
        public static final LimelightModel Limelight3A = new LimelightModel(
                        "Limelight 3A", "ov5647", 54.5, 42.0, ProjectionType.PLANAR);

        /** Limelight 3G (OV9281 global shutter → spherical). */
        public static final LimelightModel Limelight3G = new LimelightModel(
                        "Limelight 3G", "ov9281", 82.0, 56.2, ProjectionType.SPHERICAL);

        /** Limelight 4 (OV9281 global shutter → spherical). */
        public static final LimelightModel Limelight4 = new LimelightModel(
                        "Limelight 4", "ov9281", 82.0, 56.2, ProjectionType.SPHERICAL);

        /** Limelight 4 with IMX462 upgrade (rolling shutter → planar). */
        public static final LimelightModel Limelight4_IMX462 = new LimelightModel(
                        "Limelight 4 (IMX462 upgrade)", "imx462", 82.0, 56.2, ProjectionType.PLANAR);
}
