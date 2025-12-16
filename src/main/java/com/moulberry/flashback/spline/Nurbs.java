package com.moulberry.flashback.spline;

import net.minecraft.util.Mth;
import org.joml.Vector3d;

/**
 * Approximated NURBS using Uniform Cubic B-Splines.
 * This implementation uses the 4 control points context directly.
 * The curve will be very smooth but will NOT pass directly through the keyframes
 * (control points), acting more like a "magnet" pulling the path.
 */
public class Nurbs {

    public static Vector3d position(Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3,
                                    float t0, float t1, float t2, float t3, float amount) {
        double x = interpolate(p0.x, p1.x, p2.x, p3.x, amount);
        double y = interpolate(p0.y, p1.y, p2.y, p3.y, amount);
        double z = interpolate(p0.z, p1.z, p2.z, p3.z, amount);
        return new Vector3d(x, y, z);
    }

    public static double value(double p0, double p1, double p2, double p3,
                               float t0, float t1, float t2, float t3, float amount) {
        return interpolate(p0, p1, p2, p3, amount);
    }

    public static double degrees(double p0, double p1, double p2, double p3,
                                 float t0, float t1, float t2, float t3, float amount) {
        // Unwind angles relative to p1 to ensure shortest path rotation
        double start = p1;
        double v0 = start + Mth.wrapDegrees(p0 - p1);
        double v1 = start;
        double v2 = start + Mth.wrapDegrees(p2 - p1);
        double v3 = v2 + Mth.wrapDegrees(p3 - p2);

        return Mth.wrapDegrees(interpolate(v0, v1, v2, v3, amount));
    }

    private static double interpolate(double p0, double p1, double p2, double p3, float t) {
        // Uniform Cubic B-Spline Basis Functions
        // t is the normalized time [0, 1] for the segment between the characteristic points of p1 and p2
        float t2 = t * t;
        float t3 = t2 * t;

        // Standard B-Spline Matrix Coefficients
        // These coefficients weight the 4 control points to generate the curve point at t
        double b0 = (1 - t3 + 3 * t2 - 3 * t) / 6.0;
        double b1 = (4 - 6 * t2 + 3 * t3) / 6.0;
        double b2 = (1 + 3 * t + 3 * t2 - 3 * t3) / 6.0;
        double b3 = t3 / 6.0;

        return p0 * b0 + p1 * b1 + p2 * b2 + p3 * b3;
    }
}