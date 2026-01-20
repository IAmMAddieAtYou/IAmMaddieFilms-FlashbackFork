package com.moulberry.flashback.keyframe.impl;

import com.google.gson.*;
import com.moulberry.flashback.FilePlayerSkin;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.Interpolation;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.exporting.AsyncFileDialogs;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.types.SkinKeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChangePlayerSkin; // New import
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;
import imgui.ImGui;
import imgui.type.ImString; // Import ImString for editable text fields
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID; // For UUID validation
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


public class SkinKeyframe extends Keyframe {

    // Store the UUID of the entity whose skin we want to change
    private UUID entityUuid;
    // Store either a skin UUID string or a path to a local skin file
    private String skinIdentifier; // Can be a UUID string or an image path
    private boolean isUuidSkin; // Flag to indicate if skinIdentifier is a UUID or a file path

    // For ImGui editing
    private final transient ImString entityUuidImString;
    private transient ImString skinIdentifierImString;




    // Constructor for deserialization
    public SkinKeyframe(UUID entityUuid, String skinIdentifier, boolean isUuidSkin) {
        this(entityUuid, skinIdentifier, isUuidSkin, InterpolationType.HOLD); // Default to STEP interpolation for skin changes
    }

    public SkinKeyframe(UUID entityUuid, String skinIdentifier, boolean isUuidSkin, InterpolationType interpolationType) {
        this.entityUuid = entityUuid;
        this.skinIdentifier = skinIdentifier;
        this.isUuidSkin = isUuidSkin;
        this.interpolationType(interpolationType);

        // Initialize transient ImStrings for editing
        if(entityUuid != null){
            this.entityUuidImString = new ImString(entityUuid.toString(), 64);
        } else {
            this.entityUuidImString = new ImString("", 64);
        }
        this.skinIdentifierImString = new ImString(skinIdentifier, 128);
    }




    @Override
    public KeyframeType<?> keyframeType() {
        return SkinKeyframeType.INSTANCE;
    }

    @Override
    public Keyframe copy() {
        return new SkinKeyframe(this.entityUuid, this.skinIdentifier, this.isUuidSkin, this.interpolationType());
    }

    @Override
    public void renderEditKeyframe(Consumer<Consumer<Keyframe>> update) {
        // UI for entity UUID


        ImGui.text("Target Entity UUID:");
        if (ImGui.inputText("##EntityUUID", entityUuidImString)) {
            try {
                UUID newUuid = UUID.fromString(entityUuidImString.get());
                if (!newUuid.equals(this.entityUuid)) {
                    update.accept(k -> ((SkinKeyframe)k).entityUuid = newUuid);
                }
            } catch (IllegalArgumentException e) {
                // Handle invalid UUID input, maybe show an error
                ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, "Invalid UUID");
            }
        }

        // UI for skin type selection
        if (ImGui.checkbox("Use UUID for Skin", this.isUuidSkin)) {
                boolean newu = !this.isUuidSkin;
                update.accept(k -> ((SkinKeyframe)k).isUuidSkin = newu);

        }

        if (ImGui.button("Upload Skin from File")) {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            CompletableFuture<String> future = AsyncFileDialogs.openFileDialog(gameDir.toString(),
                    "Skin Texture", "png");
            future.thenAccept(pathStr -> {
                if (pathStr != null) {
                    Flashback.getReplayServer().getEditorState().skinOverride.remove(this.entityUuid);
                    this.skinIdentifier = pathStr;
                    Flashback.getReplayServer().getEditorState().skinOverrideFromFile.put(this.entityUuid, new FilePlayerSkin(pathStr));
                    File original = new File(pathStr);
                    String folder = original.getParent();
                    String newName = "depth" + original.getName();
                    String depthPath = new File(folder, newName).getAbsolutePath();
                    Flashback.getReplayServer().getEditorState().depthSkinOverrideFromFile.put(this.entityUuid, new FilePlayerSkin(depthPath));
                    update.accept(k -> ((SkinKeyframe)k).skinIdentifier = pathStr);

                }
            });
        }
    }

    @Override
    public KeyframeChange createChange() {
        // This is where the skin change is actually applied
        return new KeyframeChangePlayerSkin(this.entityUuid, this.skinIdentifier, this.isUuidSkin);
    }

    @Override
    public KeyframeChange createAkimaInterpolatedChange(Keyframe pBefore, Keyframe p1, Keyframe p2, Keyframe p3, float tBefore, float t0, float t1, float t2, float t3, float amount) {
        return new KeyframeChangePlayerSkin(this.entityUuid, this.skinIdentifier, this.isUuidSkin);
    }

    @Override
    public KeyframeChange createSmoothingInterpolatedChange(Keyframe pBefore, Keyframe p1, Keyframe p2, Keyframe p3, float tBefore, float t0, float t1, float t2, float t3, float amount) {
        return new KeyframeChangePlayerSkin(this.entityUuid, this.skinIdentifier, this.isUuidSkin);
    }

    // --- 4-POINT INTERPOLATION (Standard) ---
    // Context: this -> p1 -> p2 -> p3

    @Override
    public KeyframeChange createCircularInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        return new KeyframeChangePlayerSkin(this.entityUuid, this.skinIdentifier, this.isUuidSkin);
    }

    @Override
    public KeyframeChange createMonotoneCubicInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        return new KeyframeChangePlayerSkin(this.entityUuid, this.skinIdentifier, this.isUuidSkin);
    }

    @Override
    public KeyframeChange createNurbsInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        return new KeyframeChangePlayerSkin(this.entityUuid, this.skinIdentifier, this.isUuidSkin);
    }

    @Override
    public KeyframeChange createQuinticInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
        return new KeyframeChangePlayerSkin(this.entityUuid, this.skinIdentifier, this.isUuidSkin);
    }

    @Override
    public KeyframeChange createSmoothInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {

        return new KeyframeChangePlayerSkin(this.entityUuid, this.skinIdentifier, this.isUuidSkin);
    }



    @Override
    public KeyframeChange createHermiteInterpolatedChange(Map<Float, Keyframe> keyframes, float tick) {
        // Skin changes are discrete and not interpolated.
        return new KeyframeChangePlayerSkin(this.entityUuid, this.skinIdentifier, this.isUuidSkin);
    }

    public static class TypeAdapter implements JsonSerializer<SkinKeyframe>, JsonDeserializer<SkinKeyframe> {
        @Override
        public SkinKeyframe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            UUID entityUuid = null;
            if (jsonObject.has("entityUuid") && !jsonObject.get("entityUuid").isJsonNull()) {
                entityUuid = UUID.fromString(jsonObject.get("entityUuid").getAsString());
            }
            String skinIdentifier = jsonObject.has("skinIdentifier") ? jsonObject.get("skinIdentifier").getAsString() : "";
            boolean isUuidSkin = jsonObject.has("isUuidSkin") ? jsonObject.get("isUuidSkin").getAsBoolean() : false;
            return new SkinKeyframe(entityUuid, skinIdentifier, isUuidSkin, InterpolationType.HOLD);
        }

        @Override
        public JsonElement serialize(SkinKeyframe src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            if (src.entityUuid != null) { // Use your actual getter or field access
                jsonObject.addProperty("entityUuid", src.entityUuid.toString());
            }
            jsonObject.addProperty("skinIdentifier", src.skinIdentifier);
            jsonObject.addProperty("isUuidSkin", src.isUuidSkin);
            jsonObject.addProperty("type", "skin");
            jsonObject.add("interpolation_type", context.serialize(src.interpolationType()));
            return jsonObject;
        }
    }
}