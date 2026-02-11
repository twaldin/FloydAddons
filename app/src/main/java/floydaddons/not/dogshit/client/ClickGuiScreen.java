package floydaddons.not.dogshit.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OdinClient-style ClickGUI with draggable vertical panels, one per category.
 * Left-click toggles modules, right-click expands sub-settings.
 */
public class ClickGuiScreen extends Screen {
    private static final int PANEL_WIDTH = 200;
    private static final int HEADER_HEIGHT = 24;
    private static final int MODULE_HEIGHT = 22;
    private static final int SETTING_HEIGHT = 18;
    private static final int SEARCH_BAR_HEIGHT = 22;
    private static final int SEARCH_BAR_WIDTH = 200;
    private static final long FADE_DURATION_MS = 120;

    private static final int COLOR_HEADER = 0xFF1A1A1A;
    private static final int COLOR_MODULE = 0xFF2A2A2A;
    private static final int COLOR_MODULE_HOVER = 0xFF353535;
    private static final int COLOR_MODULE_ENABLED = 0xFF2A3A2A;
    private static final int COLOR_SETTING_BG = 0xFF222222;
    private static final int COLOR_SLIDER_BG = 0xFF333333;
    private static final int COLOR_SLIDER_FILL = 0xFF4488FF;

    private final Map<ModuleCategory, List<ModuleEntry>> modules = new LinkedHashMap<>();
    private ModuleCategory draggingPanel = null;
    private int dragOffsetX, dragOffsetY;
    private String expandedModule = null;
    private String searchQuery = "";
    private boolean searchFocused = false;
    private ModuleEntry.SliderSetting draggingSlider = null;
    private int draggingSliderX, draggingSliderWidth;

    private long openStartMs;
    private long closeStartMs;
    private boolean closing;

    public ClickGuiScreen() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    protected void init() {
        openStartMs = Util.getMeasuringTimeMs();
        closing = false;
        expandedModule = null;
        searchQuery = "";
        searchFocused = false;
        draggingSlider = null;
        initModules();
    }

