package com.teeknox.oreoracle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.teeknox.oreoracle.OreOracleMod;
import com.teeknox.oreoracle.data.Ore;
import com.teeknox.oreoracle.data.ProbabilityTier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages per-server ore tracking preferences.
 * Each server/world has its own saved selection in ore-oracle-data/<server-id>.json.
 */
public class ServerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = FabricLoader.getInstance()
            .getConfigDir().resolve("ore-oracle-data");

    private static ServerDataManager instance;
    private ServerData currentData;
    private String currentServerId;

    private ServerDataManager() {}

    public static ServerDataManager getInstance() {
        if (instance == null) {
            instance = new ServerDataManager();
        }
        return instance;
    }

    /**
     * Load data for the current server/world.
     * Should be called when joining a server or loading a world.
     */
    public void loadForCurrentServer() {
        String serverId = getServerId();
        if (serverId == null) {
            currentData = new ServerData();
            currentServerId = null;
            return;
        }

        if (serverId.equals(currentServerId) && currentData != null) {
            return; // Already loaded
        }

        currentServerId = serverId;
        currentData = loadData(serverId);
    }

    /**
     * Save current data for the current server.
     */
    public void save() {
        if (currentServerId != null && currentData != null) {
            saveData(currentServerId, currentData);
        }
    }

    /**
     * Clear current server data (called on disconnect).
     */
    public void clear() {
        currentData = null;
        currentServerId = null;
    }

    /**
     * Get the set of specifically tracked ore IDs.
     */
    public Set<String> getTrackedOres() {
        ensureLoaded();
        return currentData.trackedOres;
    }

    /**
     * Check if a specific ore is tracked.
     */
    public boolean isOreTracked(Ore ore) {
        ensureLoaded();
        return currentData.trackedOres.contains(ore.getId());
    }

    /**
     * Set whether an ore is tracked.
     */
    public void setOreTracked(Ore ore, boolean tracked) {
        ensureLoaded();
        if (tracked) {
            currentData.trackedOres.add(ore.getId());
        } else {
            currentData.trackedOres.remove(ore.getId());
        }
        save();
    }

    /**
     * Toggle tracking for an ore.
     */
    public void toggleOreTracked(Ore ore) {
        setOreTracked(ore, !isOreTracked(ore));
    }

    /**
     * Get the current probability filter.
     */
    public ProbabilityTier getProbabilityFilter() {
        ensureLoaded();
        return currentData.probabilityFilter;
    }

    /**
     * Set the probability filter.
     */
    public void setProbabilityFilter(ProbabilityTier filter) {
        ensureLoaded();
        currentData.probabilityFilter = filter;
        save();
    }

    /**
     * Check if an ore should be displayed based on current filter and tracking.
     *
     * @param ore  The ore to check
     * @param tier The ore's tier at the current Y-level
     * @return true if the ore should be displayed
     */
    public boolean shouldDisplayOre(Ore ore, ProbabilityTier tier) {
        ensureLoaded();

        // NONE filter = "Specific" mode - ONLY show tracked ores
        if (currentData.probabilityFilter == ProbabilityTier.NONE) {
            return isOreTracked(ore);
        }

        // For other filters, tracked ores are ignored (filter takes precedence)
        // Don't show if no probability at this Y-level
        if (tier == ProbabilityTier.NONE) {
            return false;
        }

        // Check against filter (cumulative)
        return switch (currentData.probabilityFilter) {
            case GREEN -> tier == ProbabilityTier.GREEN;
            case YELLOW -> tier == ProbabilityTier.GREEN || tier == ProbabilityTier.YELLOW;
            case RED -> tier != ProbabilityTier.NONE;
            case NONE -> false; // Already handled above, but needed for completeness
        };
    }

    private void ensureLoaded() {
        if (currentData == null) {
            loadForCurrentServer();
        }
    }

    /**
     * Get a sanitized server ID for the current connection.
     */
    @Nullable
    private String getServerId() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }

        // Check if on a server
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null) {
            // Multiplayer server - use address
            return sanitize(serverInfo.address);
        }

        // Singleplayer - use world name
        if (client.isIntegratedServerRunning() && client.getServer() != null) {
            String worldName = client.getServer().getSaveProperties().getLevelName();
            return "singleplayer_" + sanitize(worldName);
        }

        return null;
    }

    /**
     * Sanitize a string for use as a filename.
     */
    private String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    private ServerData loadData(String serverId) {
        Path filePath = DATA_DIR.resolve(serverId + ".json");
        if (Files.exists(filePath)) {
            try {
                String json = Files.readString(filePath);
                ServerData data = GSON.fromJson(json, ServerData.class);
                if (data != null) {
                    // Ensure non-null collections
                    if (data.trackedOres == null) {
                        data.trackedOres = new HashSet<>();
                    }
                    if (data.probabilityFilter == null) {
                        data.probabilityFilter = ProbabilityTier.RED;
                    }
                    return data;
                }
            } catch (IOException e) {
                OreOracleMod.LOGGER.error("Failed to load server data for {}", serverId, e);
            }
        }
        return new ServerData();
    }

    private void saveData(String serverId, ServerData data) {
        Path filePath = DATA_DIR.resolve(serverId + ".json");
        try {
            Files.createDirectories(DATA_DIR);
            Files.writeString(filePath, GSON.toJson(data));
        } catch (IOException e) {
            OreOracleMod.LOGGER.error("Failed to save server data for {}", serverId, e);
        }
    }

    /**
     * Per-server data structure.
     */
    private static class ServerData {
        Set<String> trackedOres = new HashSet<>();
        ProbabilityTier probabilityFilter = ProbabilityTier.RED; // Default: show all spawning ores
    }
}
