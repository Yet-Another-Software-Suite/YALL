package limelight.sim;

import edu.wpi.first.math.geometry.Pose3d;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * A field CAD model loaded from a binary STL file and drawn as solid gray surfaces into the {@link LimelightSim} video stream. See {@link LimelightSim#withVideoStream(String)}.
 *
 * <p>
 * The model is only drawn, it does not affect the simulated NetworkTables data.
 *
 * @implNote Surfaces are drawn farthest first (the "painter's algorithm"), so nearer surfaces cover farther ones. This is simple and fast, but large surfaces that overlap in depth can occasionally be
 * drawn in the wrong order. A model with too many triangles makes the simulation loop slow; FIRST's full field CAD has millions, so export a simplified model (around 100k triangles or fewer).
 */
class FieldModel
{

  /**
   * Surfaces closer to the camera than this (in meters) are cut off, like the near plane of any 3D renderer.
   */
  private static final double NEAR_PLANE_METERS = 0.05;

  /**
   * Triangle corners in meters, 9 floats per triangle: x1, y1, z1, x2, y2, z2, x3, y3, z3.
   */
  private final float[] triangles;

  /**
   * Triangle corners relative to the camera, recomputed every frame. Same layout as {@link #triangles}.
   */
  private final float[] cameraSpace;

  /**
   * Reused native point list for {@link Imgproc#fillConvexPoly(Mat, MatOfPoint, Scalar)}.
   */
  private final MatOfPoint polygon = new MatOfPoint();

  /**
   * Load a binary STL file.
   *
   * @param stlPath Path to the STL file, exported in meters.
   * @throws IOException if the file can't be read or isn't a binary STL.
   */
  FieldModel(String stlPath) throws IOException
  {
    ByteBuffer stl = ByteBuffer.wrap(Files.readAllBytes(Path.of(stlPath))).order(ByteOrder.LITTLE_ENDIAN);

    // Binary STL: 80 byte header, triangle count, then 50 bytes per triangle (normal, 3 corners, 2 unused bytes).
    int count = stl.capacity() >= 84 ? stl.getInt(80) : -1;
    if (count < 0 || stl.capacity() != 84 + 50L * count)
    {
      throw new IOException("Not a binary STL file (ASCII STL is not supported): " + stlPath);
    }

    triangles = new float[count * 9];
    cameraSpace = new float[count * 9];
    for (int i = 0; i < count; i++)
    {
      int corners = 84 + i * 50 + 12; // Skip the stored normal, it is recomputed from the corners when drawing.
      for (int j = 0; j < 9; j++)
      {
        triangles[i * 9 + j] = stl.getFloat(corners + j * 4);
      }
    }
  }

  /**
   * Draw the model as seen from the camera.
   *
   * @param frame      Image to draw on.
   * @param cameraPose Camera pose in the model's coordinates.
   * @param fx         Horizontal focal length in pixels.
   * @param fy         Vertical focal length in pixels.
   */
  void draw(Mat frame, Pose3d cameraPose, double fx, double fy)
  {
    double cx = frame.cols() / 2.0;
    double cy = frame.rows() / 2.0;

    // Columns of the rotation matrix are the camera's forward, left and up directions in model coordinates.
    var    r  = cameraPose.getRotation().toMatrix();
    double tx = cameraPose.getX(), ty = cameraPose.getY(), tz = cameraPose.getZ();

    // 1. Move every corner into camera space (X forward, Y left, Z up), and record each triangle's depth so they can be drawn farthest first.
    long[] drawOrder = new long[triangles.length / 9];
    int    drawCount = 0;
    for (int i = 0; i < triangles.length; i += 3)
    {
      double dx = triangles[i] - tx, dy = triangles[i + 1] - ty, dz = triangles[i + 2] - tz;
      cameraSpace[i] = (float) (r.get(0, 0) * dx + r.get(1, 0) * dy + r.get(2, 0) * dz);
      cameraSpace[i + 1] = (float) (r.get(0, 1) * dx + r.get(1, 1) * dy + r.get(2, 1) * dz);
      cameraSpace[i + 2] = (float) (r.get(0, 2) * dx + r.get(1, 2) * dy + r.get(2, 2) * dz);

      if (i % 9 == 6) // Last corner of a triangle.
      {
        int   t     = i - 6;
        float depth = (cameraSpace[t] + cameraSpace[t + 3] + cameraSpace[t + 6]) / 3;
        if (Math.max(cameraSpace[t], Math.max(cameraSpace[t + 3], cameraSpace[t + 6])) >= NEAR_PLANE_METERS)
        {
          // Sort key: depth in the high 32 bits, triangle index in the low 32 bits. Bits of a positive float sort in the same order as the float.
          drawOrder[drawCount++] = ((long) Float.floatToIntBits(Math.max(depth, 0)) << 32) | (t / 9);
        }
      }
    }
    Arrays.sort(drawOrder, 0, drawCount);

    // 2. Draw farthest to nearest.
    List<Point> points = new ArrayList<>(4);
    for (int k = drawCount - 1; k >= 0; k--)
    {
      int t = (int) drawOrder[k] * 9;

      // Cut the triangle at the near plane. The result has 3 or 4 corners.
      points.clear();
      for (int c = 0; c < 3; c++)
      {
        int     a   = t + c * 3, b = t + ((c + 1) % 3) * 3;
        boolean aIn = cameraSpace[a] >= NEAR_PLANE_METERS, bIn = cameraSpace[b] >= NEAR_PLANE_METERS;
        if (aIn)
        {
          points.add(project(cameraSpace[a], cameraSpace[a + 1], cameraSpace[a + 2], cx, cy, fx, fy));
        }
        if (aIn != bIn)
        {
          double s = (NEAR_PLANE_METERS - cameraSpace[a]) / (cameraSpace[b] - cameraSpace[a]);
          points.add(project(NEAR_PLANE_METERS,
                             cameraSpace[a + 1] + s * (cameraSpace[b + 1] - cameraSpace[a + 1]),
                             cameraSpace[a + 2] + s * (cameraSpace[b + 2] - cameraSpace[a + 2]),
                             cx, cy, fx, fy));
        }
      }

      if (isOffScreen(points, frame.cols(), frame.rows()) || pixelArea(points) < 1)
      {
        continue;
      }

      polygon.fromList(points);
      double gray = shade(t);
      Imgproc.fillConvexPoly(frame, polygon, new Scalar(gray, gray, gray));
    }
  }

  /**
   * Pinhole projection of a camera space point to pixels. Image X grows to the right and image Y grows downward.
   */
  private static Point project(double x, double y, double z, double cx, double cy, double fx, double fy)
  {
    return new Point(cx - fx * y / x, cy - fy * z / x);
  }

  /**
   * Whether every point is past the same edge of the image.
   */
  private static boolean isOffScreen(List<Point> points, int width, int height)
  {
    double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
    for (Point p : points)
    {
      minX = Math.min(minX, p.x);
      maxX = Math.max(maxX, p.x);
      minY = Math.min(minY, p.y);
      maxY = Math.max(maxY, p.y);
    }
    return maxX < 0 || minX > width || maxY < 0 || minY > height;
  }

  /**
   * Area of a polygon in pixels (shoelace formula). Used to skip triangles too small to see, which most CAD triangles are at a distance.
   */
  private static double pixelArea(List<Point> points)
  {
    double twiceArea = 0;
    for (int i = 0; i < points.size(); i++)
    {
      Point a = points.get(i), b = points.get((i + 1) % points.size());
      twiceArea += a.x * b.y - b.x * a.y;
    }
    return Math.abs(twiceArea) / 2;
  }

  /**
   * Gray level for a triangle: brighter the more directly it faces the camera.
   *
   * @param t Index of the triangle's first float in {@link #cameraSpace}.
   */
  private double shade(int t)
  {
    float[] c = cameraSpace;
    // Surface normal = (corner2 - corner1) x (corner3 - corner1).
    double ux = c[t + 3] - c[t], uy = c[t + 4] - c[t + 1], uz = c[t + 5] - c[t + 2];
    double vx = c[t + 6] - c[t], vy = c[t + 7] - c[t + 1], vz = c[t + 8] - c[t + 2];
    double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
    // Direction from the camera to the triangle.
    double lx = c[t] + c[t + 3] + c[t + 6], ly = c[t + 1] + c[t + 4] + c[t + 7], lz = c[t + 2] + c[t + 5] + c[t + 8];
    double facing = Math.abs(nx * lx + ny * ly + nz * lz) / (Math.sqrt(nx * nx + ny * ny + nz * nz) * Math.sqrt(lx * lx + ly * ly + lz * lz) + 1e-12);
    return 60 + 160 * facing;
  }
}
