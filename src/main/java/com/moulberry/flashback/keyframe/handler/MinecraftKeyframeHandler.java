package com.moulberry.flashback.keyframe.handler;

import com.moulberry.flashback.FilePlayerSkin;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.keyframe.change.*;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.io.File;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record MinecraftKeyframeHandler(Minecraft minecraft) implements KeyframeHandler {

    private static final Set<Class<? extends KeyframeChange>> supportedChanges = Set.of(
            KeyframeChangeCameraPosition.class, KeyframeChangeCameraPositionOrbit.class, KeyframeChangeTrackEntity.class,
            KeyframeChangeFov.class, KeyframeChangeTimeOfDay.class, KeyframeChangeCameraShake.class,KeyframeChangePlayerSkin.class
    );

    @Override
    public Minecraft getMinecraft() {
        return this.minecraft;
    }

    @Override
    public boolean supportsKeyframeChange(Class<? extends KeyframeChange> clazz) {
        return supportedChanges.contains(clazz);
    }

    @Override
    public void applyPlayerSkin(UUID entityid, String skinIdentifier, boolean isUuidSkin) {
        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState == null) {

            return;
        }

        if(entityid != null){



        // Reset any existing skin override for this entity first
        editorState.skinOverride.remove(entityid);
        editorState.skinOverrideFromFile.remove(entityid);
            editorState.depthSkinOverrideFromFile.remove(entityid);


        if (isUuidSkin) {

            try {
                UUID skinUuid = UUID.fromString(skinIdentifier);
                CompletableFuture.supplyAsync(() -> Minecraft.getInstance().getMinecraftSessionService().fetchProfile(skinUuid, true))
                        .thenAccept(profileResult -> {
                            if (profileResult != null) {
                                editorState.skinOverride.put(entityid, profileResult.profile());
                            }
                        });
            } catch (IllegalArgumentException e) {
                System.err.println("Flashback: Invalid UUID for skin keyframe: " + skinIdentifier);
            }
        } else {

            editorState.skinOverrideFromFile.put(entityid, new FilePlayerSkin(skinIdentifier));
            File original = new File(skinIdentifier);
            String folder = original.getParent();
            String newName = "depth" + original.getName();
            String depthPath = new File(folder, newName).getAbsolutePath();
            editorState.depthSkinOverrideFromFile.put(entityid, new FilePlayerSkin(depthPath));
        }}
    }

    @Override
    public void applyCameraPosition(Vector3d position, double yaw, double pitch, double roll) {
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            if (this.minecraft.cameraEntity != this.minecraft.player) {
                Minecraft.getInstance().getConnection().sendUnsignedCommand("spectate");
            }

            player.moveTo(position.x, position.y, position.z, (float) yaw, (float) pitch);
            Flashback.getReplayServer().campos = new Vec3(position.x, position.y,position.z);
            EditorState editorState = EditorStateManager.getCurrent();
            if (editorState != null) {
                if (roll > -0.01 && roll < 0.01) {
                    editorState.replayVisuals.overrideRoll = false;
                    editorState.replayVisuals.overrideRollAmount = 0.0f;
                    Flashback.getReplayServer().saveroll = 0;
                } else {
                    editorState.replayVisuals.overrideRoll = true;
                    editorState.replayVisuals.overrideRollAmount = (float) roll;
                    Flashback.getReplayServer().saveroll = roll;
                }
            }

            player.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void applyFov(float fov) {
        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState != null) {
            editorState.replayVisuals.setFov(fov);

            Flashback.getReplayServer().fov = fov;
        }
    }

    @Override
    public void applyTimeOfDay(int timeOfDay) {
        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState != null) {
            editorState.replayVisuals.overrideTimeOfDay = timeOfDay;
        }
    }

    @Override
    public void applyCameraShake(float frequencyX, float amplitudeX, float frequencyY, float amplitudeY) {
        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState != null) {
            editorState.replayVisuals.setCameraShake(frequencyX, amplitudeX, frequencyY, amplitudeY);
        }
    }
}
