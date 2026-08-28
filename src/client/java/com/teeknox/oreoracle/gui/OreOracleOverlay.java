package com.teeknox.oreoracle.gui;

import com.teeknox.oreoracle.config.ModConfig;
import com.teeknox.oreoracle.config.ServerDataManager;
import com.teeknox.oreoracle.data.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * HUD overlay that displays ore probability information based on the player's Y-level.
 * Follows Teeknox STYLE_GUIDE.md specifications.
 */
public class OreOracleOverlay {
    // Style constants from STYLE_GUIDE.md
    private static final int BG_OVERLAY = 0x90000000;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_MUTED = 0xFF666666;
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT_TEXT = 10;
    private static final int LINE_HEIGHT_ICON = 18; // Taller for 16x16 item icons
    private static final int ICON_SIZE = 16;
    private static final int STANDARD_WIDTH = 90; // Narrower width (was 120)

    // Peak indicator
    private static final String PEAK_INDICATOR = " \u2605"; // Star character

    private static OreOracleOverlay instance;

    // Cached state to avoid recalculating every frame
    private int lastY = Integer.MIN_VALUE;
    private Identifier lastBiome = null;
    private Dimension lastDimension = null;
    private List<OreEntry> cachedEntries = new ArrayList<>();

    private OreOracleOverlay() {}

    public static OreOracleOverlay getInstance() {
        if (instance == null) {
            instance = new OreOracleOverlay();
        }
        return instance;
    }

    /**
     * Render the overlay. Called from HudElementRegistry.
     */
    public void render(GuiGraphicsExtractor context, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        ModConfig config = ModConfig.getInstance();

        // Don't render if disabled or no player
        if (!config.isEnabled() || client.player == null || client.level == null) {
            return;
        }

        // Don't render when a screen is open
        if (client.gui.screen() != null) {
            return;
        }

        // Get current state
        int currentY = (int) client.player.getY();
        Identifier currentBiome = BiomeChecker.getCurrentBiome(client);
        Dimension currentDimension = Dimension.fromWorld(client.level);

        // Recalculate entries if state changed
        if (currentY != lastY || !biomeEquals(currentBiome, lastBiome) || currentDimension != lastDimension) {
            lastY = currentY;
            lastBiome = currentBiome;
            lastDimension = currentDimension;
            updateCachedEntries(currentY, currentBiome, currentDimension);
        }

        // Don't render if no entries to show
        if (cachedEntries.isEmpty()) {
            return;
        }

        Font textRenderer = client.font;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        // Calculate position
        int overlayWidth = calculateWidth(textRenderer);
        int overlayHeight = calculateHeight(config);

        int x = calculateX(config, screenWidth, overlayWidth);
        int y = calculateY(config, screenHeight, overlayHeight);

        // Draw background (no border per STYLE_GUIDE.md)
        context.fill(x, y, x + overlayWidth, y + overlayHeight, BG_OVERLAY);

        // Draw content
        int contentY = y + PADDING;

        // Determine line height based on display mode
        int lineHeight = config.getDisplayMode() == ModConfig.DisplayMode.ICON ? LINE_HEIGHT_ICON : LINE_HEIGHT_TEXT;

        // Optional header
        if (config.isShowHudHeader()) {
            String header = "Y: " + lastY;
            context.centeredText(textRenderer, header, x + overlayWidth / 2, contentY, TEXT_PRIMARY);
            contentY += LINE_HEIGHT_TEXT + PADDING;
        }

        // Draw ore entries
        int maxVisible = config.getMaxVisibleOres();
        int entriesToShow = Math.min(cachedEntries.size(), maxVisible);

        for (int i = 0; i < entriesToShow; i++) {
            OreEntry entry = cachedEntries.get(i);
            renderOreEntry(context, textRenderer, entry, x + PADDING, contentY, overlayWidth - PADDING * 2, config);
            contentY += lineHeight;
        }

        // Overflow indicator
        int remaining = cachedEntries.size() - entriesToShow;
        if (remaining > 0) {
            String overflow = "+" + remaining + " more";
            context.centeredText(textRenderer, overflow, x + overlayWidth / 2, contentY, TEXT_MUTED);
        }
    }

