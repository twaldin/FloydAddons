package floydaddons.not.dogshit.mixin;

import floydaddons.not.dogshit.client.HidersConfig;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides vignette, potion effects, hunger bar, and allows third-person crosshair.
 */
@Mixin(InGameHud.class)
public class HiderInGameHudMixin {

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void floydaddons$disableVignette(CallbackInfo ci) {
        if (HidersConfig.isDisableVignetteEnabled()) ci.cancel();
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void floydaddons$hidePotionEffects(CallbackInfo ci) {
        if (HidersConfig.isHidePotionEffectsEnabled()) ci.cancel();
    }
}
