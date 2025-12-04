package com.teeknox.modtemplate.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration manager using JSON serialization.
 *
 * Pattern: Singleton with lazy loading
 * Storage: .minecraft/config/mod-template-config.json
 *
 * Add configuration properties as private fields with getters/setters.
 * Call save() in setters to persist changes immediately.
 */
public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("mod-template");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("mod-template-config.json");

    private static ModConfig instance;

    // Configuration properties with defaults
    // Position settings: positive = from left/top, negative = from right/bottom
    private int overlayX = -10;  // 10 pixels from right edge
    private int overlayY = 10;   // 10 pixels from top edge
    private boolean enabled = true;

    private ModConfig() {}

    /**
     * Get the singleton config instance.
     * Loads from disk on first access.
     */
    public static ModConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    // Property accessors with auto-save on change

    public int getOverlayX() {
        return overlayX;
    }

    public void setOverlayX(int overlayX) {
        this.overlayX = overlayX;
        save();
    }

    public int getOverlayY() {
        return overlayY;
    }

    public void setOverlayY(int overlayY) {
        this.overlayY = overlayY;
        save();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    /**
     * Calculate actual X position supporting negative offsets (from right edge).
     */
    public int calculateActualX(int screenWidth, int boxWidth) {
        if (overlayX >= 0) {
            return overlayX;
        } else {
            return screenWidth + overlayX - boxWidth;
        }
    }

    /**
     * Calculate actual Y position supporting negative offsets (from bottom edge).
     */
    public int calculateActualY(int screenHeight, int boxHeight) {
        if (overlayY >= 0) {
            return overlayY;
        } else {
            return screenHeight + overlayY - boxHeight;
        }
    }

    private static ModConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            LOGGER.info("No config file found, using defaults");
            ModConfig config = new ModConfig();
            config.save();
            return config;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            ModConfig config = new ModConfig();
            if (data != null) {
                config.overlayX = data.overlayX;
                config.overlayY = data.overlayY;
                config.enabled = data.enabled;
            }
            LOGGER.info("Loaded config");
            return config;
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
            return new ModConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                ConfigData data = new ConfigData();
                data.overlayX = this.overlayX;
                data.overlayY = this.overlayY;
                data.enabled = this.enabled;
                GSON.toJson(data, writer);
                LOGGER.debug("Saved config");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    /**
     * Internal data class for JSON serialization.
     * Mirrors config properties with default values.
     */
    private static class ConfigData {
        int overlayX = -10;
        int overlayY = 10;
        boolean enabled = true;
    }
}
