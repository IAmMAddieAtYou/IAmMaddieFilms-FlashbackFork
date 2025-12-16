package com.moulberry.flashback.spline;

import net.minecraft.util.Mth;
import org.joml.Vector3d;

/**
 * Smoothing Spline (Gaussian Kernel Smoother).
 * Instead of forcing the camera to hit every single keyframe perfectly (which can look jerky),
 * this calculates a weighted average of the 5 local points to create a "best fit" smooth curve.
 *
 * It uses a Gaussian kernel based on index distance, ensuring consistent smoothing
 * regardless of the time duration between keyframes.
 */
public class Smoothing {

    // Sigma controls the width of the Gaussian kernel.
    // 0.5 means the influence drops off quickly (mostly neighbors).
    // Higher values would make the curve smoother but further from the keyframes.
    private static final double SIGMA = 0.5;

    public static Vector3d position(Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3, Vector3d p4,
                                    float t0, float t1, float t2, float t3, float t4, float amount) {
        double x = interpolate(p0.x, p1.x, p2.x, p3.x, p4.x, amount);
        double y = interpolate(p0.y, p1.y, p2.y, p3.y, p4.y, amount);
        double z = interpolate(p0.z, p1.z, p2.z, p3.z, p4.z, amount);
        return new Vector3d(x, y, z);
    }

    public static double value(double p0, double p1, double p2, double p3, double p4,
                               float t0, float t1, float t2, float t3, float t4, float amount) {
        return interpolate(p0, p1, p2, p3, p4, amount);
    }

    public static double degrees(double p0, double p1, double p2, double p3, double p4,
                                 float t0, float t1, float t2, float t3, float t4, float amount) {
        // Unwind angles so they are continuous relative to the center of our operation (p2)
        double start = p2;
        double v0 = start + Mth.wrapDegrees(p0 - p1) + Mth.wrapDegrees(p1 - p2);
        double v1 = start + Mth.wrapDegrees(p1 - p2);
        double v2 = start;
        double v3 = start + Mth.wrapDegrees(p3 - p2);
        double v4 = v3 + Mth.wrapDegrees(p4 - p3);

        return Mth.wrapDegrees(interpolate(v0, v1, v2, v3, v4, amount));
    }

    private static double interpolate(double y0, double y1, double y2, double y3, double y4, float amount) {
        // We use Index-Based distance for the kernel weighting.
        // This ensures the smoothing "feel" is consistent even if keyframes have varying time gaps.
        // Indices: p0=0, p1=1, p2=2, p3=3, p4=4.
        // We are interpolating between p2 and p3, so target location is 2.0 + amount.

        double targetIndex = 2.0 + amount;

        double w0 = gaussian(0.0 - targetIndex);
        double w1 = gaussian(1.0 - targetIndex);
        double w2 = gaussian(2.0 - targetIndex);
        double w3 = gaussian(3.0 - targetIndex);
        double w4 = gaussian(4.0 - targetIndex);

        double totalWeight = w0 + w1 + w2 + w3 + w4;

        return (y0 * w0 + y1 * w1 + y2 * w2 + y3 * w3 + y4 * w4) / totalWeight;
    }

    private static double gaussian(double dist) {
        return Math.exp(-(dist * dist) / (2 * SIGMA * SIGMA));
    }
}