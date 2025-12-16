package com.moulberry.flashback.keyframe.impl;

import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.change.KeyframeChangeCameraPositionOrbit;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;
import com.moulberry.flashback.keyframe.types.CameraOrbitKeyframeType;
import com.moulberry.flashback.spline.CatmullRom;
import com.moulberry.flashback.spline.Hermite;
import com.moulberry.flashback.spline.Akima;
import com.moulberry.flashback.spline.Circular;
import com.moulberry.flashback.spline.Smoothing;
import com.moulberry.flashback.spline.MonotoneCubic;
import com.moulberry.flashback.spline.Nurbs;
import com.moulberry.flashback.spline.Quintic;
import org.joml.Vector3d;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Consumer;

public class CameraOrbitKeyframe extends Keyframe {

    public Vector3d center;
    public float distance;
    public float yaw;
    public float pitch;

    public CameraOrbitKeyframe(Vector3d center, float distance, float yaw, float pitch) {
        this(center, distance, yaw, pitch, InterpolationType.getDefault());
    }

    public CameraOrbitKeyframe(Vector3d center, float distance, float yaw, float pitch, InterpolationType interpolationType) {
        this.center = center;
        this.distance = distance;
        this.yaw = yaw;
        this.pitch = pitch;
        this.interpolationType(interpolationType);
    }

    @Override
    public KeyframeType<?> keyframeType() {
        return CameraOrbitKeyframeType.INSTANCE;
    }

    @Override
    public Keyframe copy() {
        return new CameraOrbitKeyframe(new Vector3d(this.center), this.distance, this.yaw, this.pitch, this.interpolationType());
    }

    @Override
    public void renderEditKeyframe(Consumer<Consumer<Keyframe>> update) {
        float[] center = new float[]{(float) this.center.x, (float) this.center.y, (float) this.center.z};
        if (ImGuiHelper.inputFloat("Position", center)) {
            if (center[0] != this.center.x) {
                update.accept(keyframe -> ((CameraOrbitKeyframe)keyframe).center.x = center[0]);
            }
            if (center[1] != this.center.y) {
                update.accept(keyframe -> ((CameraOrbitKeyframe)keyframe).center.y = center[1]);
            }
            if (center[2] != this.center.z) {
                update.accept(keyframe -> ((CameraOrbitKeyframe)keyframe).center.z = center[2]);
            }
        }
        float[] input = new float[]{this.distance};
        if (ImGuiHelper.inputFloat("Distance", input)) {
            if (input[0] != this.distance) {
                update.accept(keyframe -> ((CameraOrbitKeyframe)keyframe).distance = input[0]);
            }
        }
        input[0] = this.yaw;
        if (ImGuiHelper.inputFloat("Yaw", input)) {
            if (input[0] != this.yaw) {
                update.accept(keyframe -> ((CameraOrbitKeyframe)keyframe).yaw = input[0]);
            }
        }
        input[0] = this.pitch;
        if (ImGuiHelper.inputFloat("Pitch", input)) {
            if (input[0] != this.pitch) {
                update.accept(keyframe -> ((CameraOrbitKeyframe)keyframe).pitch = input[0]);
            }
        }
    }

    private static KeyframeChangeCameraPositionOrbit createChangeFrom(Vector3d center, float distance, float yaw, float pitch) {
        return new KeyframeChangeCameraPositionOrbit(center, distance, yaw, pitch);
    }

    @Override
    public KeyframeChange createChange() {
        return createChangeFrom(this.center, this.distance, this.yaw, this.pitch);
    }


    // --- 5-POINT INTERPOLATION (Akima, Smoothing) ---
    // Context: pBefore -> this -> p1 -> p2 -> p3

    @Override
    public KeyframeChange createAkimaInterpolatedChange(Keyframe pBefore, Keyframe p1, Keyframe p2, Keyframe p3, float tBefore, float t0, float t1, float t2, float t3, float amount) {
        CameraOrbitKeyframe kBefore = (CameraOrbitKeyframe) pBefore;
        CameraOrbitKeyframe k1 = (CameraOrbitKeyframe) p1;
        CameraOrbitKeyframe k2 = (CameraOrbitKeyframe) p2;
        CameraOrbitKeyframe k3 = (CameraOrbitKeyframe) p3;

        // Position (Center)
        Vector3d center = Akima.position(kBefore.center, this.center, k1.center, k2.center, k3.center, tBefore, t0, t1, t2, t3, amount);

        // Values - Using .value() to allow multiple rotations
        float dist = (float) Akima.value(kBefore.distance, this.distance, k1.distance, k2.distance, k3.distance, tBefore, t0, t1, t2, t3, amount);
        float yaw = (float) Akima.value(kBefore.yaw, this.yaw, k1.yaw, k2.yaw, k3.yaw, tBefore, t0, t1, t2, t3, amount);
        float pitch = (float) Akima.value(kBefore.pitch, this.pitch, k1.pitch, k2.pitch, k3.pitch, tBefore, t0, t1, t2, t3, amount);

        return createChangeFrom(center, dist, yaw, pitch);
    }

