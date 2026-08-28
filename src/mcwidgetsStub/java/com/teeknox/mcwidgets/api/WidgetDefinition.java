package com.teeknox.mcwidgets.api;

/**
 * COMPILE-ONLY STUB of the MC Widgets API. See {@link MCWidgetsProvider}.
 */
public record WidgetDefinition(
        String id,
        String displayName,
        int minColumns,
        int minRows,
        WidgetRenderer renderer,
        boolean isReusable
) {
    public WidgetDefinition(String id, String displayName, int minColumns, int minRows, WidgetRenderer renderer) {
        this(id, displayName, minColumns, minRows, renderer, false);
    }

    public static WidgetDefinition simple(String id, String displayName, WidgetRenderer renderer) {
        return new WidgetDefinition(id, displayName, 1, 1, renderer, false);
    }

    public static WidgetDefinition reusable(String id, String displayName, int minColumns, int minRows,
                                            ReusableWidgetRenderer renderer) {
        return new WidgetDefinition(id, displayName, minColumns, minRows, renderer, true);
    }

    public static WidgetDefinition simpleReusable(String id, String displayName, ReusableWidgetRenderer renderer) {
        return new WidgetDefinition(id, displayName, 1, 1, renderer, true);
    }

    public String modId() {
        int colonIndex = id.indexOf(':');
        return colonIndex > 0 ? id.substring(0, colonIndex) : "";
    }

    public String widgetName() {
        int colonIndex = id.indexOf(':');
        return colonIndex >= 0 ? id.substring(colonIndex + 1) : id;
    }
}
