package floydaddons.not.dogshit.mixin;

import floydaddons.not.dogshit.client.NickHiderConfig;
import floydaddons.not.dogshit.client.NickTextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class NickHiderPlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void floydaddons$nickTab(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (!NickHiderConfig.isEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSession() == null || entry == null || entry.getProfile() == null) return;

        Text original = cir.getReturnValue();
        if (original == null) return;

        boolean isSelf = entry.getProfile().id().equals(client.getSession().getUuidOrNull());
        if (isSelf) {
            String username = client.getSession().getUsername();
            String nick = NickHiderConfig.getNickname();
            cir.setReturnValue(NickTextUtil.replaceLiteralTextIgnoreCase(original, username, nick));
        } else if (NickHiderConfig.isHideOthers()) {
            String entryName = entry.getProfile().name();
            if (entryName != null && !entryName.isEmpty()) {
                cir.setReturnValue(NickTextUtil.replaceLiteralTextIgnoreCase(original, entryName, NickHiderConfig.getOthersNickname()));
            }
        }
    }
}