    @Override
    public KeyframeChange createSmoothingInterpolatedChange(Keyframe pBefore, Keyframe p1, Keyframe p2, Keyframe p3, float tBefore, float t0, float t1, float t2, float t3, float amount) {
        CameraOrbitKeyframe kBefore = (CameraOrbitKeyframe) pBefore;
        CameraOrbitKeyframe k1 = (CameraOrbitKeyframe) p1;
        CameraOrbitKeyframe k2 = (CameraOrbitKeyframe) p2;
        CameraOrbitKeyframe k3 = (CameraOrbitKeyframe) p3;

        Vector3d center = Smoothing.position(kBefore.center, this.center, k1.center, k2.center, k3.center, tBefore, t0, t1, t2, t3, amount);

        float dist = (float) Smoothing.value(kBefore.distance, this.distance, k1.distance, k2.distance, k3.distance, tBefore, t0, t1, t2, t3, amount);
        float yaw = (float) Smoothing.value(kBefore.yaw, this.yaw, k1.yaw, k2.yaw, k3.yaw, tBefore, t0, t1, t2, t3, amount);
        float pitch = (float) Smoothing.value(kBefore.pitch, this.pitch, k1.pitch, k2.pitch, k3.pitch, tBefore, t0, t1, t2, t3, amount);

        return createChangeFrom(center, dist, yaw, pitch);
    }

    // --- 4-POINT INTERPOLATION (Standard) ---
    // Context: this -> p1 -> p2 -> p3

    @Override
    public KeyframeChange createCircularInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        CameraOrbitKeyframe k1 = (CameraOrbitKeyframe) p1;
        CameraOrbitKeyframe k2 = (CameraOrbitKeyframe) p2;

        // Circular Pairwise
        Vector3d center = Circular.position(k1.center, k2.center, amount);

        float dist = (float) Circular.value(k1.distance, k2.distance, amount);
        float yaw = (float) Circular.value(k1.yaw, k2.yaw, amount);
        float pitch = (float) Circular.value(k1.pitch, k2.pitch, amount);

