package floydaddons.not.dogshit.client;

/**
 * Manages the stalk target (runtime state, not persisted).
 */
public final class StalkManager {
    private static String targetName = "";
    private static boolean enabled = false;

    private StalkManager() {}

    public static boolean isEnabled() { return enabled; }
    public static String getTargetName() { return targetName; }

    public static void setTarget(String name) {
        if (name == null || name.isBlank()) {
            targetName = "";
            enabled = false;
        } else {
            targetName = name;
            enabled = true;
        }
    }

    public static void disable() {
        enabled = false;
        targetName = "";
    }
}
