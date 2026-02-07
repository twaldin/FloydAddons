package floydaddons.not.dogshit.client;

/**
 * Lightweight in-memory config for the nick hider feature.
 * Kept simple (no disk persistence) since the requirement is client-only presentation.
 */
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class NickHiderConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("floydaddons-nickhider.json");

    private static String nickname = "George Floyd";
    private static boolean enabled = false;
    private static boolean hideOthers = false;
    private static String othersNickname = "George Floyd";

    // Cached set of other player names, refreshed every second for render-path performance
    private static Set<String> cachedOtherNames = Collections.emptySet();
    private static long lastNameCacheUpdate = 0;

    private NickHiderConfig() {}

    public static String getNickname() {
        return nickname;
    }

    public static void setNickname(String nick) {
        if (nick != null && !nick.isEmpty()) {
            nickname = nick;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isHideOthers() {
        return hideOthers;
    }

    public static void setHideOthers(boolean value) {
        hideOthers = value;
    }

    public static String getOthersNickname() {
        return othersNickname;
    }

    public static void setOthersNickname(String nick) {
        if (nick != null && !nick.isEmpty()) {
            othersNickname = nick;
        }
    }

    public static Set<String> getCachedOtherNames() {
        long now = System.currentTimeMillis();
        if (now - lastNameCacheUpdate > 1000) {
            lastNameCacheUpdate = now;
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.getNetworkHandler() != null && client.getSession() != null) {
                    Set<String> names = new HashSet<>();
                    UUID selfId = client.getSession().getUuidOrNull();
                    for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                        if (entry.getProfile() != null && !entry.getProfile().id().equals(selfId)) {
                            String name = entry.getProfile().name();
                            // Filter out Skyblock fake tab entries (prefixed with !)
                            if (name != null && !name.isEmpty() && !name.startsWith("!")) {
                                names.add(name);
                            }
                        }
                    }
                    cachedOtherNames = names;
                }
            } catch (Exception ignored) {
            }
        }
        return cachedOtherNames;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) return;
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            Data data = GSON.fromJson(r, Data.class);
            if (data != null) {
                if (data.nickname != null && !data.nickname.isEmpty()) nickname = data.nickname;
                enabled = data.enabled;
                hideOthers = data.hideOthers;
                if (data.othersNickname != null && !data.othersNickname.isEmpty()) othersNickname = data.othersNickname;
            }
        } catch (IOException ignored) {
        }
    }

    public static void save() {
        Data data = new Data();
        data.nickname = nickname;
        data.enabled = enabled;
        data.hideOthers = hideOthers;
        data.othersNickname = othersNickname;
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, w);
            }
        } catch (IOException ignored) {
        }
    }

    private static class Data {
        String nickname;
        boolean enabled;
        boolean hideOthers;
        String othersNickname;
    }
}
