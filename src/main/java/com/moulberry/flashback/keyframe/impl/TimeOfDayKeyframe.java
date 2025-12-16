package com.moulberry.flashback.keyframe.impl;

import com.google.common.collect.Maps;
import com.google.gson.*;
import com.moulberry.flashback.Interpolation;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.change.KeyframeChangeTickrate;
import com.moulberry.flashback.keyframe.change.KeyframeChangeTimeOfDay;
import com.moulberry.flashback.keyframe.handler.KeyframeHandler;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;
import com.moulberry.flashback.keyframe.types.TimeOfDayKeyframeType;
import com.moulberry.flashback.spline.*;
import imgui.ImGui;
import imgui.type.ImInt;
import org.joml.Vector3d;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Consumer;

public class TimeOfDayKeyframe extends Keyframe {

    public int time;

    public TimeOfDayKeyframe(int time) {
        this(time, InterpolationType.getDefault());
    }

    public TimeOfDayKeyframe(int time, InterpolationType interpolationType) {
        this.time = time;
        this.interpolationType(interpolationType);
    }

    @Override
    public KeyframeType<?> keyframeType() {
        return TimeOfDayKeyframeType.INSTANCE;
    }

    @Override
    public Keyframe copy() {
        return new TimeOfDayKeyframe(this.time, this.interpolationType());
    }

    @Override
    public void renderEditKeyframe(Consumer<Consumer<Keyframe>> update) {
        ImGui.setNextItemWidth(160);
        int[] input = new int[]{this.time};
        if (ImGuiHelper.inputInt("Time", input)) {
            if (this.time != input[0]) {
                update.accept(keyframe -> ((TimeOfDayKeyframe)keyframe).time = input[0]);
            }
        }
    }

    @Override
    public KeyframeChange createChange() {
        return new KeyframeChangeTimeOfDay(this.time);
    }

    @Override
    public KeyframeChange createAkimaInterpolatedChange(Keyframe pBefore, Keyframe p1, Keyframe p2, Keyframe p3, float tBefore, float t0, float t1, float t2, float t3, float amount) {
        TimeOfDayKeyframe kBefore = (TimeOfDayKeyframe) pBefore;
        TimeOfDayKeyframe k1 = (TimeOfDayKeyframe) p1;
        TimeOfDayKeyframe k2 = (TimeOfDayKeyframe) p2;
        TimeOfDayKeyframe k3 = (TimeOfDayKeyframe) p3;

        int timeOfDay = (int) Akima.value(kBefore.time, this.time, k1.time, k2.time, k3.time, tBefore, t0, t1, t2, t3, amount);
        return new KeyframeChangeTimeOfDay(timeOfDay);
    }

    @Override
    public KeyframeChange createSmoothingInterpolatedChange(Keyframe pBefore, Keyframe p1, Keyframe p2, Keyframe p3, float tBefore, float t0, float t1, float t2, float t3, float amount) {
        TimeOfDayKeyframe kBefore = (TimeOfDayKeyframe) pBefore;
        TimeOfDayKeyframe k1 = (TimeOfDayKeyframe) p1;
        TimeOfDayKeyframe k2 = (TimeOfDayKeyframe) p2;
        TimeOfDayKeyframe k3 = (TimeOfDayKeyframe) p3;

        int timeOfDay = (int) Smoothing.value(kBefore.time, this.time, k1.time, k2.time, k3.time, tBefore, t0, t1, t2, t3, amount);
        return new KeyframeChangeTimeOfDay(timeOfDay);
    }

    // --- 4-POINT INTERPOLATION (Standard) ---
    // Context: this -> p1 -> p2 -> p3

    @Override
    public KeyframeChange createCircularInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        TimeOfDayKeyframe k1 = (TimeOfDayKeyframe) p1;
        TimeOfDayKeyframe k2 = (TimeOfDayKeyframe) p2;

        int timeOfDay = (int) Circular.value(k1.time, k2.time, amount);
        return new KeyframeChangeTimeOfDay(timeOfDay);
    }

    @Override
    public KeyframeChange createMonotoneCubicInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        TimeOfDayKeyframe k1 = (TimeOfDayKeyframe) p1;
        TimeOfDayKeyframe k2 = (TimeOfDayKeyframe) p2;
        TimeOfDayKeyframe k3 = (TimeOfDayKeyframe) p3;

        int timeOfDay = (int) MonotoneCubic.value(this.time, k1.time, k2.time, k3.time, t0, t1, t2, t3, amount);
        return new KeyframeChangeTimeOfDay(timeOfDay);
    }

    @Override
    public KeyframeChange createNurbsInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        TimeOfDayKeyframe k1 = (TimeOfDayKeyframe) p1;
        TimeOfDayKeyframe k2 = (TimeOfDayKeyframe) p2;
        TimeOfDayKeyframe k3 = (TimeOfDayKeyframe) p3;

        int timeOfDay = (int) Nurbs.value(this.time, k1.time, k2.time, k3.time, t0, t1, t2, t3, amount);
        return new KeyframeChangeTimeOfDay(timeOfDay);
    }

    @Override
    public KeyframeChange createQuinticInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        TimeOfDayKeyframe k1 = (TimeOfDayKeyframe) p1;
        TimeOfDayKeyframe k2 = (TimeOfDayKeyframe) p2;

        int timeOfDay = (int) Quintic.value(k1.time, k2.time, amount);
        return new KeyframeChangeTimeOfDay(timeOfDay);
    }


    @Override
    public KeyframeChange createSmoothInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        float time1 = t1 - t0;
        float time2 = t2 - t0;
        float time3 = t3 - t0;

        int timeOfDay = (int) CatmullRom.value(this.time,
                ((TimeOfDayKeyframe)p1).time, ((TimeOfDayKeyframe)p2).time,
                ((TimeOfDayKeyframe)p3).time, time1, time2, time3, amount);

        return new KeyframeChangeTimeOfDay(timeOfDay);
    }

    @Override
    public KeyframeChange createHermiteInterpolatedChange(Map<Float, Keyframe> keyframes, float amount) {
        int timeOfDay = (int) Hermite.value(Maps.transformValues(keyframes, k -> (double) ((TimeOfDayKeyframe)k).time), amount);
        return new KeyframeChangeTimeOfDay(timeOfDay);
    }

    public static class TypeAdapter implements JsonSerializer<TimeOfDayKeyframe>, JsonDeserializer<TimeOfDayKeyframe> {
        @Override
        public TimeOfDayKeyframe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            int timeOfDay = jsonObject.get("time").getAsInt();
            InterpolationType interpolationType = context.deserialize(jsonObject.get("interpolation_type"), InterpolationType.class);
            return new TimeOfDayKeyframe(timeOfDay, interpolationType);
        }

        @Override
        public JsonElement serialize(TimeOfDayKeyframe src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("time", src.time);
            jsonObject.addProperty("type", "time");
            jsonObject.add("interpolation_type", context.serialize(src.interpolationType()));
            return jsonObject;
        }
    }

}
