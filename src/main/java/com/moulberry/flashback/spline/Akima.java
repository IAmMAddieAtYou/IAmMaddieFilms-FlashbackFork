package com.moulberry.flashback.spline;

import net.minecraft.util.Mth;
import org.joml.Vector3d;

public class Akima {

    public static Vector3d position(Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3, Vector3d p4,
                                    float t0, float t1, float t2, float t3, float t4, float amount) {
        // Interpolate X, Y, Z independently
        double x = interpolate(p0.x, p1.x, p2.x, p3.x, p4.x, t0, t1, t2, t3, t4, amount);
        double y = interpolate(p0.y, p1.y, p2.y, p3.y, p4.y, t0, t1, t2, t3, t4, amount);
        double z = interpolate(p0.z, p1.z, p2.z, p3.z, p4.z, t0, t1, t2, t3, t4, amount);
        return new Vector3d(x, y, z);
    }

    public static double value(double p0, double p1, double p2, double p3, double p4,
                               float t0, float t1, float t2, float t3, float t4, float amount) {
        return interpolate(p0, p1, p2, p3, p4, t0, t1, t2, t3, t4, amount);
    }

    public static double degrees(double p0, double p1, double p2, double p3, double p4,
                                 float t0, float t1, float t2, float t3, float t4, float amount) {
        // Unwind angles relative to p2 (start of interpolation interval)
        double start = p2;
        double v0 = start + Mth.wrapDegrees(p0 - p1) + Mth.wrapDegrees(p1 - p2);
        double v1 = start + Mth.wrapDegrees(p1 - p2);
        double v2 = start;
        double v3 = start + Mth.wrapDegrees(p3 - p2);
        double v4 = v3 + Mth.wrapDegrees(p4 - p3);

        return Mth.wrapDegrees(interpolate(v0, v1, v2, v3, v4, t0, t1, t2, t3, t4, amount));
    }

    private static double interpolate(double y0, double y1, double y2, double y3, double y4,
                                      float t0, float t1, float t2, float t3, float t4, float amount) {
        // Secant slopes (m)
        // We handle potential divide by zero by checking time deltas, though KeyframeTrack ensures ordering.
        double m0 = (y1 - y0) / (t1 - t0);
        double m1 = (y2 - y1) / (t2 - t1);
        double m2 = (y3 - y2) / (t3 - t2);
        double m3 = (y4 - y3) / (t4 - t3);

        // Extrapolate boundary slopes for full Akima context
        // mMinus1 (before m0) and m4 (after m3)
        double mMinus1 = 2 * m0 - m1;
        double m4 = 2 * m3 - m2;

        // Derivative at P2 (using m0, m1, m2, m3)
        // Weight w_i = |m_{i+1} - m_i|
        double w1 = Math.abs(m3 - m2);
        double w2 = Math.abs(m1 - m0);
        double tP2;
        if (Math.abs(w1 + w2) < 1e-9) {
            tP2 = (m1 + m2) / 2.0;
        } else {
            tP2 = (w1 * m1 + w2 * m2) / (w1 + w2);
        }

        // Derivative at P3 (using m1, m2, m3, m4)
        double w3 = Math.abs(m4 - m3);
        double w4 = Math.abs(m2 - m1);
        double tP3;
        if (Math.abs(w3 + w4) < 1e-9) {
            tP3 = (m2 + m3) / 2.0;
        } else {
            tP3 = (w3 * m2 + w4 * m3) / (w3 + w4);
        }

        // Cubic Hermite Interpolation between P2 and P3
        // Normalized t from 0 to 1 is 'amount'
        // Scale derivatives by segment duration (h)
        double h = t3 - t2;
        double h00 = 2 * amount * amount * amount - 3 * amount * amount + 1;
        double h10 = amount * amount * amount - 2 * amount * amount + amount;
        double h01 = -2 * amount * amount * amount + 3 * amount * amount;
        double h11 = amount * amount * amount - amount * amount;

        return h00 * y2 + h10 * h * tP2 + h01 * y3 + h11 * h * tP3;
    }
}