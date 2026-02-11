package floydaddons.not.dogshit.mixin;

import floydaddons.not.dogshit.client.HidersConfig;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disables fog when the DisableFog hider is enabled.
 * Uses FogRenderer's built-in fogEnabled flag to prevent fog from being rendered.
 */
@Mixin(FogRenderer.class)
public class HiderFogMixin {

    @Shadow private static boolean fogEnabled;

    @Inject(
        method = "applyFog(Lnet/minecraft/client/render/Camera;IZLnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
        at = @At("HEAD"),
        require = 0
    )
    private void floydaddons$disableFog(CallbackInfoReturnable<?> cir) {
        if (HidersConfig.isDisableFogEnabled()) {
            fogEnabled = false;
        }
    }
}