        return createChangeFrom(center, dist, yaw, pitch);
    }

    @Override
    public KeyframeChange createMonotoneCubicInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        CameraOrbitKeyframe k1 = (CameraOrbitKeyframe) p1;
        CameraOrbitKeyframe k2 = (CameraOrbitKeyframe) p2;
        CameraOrbitKeyframe k3 = (CameraOrbitKeyframe) p3;

        Vector3d center = MonotoneCubic.position(this.center, k1.center, k2.center, k3.center, t0, t1, t2, t3, amount);

        float dist = (float) MonotoneCubic.value(this.distance, k1.distance, k2.distance, k3.distance, t0, t1, t2, t3, amount);
        float yaw = (float) MonotoneCubic.value(this.yaw, k1.yaw, k2.yaw, k3.yaw, t0, t1, t2, t3, amount);
        float pitch = (float) MonotoneCubic.value(this.pitch, k1.pitch, k2.pitch, k3.pitch, t0, t1, t2, t3, amount);

        return createChangeFrom(center, dist, yaw, pitch);
    }

    @Override
    public KeyframeChange createNurbsInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        CameraOrbitKeyframe k1 = (CameraOrbitKeyframe) p1;
        CameraOrbitKeyframe k2 = (CameraOrbitKeyframe) p2;
        CameraOrbitKeyframe k3 = (CameraOrbitKeyframe) p3;

        Vector3d center = Nurbs.position(this.center, k1.center, k2.center, k3.center, t0, t1, t2, t3, amount);

        float dist = (float) Nurbs.value(this.distance, k1.distance, k2.distance, k3.distance, t0, t1, t2, t3, amount);
        float yaw = (float) Nurbs.value(this.yaw, k1.yaw, k2.yaw, k3.yaw, t0, t1, t2, t3, amount);
        float pitch = (float) Nurbs.value(this.pitch, k1.pitch, k2.pitch, k3.pitch, t0, t1, t2, t3, amount);

        return createChangeFrom(center, dist, yaw, pitch);
    }

    @Override
    public KeyframeChange createQuinticInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        CameraOrbitKeyframe k1 = (CameraOrbitKeyframe) p1;
        CameraOrbitKeyframe k2 = (CameraOrbitKeyframe) p2;

        // Quintic Pairwise
        Vector3d center = Quintic.position(k1.center, k2.center, amount);

        float dist = (float) Quintic.value(k1.distance, k2.distance, amount);
        float yaw = (float) Quintic.value(k1.yaw, k2.yaw, amount);
        float pitch = (float) Quintic.value(k1.pitch, k2.pitch, amount);

        return createChangeFrom(center, dist, yaw, pitch);
    }
    

    @Override
    public KeyframeChange createSmoothInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        float time1 = t1 - t0;
        float time2 = t2 - t0;
        float time3 = t3 - t0;

        Vector3d position = CatmullRom.position(this.center,
                ((CameraOrbitKeyframe)p1).center, ((CameraOrbitKeyframe)p2).center,
                ((CameraOrbitKeyframe)p3).center, time1, time2, time3, amount);

        float distance = CatmullRom.value(this.distance, ((CameraOrbitKeyframe)p1).distance, ((CameraOrbitKeyframe)p2).distance,
                ((CameraOrbitKeyframe)p3).distance, time1, time2, time3, amount);

        // Note: we don't use CatmullRom#degrees because we want to allow multiple rotations in a single orbit
        float yaw = CatmullRom.value(this.yaw, ((CameraOrbitKeyframe)p1).yaw, ((CameraOrbitKeyframe)p2).yaw,
                ((CameraOrbitKeyframe)p3).yaw, time1, time2, time3, amount);
        float pitch = CatmullRom.value(this.pitch, ((CameraOrbitKeyframe)p1).pitch, ((CameraOrbitKeyframe)p2).pitch,
                ((CameraOrbitKeyframe)p3).pitch, time1, time2, time3, amount);

        return createChangeFrom(position, distance, yaw, pitch);
    }

    @Override
    public KeyframeChange createHermiteInterpolatedChange(Map<Float, Keyframe> keyframes, float amount) {
        Vector3d position = Hermite.position(Maps.transformValues(keyframes, k -> ((CameraOrbitKeyframe)k).center), amount);
        double distance = Hermite.value(Maps.transformValues(keyframes, k -> (double) ((CameraOrbitKeyframe)k).distance), amount);

        // Note: we don't use Hermite#degrees because we want to allow multiple rotations in a single orbit
        double yaw = Hermite.value(Maps.transformValues(keyframes, k -> (double) ((CameraOrbitKeyframe)k).yaw), amount);
        double pitch = Hermite.value(Maps.transformValues(keyframes, k -> (double) ((CameraOrbitKeyframe)k).pitch), amount);

        return createChangeFrom(position, (float) distance, (float) yaw, (float) pitch);
    }

    public static class TypeAdapter implements JsonSerializer<CameraOrbitKeyframe>, JsonDeserializer<CameraOrbitKeyframe> {
        @Override
        public CameraOrbitKeyframe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            Vector3d center = context.deserialize(jsonObject.get("center"), Vector3d.class);
            float distance = jsonObject.get("distance").getAsFloat();
            float yaw = jsonObject.get("yaw").getAsFloat();
            float pitch = jsonObject.get("pitch").getAsFloat();
            InterpolationType interpolationType = context.deserialize(jsonObject.get("interpolation_type"), InterpolationType.class);
            return new CameraOrbitKeyframe(center, distance, yaw, pitch, interpolationType);
        }

        @Override
        public JsonElement serialize(CameraOrbitKeyframe src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("center", context.serialize(src.center));
            jsonObject.addProperty("distance", src.distance);
            jsonObject.addProperty("yaw", src.yaw);
            jsonObject.addProperty("pitch", src.pitch);
            jsonObject.addProperty("type", "camera_orbit");
            jsonObject.add("interpolation_type", context.serialize(src.interpolationType()));
            return jsonObject;
        }
    }

}
