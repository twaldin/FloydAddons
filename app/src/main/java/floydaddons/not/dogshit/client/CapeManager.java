package floydaddons.not.dogshit.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles loading cape textures from config/capes.
 */
public final class CapeManager {
    private static final ConcurrentMap<String, Identifier> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Float> ASPECT_CACHE = new ConcurrentHashMap<>();

    private CapeManager() {}

    public static Path ensureDir() {
        Path dir = FloydAddonsConfig.getConfigDir().resolve("capes");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir;
    }

    public static List<String> listAvailableImages() {
        Path dir = ensureDir();
        List<String> names = new ArrayList<>();
        try {
            Files.list(dir).filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted()
                    .forEach(p -> names.add(p.getFileName().toString()));
        } catch (IOException ignored) {}
        return names;
    }

    public static Identifier getTexture(MinecraftClient mc) {
        String selected = resolveSelection();
        final String sel = selected;
        return CACHE.computeIfAbsent(sel.toLowerCase(), k -> loadTexture(mc, sel));
    }

    /** Aspect ratio width/height of the current cape texture (fallback 2.0). */
    public static float getAspectRatio() {
        String selected = resolveSelection().toLowerCase();
        return ASPECT_CACHE.getOrDefault(selected, 2.0f);
    }

    private static String resolveSelection() {
        List<String> images = listAvailableImages();
        String selected = RenderConfig.getSelectedCapeImage();
        if (selected != null && !selected.isEmpty() && images.contains(selected)) {
            return selected;
        }

        if (!images.isEmpty()) {
            String choice = images.get(0);
            if (!choice.equals(selected)) {
                RenderConfig.setSelectedCapeImage(choice);
                RenderConfig.save();
            }
            return choice;
        }

        return "";
    }

    private static Identifier loadTexture(MinecraftClient mc, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return Identifier.of("textures/entity/cape/vanilla"); // fallback
        }
        Path path = ensureDir().resolve(fileName);
        if (!Files.exists(path)) {
            return Identifier.of("textures/entity/cape/vanilla"); // fallback
        }
        try {
            NativeImage img = NativeImage.read(Files.newInputStream(path));
            NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "floydaddons_cape_" + fileName, img);
            // Use linear filtering + mipmaps so higher-res capes stay smooth instead of looking blocky.
            tex.setFilter(true, true);
            Identifier id = Identifier.of(FloydAddonsClient.MOD_ID, "cape/" + fileName.toLowerCase().replaceAll("[^a-z0-9._-]", "_"));
            mc.getTextureManager().registerTexture(id, tex);
            float aspect = img.getWidth() / (float) img.getHeight();
            ASPECT_CACHE.put(fileName.toLowerCase(), aspect > 0 ? aspect : 2.0f);
            return id;
        } catch (IOException e) {
            return Identifier.of("textures/entity/cape/vanilla"); // fallback
        }
    }
}
