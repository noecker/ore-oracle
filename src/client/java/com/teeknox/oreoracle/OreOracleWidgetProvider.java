package com.teeknox.oreoracle;

import com.teeknox.mcwidgets.api.MCWidgetsProvider;
import com.teeknox.mcwidgets.api.WidgetBounds;
import com.teeknox.mcwidgets.api.WidgetDefinition;
import com.teeknox.oreoracle.gui.OreOracleOverlay;

import java.util.List;

/**
 * MC Widgets integration provider for Ore Oracle.
 * Registers the ore oracle HUD as a widget that can be managed through MC Widgets.
 */
public class OreOracleWidgetProvider implements MCWidgetsProvider {

    @Override
    public List<WidgetDefinition> getWidgets() {
        return List.of(
            WidgetDefinition.simple(
                "ore-oracle:ore-display",
                "Ore Oracle",
                (context, bounds, tickDelta) -> {
                    OreOracleOverlay.getInstance().renderInBounds(
                        context,
                        bounds.x(),
                        bounds.y(),
                        bounds.width(),
                        bounds.height(),
                        tickDelta
                    );
                }
            )
        );
    }
}
