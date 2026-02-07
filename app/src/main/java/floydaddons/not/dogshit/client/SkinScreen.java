package floydaddons.not.dogshit.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.List;

public class SkinScreen extends Screen {
    private final Screen parent;
    private ButtonWidget selfToggle;
    private ButtonWidget othersToggle;
    private ButtonWidget doneButton;
    private ButtonWidget openFolderButton;

    private static final int BOX_WIDTH = 320;
    private static final int BOX_HEIGHT = 230;
    private static final long FADE_DURATION_MS = 90;
    private long openStartMs;
    private boolean closing = false;
    private long closeStartMs;
    private static final int DRAG_BAR_HEIGHT = 18;
    private int panelX, panelY;
    private static int savedX = -1, savedY = -1;
    private boolean dragging = false;
    private double dragStartMouseX, dragStartMouseY;
    private int dragStartPanelX, dragStartPanelY;

    // Dropdown state
    private boolean dropdownOpen = false;
    private List<String> availableSkins = List.of();
    private int dropdownScroll = 0;
    private static final int DROPDOWN_ROW_HEIGHT = 16;
    private static final int DROPDOWN_MAX_VISIBLE = 5;

    public SkinScreen(Screen parent) {
        super(Text.literal("Skin"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        openStartMs = Util.getMeasuringTimeMs();
        closing = false;

        savedX = (width - BOX_WIDTH) / 2;
        savedY = (height - BOX_HEIGHT) / 2;
        panelX = savedX;
        panelY = savedY;

        refreshSkinList();

        int cx = panelX + (BOX_WIDTH - 220) / 2;

        selfToggle = ButtonWidget.builder(Text.literal(selfLabel()), b -> {
            SkinConfig.setSelfEnabled(!SkinConfig.selfEnabled());
            b.setMessage(Text.literal(selfLabel()));
            SkinConfig.save();
            SkinManager.clearCache();
        }).dimensions(cx, panelY + 25, 220, 20).build();

        othersToggle = ButtonWidget.builder(Text.literal(othersLabel()), b -> {
            SkinConfig.setOthersEnabled(!SkinConfig.othersEnabled());
            b.setMessage(Text.literal(othersLabel()));
            SkinConfig.save();
            SkinManager.clearCache();
        }).dimensions(cx, panelY + 55, 220, 20).build();

        openFolderButton = ButtonWidget.builder(Text.literal("Open skin folder"), b -> {
            var dir = SkinManager.ensureExternalDir();
            try {
                Util.getOperatingSystem().open(dir.toFile());
            } catch (Exception ignored) {}
        }).dimensions(cx, panelY + 85, 220, 20).build();

        doneButton = ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(panelX + (BOX_WIDTH - 100) / 2, panelY + BOX_HEIGHT - 30, 100, 20)
                .build();

        addDrawableChild(selfToggle);
        addDrawableChild(othersToggle);
        addDrawableChild(openFolderButton);
        addDrawableChild(doneButton);
    }

    private void refreshSkinList() {
        availableSkins = SkinManager.listAvailableSkins();
        dropdownScroll = 0;
    }

    private String selfLabel() { return "Apply to me: " + (SkinConfig.selfEnabled() ? "ON" : "OFF"); }
    private String othersLabel() { return "Others: " + (SkinConfig.othersEnabled() ? "ON" : "OFF"); }

    private String currentSkinLabel() {
        String sel = SkinConfig.getSelectedSkin();
        if (sel == null || sel.isEmpty()) return "No skin selected";
        return sel;
    }

    @Override
    public void close() {
        requestClose();
    }

    private void requestClose() {
        if (closing) return;
        closing = true;
        closeStartMs = Util.getMeasuringTimeMs();
        SkinConfig.save();
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean mouseClicked(Click click, boolean ignoresInput) {
        double mx = click.x();
        double my = click.y();

        // Handle dropdown clicks first (higher priority)
        if (dropdownOpen && click.button() == 0) {
            int ddX = panelX + (BOX_WIDTH - 220) / 2;
            int ddTop = panelY + 130;
            int ddWidth = 220;

            int visibleCount = Math.min(availableSkins.size(), DROPDOWN_MAX_VISIBLE);
            int ddHeight = visibleCount * DROPDOWN_ROW_HEIGHT;

            if (mx >= ddX && mx <= ddX + ddWidth && my >= ddTop && my <= ddTop + ddHeight) {
                int rowIndex = (int) ((my - ddTop) / DROPDOWN_ROW_HEIGHT) + dropdownScroll;
                if (rowIndex >= 0 && rowIndex < availableSkins.size()) {
                    SkinConfig.setSelectedSkin(availableSkins.get(rowIndex));
                    SkinConfig.save();
                    SkinManager.clearCache();
                    dropdownOpen = false;
                    return true;
                }
            }
            // Clicked outside dropdown, close it
            dropdownOpen = false;
            return true;
        }

        // Handle dropdown button click (the skin selector bar)
        if (click.button() == 0) {
            int ddBtnX = panelX + (BOX_WIDTH - 220) / 2;
            int ddBtnY = panelY + 112;
            if (mx >= ddBtnX && mx <= ddBtnX + 220 && my >= ddBtnY && my <= ddBtnY + 16) {
                refreshSkinList();
                dropdownOpen = !dropdownOpen;
                return true;
            }
        }

        // Drag bar
        if (click.button() == 0 && mx >= panelX && mx <= panelX + BOX_WIDTH && my >= panelY && my <= panelY + DRAG_BAR_HEIGHT) {
            dragging = true;
            dragStartMouseX = mx;
            dragStartMouseY = my;
            dragStartPanelX = panelX;
            dragStartPanelY = panelY;
            return true;
        }
        return super.mouseClicked(click, ignoresInput);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (dropdownOpen && availableSkins.size() > DROPDOWN_MAX_VISIBLE) {
            int ddX = panelX + (BOX_WIDTH - 220) / 2;
            int ddTop = panelY + 130;
            int ddWidth = 220;
            int ddHeight = DROPDOWN_MAX_VISIBLE * DROPDOWN_ROW_HEIGHT;
            if (mouseX >= ddX && mouseX <= ddX + ddWidth && mouseY >= ddTop && mouseY <= ddTop + ddHeight) {
                dropdownScroll -= (int) verticalAmount;
                dropdownScroll = Math.max(0, Math.min(dropdownScroll, availableSkins.size() - DROPDOWN_MAX_VISIBLE));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragging && click.button() == 0) {
            int newX = dragStartPanelX + (int) (click.x() - dragStartMouseX);
            int newY = dragStartPanelY + (int) (click.y() - dragStartMouseY);
            newX = Math.max(0, Math.min(newX, width - BOX_WIDTH));
            newY = Math.max(0, Math.min(newY, height - BOX_HEIGHT));
            panelX = savedX = newX;
            panelY = savedY = newY;
            repositionWidgets();
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    private void repositionWidgets() {
        int cx = panelX + (BOX_WIDTH - 220) / 2;
        selfToggle.setX(cx);
        selfToggle.setY(panelY + 25);
        othersToggle.setX(cx);
        othersToggle.setY(panelY + 55);
        openFolderButton.setX(cx);
        openFolderButton.setY(panelY + 85);
        doneButton.setX(panelX + (BOX_WIDTH - 100) / 2);
        doneButton.setY(panelY + BOX_HEIGHT - 30);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float openProgress = Math.min(1.0f, (Util.getMeasuringTimeMs() - openStartMs) / (float) FADE_DURATION_MS);
        float closeProgress = closing ? Math.min(1.0f, (Util.getMeasuringTimeMs() - closeStartMs) / (float) FADE_DURATION_MS) : 0f;
        if (closing && closeProgress >= 1.0f) {
            if (client != null) client.setScreen(parent);
            return;
        }
        float guiAlpha = closing ? (1.0f - closeProgress) : openProgress;
        if (guiAlpha <= 0f) return;

        float scale = 0.85f + guiAlpha * 0.15f;

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate((float) (panelX + BOX_WIDTH / 2), (float) (panelY + BOX_HEIGHT / 2));
        matrices.scale(scale, scale);
        matrices.translate((float) -(panelX + BOX_WIDTH / 2), (float) -(panelY + BOX_HEIGHT / 2));

        int left = panelX;
        int top = panelY;
        int right = left + BOX_WIDTH;
        int bottom = top + BOX_HEIGHT;

        int baseColor = applyAlpha(0xAA000000, guiAlpha);
        context.fill(left, top, right, bottom, baseColor);
        drawChromaBorder(context, left - 1, top - 1, right + 1, bottom + 1, guiAlpha);

        // Buttons
        styleButton(context, selfToggle, guiAlpha, mouseX, mouseY);
        styleButton(context, othersToggle, guiAlpha, mouseX, mouseY);
        styleButton(context, openFolderButton, guiAlpha, mouseX, mouseY);
        styleButton(context, doneButton, guiAlpha, mouseX, mouseY);

        // Skin selector dropdown
        renderSkinDropdown(context, mouseX, mouseY, guiAlpha);

        // Hint text
        String hint = "Drop any .png skin file into the skin folder.";
        int hintWidth = textRenderer.getWidth(hint);
        int hintX = panelX + (BOX_WIDTH - hintWidth) / 2;
        int hintY = panelY + BOX_HEIGHT - 50;
        context.drawTextWithShadow(textRenderer, hint, hintX, hintY, applyAlpha(0xFF888888, guiAlpha));

        matrices.popMatrix();
    }

    private void renderSkinDropdown(DrawContext context, int mouseX, int mouseY, float guiAlpha) {
        int ddX = panelX + (BOX_WIDTH - 220) / 2;
        int ddY = panelY + 112;
        int ddW = 220;
        int ddH = 16;

        // Dropdown button bar
        boolean barHover = mouseX >= ddX && mouseX <= ddX + ddW && mouseY >= ddY && mouseY <= ddY + ddH;
        int barFill = applyAlpha(barHover ? 0xFF444444 : 0xFF333333, guiAlpha);
        context.fill(ddX, ddY, ddX + ddW, ddY + ddH, barFill);
        drawChromaBorder(context, ddX - 1, ddY - 1, ddX + ddW + 1, ddY + ddH + 1, guiAlpha);

        // Current selection label + arrow
        String label = currentSkinLabel();
        String arrow = dropdownOpen ? " \u25B2" : " \u25BC";
        int maxLabelWidth = ddW - textRenderer.getWidth(arrow) - 8;
        // Truncate label if too long
        while (textRenderer.getWidth(label) > maxLabelWidth && label.length() > 3) {
            label = label.substring(0, label.length() - 1);
        }
        if (textRenderer.getWidth(label + arrow) > ddW - 4) {
            label = label.substring(0, Math.max(0, label.length() - 3)) + "...";
        }
        String display = label + arrow;
        int textW = textRenderer.getWidth(display);
        int textX = ddX + (ddW - textW) / 2;
        int textY = ddY + (ddH - textRenderer.fontHeight) / 2;
        int chroma = applyAlpha(chromaColor((System.currentTimeMillis() % 4000) / 4000f), guiAlpha);
        context.drawTextWithShadow(textRenderer, display, textX, textY, chroma);

        // Dropdown list
        if (dropdownOpen && !availableSkins.isEmpty()) {
            int listTop = ddY + ddH + 2;
            int visibleCount = Math.min(availableSkins.size(), DROPDOWN_MAX_VISIBLE);
            int listHeight = visibleCount * DROPDOWN_ROW_HEIGHT;

            // Background
            context.fill(ddX, listTop, ddX + ddW, listTop + listHeight, applyAlpha(0xDD000000, guiAlpha));
            drawChromaBorder(context, ddX - 1, listTop - 1, ddX + ddW + 1, listTop + listHeight + 1, guiAlpha);

            String selected = SkinConfig.getSelectedSkin();
            for (int i = 0; i < visibleCount; i++) {
                int idx = i + dropdownScroll;
                if (idx >= availableSkins.size()) break;

                String skinName = availableSkins.get(idx);
                int rowY = listTop + i * DROPDOWN_ROW_HEIGHT;
                boolean rowHover = mouseX >= ddX && mouseX <= ddX + ddW && mouseY >= rowY && mouseY <= rowY + DROPDOWN_ROW_HEIGHT;
                boolean isSelected = skinName.equals(selected);

                if (rowHover) {
                    context.fill(ddX + 1, rowY, ddX + ddW - 1, rowY + DROPDOWN_ROW_HEIGHT, applyAlpha(0xFF555555, guiAlpha));
                } else if (isSelected) {
                    context.fill(ddX + 1, rowY, ddX + ddW - 1, rowY + DROPDOWN_ROW_HEIGHT, applyAlpha(0xFF3A3A3A, guiAlpha));
                }

                // Truncate name if needed
                String name = skinName;
                while (textRenderer.getWidth(name) > ddW - 10 && name.length() > 3) {
                    name = name.substring(0, name.length() - 1);
                }

                int nameColor = isSelected ? chroma : applyAlpha(0xFFCCCCCC, guiAlpha);
                context.drawTextWithShadow(textRenderer, name, ddX + 5, rowY + (DROPDOWN_ROW_HEIGHT - textRenderer.fontHeight) / 2, nameColor);
            }

            // Scroll indicator
            if (availableSkins.size() > DROPDOWN_MAX_VISIBLE) {
                int scrollBarH = Math.max(4, (int) ((float) visibleCount / availableSkins.size() * listHeight));
                int scrollBarY = listTop + (int) ((float) dropdownScroll / (availableSkins.size() - visibleCount) * (listHeight - scrollBarH));
                context.fill(ddX + ddW - 3, scrollBarY, ddX + ddW - 1, scrollBarY + scrollBarH, applyAlpha(0xFF888888, guiAlpha));
            }
        } else if (dropdownOpen && availableSkins.isEmpty()) {
            int listTop = ddY + ddH + 2;
            context.fill(ddX, listTop, ddX + ddW, listTop + DROPDOWN_ROW_HEIGHT, applyAlpha(0xDD000000, guiAlpha));
            drawChromaBorder(context, ddX - 1, listTop - 1, ddX + ddW + 1, listTop + DROPDOWN_ROW_HEIGHT + 1, guiAlpha);
            String empty = "No skins found";
            int ew = textRenderer.getWidth(empty);
            context.drawTextWithShadow(textRenderer, empty, ddX + (ddW - ew) / 2, listTop + (DROPDOWN_ROW_HEIGHT - textRenderer.fontHeight) / 2, applyAlpha(0xFF888888, guiAlpha));
        }
    }

    private void styleButton(DrawContext context, ButtonWidget button, float alpha, int mouseX, int mouseY) {
        int bx = button.getX();
        int by = button.getY();
        int bw = button.getWidth();
        int bh = button.getHeight();
        boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        int fill = applyAlpha(hover ? 0xFF666666 : 0xFF555555, alpha);
        context.fill(bx, by, bx + bw, by + bh, fill);
        drawChromaBorder(context, bx - 1, by - 1, bx + bw + 1, by + bh + 1, alpha);
        String label = button.getMessage().getString();
        int textWidth = textRenderer.getWidth(label);
        int tx = bx + (bw - textWidth) / 2;
        int ty = by + (bh - textRenderer.fontHeight) / 2;
        int chroma = chromaColor((System.currentTimeMillis() % 4000) / 4000f);
        context.drawTextWithShadow(textRenderer, label, tx, ty, applyAlpha(chroma, alpha));
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

    private void drawChromaBorder(DrawContext context, int left, int top, int right, int bottom, float alpha) {
        int w = right - left;
        int h = bottom - top;
        int perimeter = w * 2 + h * 2;
        if (perimeter <= 0) return;
        int pos = 0;
        for (int x = 0; x < w; x++, pos++) {
            int c = applyAlpha(chromaColor(pos / (float) perimeter), alpha);
            context.fill(left + x, top, left + x + 1, top + 1, c);
        }
        for (int y = 0; y < h; y++, pos++) {
            int c = applyAlpha(chromaColor(pos / (float) perimeter), alpha);
            context.fill(right - 1, top + y, right, top + y + 1, c);
        }
        for (int x = w - 1; x >= 0; x--, pos++) {
            int c = applyAlpha(chromaColor(pos / (float) perimeter), alpha);
            context.fill(left + x, bottom - 1, left + x + 1, bottom, c);
        }
        for (int y = h - 1; y >= 0; y--, pos++) {
            int c = applyAlpha(chromaColor(pos / (float) perimeter), alpha);
            context.fill(left, top + y, left + 1, top + y + 1, c);
        }
    }
}
