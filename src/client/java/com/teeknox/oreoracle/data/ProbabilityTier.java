package com.teeknox.oreoracle.data;

/**
 * Represents the probability tier for an ore at a given Y-level.
 */
public enum ProbabilityTier {
    GREEN(0xFF55FF55),   // High probability - optimal zone
    YELLOW(0xFFFFFF55),  // Medium probability
    RED(0xFFFF5555),     // Low probability
    NONE(0xFF888888);    // No probability at this Y-level (gray)

    private final int color;

    ProbabilityTier(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