    private void renderOreEntry(GuiGraphicsExtractor context, Font textRenderer, OreEntry entry,
                                 int x, int y, int availableWidth, ModConfig config) {
        if (config.getDisplayMode() == ModConfig.DisplayMode.ICON) {
            // Icon mode: colored dot + item icon + peak star
            int indicatorSize = 6;
            int indicatorY = y + (LINE_HEIGHT_ICON - indicatorSize) / 2;
            context.fill(x, indicatorY, x + indicatorSize, indicatorY + indicatorSize, entry.tier.getColor());

            // Draw item icon
            int iconX = x + indicatorSize + 2;
            int iconY = y + (LINE_HEIGHT_ICON - ICON_SIZE) / 2;
            ItemStack stack = new ItemStack(entry.ore.getIconItem());
            context.item(stack, iconX, iconY);

            // Draw peak indicator star if at peak
            if (entry.isAtPeak) {
                int starX = iconX + ICON_SIZE + 1;
                int starY = y + (LINE_HEIGHT_ICON - textRenderer.lineHeight) / 2;
                context.text(textRenderer, PEAK_INDICATOR.trim(), starX, starY, TEXT_PRIMARY, true);
            }
        } else {
            // Text mode: colored dot + ore name + peak star
            String displayText = entry.ore.getDisplayName();
            if (entry.isAtPeak) {
                displayText += PEAK_INDICATOR;
            }

            int indicatorSize = 6;
            int indicatorY = y + (LINE_HEIGHT_TEXT - indicatorSize) / 2;
            context.fill(x, indicatorY, x + indicatorSize, indicatorY + indicatorSize, entry.tier.getColor());

            int textX = x + indicatorSize + 4;
            context.text(textRenderer, displayText, textX, y, TEXT_PRIMARY, true);
        }
    }

    private void updateCachedEntries(int y, Identifier biome, Dimension dimension) {
        cachedEntries.clear();
        ServerDataManager serverData = ServerDataManager.getInstance();

        for (Ore ore : Ore.values()) {
            // Skip ores from other dimensions
            if (ore.getDimension() != dimension) {
                continue;
            }

            ProbabilityTier tier = OreDistribution.getTier(ore, y, biome);

            // Check if this ore should be displayed
            if (serverData.shouldDisplayOre(ore, tier)) {
                boolean atPeak = tier != ProbabilityTier.NONE && OreDistribution.isAtPeak(ore, y);
                cachedEntries.add(new OreEntry(ore, tier, atPeak));
            }
        }

        // Sort by tier (GREEN first, then YELLOW, RED, NONE)
        cachedEntries.sort((a, b) -> a.tier.ordinal() - b.tier.ordinal());
    }

    private int calculateWidth(Font textRenderer) {
        ModConfig config = ModConfig.getInstance();

        if (config.getDisplayMode() == ModConfig.DisplayMode.ICON) {
            // Icon mode: padding + indicator + gap + icon + gap + star + padding
            // Fixed width since icons are uniform size
            int indicatorWidth = 6 + 2; // indicator size + gap
            int starWidth = textRenderer.width(PEAK_INDICATOR.trim()) + 1;
            return PADDING + indicatorWidth + ICON_SIZE + starWidth + PADDING;
        } else {
            // Text mode: find the widest entry
            int maxTextWidth = 0;
            for (OreEntry entry : cachedEntries) {
                String text = entry.ore.getDisplayName();
                if (entry.isAtPeak) {
                    text += PEAK_INDICATOR;
                }
                maxTextWidth = Math.max(maxTextWidth, textRenderer.width(text));
            }
            // Width = padding + indicator + gap + text + padding
            int indicatorWidth = 6 + 4; // indicator size + gap
            return Math.max(STANDARD_WIDTH, PADDING + indicatorWidth + maxTextWidth + PADDING);
        }
    }

    private int calculateHeight(ModConfig config) {
        int lines = Math.min(cachedEntries.size(), config.getMaxVisibleOres());

        // Account for overflow indicator if needed
        if (cachedEntries.size() > config.getMaxVisibleOres()) {
            lines++;
        }

        int lineHeight = config.getDisplayMode() == ModConfig.DisplayMode.ICON ? LINE_HEIGHT_ICON : LINE_HEIGHT_TEXT;
        int height = PADDING + (lines * lineHeight) + PADDING;

        // Add header height if shown
        if (config.isShowHudHeader()) {
            height += LINE_HEIGHT_TEXT + PADDING;
        }

        return height;
    }

