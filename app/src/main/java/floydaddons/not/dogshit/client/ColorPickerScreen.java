package floydaddons.not.dogshit.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

import floydaddons.not.dogshit.client.FloydAddonsClient;
import floydaddons.not.dogshit.client.InventoryHudRenderer;
import floydaddons.not.dogshit.client.RenderConfig;

/**
 * Standalone color picker with wheel + value slider.
 */
public class ColorPickerScreen extends Screen {
    private static final int BOX_WIDTH = 240;
    private static final int BOX_HEIGHT = 220;
    private static final int DRAG_BAR_HEIGHT = 16;
    private static final int WHEEL_SIZE = 96;
    private static final int SLIDER_W = 12;
    private static final int SLIDER_H = 96;
    private static final long FADE_DURATION_MS = 90;

    private final Screen parent;
    private final String titleText;
    private final Consumer<Integer> applyCallback;
    private final BooleanSupplier chromaGetter;
    private final Consumer<Boolean> chromaSetter;

    private int panelX, panelY;
    private boolean dragging = false;
    private boolean draggingWheel = false;
    private boolean draggingValue = false;
    private double dragStartMouseX, dragStartMouseY;
    private int dragStartPanelX, dragStartPanelY;

    private long openStartMs;
    private long closeStartMs;
    private boolean closing = false;

    private float hue;
    private float sat;
    private float val;

    private ButtonWidget applyButton;
    private ButtonWidget cancelButton;
    private TextFieldWidget hexField;
    private ButtonWidget chromaButton;
    private boolean chromaEnabled;

    public ColorPickerScreen(Screen parent, String titleText, int initialColor, Consumer<Integer> applyCallback,
                             BooleanSupplier chromaGetter, Consumer<Boolean> chromaSetter) {
        super(Text.literal(titleText + " Picker"));
        this.parent = parent;
        this.titleText = titleText;
        this.applyCallback = applyCallback;
        this.chromaGetter = chromaGetter;
        this.chromaSetter = chromaSetter;

        float[] hsv = Color.RGBtoHSB((initialColor >> 16) & 0xFF, (initialColor >> 8) & 0xFF, initialColor & 0xFF, null);
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.val = hsv[2];
    }

    private int pickerX() { return panelX + 16; }
    private int pickerY() { return panelY + 30; }

    @Override
    protected void init() {
        openStartMs = Util.getMeasuringTimeMs();
        closing = false;

        panelX = (width - BOX_WIDTH) / 2;
        panelY = (height - BOX_HEIGHT) / 2;
        chromaEnabled = chromaGetter.getAsBoolean();

        hexField = new TextFieldWidget(textRenderer, 0, 0, 90, 18, Text.literal("#"));
        hexField.setMaxLength(7);
        hexField.setText(colorToHex(currentColor()));
        addSelectableChild(hexField);

        chromaButton = ButtonWidget.builder(Text.literal(chromaLabel()), b -> toggleChroma())
                .dimensions(0, 0, 90, 18).build();
        addDrawableChild(chromaButton);

        applyButton = ButtonWidget.builder(Text.literal("Apply"), b -> applyAndClose())
                .dimensions(0, 0, 80, 18).build();
        cancelButton = ButtonWidget.builder(Text.literal("Cancel"), b -> close())
                .dimensions(0, 0, 80, 18).build();
        addDrawableChild(applyButton);
        addDrawableChild(cancelButton);

        repositionWidgets();

        ColorWheel.ensure();
    }

    private void applyAndClose() {
        int parsed = parseHex(hexField.getText(), currentColor());
        setFromColor(parsed);
        applyCallback.accept(parsed);
        chromaSetter.accept(chromaEnabled);
        RenderConfig.save();
        close();
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        closeStartMs = Util.getMeasuringTimeMs();
    }

