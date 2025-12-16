package com.moulberry.flashback.spline;

import net.minecraft.util.Mth;
import org.joml.Vector3d;

/**
 * Quintic Interpolation (Smootherstep).
 * * Unlike standard Hermite (Smoothstep) which is a 3rd-degree polynomial,
 * Quintic is a 5th-degree polynomial.
 * * It ensures that both Velocity (1st derivative) AND Acceleration (2nd derivative)
 * are zero at the endpoints. This creates extremely smooth, "glassy" transitions
 * where the camera eases in and out of movement with almost imperceptible jerks.
 */
public class Quintic {

    public static Vector3d position(Vector3d p1, Vector3d p2, float amount) {
        // Pairwise easing (Smootherstep)
        float t = smootherStep(amount);
        return new Vector3d(p1).lerp(p2, t);
    }

    public static double value(double p1, double p2, float amount) {
        return Mth.lerp(smootherStep(amount), p1, p2);
    }

    public static double degrees(double p1, double p2, float amount) {
        return Mth.rotLerp(smootherStep(amount), (float)p1, (float)p2);
    }

    private static float smootherStep(float t) {
        // 6t^5 - 15t^4 + 10t^3
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
}