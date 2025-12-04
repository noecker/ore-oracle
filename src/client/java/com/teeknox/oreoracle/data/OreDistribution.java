package com.teeknox.oreoracle.data;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates probability tiers for ores based on Y-level and biome.
 * All tier boundaries are pre-computed based on ore distribution charts from CLAUDE.md.
 */
public class OreDistribution {

    /**
     * Get the probability tier for an ore at the given Y-level.
     *
     * @param ore   The ore to check
     * @param y     The Y-level
     * @param biome The current biome (nullable, used for emerald/gold special handling)
     * @return The probability tier for the ore at this Y-level
     */
    public static ProbabilityTier getTier(Ore ore, int y, @Nullable Identifier biome) {
        return switch (ore) {
            case COAL -> getCoalTier(y);
            case COPPER -> getCopperTier(y);
            case IRON -> getIronTier(y);
            case GOLD -> getGoldTier(y, biome);
            case DIAMOND -> getDiamondTier(y);
            case REDSTONE -> getRedstoneTier(y);
            case LAPIS -> getLapisTier(y);
            case EMERALD -> getEmeraldTier(y, biome);
            case NETHER_QUARTZ -> getNetherQuartzTier(y);
            case NETHER_GOLD -> getNetherGoldTier(y);
            case ANCIENT_DEBRIS -> getAncientDebrisTier(y);
        };
    }

    /**
     * Check if the player is at a peak Y-level for the given ore (within ±1).
     *
     * @param ore The ore to check
     * @param y   The Y-level
     * @return true if at peak level for this ore
     */
    public static boolean isAtPeak(Ore ore, int y) {
        if (!ore.hasPeakIndicator()) {
            return false;
        }

        for (int peakY : ore.getPeakYLevels()) {
            // Iron upper peak (256) is excluded from indicator
            if (ore == Ore.IRON && peakY == 256) {
                continue;
            }
            if (y >= peakY - 1 && y <= peakY + 1) {
                return true;
            }
        }
        return false;
    }

    // ==================== Overworld Ores ====================

    /**
     * Coal: Y 0-320
     * - GREEN at Y >= 136 (uniform distribution, consistent spawning)
     * - GREEN at Y 67-125 (peak zone of triangular distribution)
     * - YELLOW at Y 40-67 or 125-136
     * - RED at Y 0-40
     */
    private static ProbabilityTier getCoalTier(int y) {
        if (y < 0 || y > 320) {
            return ProbabilityTier.NONE;
        }
        if (y >= 136) {
            return ProbabilityTier.GREEN;
        }
        if (y >= 67 && y <= 125) {
            return ProbabilityTier.GREEN;
        }
        if ((y >= 40 && y < 67) || (y > 125 && y < 136)) {
            return ProbabilityTier.YELLOW;
        }
        // y < 40
        return ProbabilityTier.RED;
    }

    /**
     * Copper: Y -16 to 112
     * - GREEN at Y 32-64
     * - YELLOW at Y 0-32 or 64-96
     * - RED at Y -16 to 0 or 96-112
     */
    private static ProbabilityTier getCopperTier(int y) {
        if (y < -16 || y > 112) {
            return ProbabilityTier.NONE;
        }
        if (y >= 32 && y <= 64) {
            return ProbabilityTier.GREEN;
        }
        if ((y >= 0 && y < 32) || (y > 64 && y <= 96)) {
            return ProbabilityTier.YELLOW;
        }
        // y < 0 or y > 96
        return ProbabilityTier.RED;
    }

    /**
     * Iron: Y -64 to 384 (two triangular distributions)
     * - GREEN at Upper Triangle: Y 200-256 (mountain peaks)
     * - GREEN at Lower Triangle: Y -8 to 32 (main ore concentration)
     * - YELLOW for Y < 72 and not in green zone (uniform small blob distribution)
     * - RED at Y 72-200 (sparse area between distributions)
     */
    private static ProbabilityTier getIronTier(int y) {
        if (y < -64 || y > 384) {
            return ProbabilityTier.NONE;
        }
        // Upper triangle green zone (mountain peaks)
        if (y >= 200 && y <= 256) {
            return ProbabilityTier.GREEN;
        }
        // Lower triangle green zone (main concentration)
        if (y >= -8 && y <= 32) {
            return ProbabilityTier.GREEN;
        }
        // Y < 72 is always at least YELLOW due to uniform distribution
        if (y < 72) {
            return ProbabilityTier.YELLOW;
        }
        // Y 72-200 is the sparse red zone, Y > 256 is also sparse
        return ProbabilityTier.RED;
    }