    private void repositionWidgets() {
        int buttonsY = panelY + BOX_HEIGHT - 32;
        int hexY = panelY + BOX_HEIGHT - 56;
        hexField.setX(panelX + 16);
        hexField.setY(hexY);
        chromaButton.setX(panelX + 16 + 90 + 8);
        chromaButton.setY(hexY);
        applyButton.setX(panelX + 20);
        applyButton.setY(buttonsY);
        cancelButton.setX(panelX + BOX_WIDTH - 100);
        cancelButton.setY(buttonsY);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

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

        int left = panelX;
        int top = panelY;
        int right = left + BOX_WIDTH;
        int bottom = top + BOX_HEIGHT;

        context.fill(left, top, right, bottom, applyAlpha(0xCC000000, guiAlpha));
        InventoryHudRenderer.drawButtonBorder(context, left - 1, top - 1, right + 1, bottom + 1, guiAlpha);

        // Title
        String ttl = titleText + " Picker";
        int tw = textRenderer.getWidth(ttl);
        context.drawTextWithShadow(textRenderer, ttl, left + (BOX_WIDTH - tw) / 2, top + 6,
                applyAlpha(0xFFFFFFFF, guiAlpha));

        drawPicker(context, guiAlpha, mouseX, mouseY);
        drawHexField(context, guiAlpha, mouseX, mouseY, delta);
        chromaButton.active = true;

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawHexField(DrawContext context, float alpha, int mouseX, int mouseY, float delta) {
        int bg = applyAlpha(0xFF222222, alpha);
        context.fill(hexField.getX() - 2, hexField.getY() - 2, hexField.getX() + hexField.getWidth() + 2, hexField.getY() + hexField.getHeight() + 2, bg);
        hexField.setEditableColor(0xFFFFFFFF);
        hexField.render(context, mouseX, mouseY, delta);
        // Chroma toggle label
        int cbx = chromaButton.getX();
        int cby = chromaButton.getY();
        int cbw = chromaButton.getWidth();
        int cbh = chromaButton.getHeight();
        boolean hover = mouseX >= cbx && mouseX <= cbx + cbw && mouseY >= cby && mouseY <= cby + cbh;
        int fill = applyAlpha(hover ? 0xFF666666 : 0xFF555555, alpha);
        context.fill(cbx, cby, cbx + cbw, cby + cbh, fill);
        InventoryHudRenderer.drawButtonBorder(context, cbx - 1, cby - 1, cbx + cbw + 1, cby + cbh + 1, alpha);
        int tw = textRenderer.getWidth(chromaButton.getMessage());
        int tx = cbx + (cbw - tw) / 2;
        int ty = cby + (cbh - textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(textRenderer, chromaButton.getMessage(), tx, ty, applyAlpha(0xFFFFFFFF, alpha));
    }

    private void drawPicker(DrawContext context, float alpha, int mouseX, int mouseY) {
        int wheelX = pickerX();
        int wheelY = pickerY();
        int sliderX = wheelX + WHEEL_SIZE + 8;
        int sliderY = wheelY;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, ColorWheel.ID, wheelX, wheelY,
                0f, 0f, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE);

        for (int i = 0; i < SLIDER_H; i++) {
            float v = 1f - (i / (float) SLIDER_H);
            int c = applyAlpha(applyVToCurrent(v), alpha);
            context.fill(sliderX, sliderY + i, sliderX + SLIDER_W, sliderY + i + 1, c);
        }
        InventoryHudRenderer.drawButtonBorder(context, sliderX - 1, sliderY - 1, sliderX + SLIDER_W + 1, sliderY + SLIDER_H + 1, alpha);

        // Wheel handle
        int cx = wheelX + WHEEL_SIZE / 2;
        int cy = wheelY + WHEEL_SIZE / 2;
        float r = (WHEEL_SIZE / 2f) * sat;
        double angle = hue * 2 * Math.PI;
        int hx = cx + (int) (Math.cos(angle) * r);
        int hy = cy + (int) (Math.sin(angle) * r);
        // black outline + white fill for visibility
        context.fill(hx - 3, hy - 3, hx + 4, hy + 4, applyAlpha(0xFF000000, alpha));
        context.fill(hx - 2, hy - 2, hx + 3, hy + 3, applyAlpha(0xFFFFFFFF, alpha));

        // Slider handle
        int sy = sliderY + (int) ((1f - val) * SLIDER_H);
        context.fill(sliderX - 2, sy - 1, sliderX + SLIDER_W + 2, sy + 2, applyAlpha(0xFF000000, alpha));
        context.fill(sliderX - 1, sy, sliderX + SLIDER_W + 1, sy + 1, applyAlpha(0xFFFFFFFF, alpha));
    }

    @Override
    public boolean mouseClicked(Click click, boolean ignoresInput) {
        double mx = click.x();
        double my = click.y();
        if (click.button() == 0 && mx >= panelX && mx <= panelX + BOX_WIDTH && my >= panelY && my <= panelY + DRAG_BAR_HEIGHT) {
            dragging = true;
            dragStartMouseX = mx;
            dragStartMouseY = my;
            dragStartPanelX = panelX;
            dragStartPanelY = panelY;
            return true;
        }
        if (click.button() == 0) {
            int wheelX = pickerX();
            int wheelY = pickerY();
            int sliderX = wheelX + WHEEL_SIZE + 8;
            int sliderY = wheelY;
            if (mx >= wheelX && mx <= wheelX + WHEEL_SIZE && my >= wheelY && my <= wheelY + WHEEL_SIZE) {
                draggingWheel = true;
                updateHueSat(mx - wheelX, my - wheelY);
                hexField.setText(colorToHex(currentColor()));
                return true;
            }
            if (mx >= sliderX && mx <= sliderX + SLIDER_W && my >= sliderY && my <= sliderY + SLIDER_H) {
                draggingValue = true;
                updateValue(my - sliderY);
                hexField.setText(colorToHex(currentColor()));
                return true;
            }
            if (hexField.mouseClicked(click, ignoresInput)) {
                syncFromHex();
                return true;
            }
            if (mx >= chromaButton.getX() && mx <= chromaButton.getX() + chromaButton.getWidth()
                    && my >= chromaButton.getY() && my <= chromaButton.getY() + chromaButton.getHeight()) {
                toggleChroma();
                return true;
            }
        }
        return super.mouseClicked(click, ignoresInput);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragging && click.button() == 0) {
            double mx = click.x();
            double my = click.y();
            int newX = dragStartPanelX + (int) (mx - dragStartMouseX);
            int newY = dragStartPanelY + (int) (my - dragStartMouseY);
            newX = clamp(newX, 0, width - BOX_WIDTH);
            newY = clamp(newY, 0, height - BOX_HEIGHT);
            panelX = newX;
            panelY = newY;
            repositionWidgets();
            return true;
        }
        if (click.button() == 0) {
            int wheelX = pickerX();
            int wheelY = pickerY();
            int sliderX = wheelX + WHEEL_SIZE + 8;
            int sliderY = wheelY;
            if (draggingWheel || (click.x() >= wheelX && click.x() <= wheelX + WHEEL_SIZE && click.y() >= wheelY && click.y() <= wheelY + WHEEL_SIZE)) {
                updateHueSat(click.x() - wheelX, click.y() - wheelY);
                hexField.setText(colorToHex(currentColor()));
                return true;
            }
            if (draggingValue || (click.x() >= sliderX && click.x() <= sliderX + SLIDER_W && click.y() >= sliderY && click.y() <= sliderY + SLIDER_H)) {
                updateValue(click.y() - sliderY);
                hexField.setText(colorToHex(currentColor()));
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging && click.button() == 0) {
            dragging = false;
            draggingWheel = false;
            draggingValue = false;
            return true;
        }
        if ((draggingWheel || draggingValue) && click.button() == 0) {
            draggingWheel = false;
            draggingValue = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (hexField.keyPressed(input)) {
            syncFromHex();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        if (hexField.charTyped(input)) {
            syncFromHex();
            return true;
        }
        return super.charTyped(input);
    }

    private void updateHueSat(double localX, double localY) {
        double cx = WHEEL_SIZE / 2.0;
        double cy = WHEEL_SIZE / 2.0;
        double dx = localX - cx;
        double dy = localY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double maxR = WHEEL_SIZE / 2.0;
        double clampedDist = Math.min(dist, maxR);
        sat = (float) (clampedDist / maxR);
        hue = (float) ((Math.atan2(dy, dx) + Math.PI) / (2 * Math.PI));
    }

    private void updateValue(double sliderLocalY) {
        double t = Math.max(0, Math.min(sliderLocalY, SLIDER_H));
        val = (float) (1.0 - t / SLIDER_H);
    }

    private int applyVToCurrent(float value) {
        return Color.HSBtoRGB(hue, sat, value) | 0xFF000000;
    }

    private int currentColor() {
        return applyVToCurrent(val);
    }

    private void syncFromHex() {
        int parsed = parseHex(hexField.getText(), currentColor());
        setFromColor(parsed);
    }

    private void setFromColor(int color) {
        float[] hsv = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        hexField.setText(colorToHex(color));
    }

    private int applyAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    private String colorToHex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }

    private void toggleChroma() {
        chromaEnabled = !chromaEnabled;
        chromaButton.setMessage(Text.literal(chromaLabel()));
    }

    private String chromaLabel() {
        return chromaEnabled ? "Chroma: ON" : "Chroma: OFF";
    }

    private int parseHex(String text, int fallback) {
        if (text == null) return fallback;
        String s = text.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 3) {
            s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
        }
        if (s.length() != 6) return fallback;
        try {
            int val = Integer.parseInt(s, 16);
            return 0xFF000000 | val;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static class ColorWheel {
        private static NativeImageBackedTexture TEX;
        private static Identifier ID;

        static void ensure() {
            if (TEX != null && ID != null) return;
            NativeImage image = new NativeImage(WHEEL_SIZE, WHEEL_SIZE, true);
            double cx = WHEEL_SIZE / 2.0;
            double cy = WHEEL_SIZE / 2.0;
            double maxR = WHEEL_SIZE / 2.0 - 1;
            for (int y = 0; y < WHEEL_SIZE; y++) {
                for (int x = 0; x < WHEEL_SIZE; x++) {
                    double dx = x - cx;
                    double dy = y - cy;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist > maxR) {
                        image.setColor(x, y, 0x00000000);
                        continue;
                    }
                    float sat = (float) (dist / maxR);
                    float hue = (float) ((Math.atan2(dy, dx) + Math.PI) / (2 * Math.PI));
                    int rgb = Color.HSBtoRGB(hue, sat, 1.0f);
                    image.setColor(x, y, 0xFF000000 | (rgb & 0xFFFFFF));
                }
            }
            TEX = new NativeImageBackedTexture(() -> "floydaddons_colorwheel", image);
            ID = Identifier.of(FloydAddonsClient.MOD_ID, "colorwheel");
            MinecraftClient.getInstance().getTextureManager().registerTexture(ID, TEX);
        }
    }
}
