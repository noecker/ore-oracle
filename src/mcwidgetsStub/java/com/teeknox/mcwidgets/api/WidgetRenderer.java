package com.teeknox.mcwidgets.api;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * COMPILE-ONLY STUB of the MC Widgets API. See {@link MCWidgetsProvider}.
 * 26.x port of the pre-26.x signature {@code render(DrawContext, WidgetBounds, float)}.
 */
@FunctionalInterface
public interface WidgetRenderer {

    void render(GuiGraphicsExtractor context, WidgetBounds bounds, float tickDelta);
}