    private void initModules() {
        modules.clear();

        // --- RENDER ---
        List<ModuleEntry> render = new ArrayList<>();
        render.add(new ModuleEntry("X-Ray", "Toggle X-Ray vision",
                RenderConfig::isXrayEnabled, RenderConfig::toggleXray,
                List.of(new ModuleEntry.SliderSetting("Opacity", RenderConfig::getXrayOpacity,
                        RenderConfig::setXrayOpacity, 0.05f, 1.0f, "%.0f%%") {
                    @Override public String getFormattedValue() { return String.format("%.0f%%", getValue() * 100); }
                })));
        render.add(new ModuleEntry("Mob ESP", "Highlight mobs through walls",
                RenderConfig::isMobEspEnabled, RenderConfig::toggleMobEsp,
                List.of(
                        new ModuleEntry.BooleanSetting("Tracers", RenderConfig::isMobEspTracers,
                                () -> RenderConfig.setMobEspTracers(!RenderConfig.isMobEspTracers())),
                        new ModuleEntry.BooleanSetting("Hitboxes", RenderConfig::isMobEspHitboxes,
                                () -> RenderConfig.setMobEspHitboxes(!RenderConfig.isMobEspHitboxes())),
                        new ModuleEntry.BooleanSetting("Star Mobs", RenderConfig::isMobEspStarMobs,
                                () -> RenderConfig.setMobEspStarMobs(!RenderConfig.isMobEspStarMobs()))
                )));
        render.add(new ModuleEntry("Cone Hat", "Floyd cone hat cosmetic",
                RenderConfig::isFloydHatEnabled,
                () -> RenderConfig.setFloydHatEnabled(!RenderConfig.isFloydHatEnabled())));
        render.add(new ModuleEntry("Server ID Hider", "Hide server address display",
                RenderConfig::isServerIdHiderEnabled,
                () -> RenderConfig.setServerIdHiderEnabled(!RenderConfig.isServerIdHiderEnabled())));
        render.add(new ModuleEntry("Inventory HUD", "Show inventory overlay",
                RenderConfig::isInventoryHudEnabled,
                () -> RenderConfig.setInventoryHudEnabled(!RenderConfig.isInventoryHudEnabled())));
        render.add(new ModuleEntry("Custom Scoreboard", "Styled scoreboard sidebar",
                RenderConfig::isCustomScoreboardEnabled,
                () -> RenderConfig.setCustomScoreboardEnabled(!RenderConfig.isCustomScoreboardEnabled())));
        modules.put(ModuleCategory.RENDER, render);

        // --- HIDERS ---
        List<ModuleEntry> hiders = new ArrayList<>();
        addHiderToggle(hiders, "Fullbright", "Maximum gamma", HidersConfig::isFullbrightEnabled,
                () -> HidersConfig.setFullbrightEnabled(!HidersConfig.isFullbrightEnabled()));
        addHiderToggle(hiders, "Disable Fog", "Remove fog rendering", HidersConfig::isDisableFogEnabled,
                () -> HidersConfig.setDisableFogEnabled(!HidersConfig.isDisableFogEnabled()));
        addHiderToggle(hiders, "Disable Blindness", "Remove blindness effect", HidersConfig::isDisableBlindnessEnabled,
                () -> HidersConfig.setDisableBlindnessEnabled(!HidersConfig.isDisableBlindnessEnabled()));
        addHiderToggle(hiders, "No Hurt Camera", "Remove damage camera shake", HidersConfig::isNoHurtCameraEnabled,
                () -> HidersConfig.setNoHurtCameraEnabled(!HidersConfig.isNoHurtCameraEnabled()));
        addHiderToggle(hiders, "Remove Fire Overlay", "Hide fire screen overlay", HidersConfig::isRemoveFireOverlayEnabled,
                () -> HidersConfig.setRemoveFireOverlayEnabled(!HidersConfig.isRemoveFireOverlayEnabled()));
        addHiderToggle(hiders, "Remove Water Overlay", "Hide underwater overlay", HidersConfig::isRemoveWaterOverlayEnabled,
                () -> HidersConfig.setRemoveWaterOverlayEnabled(!HidersConfig.isRemoveWaterOverlayEnabled()));
        addHiderToggle(hiders, "Remove Suffocation", "Hide suffocation overlay", HidersConfig::isRemoveSuffocationOverlayEnabled,
                () -> HidersConfig.setRemoveSuffocationOverlayEnabled(!HidersConfig.isRemoveSuffocationOverlayEnabled()));
        addHiderToggle(hiders, "Disable Vignette", "Remove screen edge darkening", HidersConfig::isDisableVignetteEnabled,
                () -> HidersConfig.setDisableVignetteEnabled(!HidersConfig.isDisableVignetteEnabled()));
        addHiderToggle(hiders, "Disable Hunger Bar", "Hide hunger display", HidersConfig::isDisableHungerBarEnabled,
                () -> HidersConfig.setDisableHungerBarEnabled(!HidersConfig.isDisableHungerBarEnabled()));
        addHiderToggle(hiders, "Hide Potion Effects", "Hide potion effect icons", HidersConfig::isHidePotionEffectsEnabled,
                () -> HidersConfig.setHidePotionEffectsEnabled(!HidersConfig.isHidePotionEffectsEnabled()));
        addHiderToggle(hiders, "3rd Person Crosshair", "Show crosshair in 3rd person", HidersConfig::isThirdPersonCrosshairEnabled,
                () -> HidersConfig.setThirdPersonCrosshairEnabled(!HidersConfig.isThirdPersonCrosshairEnabled()));
        addHiderToggle(hiders, "Hide Entity Fire", "Hide fire on entities", HidersConfig::isHideEntityFireEnabled,
                () -> HidersConfig.setHideEntityFireEnabled(!HidersConfig.isHideEntityFireEnabled()));
        addHiderToggle(hiders, "Disable Arrows", "Hide arrows stuck in models", HidersConfig::isDisableAttachedArrowsEnabled,
                () -> HidersConfig.setDisableAttachedArrowsEnabled(!HidersConfig.isDisableAttachedArrowsEnabled()));
        addHiderToggle(hiders, "No Death Animation", "Remove entity death tilt", HidersConfig::isNoDeathAnimationEnabled,
                () -> HidersConfig.setNoDeathAnimationEnabled(!HidersConfig.isNoDeathAnimationEnabled()));
        addHiderToggle(hiders, "Remove Falling Blocks", "Hide falling block entities", HidersConfig::isRemoveFallingBlocksEnabled,
                () -> HidersConfig.setRemoveFallingBlocksEnabled(!HidersConfig.isRemoveFallingBlocksEnabled()));
        addHiderToggle(hiders, "Remove Lightning", "Hide lightning bolts", HidersConfig::isRemoveLightningEnabled,
                () -> HidersConfig.setRemoveLightningEnabled(!HidersConfig.isRemoveLightningEnabled()));
        addHiderToggle(hiders, "No Block Particles", "Hide block break particles", HidersConfig::isRemoveBlockBreakParticlesEnabled,
                () -> HidersConfig.setRemoveBlockBreakParticlesEnabled(!HidersConfig.isRemoveBlockBreakParticlesEnabled()));
        addHiderToggle(hiders, "No Explosion Particles", "Hide explosion particles", HidersConfig::isRemoveExplosionParticlesEnabled,
                () -> HidersConfig.setRemoveExplosionParticlesEnabled(!HidersConfig.isRemoveExplosionParticlesEnabled()));
        addHiderToggle(hiders, "Remove Tab Ping", "Hide ping icons in tab list", HidersConfig::isRemoveTabPingEnabled,
                () -> HidersConfig.setRemoveTabPingEnabled(!HidersConfig.isRemoveTabPingEnabled()));
        addHiderToggle(hiders, "No Nametag BG", "Remove nametag background", HidersConfig::isDisableNametagBackgroundEnabled,
                () -> HidersConfig.setDisableNametagBackgroundEnabled(!HidersConfig.isDisableNametagBackgroundEnabled()));
        addHiderToggle(hiders, "Remove Glow", "Remove entity glow outlines", HidersConfig::isRemoveGlowEffectEnabled,
                () -> HidersConfig.setRemoveGlowEffectEnabled(!HidersConfig.isRemoveGlowEffectEnabled()));
        addHiderToggle(hiders, "Hide Ground Arrows", "Hide arrow entities", HidersConfig::isHideGroundedArrowsEnabled,
                () -> HidersConfig.setHideGroundedArrowsEnabled(!HidersConfig.isHideGroundedArrowsEnabled()));
        addHiderToggle(hiders, "Cancel Bad Sound", "Mute incorrect action sound", HidersConfig::isCancelIncorrectSoundEnabled,
                () -> HidersConfig.setCancelIncorrectSoundEnabled(!HidersConfig.isCancelIncorrectSoundEnabled()));
        modules.put(ModuleCategory.HIDERS, hiders);

        // --- PLAYER ---
        List<ModuleEntry> player = new ArrayList<>();
        player.add(new ModuleEntry("Nick Hider", "Hide/replace player names",
                NickHiderConfig::isEnabled,
                () -> NickHiderConfig.setEnabled(!NickHiderConfig.isEnabled())));
        player.add(new ModuleEntry("Skin Swap", "Use custom player skin",
                () -> SkinConfig.selfEnabled() || SkinConfig.othersEnabled(),
                () -> {
                    boolean newState = !(SkinConfig.selfEnabled() || SkinConfig.othersEnabled());
                    SkinConfig.setSelfEnabled(newState);
                    SkinConfig.setOthersEnabled(newState);
                },
                List.of(
                        new ModuleEntry.BooleanSetting("Self", SkinConfig::selfEnabled,
                                () -> SkinConfig.setSelfEnabled(!SkinConfig.selfEnabled())),
                        new ModuleEntry.BooleanSetting("Others", SkinConfig::othersEnabled,
                                () -> SkinConfig.setOthersEnabled(!SkinConfig.othersEnabled()))
                )));
        modules.put(ModuleCategory.PLAYER, player);
    }

