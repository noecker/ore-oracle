package com.teeknox.modtemplate.gui;

import com.teeknox.modtemplate.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Example HUD overlay renderer.
 *
 * Register with HudRenderCallback.EVENT in your ClientModInitializer:
 *
 *   ExampleOverlay overlay = new ExampleOverlay();
 *   HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
 *       overlay.render(drawContext, 1.0f);
 *   });
 *
 * Rendering notes (1.21.9+):
 * - Use DrawContext for all rendering operations
 * - context.fill() for rectangles
 * - context.drawText() for text
 * - context.drawTexture() for textures
 * - drawBorder() helper for custom borders (built-in drawBorder was removed)
 */
public class ExampleOverlay {
    // Styling constants
    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 12;

    // Colors (ARGB format)
    private static final int BG_COLOR = 0x80000000;       // Semi-transparent black
    private static final int BORDER_COLOR = 0xFF444444;   // Dark gray
    private static final int TEXT_COLOR = 0xFFFFFFFF;     // White
    private static final int HEADER_COLOR = 0xFFFFCC00;   // Gold

    /**
     * Render the overlay.
     *
     * @param context DrawContext for rendering
     * @param tickDelta Partial tick for smooth animations
     */
    public void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        ModConfig config = ModConfig.getInstance();

        if (!config.isEnabled()) {
            return;
        }

        // Calculate dimensions
        int boxWidth = 120;
        int boxHeight = PADDING * 2 + LINE_HEIGHT * 2;

        // Calculate position (supports negative offset for right/bottom alignment)
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int x = config.calculateActualX(screenWidth, boxWidth);
        int y = config.calculateActualY(screenHeight, boxHeight);

        // Draw background
        context.fill(x, y, x + boxWidth, y + boxHeight, BG_COLOR);

        // Draw border
        drawBorder(context, x, y, boxWidth, boxHeight, BORDER_COLOR);

        // Draw content
        context.drawText(textRenderer, "Example Overlay", x + PADDING, y + PADDING, HEADER_COLOR, true);
        context.drawText(textRenderer, "It works!", x + PADDING, y + PADDING + LINE_HEIGHT, TEXT_COLOR, false);
    }

    /**
     * Draw a 1-pixel border around a rectangle.
     * (Built-in drawBorder was removed in 1.21.9+)
     */
    private static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        // Top edge
        context.fill(x, y, x + width, y + 1, color);
        // Bottom edge
        context.fill(x, y + height - 1, x + width, y + height, color);
        // Left edge
        context.fill(x, y, x + 1, y + height, color);
        // Right edge
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
}
