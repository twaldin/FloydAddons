package floydaddons.not.dogshit.mixin;

import floydaddons.not.dogshit.client.MapOverrideManager;
import net.minecraft.client.texture.MapTextureManager;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapTextureManager.class)
public class MapTextureOverrideMixin {

    @Inject(
            method = "getTextureId",
            at = @At("HEAD")
    )
    private void floyd$overrideMap(MapIdComponent mapId, MapState state, CallbackInfoReturnable<Identifier> cir) {
        MapOverrideManager.maybeApply(mapId, state);
        // Mark dirty so the map texture re-uploads after we mutate colors.
        ((MapTextureManager) (Object) this).setNeedsUpdate(mapId, state);
    }
}
