package floydaddons.not.dogshit.client;

import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Overrides Hypixel map art with a single local PNG/GIF that can span multiple connected maps.
 *
 * Drop one image in config/floydaddons/maps. If multiple files exist, the first alphabetically is used.
 * The image is scaled to cover the full detected map grid (based on map centers/scale) and split per map.
 */
public final class MapOverrideManager {
    private static final int MAP_SIZE = 128; // map pixel resolution
    private static final int[] PALETTE = new int[256];
    private static final Path MAP_DIR = FloydAddonsConfig.getConfigDir().resolve("maps");
    private static final long RESCAN_MS = 3_000L;

    private static volatile List<BufferedImage> frames = List.of();
    private static volatile int[] delays = new int[0];
    private static volatile int frameIndex = 0;
    private static volatile long lastSwitchMs = 0L;
    private static volatile boolean loaded = false;
    private static volatile long lastLoadMs = 0L;

    // map grid tracking
    private static final ConcurrentMap<Integer, Tile> TILES = new ConcurrentHashMap<>();
    private static volatile int originX = Integer.MAX_VALUE;
    private static volatile int originZ = Integer.MAX_VALUE;
    private static volatile int maxCol = 0;
    private static volatile int maxRow = 0;
    private static volatile int gridStep = MAP_SIZE; // 128 << scale, assumed uniform
    private static volatile int gridWidth = 1;
    private static volatile int gridHeight = 1;
    private static volatile boolean gridDirty = true;

    // cached scaled frame for current grid/frame
    private static volatile byte[] cachedBigFrame = new byte[0];
    private static volatile int cachedBigW = 0;
    private static volatile int cachedBigH = 0;
    private static volatile int cachedFrameIdx = -1;
    private static volatile int cachedGridW = -1;
    private static volatile int cachedGridH = -1;
    private static volatile int tileCounter = 0;

    static {
        for (int i = 0; i < 256; i++) {
            PALETTE[i] = MapColor.getRenderColor(i);
        }
    }

    private MapOverrideManager() {}

    public static void tick() {
        if (!RenderConfig.isMapOverrideEnabled()) return;
        if (frames.size() <= 1) return;
        long now = System.currentTimeMillis();
        int delay = delays[Math.max(0, Math.min(frameIndex, delays.length - 1))];
        if (delay <= 0) delay = 100;
        if (now - lastSwitchMs < delay) return;
        frameIndex = (frameIndex + 1) % frames.size();
        lastSwitchMs = now;
        cachedFrameIdx = -1; // force rebuild on next apply
    }

    public static void maybeApply(MapIdComponent mapId, MapState state) {
        try {
            if (!RenderConfig.isMapOverrideEnabled()) return;
            if (!isHypixel()) return;
            ensureLoaded();
            if (frames.isEmpty()) return;
            if (!registerTile(mapId, state)) return;
            refreshBigFrameIfNeeded();
            int expected = cachedBigW * cachedBigH;
            if (cachedBigFrame.length < expected || expected == 0) return;

            Tile tile = TILES.get(mapId.id());
            if (tile == null) return; // should not happen, but safety
            int col = tile.col;
            int row = tile.row;
            if (col < 0 || row < 0 || col >= gridWidth || row >= gridHeight) return;

            byte[] mapBytes = new byte[MAP_SIZE * MAP_SIZE];
            int startX = col * MAP_SIZE;
            int startY = row * MAP_SIZE;
            int bigW = cachedBigW;
            byte[] src = cachedBigFrame;
            for (int y = 0; y < MAP_SIZE; y++) {
                int srcPos = (startY + y) * bigW + startX;
                if (srcPos < 0 || srcPos + MAP_SIZE > src.length) return; // guard against OOB
                System.arraycopy(src, srcPos, mapBytes, y * MAP_SIZE, MAP_SIZE);
            }
            state.colors = mapBytes;
        } catch (Exception ignored) {
            // Never crash the render thread over map overrides.
        }
    }

