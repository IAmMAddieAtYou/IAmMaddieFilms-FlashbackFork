package com.moulberry.flashback.spline;

import net.minecraft.util.Mth;
import org.joml.Vector3d;

public class Akima {

    public static Vector3d position(Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3, Vector3d p4,
                                    float t0, float t1, float t2, float t3, float t4, float amount) {
        double x = interpolate(p0.x, p1.x, p2.x, p3.x, p4.x, t0, t1, t2, t3, t4, amount);
        double y = interpolate(p0.y, p1.y, p2.y, p3.y, p4.y, t0, t1, t2, t3, t4, amount);
        double z = interpolate(p0.z, p1.z, p2.z, p3.z, p4.z, t0, t1, t2, t3, t4, amount);

        // Final safety check for NaN (fallback to p2/current keyframe if math blew up)
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            return new Vector3d(p2);
        }
        return new Vector3d(x, y, z);
    }

    public static double value(double p0, double p1, double p2, double p3, double p4,
                               float t0, float t1, float t2, float t3, float t4, float amount) {
        double val = interpolate(p0, p1, p2, p3, p4, t0, t1, t2, t3, t4, amount);
        return Double.isNaN(val) ? p2 : val;
    }

    public static double degrees(double p0, double p1, double p2, double p3, double p4,
                                 float t0, float t1, float t2, float t3, float t4, float amount) {
        // Unwind angles relative to p2
        double start = p2;
        double v0 = start + Mth.wrapDegrees(p0 - p1) + Mth.wrapDegrees(p1 - p2);
        double v1 = start + Mth.wrapDegrees(p1 - p2);
        double v2 = start;
        double v3 = start + Mth.wrapDegrees(p3 - p2);
        double v4 = v3 + Mth.wrapDegrees(p4 - p3);

        double val = interpolate(v0, v1, v2, v3, v4, t0, t1, t2, t3, t4, amount);
        return Mth.wrapDegrees(Double.isNaN(val) ? p2 : val);
    }

    private static double interpolate(double y0, double y1, double y2, double y3, double y4,
                                      float t0, float t1, float t2, float t3, float t4, float amount) {

        // 1. Calculate Slopes (Finite Difference)
        // m0: slope 0->1
        // m1: slope 1->2
        // m2: slope 2->3 (The active interval)
        // m3: slope 3->4

        // Determine the primary slope first (m2). This is the active interval, so t3 > t2 generally.
        // If t3 == t2, we have a 0-length interval, just return the value.
        if (Math.abs(t3 - t2) < 1e-9) return y2;

        double m2 = (y3 - y2) / (t3 - t2);

        // Calculate neighbors, handling zero-duration duplicates by extending the slope
        double m1 = safeSlope(y1, y2, t1, t2, m2);
        double m0 = safeSlope(y0, y1, t0, t1, m1);
        double m3 = safeSlope(y3, y4, t3, t4, m2);

        // 2. Calculate Derivative at P2 (start of interval)
        // Weights based on slope differences
        double w2_a = Math.abs(m3 - m2);
        double w2_b = Math.abs(m1 - m0);

        double tP2;
        if (w2_a + w2_b < 1e-9) {
            tP2 = (m1 + m2) / 2.0;
        } else {
            tP2 = (w2_a * m1 + w2_b * m2) / (w2_a + w2_b);
        }

        // 3. Calculate Derivative at P3 (end of interval)
        // We need m4 (slope 4->5). Extrapolate m4 assuming linear change: m4 - m3 = m3 - m2 => m4 = 2*m3 - m2
        double m4 = 2.0 * m3 - m2;

        double w3_a = Math.abs(m4 - m3);
        double w3_b = Math.abs(m2 - m1);

        double tP3;
        if (w3_a + w3_b < 1e-9) {
            tP3 = (m2 + m3) / 2.0;
        } else {
            tP3 = (w3_a * m2 + w3_b * m3) / (w3_a + w3_b);
        }

        // 4. Cubic Hermite Interpolation
        double h = t3 - t2;
        double t = amount; // Normalized 0..1

        double tSq = t * t;
        double tCu = tSq * t;

        // Basis functions
        double h00 = 2 * tCu - 3 * tSq + 1;
        double h10 = tCu - 2 * tSq + t;
        double h01 = -2 * tCu + 3 * tSq;
        double h11 = tCu - tSq;

        // tP2 and tP3 are slopes (dy/dt). Multiply by h (dt) to get dy for normalized basis
        return h00 * y2 + h10 * h * tP2 + h01 * y3 + h11 * h * tP3;
    }

    private static double safeSlope(double yA, double yB, float tA, float tB, double fallbackSlope) {
        if (Math.abs(tB - tA) < 1e-9) {
            return fallbackSlope;
        }
        return (yB - yA) / (tB - tA);
    }
}