    private int calculateX(ModConfig config, int screenWidth, int overlayWidth) {
        int x = config.getOverlayX();
        if (config.getHudPosition() == ModConfig.HudPosition.RIGHT) {
            // Right side: negative offset means from right edge
            return screenWidth - overlayWidth - Math.abs(x);
        }
        // Left side: positive x is from left edge, negative means from left edge counting backwards
        return x < 0 ? screenWidth + x - overlayWidth : x;
    }

    private int calculateY(ModConfig config, int screenHeight, int overlayHeight) {
        int offset = config.getOverlayY();
        return switch (config.getVerticalPosition()) {
            case TOP -> offset;
            case CENTER -> (screenHeight - overlayHeight) / 2 + offset;
            case BOTTOM -> screenHeight - overlayHeight - Math.abs(offset);
        };
    }

    private boolean biomeEquals(Identifier a, Identifier b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * Force recalculation of entries (called when settings change).
     */
    public void invalidateCache() {
        lastY = Integer.MIN_VALUE;
        lastBiome = null;
        lastDimension = null;
    }

    /**
     * Render the overlay within specified bounds.
     * Used by MC Widgets integration to render within the widget's allocated space.
     *
     * @param context   The draw context
     * @param x         Left edge of the render area
     * @param y         Top edge of the render area
     * @param width     Width of the render area
     * @param height    Height of the render area
     * @param tickDelta Partial tick for smooth animations
     */
    public void renderInBounds(GuiGraphicsExtractor context, int x, int y, int width, int height, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        ModConfig config = ModConfig.getInstance();

        // Don't render if disabled or no player
        if (!config.isEnabled() || client.player == null || client.level == null) {
            return;
        }

        // Get current state
        int currentY = (int) client.player.getY();
        Identifier currentBiome = BiomeChecker.getCurrentBiome(client);
        Dimension currentDimension = Dimension.fromWorld(client.level);

        // Recalculate entries if state changed
        if (currentY != lastY || !biomeEquals(currentBiome, lastBiome) || currentDimension != lastDimension) {
            lastY = currentY;
            lastBiome = currentBiome;
            lastDimension = currentDimension;
            updateCachedEntries(currentY, currentBiome, currentDimension);
        }

        // Don't render if no entries to show
        if (cachedEntries.isEmpty()) {
            return;
        }

        Font textRenderer = client.font;

        // Draw background (no border per STYLE_GUIDE.md)
        context.fill(x, y, x + width, y + height, BG_OVERLAY);

        // Draw content
        int contentY = y + PADDING;

        // Determine line height based on display mode
        int lineHeight = config.getDisplayMode() == ModConfig.DisplayMode.ICON ? LINE_HEIGHT_ICON : LINE_HEIGHT_TEXT;

        // Calculate how many items can fit
        int availableHeight = height - PADDING * 2;
        if (config.isShowHudHeader()) {
            availableHeight -= LINE_HEIGHT_TEXT + PADDING;
        }
        int maxItemsByHeight = Math.max(1, availableHeight / lineHeight);
        int maxVisible = Math.min(config.getMaxVisibleOres(), maxItemsByHeight);

        // Optional header
        if (config.isShowHudHeader()) {
            String header = "Y: " + lastY;
            context.centeredText(textRenderer, header, x + width / 2, contentY, TEXT_PRIMARY);
            contentY += LINE_HEIGHT_TEXT + PADDING;
        }

        // Draw ore entries
        int entriesToShow = Math.min(cachedEntries.size(), maxVisible);

        for (int i = 0; i < entriesToShow; i++) {
            OreEntry entry = cachedEntries.get(i);
            renderOreEntry(context, textRenderer, entry, x + PADDING, contentY, width - PADDING * 2, config);
            contentY += lineHeight;
        }

        // Overflow indicator
        int remaining = cachedEntries.size() - entriesToShow;
        if (remaining > 0 && contentY + LINE_HEIGHT_TEXT <= y + height) {
            String overflow = "+" + remaining + " more";
            context.centeredText(textRenderer, overflow, x + width / 2, contentY, TEXT_MUTED);
        }
    }

    /**
     * Represents an ore entry to display in the HUD.
     */
    private record OreEntry(Ore ore, ProbabilityTier tier, boolean isAtPeak) {}
}
