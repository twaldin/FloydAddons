package floydaddons.not.dogshit.client;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Config backing the Render screen (inventory HUD).
 */
public final class RenderConfig {
    private static boolean inventoryHudEnabled = false;
    private static int inventoryHudX = 12;
    private static int inventoryHudY = 12;
    private static float inventoryHudScale = 1.1f;
    private static boolean floydHatEnabled = false;
    private static boolean customScoreboardEnabled = false;
    private static int customScoreboardX = -1;
    private static int customScoreboardY = -1;
    private static boolean serverIdHiderEnabled = false;
    private static String serverIdReplacement = "";
    private static volatile boolean xrayEnabled = false;
    private static float xrayOpacity = 0.3f;
    private static volatile Set<String> xrayOpaqueBlocks = defaultXrayOpaqueBlocks();

    private RenderConfig() {}

    public static boolean isInventoryHudEnabled() { return inventoryHudEnabled; }
    public static void setInventoryHudEnabled(boolean enabled) { inventoryHudEnabled = enabled; }

    public static int getInventoryHudX() { return inventoryHudX; }
    public static int getInventoryHudY() { return inventoryHudY; }
    public static void setInventoryHudX(int x) { inventoryHudX = x; }
    public static void setInventoryHudY(int y) { inventoryHudY = y; }
    public static float getInventoryHudScale() { return inventoryHudScale; }
    public static void setInventoryHudScale(float scale) { inventoryHudScale = 1.1f; }

    public static boolean isFloydHatEnabled() { return floydHatEnabled; }
    public static void setFloydHatEnabled(boolean v) { floydHatEnabled = v; }

    public static boolean isCustomScoreboardEnabled() { return customScoreboardEnabled; }
    public static void setCustomScoreboardEnabled(boolean v) { customScoreboardEnabled = v; }

    public static int getCustomScoreboardX() { return customScoreboardX; }
    public static void setCustomScoreboardX(int x) { customScoreboardX = x; }

    public static int getCustomScoreboardY() { return customScoreboardY; }
    public static void setCustomScoreboardY(int y) { customScoreboardY = y; }

    public static boolean isServerIdHiderEnabled() { return serverIdHiderEnabled; }
    public static void setServerIdHiderEnabled(boolean v) { serverIdHiderEnabled = v; }

    public static String getServerIdReplacement() { return serverIdReplacement; }
    public static void setServerIdReplacement(String v) { serverIdReplacement = v; }

    public static boolean isXrayEnabled() { return xrayEnabled; }
    public static float getXrayOpacity() { return xrayOpacity; }
    public static void setXrayOpacity(float v) { xrayOpacity = Math.max(0.05f, Math.min(1.0f, v)); }

    public static void toggleXray() {
        xrayEnabled = !xrayEnabled;
        rebuildChunks();
    }

    /** Forces a full chunk rebuild for both vanilla and Sodium renderers. */
    public static void rebuildChunks() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.worldRenderer != null) {
            client.worldRenderer.reload();
            try {
                Class<?> swr = Class.forName("net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer");
                Object instance = swr.getMethod("instance").invoke(null);
                swr.getMethod("scheduleRebuildForAllChunks", boolean.class).invoke(instance, true);
            } catch (Exception ignored) {
            }
        }
    }

    public static Set<String> getXrayOpaqueBlocks() { return xrayOpaqueBlocks; }
    public static void setXrayOpaqueBlocks(Set<String> blocks) { xrayOpaqueBlocks = blocks; }

    /** Check if a block should retain its original opacity during xray. */
    public static boolean isXrayOpaque(BlockState state) {
        String id = Registries.BLOCK.getId(state.getBlock()).toString();
        return xrayOpaqueBlocks.contains(id);
    }

    static Set<String> defaultXrayOpaqueBlocks() {
        Set<String> set = new LinkedHashSet<>();
        set.add("minecraft:glass");
        set.add("minecraft:tinted_glass");
        set.add("minecraft:glass_pane");
        String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime",
                "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green",
                "red", "black"};
        for (String c : colors) {
            set.add("minecraft:" + c + "_stained_glass");
            set.add("minecraft:" + c + "_stained_glass_pane");
        }
        return Collections.unmodifiableSet(set);
    }

    /** Convenience: delegates to the unified config. */
    public static void save() { FloydAddonsConfig.save(); }
}