    private static boolean registerTile(MapIdComponent id, MapState state) {
        int step = MAP_SIZE << state.scale;
        int manualW = RenderConfig.getMapOverrideCols();
        int manualH = RenderConfig.getMapOverrideRows();

        if (manualW > 0 && manualH > 0) {
            gridStep = step;
            gridWidth = manualW;
            gridHeight = manualH;
            int area = manualW * manualH;
            if (area <= 0) return false;
            // If map ids churn (e.g., after warp) and we exceeded area, start fresh.
            if (TILES.size() > area || tileCounter >= area) {
                TILES.clear();
                tileCounter = 0;
                gridDirty = true;
            }
        } else {
            if (TILES.isEmpty()) {
                gridStep = step;
                originX = snapOrigin(state.centerX, gridStep);
                originZ = snapOrigin(state.centerZ, gridStep);
            } else if (step != gridStep) {
                return false;
            }
        }

        int mapId = id.id();
        if (TILES.containsKey(mapId)) return true;

        int col;
        int row;
        if (manualW > 0 && manualH > 0) {
            int area = manualW * manualH;
            int idx = tileCounter++;
            if (idx >= area) {
                // Too many maps for declared grid; ignore extras to avoid OOB.
                return false;
            }
            col = idx % manualW;
            row = idx / manualW;
        } else {
            // center-based placement
            col = (state.centerX - originX + gridStep / 2) / gridStep;
            row = (state.centerZ - originZ + gridStep / 2) / gridStep;
            maxCol = Math.max(maxCol, col);
            maxRow = Math.max(maxRow, row);
            gridWidth = maxCol + 1;
            gridHeight = maxRow + 1;
        }

        TILES.put(mapId, new Tile(mapId, col, row, state.scale));
        gridDirty = true;
        return true;
    }

    private static void refreshBigFrameIfNeeded() {
        if (frames.isEmpty()) return;
        if (!gridDirty && cachedFrameIdx == frameIndex && cachedGridW == gridWidth && cachedGridH == gridHeight) return;

        int bigW = gridWidth * MAP_SIZE;
        int bigH = gridHeight * MAP_SIZE;
        if (bigW <= 0 || bigH <= 0) return;
        BufferedImage src = frames.get(frameIndex);
        if (src == null || src.getWidth() <= 0 || src.getHeight() <= 0) return;
        BufferedImage scaled = scale(src, bigW, bigH);
        cachedBigFrame = toMapBytes(scaled);
        cachedBigW = bigW;
        cachedBigH = bigH;
        cachedFrameIdx = frameIndex;
        cachedGridW = gridWidth;
        cachedGridH = gridHeight;
        gridDirty = false;
    }

