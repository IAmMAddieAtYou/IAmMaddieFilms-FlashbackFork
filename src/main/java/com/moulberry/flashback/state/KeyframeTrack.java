package com.moulberry.flashback.state;

import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.change.KeyframeChangeTickrate;
import com.moulberry.flashback.keyframe.impl.TimelapseKeyframe;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;
import com.moulberry.flashback.keyframe.interpolation.SidedInterpolationType;
import com.moulberry.flashback.keyframe.types.TimelapseKeyframeType;
import imgui.type.ImString;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class KeyframeTrack {

    public final KeyframeType<?> keyframeType;
    public TreeMap<Integer, Keyframe> keyframesByTick = new TreeMap<>();
    public boolean enabled = true;
    public String customName = null;
    public int customColour = 0;

    public transient ImString nameEditField = null;
    public transient boolean forceFocusTrack = false;
    public transient float animatedOffsetInUi = 0.0f;

    public KeyframeTrack(KeyframeType<?> keyframeType) {
        this.keyframeType = keyframeType;
    }

    @Nullable
    public KeyframeChange createKeyframeChange(float tick, @Nullable RealTimeMapping realTimeMapping) {
        if (this.keyframeType == TimelapseKeyframeType.INSTANCE) {
            return this.tryApplyKeyframesTimelapse(tick);
        }

        // Skip if empty
        TreeMap<Integer, Keyframe> keyframeTimes = this.keyframesByTick;
        if (keyframeTimes.isEmpty()) {
            return null;
        }

        Map.Entry<Integer, Keyframe> lowerEntry = keyframeTimes.floorEntry((int) tick);
        if (lowerEntry == null) {
            return null;
        }

        Keyframe lowerKeyframe = lowerEntry.getValue();

//        if (tick == lowerEntry.getKey()) {
//            return lowerKeyframe.createChange();
//        }

        SidedInterpolationType leftInterpolation = lowerEntry.getValue().interpolationType().rightSide;

        // Immediately apply hold (Step function, so snapping to keyframe is correct/intended here)
        if (leftInterpolation == SidedInterpolationType.HOLD) {
            return lowerKeyframe.createChange();
        }

        // Get next entry, skip if tick is not between two keyframes
        Map.Entry<Integer, Keyframe> ceilEntry = keyframeTimes.ceilingEntry(lowerEntry.getKey() + 1);
        if (ceilEntry == null) {
            // If we are at the very last keyframe, we have no forward segment.
            // In this specific case, we must return the keyframe value because we can't interpolate forward.
            if ((int) tick == lowerEntry.getKey()) {
                return lowerKeyframe.createChange();
            }
            return null;
        }

        SidedInterpolationType rightInterpolation = ceilEntry.getValue().interpolationType().leftSide;
        if (rightInterpolation == SidedInterpolationType.HOLD) {
            rightInterpolation = leftInterpolation;
        }

        // --- FIX: Force interpolation in Tick Space ---
        // We use raw ticks to ensure shape consistency regardless of Speed Ramps (RealTimeMapping).
        float interpTick = tick;
        float interpLowerTick = lowerEntry.getKey();
        float interpCeilTick = ceilEntry.getKey();

        // Ensure non-zero duration to prevent division by zero in interpolation math
        float segmentDuration = interpCeilTick - interpLowerTick;
        if (segmentDuration <= 0.001f) segmentDuration = 1.0f;

        float amount = (interpTick - interpLowerTick) / segmentDuration;

        KeyframeChange leftChange = null;
        KeyframeChange rightChange = null;

        // --- 4-POINT INTERPOLATION (Smooth, Circular, Monotone, Nurbs, Quintic) ---
        boolean leftIs4Point = is4Point(leftInterpolation);
        boolean rightIs4Point = is4Point(rightInterpolation);

        if (leftIs4Point || rightIs4Point) {
            Map.Entry<Integer, Keyframe> beforeEntry = keyframeTimes.floorEntry(lowerEntry.getKey() - 1);
            float interpBeforeTick;

            // Fix boundary condition: If no 'before' keyframe, extrapolate time backwards.
            if (beforeEntry == null || beforeEntry.getValue().interpolationType() == InterpolationType.HOLD) {
                beforeEntry = lowerEntry;
                interpBeforeTick = interpLowerTick - segmentDuration;
            } else {
                interpBeforeTick = beforeEntry.getKey();
            }

            Map.Entry<Integer, Keyframe> afterEntry = keyframeTimes.ceilingEntry(ceilEntry.getKey() + 1);
            float interpAfterTick;

            // Fix boundary condition: If no 'after' keyframe, extrapolate time forwards.
            if (afterEntry == null || ceilEntry.getValue().interpolationType() == InterpolationType.HOLD) {
                afterEntry = ceilEntry;
                interpAfterTick = interpCeilTick + segmentDuration;
            } else {
                interpAfterTick = afterEntry.getKey();
            }

            // Generate Change
            if (leftIs4Point) {
                leftChange = create4PointChange(leftInterpolation, beforeEntry.getValue(), lowerKeyframe, ceilEntry.getValue(), afterEntry.getValue(),
                        interpBeforeTick, interpLowerTick, interpCeilTick, interpAfterTick, amount);
            }
            if (rightIs4Point) {
                if (leftIs4Point && leftInterpolation == rightInterpolation) {
                    rightChange = leftChange;
                } else {
                    rightChange = create4PointChange(rightInterpolation, beforeEntry.getValue(), lowerKeyframe, ceilEntry.getValue(), afterEntry.getValue(),
                            interpBeforeTick, interpLowerTick, interpCeilTick, interpAfterTick, amount);
                }
            }
        }

        // --- 5-POINT INTERPOLATION (Akima, Smoothing) ---
        boolean leftIs5Point = is5Point(leftInterpolation);
        boolean rightIs5Point = is5Point(rightInterpolation);

        if (leftIs5Point || rightIs5Point) {
            Map.Entry<Integer, Keyframe> beforeEntry = keyframeTimes.floorEntry(lowerEntry.getKey() - 1);
            float interpBeforeTick;
            if (beforeEntry == null || beforeEntry.getValue().interpolationType() == InterpolationType.HOLD) {
                beforeEntry = lowerEntry;
                interpBeforeTick = interpLowerTick - segmentDuration;
            } else {
                interpBeforeTick = beforeEntry.getKey();
            }

            Map.Entry<Integer, Keyframe> afterEntry = keyframeTimes.ceilingEntry(ceilEntry.getKey() + 1);
            float interpAfterTick;
            if (afterEntry == null || ceilEntry.getValue().interpolationType() == InterpolationType.HOLD) {
                afterEntry = ceilEntry;
                interpAfterTick = interpCeilTick + segmentDuration;
            } else {
                interpAfterTick = afterEntry.getKey();
            }

            Map.Entry<Integer, Keyframe> beforeBeforeEntry = null;
            float interpBeforeBeforeTick;

            if (beforeEntry != lowerEntry) {
                beforeBeforeEntry = keyframeTimes.floorEntry(beforeEntry.getKey() - 1);
            }
            if (beforeBeforeEntry == null || beforeBeforeEntry.getValue().interpolationType() == InterpolationType.HOLD) {
                beforeBeforeEntry = beforeEntry;
                interpBeforeBeforeTick = interpBeforeTick - segmentDuration;
            } else {
                interpBeforeBeforeTick = beforeBeforeEntry.getKey();
            }

            if (leftIs5Point) {
                leftChange = create5PointChange(leftInterpolation, beforeEntry.getValue(), beforeBeforeEntry.getValue(), lowerKeyframe, ceilEntry.getValue(), afterEntry.getValue(),
                        interpBeforeBeforeTick, interpBeforeTick, interpLowerTick, interpCeilTick, interpAfterTick, amount);
            }
            if (rightIs5Point) {
                if (leftIs5Point && leftInterpolation == rightInterpolation) {
                    rightChange = leftChange;
                } else {
                    rightChange = create5PointChange(rightInterpolation, beforeEntry.getValue(), beforeBeforeEntry.getValue(), lowerKeyframe, ceilEntry.getValue(), afterEntry.getValue(),
                            interpBeforeBeforeTick, interpBeforeTick, interpLowerTick, interpCeilTick, interpAfterTick, amount);
                }
            }
        }

        // --- MAP-BASED INTERPOLATION (Hermite) ---
        if (leftInterpolation == SidedInterpolationType.HERMITE ||
                rightInterpolation == SidedInterpolationType.HERMITE) {
            Integer minKey = lowerEntry.getKey();

            while (minKey != null) {
                Map.Entry<Integer, Keyframe> before = keyframeTimes.floorEntry(minKey - 1);

                if (before == null) {
                    minKey = null;
                    break;
                } else if (before.getValue().interpolationType() == InterpolationType.HOLD) {
                    break;
                }

                minKey = before.getKey();
            }

            Integer maxKey = ceilEntry.getKey();

            while (maxKey != null) {
                Map.Entry<Integer, Keyframe> after = keyframeTimes.ceilingEntry(maxKey + 1);

                if (after == null) {
                    maxKey = null;
                    break;
                } else if (after.getValue().interpolationType() == InterpolationType.HOLD) {
                    maxKey = after.getKey();
                    break;
                }

                maxKey = after.getKey();
            }

            Map<Integer, Keyframe> subMap;

            if (minKey != null) {
                if (maxKey != null) {
                    subMap = Maps.subMap(this.keyframesByTick, Range.closed(minKey, maxKey));
                } else {
                    subMap = Maps.subMap(this.keyframesByTick, Range.atLeast(minKey));
                }
            } else if (maxKey != null) {
                subMap = Maps.subMap(this.keyframesByTick, Range.atMost(maxKey));
            } else {
                subMap = this.keyframesByTick;
            }

            Map<Float, Keyframe> transformedMap = new HashMap<>();
            for (Map.Entry<Integer, Keyframe> entry : subMap.entrySet()) {
                // Fix for Speed Ramps with Hermite: Use Ticks (entry.getKey()) directly.
                // This matches the behavior of 4-point/5-point splines which ignore RealTimeMapping
                // to preserve the path shape.
                float time = (float) entry.getKey();
                transformedMap.put(time, entry.getValue());
            }

            KeyframeChange hermiteChange = lowerKeyframe.createHermiteInterpolatedChange(transformedMap, interpTick);
            if (leftInterpolation == SidedInterpolationType.HERMITE) {
                leftChange = hermiteChange;
            }
            if (rightInterpolation == SidedInterpolationType.HERMITE) {
                rightChange = hermiteChange;
            }
        }

        if (leftChange == null || rightChange == null) {
            double adjustedAmount = SidedInterpolationType.interpolate(leftInterpolation, rightInterpolation, amount);

            KeyframeChange keyframeChange = lowerKeyframe.createChange();

            if (adjustedAmount != 0.0) {
                KeyframeChange keyframeChangeCeil = ceilEntry.getValue().createChange();
                keyframeChange = KeyframeChange.interpolateSafe(keyframeChange, keyframeChangeCeil, (float) adjustedAmount);
            }

            if (leftChange == null) {
                leftChange = keyframeChange;
            }
            if (rightChange == null) {
                rightChange = keyframeChange;
            }
        }

        return KeyframeChange.interpolateSafe(leftChange, rightChange, amount);
    }

    private boolean is4Point(SidedInterpolationType type) {
        return type == SidedInterpolationType.SMOOTH ||
                type == SidedInterpolationType.CIRCULAR ||
                type == SidedInterpolationType.MONOTONECUBIC ||
                type == SidedInterpolationType.NURBS ||
                type == SidedInterpolationType.QUINTIC;
    }

    private boolean is5Point(SidedInterpolationType type) {
        return type == SidedInterpolationType.AKIMA ||
                type == SidedInterpolationType.SMOOTHING;
    }

    private KeyframeChange create4PointChange(SidedInterpolationType type, Keyframe before, Keyframe lower, Keyframe ceil, Keyframe after,
                                              float t0, float t1, float t2, float t3, float amount) {
        switch (type) {
            case SMOOTH: return before.createSmoothInterpolatedChange(lower, ceil, after, t0, t1, t2, t3, amount);
            case CIRCULAR: return before.createCircularInterpolatedChange(lower, ceil, after, t0, t1, t2, t3, amount);
            case MONOTONECUBIC: return before.createMonotoneCubicInterpolatedChange(lower, ceil, after, t0, t1, t2, t3, amount);
            case NURBS: return before.createNurbsInterpolatedChange(lower, ceil, after, t0, t1, t2, t3, amount);
            case QUINTIC: return before.createQuinticInterpolatedChange(lower, ceil, after, t0, t1, t2, t3, amount);
            default: throw new IllegalArgumentException("Unsupported 4-point interpolation: " + type);
        }
    }

    private KeyframeChange create5PointChange(SidedInterpolationType type, Keyframe before, Keyframe beforeBefore, Keyframe lower, Keyframe ceil, Keyframe after,
                                              float tBefore, float t0, float t1, float t2, float t3, float amount) {
        switch (type) {
            case AKIMA: return before.createAkimaInterpolatedChange(beforeBefore, lower, ceil, after, tBefore, t0, t1, t2, t3, amount);
            case SMOOTHING: return before.createSmoothingInterpolatedChange(beforeBefore, lower, ceil, after, tBefore, t0, t1, t2, t3, amount);
            default: throw new IllegalArgumentException("Unsupported 5-point interpolation: " + type);
        }
    }

    @Nullable
    private KeyframeChangeTickrate tryApplyKeyframesTimelapse(float tick) {
        // Skip if empty
        TreeMap<Integer, Keyframe> keyframeTimes = this.keyframesByTick;
        if (keyframeTimes.isEmpty()) {
            return null;
        }

        Map.Entry<Integer, Keyframe> lowerEntry = keyframeTimes.floorEntry((int) tick);
        Map.Entry<Integer, Keyframe> ceilEntry = keyframeTimes.ceilingEntry(((int) tick) + 1);

        if (ceilEntry == null && lowerEntry != null && lowerEntry.getKey() == (int) tick) {
            ceilEntry = lowerEntry;
            lowerEntry = keyframeTimes.floorEntry((int) tick - 1);
        }

        if (lowerEntry != null && ceilEntry != null) {
            int lowerTicks = ((TimelapseKeyframe) lowerEntry.getValue()).ticks;
            int ceilTicks = ((TimelapseKeyframe) ceilEntry.getValue()).ticks;

            if (ceilTicks <= lowerTicks) {
                ReplayUI.setInfoOverlayShort("Unable to timelapse. Right keyframe's time must be greater than left keyframe's time");
                return null;
            } else {
                double tickrate = (double) (ceilEntry.getKey() - lowerEntry.getKey()) / (ceilTicks - lowerTicks) * 20;
                return new KeyframeChangeTickrate((float) tickrate);
            }
        }

        return null;
    }

}
