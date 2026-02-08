package floydaddons.not.dogshit.client;

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

    /** Convenience: delegates to the unified config. */
    public static void save() { FloydAddonsConfig.save(); }
}
