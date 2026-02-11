package floydaddons.not.dogshit.mixin;

import floydaddons.not.dogshit.client.HidersConfig;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides entity fire, death animation, and attached arrows on living entities.
 */
@Mixin(LivingEntityRenderer.class)
public class HiderEntityRendererMixin {

    @Inject(method = "setupTransforms", at = @At("HEAD"), require = 0)
    private void floydaddons$noDeathAnimation(LivingEntityRenderState state, MatrixStack matrices, float bodyYaw, float baseHeight, CallbackInfo ci) {
        if (HidersConfig.isNoDeathAnimationEnabled()) {
            state.deathTime = 0;
        }
    }
}
