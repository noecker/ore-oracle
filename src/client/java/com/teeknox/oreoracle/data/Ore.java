package com.teeknox.oreoracle.data;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Enum representing all vanilla ores with their distribution properties.
 */
public enum Ore {
    // Overworld ores (using raw material icons for better visibility at small sizes)
    COAL("coal", "Coal", Dimension.OVERWORLD, 0, 320, new int[]{96}, true, Items.COAL),
    COPPER("copper", "Copper", Dimension.OVERWORLD, -16, 112, new int[]{48}, true, Items.RAW_COPPER),
    IRON("iron", "Iron", Dimension.OVERWORLD, -64, 384, new int[]{16, 232}, true, Items.RAW_IRON), // Both peaks get indicator
    GOLD("gold", "Gold", Dimension.OVERWORLD, -64, 32, new int[]{-16}, true, Items.RAW_GOLD),
    DIAMOND("diamond", "Diamond", Dimension.OVERWORLD, -64, 16, new int[]{-59}, true, Items.DIAMOND),
    REDSTONE("redstone", "Redstone", Dimension.OVERWORLD, -64, 15, new int[]{-59}, true, Items.REDSTONE),
    LAPIS("lapis", "Lapis", Dimension.OVERWORLD, -64, 64, new int[]{0}, true, Items.LAPIS_LAZULI),
    EMERALD("emerald", "Emerald", Dimension.OVERWORLD, -16, 320, new int[]{232}, true, Items.EMERALD),

    // Nether ores (uniform distribution - no peaks, entire range is equally good)
    NETHER_QUARTZ("nether_quartz", "Nether Quartz", Dimension.NETHER, 10, 117, new int[]{}, false, Items.QUARTZ),
    NETHER_GOLD("nether_gold", "Nether Gold", Dimension.NETHER, 10, 117, new int[]{}, false, Items.GOLD_NUGGET),
    ANCIENT_DEBRIS("ancient_debris", "Ancient Debris", Dimension.NETHER, 8, 119, new int[]{15}, true, Items.NETHERITE_SCRAP);

    private final String id;
    private final String displayName;
    private final Dimension dimension;
    private final int minY;
    private final int maxY;
    private final int[] peakYLevels;
    private final boolean hasPeakIndicator;
    private final Item iconItem;

    Ore(String id, String displayName, Dimension dimension, int minY, int maxY,
        int[] peakYLevels, boolean hasPeakIndicator, Item iconItem) {
        this.id = id;
        this.displayName = displayName;
        this.dimension = dimension;
        this.minY = minY;
        this.maxY = maxY;
        this.peakYLevels = peakYLevels;
        this.hasPeakIndicator = hasPeakIndicator;
        this.iconItem = iconItem;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public int[] getPeakYLevels() {
        return peakYLevels;
    }

    public boolean hasPeakIndicator() {
        return hasPeakIndicator;
    }

    /**
     * Get the item to use for icon display (raw material form).
     */
    public Item getIconItem() {
        return iconItem;
    }

    /**
     * Check if the given Y-level is within the spawn range for this ore.
     */
    public boolean isInRange(int y) {
        return y >= minY && y <= maxY;
    }

    /**
     * Check if this ore requires biome checking (emerald for mountains, gold for badlands).
     */
    public boolean requiresBiomeCheck() {
        return this == EMERALD || this == GOLD;
    }
}
