package floydaddons.not.dogshit.client;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public final class NickHiderConfig {

    private static String nickname = "George Floyd";
    private static boolean enabled = false;

    // Per-player name mappings (managed by FloydAddonsConfig)
    private static Map<String, String> nameMappings = Collections.emptyMap();

    private NickHiderConfig() {}

    public static String getNickname() { return nickname; }
    public static void setNickname(String nick) { if (nick != null && !nick.isEmpty()) nickname = nick; }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }

    public static Map<String, String> getNameMappings() { return nameMappings; }

    /** Called by FloydAddonsConfig when loading name mappings from disk. */
    static void setNameMappingsRaw(Map<String, String> raw) {
        nameMappings = Map.copyOf(raw);
    }

    public static Path getNamesConfigPath() {
        return FloydAddonsConfig.getNamesPath();
    }

    /** Convenience: delegates to the unified config. */
    public static void save() { FloydAddonsConfig.save(); }
    public static void loadNameMappings() { FloydAddonsConfig.loadNameMappings(); }
}
