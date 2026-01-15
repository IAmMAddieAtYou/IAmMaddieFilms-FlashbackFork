package com.moulberry.flashback.mixin.visuals;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.exporting.depthsettings.DEPTHEXPORT;
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
    private void dynamicDepthAllocation(int target, int level, int internalFormat, int width, int height, int border, int format, int type, IntBuffer pixels) {

        // CHECK YOUR ENUM HERE
        boolean useHighPrecision = (Flashback.depthprecision == DEPTHEXPORT.HIGHPRECISION);

        if (useHighPrecision && format == GL30.GL_DEPTH_COMPONENT) {
            // Force 32-bit Float
            GlStateManager._texImage2D(target, level, GL30.GL_DEPTH_COMPONENT32F, width, height, border, format, GL11.GL_FLOAT, pixels);
            // Flashback.LOGGER.info("Allocated Main Screen Depth: 32-BIT FLOAT");
        } else {
            // Normal (Vanilla 24-bit Int)
            GlStateManager._texImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
        }
    }
}