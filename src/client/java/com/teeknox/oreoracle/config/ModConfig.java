package com.teeknox.oreoracle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.teeknox.oreoracle.OreOracleMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Global configuration for the Ore Oracle mod.
 * Settings persist across game sessions in ore-oracle-config.json.
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("ore-oracle-config.json");

    private static ModConfig instance;

    // HUD settings
    private boolean enabled = true;
    private boolean showHudHeader = true;
    private HudPosition hudPosition = HudPosition.LEFT;
    private int overlayX = 4;
    private int overlayY = 4;
    private DisplayMode displayMode = DisplayMode.NAME;
    private int maxVisibleOres = 8;

    // Hidden default constructor for GSON
    private ModConfig() {}

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /**
     * Load config from file, or create default if not exists.
     */
    private static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ModConfig config = GSON.fromJson(json, ModConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException e) {
                OreOracleMod.LOGGER.error("Failed to load config", e);
            }
        }
        // Return default config
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }

    /**
     * Save config to file.
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            OreOracleMod.LOGGER.error("Failed to save config", e);
        }
    }

    // Getters and setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isShowHudHeader() {
        return showHudHeader;
    }

    public void setShowHudHeader(boolean showHudHeader) {
        this.showHudHeader = showHudHeader;
    }

    public HudPosition getHudPosition() {
        return hudPosition;
    }

    public void setHudPosition(HudPosition hudPosition) {
        this.hudPosition = hudPosition;
    }

    public int getOverlayX() {
        return overlayX;
    }

    public void setOverlayX(int overlayX) {
        this.overlayX = overlayX;
    }

    public int getOverlayY() {
        return overlayY;
    }

    public void setOverlayY(int overlayY) {
        this.overlayY = overlayY;
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

    public int getMaxVisibleOres() {
        return maxVisibleOres;
    }

    public void setMaxVisibleOres(int maxVisibleOres) {
        this.maxVisibleOres = maxVisibleOres;
    }

    /**
     * HUD position on screen.
     */
    public enum HudPosition {
        LEFT,
        RIGHT
    }

    /**
     * How to display ore entries.
     */
    public enum DisplayMode {
        ICON,
        NAME
    }
}
