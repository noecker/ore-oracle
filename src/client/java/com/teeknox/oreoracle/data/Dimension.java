package com.teeknox.oreoracle.data;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the three Minecraft dimensions.
 */
public enum Dimension {
    OVERWORLD("overworld"),
    NETHER("the_nether"),
    END("the_end");

    private final Identifier id;

    Dimension(String path) {
        this.id = Identifier.fromNamespaceAndPath("minecraft", path);
    }

    public Identifier getId() {
        return id;
    }

    /**
     * Get the dimension from a world's registry key.
     */
    @Nullable
    public static Dimension fromWorld(Level world) {
        if (world == null) return null;

        Identifier worldId = world.dimension().identifier();

        for (Dimension dim : values()) {
            if (dim.id.equals(worldId)) {
                return dim;
            }
        }

        return null;
    }
}
