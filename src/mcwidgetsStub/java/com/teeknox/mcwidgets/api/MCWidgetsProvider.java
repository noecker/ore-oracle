package com.teeknox.mcwidgets.api;

import java.util.List;

/**
 * COMPILE-ONLY STUB of the MC Widgets API, ported to Minecraft 26.x types.
 *
 * These classes are never packaged into the ore-oracle jar; at runtime the real
 * classes are provided by the mc-widgets mod. The 26.x port of mc-widgets must
 * keep these exact signatures (DrawContext -> GuiGraphicsExtractor,
 * net.minecraft.client.gui.screen.Screen -> net.minecraft.client.gui.screens.Screen)
 * for the integration to link. Once mc-widgets publishes a 26.x jar, this stub
 * source set can be replaced with a compileOnly dependency on that jar.
 */
public interface MCWidgetsProvider {

    /**
     * Called during MC Widgets initialization to discover available widgets.
     */
    List<WidgetDefinition> getWidgets();
}
