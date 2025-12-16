package com.moulberry.flashback.spline;

import net.minecraft.util.Mth;
import org.joml.Vector3d;

/**
 * Monotone Cubic Interpolation (Fritsch-Carlson method).
 * Preserves monotonicity (direction) of the data points, preventing overshooting
 * or oscillation artifacts common in standard Cubic Splines.
 */
public class MonotoneCubic {

    public static Vector3d position(Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3,
                                    float t0, float t1, float t2, float t3, float amount) {
        double x = interpolate(p0.x, p1.x, p2.x, p3.x, t0, t1, t2, t3, amount);
        double y = interpolate(p0.y, p1.y, p2.y, p3.y, t0, t1, t2, t3, amount);
        double z = interpolate(p0.z, p1.z, p2.z, p3.z, t0, t1, t2, t3, amount);
        return new Vector3d(x, y, z);
    }

    public static double value(double p0, double p1, double p2, double p3,
                               float t0, float t1, float t2, float t3, float amount) {
        return interpolate(p0, p1, p2, p3, t0, t1, t2, t3, amount);
    }

    public static double degrees(double p0, double p1, double p2, double p3,
                                 float t0, float t1, float t2, float t3, float amount) {
        // Unwind angles relative to p1 (start of current interval)
        double start = p1;
        double v0 = start + Mth.wrapDegrees(p0 - p1);
        double v1 = start;
        double v2 = start + Mth.wrapDegrees(p2 - p1);
        double v3 = v2 + Mth.wrapDegrees(p3 - p2);

        return Mth.wrapDegrees(interpolate(v0, v1, v2, v3, t0, t1, t2, t3, amount));
    }

    private static double interpolate(double y0, double y1, double y2, double y3,
                                      float t0, float t1, float t2, float t3, float amount) {
        // Safety checks for zero-length intervals to prevent NaN
        if (Math.abs(t1 - t0) < 1e-5 || Math.abs(t2 - t1) < 1e-5 || Math.abs(t3 - t2) < 1e-5) {
            return y1;
        }

        // 1. Calculate Secant Slopes (m)
        double m0 = (y1 - y0) / (t1 - t0);
        double m1 = (y2 - y1) / (t2 - t1);
        double m2 = (y3 - y2) / (t3 - t2);

        // 2. Initialize Tangents at p1 and p2 (standard arithmetic mean)
        double t1_val = (m0 + m1) / 2.0;
        double t2_val = (m1 + m2) / 2.0;

        // 3. Fritsch-Carlson Monotonicity Logic
        // If the secant m1 is zero (flat interval), tangents must be 0 to avoid oscillation
        if (Math.abs(y2 - y1) < 1e-9) {
            t1_val = 0;
            t2_val = 0;
        } else {
            // If m0 and m1 have opposite signs, p1 is a local extremum -> force tangent to 0
            if (m0 * m1 <= 0) t1_val = 0;
            // If m1 and m2 have opposite signs, p2 is a local extremum -> force tangent to 0
            if (m1 * m2 <= 0) t2_val = 0;

            // Constrain magnitude to prevent overshoot
            double alpha = t1_val / m1;
            double beta = t2_val / m1;

            // Constraint circle: alpha^2 + beta^2 <= 9
            if (alpha * alpha + beta * beta > 9) {
                double tau = 3.0 / Math.sqrt(alpha * alpha + beta * beta);
                t1_val = tau * alpha * m1;
                t2_val = tau * beta * m1;
            }
        }

        // 4. Cubic Hermite Interpolation
        // We use the calculated tangents t1_val and t2_val
        double h = t2 - t1; // Interval length
        double t = amount;  // Normalized time [0, 1]

        double tSq = t * t;
        double tCu = tSq * t;

        // Basis functions
        double h00 = 2 * tCu - 3 * tSq + 1;
        double h10 = tCu - 2 * tSq + t;
        double h01 = -2 * tCu + 3 * tSq;
        double h11 = tCu - tSq;

        // Multiply tangents by h to scale derivative from dy/dt to dy/d(normalized_t)
        return h00 * y1 + h10 * h * t1_val + h01 * y2 + h11 * h * t2_val;
    }
}