package floydaddons.not.dogshit.mixin;

import floydaddons.not.dogshit.client.HidersConfig;
import net.minecraft.client.render.entity.LightningEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents lightning from rendering when RemoveLightning is enabled.
 */
@Mixin(LightningEntityRenderer.class)
public class HiderLightningMixin {

    @Inject(method = "render*", at = @At("HEAD"), cancellable = true, require = 0)
    private void floydaddons$removeLightning(CallbackInfo ci) {
        if (HidersConfig.isRemoveLightningEnabled()) ci.cancel();
    }
}
