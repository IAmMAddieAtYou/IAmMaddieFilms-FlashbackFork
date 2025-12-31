package com.moulberry.flashback.mixin.visuals;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.nio.IntBuffer;

@Mixin(MainTarget.class)
public class MixinMainTarget {

    /**
     * Hijacks the creation of the Depth Texture to force 32-bit Float Precision.
     * We target 'allocateDepthAttachment' because that is where the actual allocation happens.
     */
    @Redirect(
            method = "allocateDepthAttachment",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"
            )
    )
    private void forceFloatDepthScreen(int target, int level, int internalFormat, int width, int height, int border, int format, int type, IntBuffer pixels) {

        System.out.println("Flashback Mixin (MainScreen): Checking Depth Format...");

        if (format == GL30.GL_DEPTH_COMPONENT) {
            System.out.println("Flashback Mixin (MainScreen): UPGRADING SCREEN TO 32-BIT FLOAT");
            // Force the Main Game Screen to use high precision
            GlStateManager._texImage2D(target, level, GL30.GL_DEPTH_COMPONENT32F, width, height, border, format, GL11.GL_FLOAT, pixels);
        } else {
            GlStateManager._texImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
        }
    }
}