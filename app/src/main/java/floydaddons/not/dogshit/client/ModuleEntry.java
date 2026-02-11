package floydaddons.not.dogshit.client;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Represents one toggleable module in the ClickGUI.
 * Handles left-click (toggle) and right-click (expand settings).
 */
public class ModuleEntry {
    private final String name;
    private final String description;
    private final BooleanSupplier enabledSupplier;
    private final Runnable toggleAction;
    private final List<SubSetting> settings;

    public ModuleEntry(String name, String description, BooleanSupplier enabledSupplier, Runnable toggleAction) {
        this(name, description, enabledSupplier, toggleAction, Collections.emptyList());
    }

    public ModuleEntry(String name, String description, BooleanSupplier enabledSupplier, Runnable toggleAction, List<SubSetting> settings) {
        this.name = name;
        this.description = description;
        this.enabledSupplier = enabledSupplier;
        this.toggleAction = toggleAction;
        this.settings = settings;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabledSupplier.getAsBoolean(); }
    public void toggle() { toggleAction.run(); }
    public List<SubSetting> getSettings() { return settings; }
    public boolean hasSettings() { return !settings.isEmpty(); }

    /**
     * Base class for sub-settings displayed when a module is expanded.
     */
    public static abstract class SubSetting {
        private final String label;

        protected SubSetting(String label) {
            this.label = label;
        }

        public String getLabel() { return label; }
    }

    /**
     * A boolean toggle sub-setting.
     */
    public static class BooleanSetting extends SubSetting {
        private final BooleanSupplier getter;
        private final Runnable toggle;

        public BooleanSetting(String label, BooleanSupplier getter, Runnable toggle) {
            super(label);
            this.getter = getter;
            this.toggle = toggle;
        }

        public boolean isEnabled() { return getter.getAsBoolean(); }
        public void toggle() { toggle.run(); }
    }

    /**
     * A slider sub-setting for float values.
     */
    public static class SliderSetting extends SubSetting {
        private final java.util.function.Supplier<Float> getter;
        private final java.util.function.Consumer<Float> setter;
        private final float min;
        private final float max;
        private final String format;

        public SliderSetting(String label, java.util.function.Supplier<Float> getter,
                             java.util.function.Consumer<Float> setter, float min, float max, String format) {
            super(label);
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.format = format;
        }

        public float getValue() { return getter.get(); }
        public void setValue(float v) { setter.accept(Math.max(min, Math.min(max, v))); }
        public float getMin() { return min; }
        public float getMax() { return max; }
        public float getNormalized() { return (getValue() - min) / (max - min); }
        public void setNormalized(float t) { setValue(min + t * (max - min)); }
        public String getFormattedValue() { return String.format(format, getValue()); }
    }

    /**
     * A button sub-setting that opens a screen or runs an action.
     */
    public static class ButtonSetting extends SubSetting {
        private final Runnable action;

        public ButtonSetting(String label, Runnable action) {
            super(label);
            this.action = action;
        }

        public void click() { action.run(); }
    }
}
