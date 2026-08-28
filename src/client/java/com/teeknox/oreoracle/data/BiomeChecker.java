package com.teeknox.oreoracle.data;

import org.jetbrains.annotations.Nullable;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

/**
 * Utility class for checking biomes relevant to ore spawning.
 */
public class BiomeChecker {

    /**
     * Mountain biomes where Emerald can spawn.
     */
    private static final Set<Identifier> MOUNTAIN_BIOMES = Set.of(
            Identifier.fromNamespaceAndPath("minecraft", "meadow"),
            Identifier.fromNamespaceAndPath("minecraft", "cherry_grove"),
            Identifier.fromNamespaceAndPath("minecraft", "grove"),
            Identifier.fromNamespaceAndPath("minecraft", "snowy_slopes"),
            Identifier.fromNamespaceAndPath("minecraft", "jagged_peaks"),
            Identifier.fromNamespaceAndPath("minecraft", "frozen_peaks"),
            Identifier.fromNamespaceAndPath("minecraft", "stony_peaks"),
            Identifier.fromNamespaceAndPath("minecraft", "windswept_hills"),
            Identifier.fromNamespaceAndPath("minecraft", "windswept_gravelly_hills"),
            Identifier.fromNamespaceAndPath("minecraft", "windswept_forest")
    );

    /**
     * Badlands biomes where Gold spawns up to Y=255.
     */
    private static final Set<Identifier> BADLANDS_BIOMES = Set.of(
            Identifier.fromNamespaceAndPath("minecraft", "badlands"),
            Identifier.fromNamespaceAndPath("minecraft", "wooded_badlands"),
            Identifier.fromNamespaceAndPath("minecraft", "eroded_badlands")
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
    public static Identifier getCurrentBiome(Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }

        BlockPos pos = client.player.blockPosition();
        Holder<Biome> biomeEntry = client.level.getBiome(pos);

        return biomeEntry.unwrapKey()
                .map(ResourceKey::identifier)
                .orElse(null);
    }
}
