package floydaddons.not.dogshit.mixin;

import floydaddons.not.dogshit.client.HidersConfig;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Miscellaneous hiders: glow effect removal.
 */
@Mixin(Entity.class)
public class HiderMiscMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true, require = 0)
    private void floydaddons$removeGlowEffect(CallbackInfoReturnable<Boolean> cir) {
        if (HidersConfig.isRemoveGlowEffectEnabled()) cir.setReturnValue(false);
    }
}