    private static void addHiderToggle(List<ModuleEntry> list, String name, String desc,
                                        java.util.function.BooleanSupplier getter, Runnable toggle) {
        list.add(new ModuleEntry(name, desc, getter, toggle));
    }

    // --- Rendering ---

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float openProgress = Math.min(1.0f, (Util.getMeasuringTimeMs() - openStartMs) / (float) FADE_DURATION_MS);
        float closeProgress = closing ? Math.min(1.0f, (Util.getMeasuringTimeMs() - closeStartMs) / (float) FADE_DURATION_MS) : 0f;
        if (closing && closeProgress >= 1.0f) {
            super.close();
            return;
        }
        float alpha = closing ? (1.0f - closeProgress) : openProgress;
        if (alpha <= 0f) return;

        // Dark overlay background
        context.fill(0, 0, width, height, applyAlpha(0x88000000, alpha));

        // Search bar
        renderSearchBar(context, mouseX, mouseY, alpha);

        // Render each category panel
        for (var entry : modules.entrySet()) {
            renderPanel(context, entry.getKey(), entry.getValue(), mouseX, mouseY, alpha);
        }
    }

    private void renderSearchBar(DrawContext context, int mouseX, int mouseY, float alpha) {
        int x = (width - SEARCH_BAR_WIDTH) / 2;
        int y = 4;
        context.fill(x, y, x + SEARCH_BAR_WIDTH, y + SEARCH_BAR_HEIGHT, applyAlpha(0xFF1A1A1A, alpha));
        InventoryHudRenderer.drawChromaBorder(context, x - 1, y - 1, x + SEARCH_BAR_WIDTH + 1, y + SEARCH_BAR_HEIGHT + 1, alpha);

        String display = searchQuery.isEmpty() && !searchFocused ? "Search..." : searchQuery + (searchFocused ? "_" : "");
        int textColor = searchQuery.isEmpty() && !searchFocused ? applyAlpha(0xFF888888, alpha) : applyAlpha(0xFFFFFFFF, alpha);
        context.drawTextWithShadow(textRenderer, display, x + 6, y + (SEARCH_BAR_HEIGHT - textRenderer.fontHeight) / 2, textColor);
    }

    private void renderPanel(DrawContext context, ModuleCategory category, List<ModuleEntry> entries,
                             int mouseX, int mouseY, float alpha) {
        ClickGuiConfig.PanelState state = ClickGuiConfig.getState(category);
        int px = state.x;
        int py = state.y;

        List<ModuleEntry> filtered = filterModules(entries);
        int contentHeight = calculateContentHeight(filtered);
        int totalHeight = HEADER_HEIGHT + (state.collapsed ? 0 : contentHeight);

        // Panel background
        context.fill(px, py, px + PANEL_WIDTH, py + totalHeight, applyAlpha(COLOR_HEADER, alpha));

        // Header
        renderHeader(context, category, px, py, mouseX, mouseY, alpha, state.collapsed);

        if (!state.collapsed && !filtered.isEmpty()) {
            // Content area
            int contentTop = py + HEADER_HEIGHT;
            int contentBottom = py + totalHeight;
            context.enableScissor(px, contentTop, px + PANEL_WIDTH, contentBottom);

            int moduleY = contentTop - (int) state.scrollOffset;
            for (ModuleEntry entry : filtered) {
                renderModule(context, entry, px, moduleY, mouseX, mouseY, alpha);
                moduleY += MODULE_HEIGHT;

                if (entry.getName().equals(expandedModule) && entry.hasSettings()) {
                    for (ModuleEntry.SubSetting setting : entry.getSettings()) {
                        renderSetting(context, setting, px, moduleY, mouseX, mouseY, alpha);
                        moduleY += SETTING_HEIGHT;
                    }
                }
            }

            context.disableScissor();
        }

        // Chroma border around entire panel
        InventoryHudRenderer.drawChromaBorder(context, px - 1, py - 1, px + PANEL_WIDTH + 1, py + totalHeight + 1, alpha);
    }

    private void renderHeader(DrawContext context, ModuleCategory category, int x, int y,
                               int mouseX, int mouseY, float alpha, boolean collapsed) {
        boolean hover = mouseX >= x && mouseX <= x + PANEL_WIDTH && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
        int headerColor = applyAlpha(hover ? 0xFF222222 : COLOR_HEADER, alpha);
        context.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, headerColor);

        // Category name in chroma
        String name = category.getDisplayName() + (collapsed ? " [+]" : "");
        int textColor = applyAlpha(chromaColor(0f), alpha);
        int textX = x + (PANEL_WIDTH - textRenderer.getWidth(name)) / 2;
        int textY = y + (HEADER_HEIGHT - textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(textRenderer, name, textX, textY, textColor);
    }

    private void renderModule(DrawContext context, ModuleEntry entry, int px, int y,
                               int mouseX, int mouseY, float alpha) {
        boolean hover = mouseX >= px && mouseX <= px + PANEL_WIDTH && mouseY >= y && mouseY <= y + MODULE_HEIGHT;
        boolean enabled = entry.isEnabled();
        int bgColor;
        if (hover) {
            bgColor = enabled ? applyAlpha(0xFF2A4A2A, alpha) : applyAlpha(COLOR_MODULE_HOVER, alpha);
        } else {
            bgColor = enabled ? applyAlpha(COLOR_MODULE_ENABLED, alpha) : applyAlpha(COLOR_MODULE, alpha);
        }
        context.fill(px, y, px + PANEL_WIDTH, y + MODULE_HEIGHT, bgColor);

        // Module name
        int nameColor = enabled ? applyAlpha(chromaColor(0f), alpha) : applyAlpha(0xFFCCCCCC, alpha);
        context.drawTextWithShadow(textRenderer, entry.getName(), px + 8, y + (MODULE_HEIGHT - textRenderer.fontHeight) / 2, nameColor);

        // Status indicator
        String status = enabled ? "ON" : "OFF";
        int statusColor = enabled ? applyAlpha(0xFF44FF44, alpha) : applyAlpha(0xFF666666, alpha);
        int statusX = px + PANEL_WIDTH - textRenderer.getWidth(status) - 8;
        context.drawTextWithShadow(textRenderer, status, statusX, y + (MODULE_HEIGHT - textRenderer.fontHeight) / 2, statusColor);

        // Expand indicator if module has settings
        if (entry.hasSettings()) {
            String arrow = entry.getName().equals(expandedModule) ? "v" : ">";
            int arrowColor = applyAlpha(0xFF888888, alpha);
            context.drawTextWithShadow(textRenderer, arrow, px + PANEL_WIDTH - 30 - textRenderer.getWidth(status), y + (MODULE_HEIGHT - textRenderer.fontHeight) / 2, arrowColor);
        }
    }

    private void renderSetting(DrawContext context, ModuleEntry.SubSetting setting, int px, int y,
                                int mouseX, int mouseY, float alpha) {
        context.fill(px, y, px + PANEL_WIDTH, y + SETTING_HEIGHT, applyAlpha(COLOR_SETTING_BG, alpha));

        if (setting instanceof ModuleEntry.BooleanSetting boolSetting) {
            // Indented boolean toggle
            String label = "  " + setting.getLabel();
            boolean on = boolSetting.isEnabled();
            int labelColor = on ? applyAlpha(0xFF88FF88, alpha) : applyAlpha(0xFFAAAAAA, alpha);
            context.drawTextWithShadow(textRenderer, label, px + 14, y + (SETTING_HEIGHT - textRenderer.fontHeight) / 2, labelColor);

            String val = on ? "ON" : "OFF";
            int valColor = on ? applyAlpha(0xFF44FF44, alpha) : applyAlpha(0xFF666666, alpha);
            context.drawTextWithShadow(textRenderer, val, px + PANEL_WIDTH - textRenderer.getWidth(val) - 8,
                    y + (SETTING_HEIGHT - textRenderer.fontHeight) / 2, valColor);

        } else if (setting instanceof ModuleEntry.SliderSetting slider) {
            // Indented slider
            String label = "  " + setting.getLabel() + ": " + slider.getFormattedValue();
            context.drawTextWithShadow(textRenderer, label, px + 14, y + 2, applyAlpha(0xFFAAAAAA, alpha));

            int barX = px + 14;
            int barY = y + SETTING_HEIGHT - 5;
            int barW = PANEL_WIDTH - 28;
            context.fill(barX, barY, barX + barW, barY + 3, applyAlpha(COLOR_SLIDER_BG, alpha));
            int fillW = (int) (barW * slider.getNormalized());
            context.fill(barX, barY, barX + fillW, barY + 3, applyAlpha(chromaColor(0f), alpha));

        } else if (setting instanceof ModuleEntry.ButtonSetting) {
            String label = "  [" + setting.getLabel() + "]";
            boolean hover = mouseX >= px && mouseX <= px + PANEL_WIDTH && mouseY >= y && mouseY <= y + SETTING_HEIGHT;
            int color = hover ? applyAlpha(chromaColor(0f), alpha) : applyAlpha(0xFFAAAAAA, alpha);
            context.drawTextWithShadow(textRenderer, label, px + 14, y + (SETTING_HEIGHT - textRenderer.fontHeight) / 2, color);
        }
    }

    // --- Input Handling ---

    @Override
    public boolean mouseClicked(Click click, boolean ignoresInput) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();

        // Search bar click
        int searchX = (width - SEARCH_BAR_WIDTH) / 2;
        int searchY = 4;
        if (mx >= searchX && mx <= searchX + SEARCH_BAR_WIDTH && my >= searchY && my <= searchY + SEARCH_BAR_HEIGHT) {
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }

        // Check each panel
        for (var entry : modules.entrySet()) {
            ModuleCategory category = entry.getKey();
            List<ModuleEntry> entries = entry.getValue();
            ClickGuiConfig.PanelState state = ClickGuiConfig.getState(category);
            int px = state.x;
            int py = state.y;

            // Check bounds
            if (mx < px || mx > px + PANEL_WIDTH) continue;

            // Header click — left: start drag, right: toggle collapse
            if (my >= py && my <= py + HEADER_HEIGHT) {
                if (button == 1) {
                    state.collapsed = !state.collapsed;
                    return true;
                }
                draggingPanel = category;
                dragOffsetX = (int) (mx - px);
                dragOffsetY = (int) (my - py);
                return true;
            }

            if (state.collapsed) continue;

            // Module clicks
            List<ModuleEntry> filtered = filterModules(entries);
            int contentHeight = calculateContentHeight(filtered);
            int totalHeight = HEADER_HEIGHT + contentHeight;
            if (my < py + HEADER_HEIGHT || my > py + totalHeight) continue;

            int moduleY = py + HEADER_HEIGHT - (int) state.scrollOffset;
            for (ModuleEntry mod : filtered) {
                if (my >= moduleY && my < moduleY + MODULE_HEIGHT) {
                    if (button == 0) {
                        mod.toggle();
                        FloydAddonsConfig.save();
                    } else if (button == 1 && mod.hasSettings()) {
                        expandedModule = mod.getName().equals(expandedModule) ? null : mod.getName();
                    }
                    return true;
                }
                moduleY += MODULE_HEIGHT;

                // Sub-settings clicks
                if (mod.getName().equals(expandedModule) && mod.hasSettings()) {
                    for (ModuleEntry.SubSetting setting : mod.getSettings()) {
                        if (my >= moduleY && my < moduleY + SETTING_HEIGHT) {
                            if (button == 0) {
                                handleSettingClick(setting, mx, px, moduleY);
                            }
                            return true;
                        }
                        moduleY += SETTING_HEIGHT;
                    }
                }
            }
        }

        return super.mouseClicked(click, ignoresInput);
    }

    private void handleSettingClick(ModuleEntry.SubSetting setting, double mx, int px, int y) {
        if (setting instanceof ModuleEntry.BooleanSetting boolSetting) {
            boolSetting.toggle();
            FloydAddonsConfig.save();
        } else if (setting instanceof ModuleEntry.SliderSetting slider) {
            int barX = px + 14;
            int barW = PANEL_WIDTH - 28;
            float t = (float) Math.max(0, Math.min(1, (mx - barX) / barW));
            slider.setNormalized(t);
            draggingSlider = slider;
            draggingSliderX = barX;
            draggingSliderWidth = barW;
            FloydAddonsConfig.save();
        } else if (setting instanceof ModuleEntry.ButtonSetting btnSetting) {
            btnSetting.click();
        }
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mx = click.x();
        double my = click.y();

        // Panel dragging
        if (draggingPanel != null && click.button() == 0) {
            ClickGuiConfig.PanelState state = ClickGuiConfig.getState(draggingPanel);
            state.x = Math.max(0, Math.min(width - PANEL_WIDTH, (int) (mx - dragOffsetX)));
            state.y = Math.max(0, Math.min(height - HEADER_HEIGHT, (int) (my - dragOffsetY)));
            return true;
        }

        // Slider dragging
        if (draggingSlider != null) {
            float t = (float) Math.max(0, Math.min(1, (mx - draggingSliderX) / draggingSliderWidth));
            draggingSlider.setNormalized(t);
            FloydAddonsConfig.save();
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            if (draggingPanel != null) {
                draggingPanel = null;
                FloydAddonsConfig.save();
                return true;
            }
            if (draggingSlider != null) {
                draggingSlider = null;
                return true;
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (var entry : modules.entrySet()) {
            ClickGuiConfig.PanelState state = ClickGuiConfig.getState(entry.getKey());
            int px = state.x;
            int py = state.y;
            List<ModuleEntry> filtered = filterModules(entry.getValue());
            int contentHeight = calculateContentHeight(filtered);
            int totalHeight = HEADER_HEIGHT + contentHeight;

            if (mouseX >= px && mouseX <= px + PANEL_WIDTH && mouseY >= py && mouseY <= py + totalHeight) {
                state.scrollOffset = Math.max(0, Math.min(Math.max(0, contentHeight - 200),
                        state.scrollOffset - (float) verticalAmount * 16));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (searchFocused) {
            if (input.key() == 259 && !searchQuery.isEmpty()) { // Backspace
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            if (input.isEscape()) {
                searchFocused = false;
                searchQuery = "";
                return true;
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchFocused && input.codepoint() >= 32) {
            searchQuery += input.asString();
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        if (closing) return;
        FloydAddonsConfig.save();
        closing = true;
        closeStartMs = Util.getMeasuringTimeMs();
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    // --- Utilities ---

    private List<ModuleEntry> filterModules(List<ModuleEntry> entries) {
        if (searchQuery.isEmpty()) return entries;
        String query = searchQuery.toLowerCase();
        List<ModuleEntry> filtered = new ArrayList<>();
        for (ModuleEntry e : entries) {
            if (e.getName().toLowerCase().contains(query) || e.getDescription().toLowerCase().contains(query)) {
                filtered.add(e);
            }
        }
        return filtered;
    }

    private int calculateContentHeight(List<ModuleEntry> filtered) {
        int h = 0;
        for (ModuleEntry entry : filtered) {
            h += MODULE_HEIGHT;
            if (entry.getName().equals(expandedModule) && entry.hasSettings()) {
                h += entry.getSettings().size() * SETTING_HEIGHT;
            }
        }
        return h;
    }

    private int applyAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private int chromaColor(float offset) {
        double time = (System.currentTimeMillis() % 4000) / 4000.0;
        float hue = (float) ((time + offset) % 1.0);
        int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
