package floydaddons.not.dogshit.mixin;

import floydaddons.not.dogshit.client.HidersConfig;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Removes the nametag background when DisableNametagBackground is enabled.
 * Targets the background opacity argument in nametag rendering.
 */
@Mixin(EntityRenderer.class)
public class HiderNametagMixin {

    @ModifyArg(
        method = "renderLabelIfPresent",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I", ordinal = 0),
        index = 7,
        require = 0
    )
    private int floydaddons$removeNametagBg(int backgroundColor) {
        if (HidersConfig.isDisableNametagBackgroundEnabled()) return 0;
        return backgroundColor;
    }
}
