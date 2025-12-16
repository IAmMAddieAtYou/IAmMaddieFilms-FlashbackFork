package com.moulberry.flashback.spline;

import net.minecraft.util.Mth;
import org.joml.Vector3d;

/**
 * Performs interpolation optimized for circular or spherical paths.
 * For positions, it uses Spherical Linear Interpolation (SLERP) relative to the origin (0,0,0).
 * For values/degrees, it performs linear interpolation but wrapped appropriately.
 */
public class Circular {

    public static Vector3d position(Vector3d p1, Vector3d p2, float amount) {
        // Spherical Linear Interpolation between p1 and p2
        return slerp(p1, p2, amount);
    }

    public static double value(double p1, double p2, float amount) {
        // For circular value interpolation, simple linear is usually preferred
        return Mth.lerp(amount, p1, p2);
    }

    public static double degrees(double p1, double p2, float amount) {
        // This calculates the shortest path around the circle (standard interpolation for rotation)
        return Mth.rotLerp(amount, (float)p1, (float)p2);
    }

    private static Vector3d slerp(Vector3d v1, Vector3d v2, float t) {
        double dot = v1.dot(v2);
        double lenSq = v1.lengthSquared() * v2.lengthSquared();
        double denom = Math.sqrt(lenSq);

        if (denom < 1e-9) return new Vector3d(v1).lerp(v2, t);

        double cosTheta = dot / denom;

        // Clamp to avoid NaN acos
        if (cosTheta > 1.0) cosTheta = 1.0;
        else if (cosTheta < -1.0) cosTheta = -1.0;

        double theta = Math.acos(cosTheta);

        if (Math.abs(theta) < 1e-6) {
            return new Vector3d(v1).lerp(v2, t);
        }

        double st = Math.sin(theta);
        double c1 = Math.sin((1 - t) * theta) / st;
        double c2 = Math.sin(t * theta) / st;

        Vector3d result = new Vector3d(v1).mul(c1).add(new Vector3d(v2).mul(c2));
        return result;
    }
}