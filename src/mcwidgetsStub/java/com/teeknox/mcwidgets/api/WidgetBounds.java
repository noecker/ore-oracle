package com.teeknox.mcwidgets.api;

/**
 * COMPILE-ONLY STUB of the MC Widgets API. See {@link MCWidgetsProvider}.
 */
public record WidgetBounds(int x, int y, int width, int height) {

    public boolean contains(int px, int py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }

    public WidgetBounds withPadding(int padding) {
        return new WidgetBounds(
                x + padding,
                y + padding,
                Math.max(0, width - padding * 2),
                Math.max(0, height - padding * 2)
        );
    }
}
