package floydaddons.not.dogshit.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class FloydAddonsClient implements ClientModInitializer {
    public static final String MOD_ID = "floydaddons";

    private KeyBinding openGuiKey;
    private KeyBinding xrayToggleKey;
    private static final KeyBinding.Category KEY_CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, "category"));

    @Override
    public void onInitializeClient() {
        FloydAddonsConfig.load();
        InventoryHudRenderer.register();
        ScoreboardHudRenderer.register();
        StalkRenderer.register();
        FloydAddonsCommand.register();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.floydaddons.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                KEY_CATEGORY
        ));

        xrayToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.floydaddons.toggle_xray",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SkinManager.extractDefaultSkin(client);
            if (client.player == null) {
                return;
            }
            while (openGuiKey.wasPressed()) {
                client.setScreen(new FloydAddonsScreen(Text.literal("FloydAddons")));
            }
            while (xrayToggleKey.wasPressed()) {
                RenderConfig.toggleXray();
            }
        });

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, entityRenderer, registrationHelper, context) -> {
                    if (entityRenderer instanceof PlayerEntityRenderer<?>) {
                        @SuppressWarnings("unchecked")
                        var ctx = (net.minecraft.client.render.entity.feature.FeatureRendererContext<
                                net.minecraft.client.render.entity.state.PlayerEntityRenderState,
                                net.minecraft.client.render.entity.model.PlayerEntityModel>) entityRenderer;
                        registrationHelper.register(new ConeFeatureRenderer(ctx));
                    }
                }
        );

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            FloydAddonsConfig.save();
        });
    }
}