    /**
     * Gold: Y -64 to 32 (normal), with two peaks
     * Upper Peak (-16): GREEN zone is -30 to -5
     * Lower Peak (~-50): GREEN zone is -54 to -48
     * - YELLOW at Y -48 to -30 (between peaks), -5 to 8 (above upper peak)
     * - RED at Y 8-32
     * - Badlands special: GREEN for any Y > 32
     */
    private static ProbabilityTier getGoldTier(int y, @Nullable Identifier biome) {
        // Badlands special case: GREEN for any Y > 32
        if (BiomeChecker.isBadlandsBiome(biome) && y > 32) {
            return ProbabilityTier.GREEN;
        }

        // Normal gold distribution: -64 to 32
        if (y < -64 || y > 32) {
            // Badlands allows up to 256, but that's handled above
            return ProbabilityTier.NONE;
        }

        // Upper peak green zone: -30 to -5
        if (y >= -30 && y <= -5) {
            return ProbabilityTier.GREEN;
        }
        // Lower peak green zone: -54 to -48
        if (y >= -54 && y <= -48) {
            return ProbabilityTier.GREEN;
        }
        // Yellow zones: between peaks (-48 to -30) and above upper peak (-5 to 8)
        if ((y > -48 && y < -30) || (y > -5 && y <= 8)) {
            return ProbabilityTier.YELLOW;
        }
        // Red zone: 8 to 32 and below -54
        return ProbabilityTier.RED;
    }

    /**
     * Diamond: Y -64 to 16
     * - GREEN at Y -64 to -48
     * - YELLOW at Y -48 to -16
     * - RED at Y -16 to 16
     */
    private static ProbabilityTier getDiamondTier(int y) {
        if (y < -64 || y > 16) {
            return ProbabilityTier.NONE;
        }
        if (y <= -48) {
            return ProbabilityTier.GREEN;
        }
        if (y <= -16) {
            return ProbabilityTier.YELLOW;
        }
        // y > -16
        return ProbabilityTier.RED;
    }

    /**
     * Redstone: Y -64 to 15
     * - GREEN at Y -64 to -32
     * - YELLOW at Y -32 to 16 (jumps straight to yellow, no red zone)
     * - NONE above Y 16 (no spawning)
     */
    private static ProbabilityTier getRedstoneTier(int y) {
        if (y < -64 || y > 15) {
            return ProbabilityTier.NONE;
        }
        if (y <= -32) {
            return ProbabilityTier.GREEN;
        }
        // y > -32 and <= 15
        return ProbabilityTier.YELLOW;
    }

    /**
     * Lapis Lazuli: Y -64 to 64
     * - GREEN at Y -16 to 16
     * - YELLOW at Y -64 to -16 or 16-64 (jumps straight to yellow, no red zone)
     */
    private static ProbabilityTier getLapisTier(int y) {
        if (y < -64 || y > 64) {
            return ProbabilityTier.NONE;
        }
        if (y >= -16 && y <= 16) {
            return ProbabilityTier.GREEN;
        }
        // Outside green zone but within spawn range = yellow
        return ProbabilityTier.YELLOW;
    }

    /**
     * Emerald: Y -16 to 320 (MOUNTAIN BIOMES ONLY)
     * - NONE if not in mountain biome
     * - GREEN at Y 64-136
     * - YELLOW at Y 32-64 or 136-200
     * - RED at Y -16 to 32 or 200-320
     */
    private static ProbabilityTier getEmeraldTier(int y, @Nullable Identifier biome) {
        // Emerald ONLY spawns in mountain biomes
        if (!BiomeChecker.isMountainBiome(biome)) {
            return ProbabilityTier.NONE;
        }

        if (y < -16 || y > 320) {
            return ProbabilityTier.NONE;
        }
        if (y >= 64 && y <= 136) {
            return ProbabilityTier.GREEN;
        }
        if ((y >= 32 && y < 64) || (y > 136 && y <= 200)) {
            return ProbabilityTier.YELLOW;
        }
        // y < 32 or y > 200
        return ProbabilityTier.RED;
    }

    // ==================== Nether Ores ====================

    /**
     * Nether Quartz: Y 10-117 (two peaks at floor and ceiling)
     * - GREEN at Y 10-22 (floor peak) and Y 105-117 (ceiling peak)
     * - YELLOW at Y 22-35 and 95-105
     * - RED at Y 35-95
     */
    private static ProbabilityTier getNetherQuartzTier(int y) {
        if (y < 10 || y > 117) {
            return ProbabilityTier.NONE;
        }
        // Floor peak green zone
        if (y <= 22) {
            return ProbabilityTier.GREEN;
        }
        // Ceiling peak green zone
        if (y >= 105) {
            return ProbabilityTier.GREEN;
        }
        // Yellow zones near peaks
        if (y <= 35 || y >= 95) {
            return ProbabilityTier.YELLOW;
        }
        // Middle section is red
        return ProbabilityTier.RED;
    }

    /**
     * Nether Gold: Y 10-117 (same distribution pattern as Nether Quartz)
     * - GREEN at Y 10-22 (floor peak) and Y 105-117 (ceiling peak)
     * - YELLOW at Y 22-35 and 95-105
     * - RED at Y 35-95
     */
    private static ProbabilityTier getNetherGoldTier(int y) {
        // Same distribution as Nether Quartz
        return getNetherQuartzTier(y);
    }

    /**
     * Ancient Debris: Y 8-119
     * - GREEN at Y 8-22 (peak zone)
     * - RED at Y 22-119 (trace amounts, no yellow zone)
     */
    private static ProbabilityTier getAncientDebrisTier(int y) {
        if (y < 8 || y > 119) {
            return ProbabilityTier.NONE;
        }
        if (y <= 22) {
            return ProbabilityTier.GREEN;
        }
        // No yellow zone - goes straight to red
        return ProbabilityTier.RED;
    }
}