    private static boolean isHypixel() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.isIntegratedServerRunning()) return false;
        var server = mc.getCurrentServerEntry();
        if (server == null || server.address == null) return false;
        String addr = server.address.toLowerCase(Locale.ROOT);
        return addr.contains("hypixel.net");
    }

    private static void ensureLoaded() {
        long now = System.currentTimeMillis();
        if (!loaded || frames.isEmpty() || now - lastLoadMs > RESCAN_MS) {
            loadFirstImage();
            loaded = true;
            lastLoadMs = now;
            gridDirty = true;
        }
    }

    private static void loadFirstImage() {
        frames = List.of();
        delays = new int[0];
        frameIndex = 0;
        lastSwitchMs = System.currentTimeMillis();
        cachedFrameIdx = -1;
        cachedBigFrame = new byte[0];
        originX = Integer.MAX_VALUE;
        originZ = Integer.MAX_VALUE;
        maxCol = maxRow = 0;
        gridWidth = gridHeight = 1;
        tileCounter = 0;
        TILES.clear();

        Path dir = ensureDir();
        try (var stream = Files.list(dir)) {
            Path file = stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return n.endsWith(".png") || n.endsWith(".gif");
                    })
                    .sorted()
                    .findFirst()
                    .orElse(null);
            if (file == null) return;
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".gif")) {
                loadGif(file);
            } else {
                loadPng(file);
            }
        } catch (IOException ignored) {}
    }

    private static void loadPng(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) return;
            frames = List.of(img);
            delays = new int[]{100};
        } catch (IOException ignored) {}
    }

    private static void loadGif(Path file) {
        List<BufferedImage> loadedFrames = new ArrayList<>();
        List<Integer> loadedDelays = new ArrayList<>();

        try (InputStream in = Files.newInputStream(file);
             ImageInputStream imageStream = ImageIO.createImageInputStream(in)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext() || imageStream == null) return;
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageStream, false);
                int frameCount = reader.getNumImages(true);
                if (frameCount <= 0) return;

                int canvasW = reader.getWidth(0);
                int canvasH = reader.getHeight(0);
                BufferedImage composite = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = composite.createGraphics();
                g.setComposite(AlphaComposite.Src);
                for (int i = 0; i < frameCount; i++) {
                    IIOMetadata meta = reader.getImageMetadata(i);
                    BufferedImage raw = reader.read(i);
                    if (raw == null) continue;

                    int frameX = getIntAttr(meta, "imageLeftPosition", 0);
                    int frameY = getIntAttr(meta, "imageTopPosition", 0);
                    String disposal = getDisposal(meta);
                    int delay = readDelay(meta);

                    BufferedImage before = null;
                    if ("restoreToPrevious".equals(disposal)) {
                        before = deepCopy(composite);
                    }
                    if ("restoreToBackgroundColor".equals(disposal)) {
                        g.clearRect(frameX, frameY, raw.getWidth(), raw.getHeight());
                    }

                    g.drawImage(raw, frameX, frameY, null);

                    loadedFrames.add(deepCopy(composite));
                    loadedDelays.add(delay);

                    if ("restoreToBackgroundColor".equals(disposal)) {
                        g.clearRect(frameX, frameY, raw.getWidth(), raw.getHeight());
                    } else if ("restoreToPrevious".equals(disposal) && before != null) {
                        g.setComposite(AlphaComposite.Src);
                        g.drawImage(before, 0, 0, null);
                    }
                }
                g.dispose();
            } finally {
                reader.dispose();
            }
        } catch (IOException ignored) {}

        if (!loadedFrames.isEmpty()) {
            frames = loadedFrames;
            delays = loadedDelays.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    private static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static byte[] toMapBytes(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        byte[] out = new byte[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a < 8) {
                    out[x + y * w] = 0; // transparent
                    continue;
                }
                out[x + y * w] = nearestColor(argb);
            }
        }
        return out;
    }

    private static byte nearestColor(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int best = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < PALETTE.length; i++) {
            int c = PALETTE[i];
            if (((c >>> 24) & 0xFF) < 16) continue; // skip transparent palette entries
            int cr = (c >> 16) & 0xFF;
            int cg = (c >> 8) & 0xFF;
            int cb = c & 0xFF;
            int dr = cr - r;
            int dg = cg - g;
            int db = cb - b;
            int dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return (byte) best;
    }

    private static int readDelay(IIOMetadata meta) {
        try {
            String format = meta.getNativeMetadataFormatName();
            if (format == null) return 100;
            var root = meta.getAsTree(format);
            var gce = findNode(root, "GraphicControlExtension");
            if (gce != null && gce.getAttributes() != null) {
                var attrs = gce.getAttributes();
                var delayNode = attrs.getNamedItem("delayTime");
                if (delayNode != null) {
                    int hundredths = Integer.parseInt(delayNode.getNodeValue());
                    int ms = hundredths * 10;
                    return ms > 0 ? ms : 100;
                }
            }
        } catch (Exception ignored) {}
        return 100;
    }

    private static String getDisposal(IIOMetadata meta) {
        try {
            String format = meta.getNativeMetadataFormatName();
            if (format == null) return "none";
            var root = meta.getAsTree(format);
            var gce = findNode(root, "GraphicControlExtension");
            if (gce != null && gce.getAttributes() != null) {
                var attrs = gce.getAttributes();
                var node = attrs.getNamedItem("disposalMethod");
                if (node != null) return node.getNodeValue();
            }
        } catch (Exception ignored) {}
        return "none";
    }

    private static int getIntAttr(IIOMetadata meta, String attr, int def) {
        try {
            String format = meta.getNativeMetadataFormatName();
            if (format == null) return def;
            var root = meta.getAsTree(format);
            var desc = findNode(root, "ImageDescriptor");
            if (desc != null && desc.getAttributes() != null) {
                var attrs = desc.getAttributes();
                var node = attrs.getNamedItem(attr);
                if (node != null) {
                    return Integer.parseInt(node.getNodeValue());
                }
            }
        } catch (Exception ignored) {}
        return def;
    }

    private static org.w3c.dom.Node findNode(org.w3c.dom.Node node, String name) {
        if (node == null) return null;
        if (name.equals(node.getNodeName())) return node;
        for (org.w3c.dom.Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            org.w3c.dom.Node found = findNode(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private static BufferedImage deepCopy(BufferedImage src) {
        ColorModel cm = src.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = src.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /** Public so UI can show/open the folder. */
    public static Path ensureDir() {
        try { Files.createDirectories(MAP_DIR); } catch (IOException ignored) {}
        return MAP_DIR;
    }

    private static int snapOrigin(int center, int step) {
        // snap down to nearest multiple of step to stabilize grid origin
        return Math.floorDiv(center, step) * step;
    }

    private static final class Tile {
        final int id;
        final int col;
        final int row;
        final int scale;
        Tile(int id, int col, int row, int scale) {
            this.id = id;
            this.col = col;
            this.row = row;
            this.scale = scale;
        }
    }
}
