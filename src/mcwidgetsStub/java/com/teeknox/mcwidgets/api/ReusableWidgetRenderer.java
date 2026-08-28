package com.teeknox.mcwidgets.api;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/**
 * COMPILE-ONLY STUB of the MC Widgets API. See {@link MCWidgetsProvider}.
 */
public interface ReusableWidgetRenderer extends WidgetRenderer {

    void render(GuiGraphicsExtractor context, WidgetBounds bounds, float tickDelta, JsonObject config);

    @Override
    default void render(GuiGraphicsExtractor context, WidgetBounds bounds, float tickDelta) {
        render(context, bounds, tickDelta, new JsonObject());
    }

    JsonObject getDefaultConfig();

    Screen createConfigScreen(Screen parent, String instanceId);
}
