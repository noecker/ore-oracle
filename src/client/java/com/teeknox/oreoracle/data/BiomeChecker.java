package com.teeknox.oreoracle.data;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Utility class for checking biomes relevant to ore spawning.
 */
public class BiomeChecker {

    /**
     * Mountain biomes where Emerald can spawn.
     */
    private static final Set<Identifier> MOUNTAIN_BIOMES = Set.of(
            Identifier.of("minecraft", "meadow"),
            Identifier.of("minecraft", "cherry_grove"),
            Identifier.of("minecraft", "grove"),
            Identifier.of("minecraft", "snowy_slopes"),
            Identifier.of("minecraft", "jagged_peaks"),
            Identifier.of("minecraft", "frozen_peaks"),
            Identifier.of("minecraft", "stony_peaks"),
            Identifier.of("minecraft", "windswept_hills"),
            Identifier.of("minecraft", "windswept_gravelly_hills"),
            Identifier.of("minecraft", "windswept_forest")
    );

    /**
     * Badlands biomes where Gold spawns up to Y=255.
     */
    private static final Set<Identifier> BADLANDS_BIOMES = Set.of(
            Identifier.of("minecraft", "badlands"),
            Identifier.of("minecraft", "wooded_badlands"),
            Identifier.of("minecraft", "eroded_badlands")
    );

    /**
     * Check if the given biome is a mountain biome (where emerald spawns).
     */
    public static boolean isMountainBiome(@Nullable Identifier biome) {
        return biome != null && MOUNTAIN_BIOMES.contains(biome);
    }

    /**
     * Check if the given biome is a badlands biome (where gold has extended spawning).
     */
    public static boolean isBadlandsBiome(@Nullable Identifier biome) {
        return biome != null && BADLANDS_BIOMES.contains(biome);
    }

    /**
     * Get the current biome at the player's position.
     */
    @Nullable
    public static Identifier getCurrentBiome(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return null;
        }

        BlockPos pos = client.player.getBlockPos();
        RegistryEntry<Biome> biomeEntry = client.world.getBiome(pos);

        return biomeEntry.getKey()
                .map(RegistryKey::getValue)
                .orElse(null);
    }
}
