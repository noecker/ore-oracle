package com.teeknox.oreoracle.data;

import net.minecraft.util.Identifier;
import net.minecraft.world.World;
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
        this.id = Identifier.of("minecraft", path);
    }

    public Identifier getId() {
        return id;
    }

    /**
     * Get the dimension from a world's registry key.
     */
    @Nullable
    public static Dimension fromWorld(World world) {
        if (world == null) return null;

        Identifier worldId = world.getRegistryKey().getValue();

        for (Dimension dim : values()) {
            if (dim.id.equals(worldId)) {
                return dim;
            }
        }

        return null;
    }
}
