package floydaddons.not.dogshit.client;

/**
 * Config backing the Hiders module — ~23 visual toggle features.
 * All fields default to false (off). Persistence goes through FloydAddonsConfig.
 */
public final class HidersConfig {
    private static boolean fullbright;
    private static boolean disableFog;
    private static boolean disableBlindness;
    private static boolean noHurtCamera;
    private static boolean removeFireOverlay;
    private static boolean removeWaterOverlay;
    private static boolean removeSuffocationOverlay;
    private static boolean disableVignette;
    private static boolean disableHungerBar;
    private static boolean hidePotionEffects;
    private static boolean thirdPersonCrosshair;
    private static boolean hideEntityFire;
    private static boolean disableAttachedArrows;
    private static boolean noDeathAnimation;
    private static boolean removeFallingBlocks;
    private static boolean removeLightning;
    private static boolean removeBlockBreakParticles;
    private static boolean removeExplosionParticles;
    private static boolean removeTabPing;
    private static boolean disableNametagBackground;
    private static boolean removeGlowEffect;
    private static boolean hideGroundedArrows;
    private static boolean cancelIncorrectSound;

    private HidersConfig() {}

    public static boolean isFullbrightEnabled() { return fullbright; }
    public static void setFullbrightEnabled(boolean v) { fullbright = v; }

    public static boolean isDisableFogEnabled() { return disableFog; }
    public static void setDisableFogEnabled(boolean v) { disableFog = v; }

    public static boolean isDisableBlindnessEnabled() { return disableBlindness; }
    public static void setDisableBlindnessEnabled(boolean v) { disableBlindness = v; }

    public static boolean isNoHurtCameraEnabled() { return noHurtCamera; }
    public static void setNoHurtCameraEnabled(boolean v) { noHurtCamera = v; }

    public static boolean isRemoveFireOverlayEnabled() { return removeFireOverlay; }
    public static void setRemoveFireOverlayEnabled(boolean v) { removeFireOverlay = v; }

    public static boolean isRemoveWaterOverlayEnabled() { return removeWaterOverlay; }
    public static void setRemoveWaterOverlayEnabled(boolean v) { removeWaterOverlay = v; }

    public static boolean isRemoveSuffocationOverlayEnabled() { return removeSuffocationOverlay; }
    public static void setRemoveSuffocationOverlayEnabled(boolean v) { removeSuffocationOverlay = v; }

    public static boolean isDisableVignetteEnabled() { return disableVignette; }
    public static void setDisableVignetteEnabled(boolean v) { disableVignette = v; }

    public static boolean isDisableHungerBarEnabled() { return disableHungerBar; }
    public static void setDisableHungerBarEnabled(boolean v) { disableHungerBar = v; }

    public static boolean isHidePotionEffectsEnabled() { return hidePotionEffects; }
    public static void setHidePotionEffectsEnabled(boolean v) { hidePotionEffects = v; }

    public static boolean isThirdPersonCrosshairEnabled() { return thirdPersonCrosshair; }
    public static void setThirdPersonCrosshairEnabled(boolean v) { thirdPersonCrosshair = v; }

    public static boolean isHideEntityFireEnabled() { return hideEntityFire; }
    public static void setHideEntityFireEnabled(boolean v) { hideEntityFire = v; }

    public static boolean isDisableAttachedArrowsEnabled() { return disableAttachedArrows; }
    public static void setDisableAttachedArrowsEnabled(boolean v) { disableAttachedArrows = v; }

    public static boolean isNoDeathAnimationEnabled() { return noDeathAnimation; }
    public static void setNoDeathAnimationEnabled(boolean v) { noDeathAnimation = v; }

    public static boolean isRemoveFallingBlocksEnabled() { return removeFallingBlocks; }
    public static void setRemoveFallingBlocksEnabled(boolean v) { removeFallingBlocks = v; }

    public static boolean isRemoveLightningEnabled() { return removeLightning; }
    public static void setRemoveLightningEnabled(boolean v) { removeLightning = v; }

    public static boolean isRemoveBlockBreakParticlesEnabled() { return removeBlockBreakParticles; }
    public static void setRemoveBlockBreakParticlesEnabled(boolean v) { removeBlockBreakParticles = v; }

    public static boolean isRemoveExplosionParticlesEnabled() { return removeExplosionParticles; }
    public static void setRemoveExplosionParticlesEnabled(boolean v) { removeExplosionParticles = v; }

    public static boolean isRemoveTabPingEnabled() { return removeTabPing; }
    public static void setRemoveTabPingEnabled(boolean v) { removeTabPing = v; }

    public static boolean isDisableNametagBackgroundEnabled() { return disableNametagBackground; }
    public static void setDisableNametagBackgroundEnabled(boolean v) { disableNametagBackground = v; }

    public static boolean isRemoveGlowEffectEnabled() { return removeGlowEffect; }
    public static void setRemoveGlowEffectEnabled(boolean v) { removeGlowEffect = v; }

    public static boolean isHideGroundedArrowsEnabled() { return hideGroundedArrows; }
    public static void setHideGroundedArrowsEnabled(boolean v) { hideGroundedArrows = v; }

    public static boolean isCancelIncorrectSoundEnabled() { return cancelIncorrectSound; }
    public static void setCancelIncorrectSoundEnabled(boolean v) { cancelIncorrectSound = v; }

    /** Convenience: delegates to the unified config. */
    public static void save() { FloydAddonsConfig.save(); }
}
