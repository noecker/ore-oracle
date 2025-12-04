# Teeknox Mods Visual Style Guide

This document defines the consistent visual style to be used across all Teeknox mods. All mods should be updated to match these specifications.

**Reference Implementations:**
- **Window Style (HUD & Modal):** Whisper Window
- **Settings Screen Layout:** Clockwork Config's export screen (centered column)

---

## Table of Contents

1. [Color Palette](#color-palette)
2. [Typography](#typography)
3. [Window Dimensions](#window-dimensions)
4. [HUD Overlay Windows](#hud-overlay-windows)
5. [Pop-up Windows & Settings Screens](#pop-up-windows--settings-screens)
6. [Common UI Components](#common-ui-components)
7. [Implementation Reference](#implementation-reference)

---

## Color Palette

**IMPORTANT: All color values MUST use 8-digit ARGB format (0xAARRGGBB).** Never use 6-digit RGB hex values. Minecraft's rendering APIs expect the alpha channel, and omitting it causes undefined behavior (usually fully transparent or fully opaque depending on context).

- `0xFF` prefix = fully opaque (255 alpha)
- `0x90` prefix = ~56% transparent (144 alpha) - used for HUD overlays
- `0xE0` prefix = ~88% opaque (224 alpha) - used for modal backgrounds
- `0x80` prefix = 50% transparent (128 alpha)
- `0x40` prefix = 25% transparent (64 alpha)
- `0x20` prefix = ~12% transparent (32 alpha)
- `0x00` prefix = fully transparent (0 alpha)

### Core Colors (ARGB Format)

Based on Whisper Window's color scheme:

```java
// Backgrounds
public static final int BG_OVERLAY = 0x90000000;          // HUD overlay background (~56% transparent black)
public static final int BG_MODAL = 0xE0000000;            // Modal/fullscreen background (~88% opaque black)
public static final int BG_DIM = 0x80000000;              // Screen dim overlay behind modals (50%)

// Text
public static final int TEXT_PRIMARY = 0xFFFFFFFF;        // Primary text (white)
public static final int TEXT_SECONDARY = 0xFF888888;      // Secondary/timestamp text (gray)
public static final int TEXT_MUTED = 0xFF666666;          // Muted text, separators (dark gray)

// Semantic Colors
public static final int COLOR_SENT = 0xFF55FFFF;          // Sent/outgoing messages (cyan)
public static final int COLOR_RECEIVED = 0xFFFFFFFF;      // Received/incoming messages (white)
public static final int COLOR_SEPARATOR = 0xFF666666;     // Date separators

// Scrollbar (subtle, semi-transparent)
public static final int SCROLLBAR_TRACK = 0x20FFFFFF;     // Scrollbar track (very subtle white)
public static final int SCROLLBAR_THUMB = 0x40FFFFFF;     // Scrollbar thumb (subtle white)
public static final int SCROLLBAR_THUMB_HOVER = 0x60FFFFFF; // Scrollbar thumb hover/drag

// For modal/maximized screens (more visible scrollbar)
public static final int SCROLLBAR_TRACK_MODAL = 0x40FFFFFF;
public static final int SCROLLBAR_THUMB_MODAL = 0x80FFFFFF;
public static final int SCROLLBAR_THUMB_MODAL_HOVER = 0xA0FFFFFF;

// Settings screen colors (from Clockwork Config export screen)
public static final int SETTINGS_BG = 0x80000000;         // Settings screen dimmed background
public static final int SETTINGS_CHECKBOX_OUTLINE = 0xFFAAAAAA;
public static final int SETTINGS_CHECKBOX_CHECKED = 0xFF00FF00;
public static final int SETTINGS_MODIFIED = 0xFFFFFF00;   // Yellow for modified/non-default values
public static final int SETTINGS_UNSELECTED = 0xFF666666;
```

### Color Usage Summary

| Context | Background | Text | Scrollbar |
|---------|------------|------|-----------|
| HUD Overlay | `BG_OVERLAY` (0x90000000) | `TEXT_PRIMARY` | Subtle (0x40FFFFFF) |
| Modal/Fullscreen | `BG_MODAL` (0xE0000000) | `TEXT_PRIMARY` | Visible (0x80FFFFFF) |
| Settings Screen | `SETTINGS_BG` (0x80000000) | `TEXT_PRIMARY` | N/A |

---

## Typography

Minecraft's TextRenderer uses a fixed font (font height ~9px).

| Element | Line Height | Notes |
|---------|-------------|-------|
| Standard text | 12px | Default for modal/maximized screens |
| Compact text | 10px | Dense lists, HUD overlays |
| Separator height | 12-14px | Date/section separators |

### Text Rendering Patterns

```java
// Primary text with shadow (default)
context.drawText(textRenderer, text, x, y, TEXT_PRIMARY, true);

// Secondary text without shadow
context.drawText(textRenderer, text, x, y, TEXT_SECONDARY, false);

// Centered title with shadow
context.drawCenteredTextWithShadow(textRenderer, title, centerX, y, TEXT_PRIMARY);
```

---

## Window Dimensions

**Standard window width: 300 pixels** (from Whisper Window)

This width is used for:
- HUD overlay windows (default width)
- Modal/maximized windows
- Settings screen central column

```java
// Core dimensions (from Whisper Window)
public static final int STANDARD_WIDTH = 300;             // Standard window width
public static final int MIN_WIDTH = 150;                  // Minimum configurable width
public static final int MAX_WIDTH = 500;                  // Maximum width for maximized screens

// Height is typically calculated dynamically:
// - HUD overlays: Based on content + configurable max height
// - Modal screens: 85% of screen height, capped at content needs
```

### Maximized/Modal Screen Sizing

```java
// For maximized/modal screens (from Whisper Window's MaximizedWhisperScreen)
int screenWidth = this.width;
int screenHeight = this.height;

// Width: min(500px, screenWidth - 40px margins)
int modalWidth = Math.min(MAX_WIDTH, screenWidth - 40);

// Height: 85% of screen height
int modalHeight = (int)(screenHeight * 0.85f);

// Center on screen
int modalX = (screenWidth - modalWidth) / 2;
int modalY = (screenHeight - modalHeight) / 2;
```

---

## HUD Overlay Windows

HUD overlays render on top of the game world. **Style based on Whisper Window.**

### Configuration Option: Header Toggle

**Mods with HUD windows MUST provide a settings option to show/hide the header.**

```java
// In Config class
private boolean showHudHeader = true;  // Default: header visible

public boolean isShowHudHeader() { return showHudHeader; }
public void setShowHudHeader(boolean show) { this.showHudHeader = show; }
```

This setting should appear in the mod's settings screen with a toggle/checkbox.

### Layout Specifications

```java
// From Whisper Window
public static final int PADDING = 4;                      // Inner padding
public static final int LINE_HEIGHT = 10;                 // Compact line height for HUD
public static final int SEPARATOR_HEIGHT = 12;            // Date separator height
// Note: HUD windows do NOT have scrollbars - use maxVisibleItems config instead
```

### Visual Structure

HUD windows have NO border and NO scrollbar:

```
┌────────────────────────────────┐
│       Title (optional)          │  ← Header (if showHudHeader = true)
│                                 │
│ Content line 1                  │
│ Content line 2                  │
│ Content line 3                  │
│       +N more                   │  ← Overflow indicator (if content exceeds max)
│                                 │
└────────────────────────────────┘
    ↑ BG_OVERLAY (0x90000000)
```

**HUD windows NEVER have scrollbars.** If content exceeds the visible area, show an overflow indicator (e.g., "+N more") or truncate content based on a configurable max items setting.

### Position Calculation (Negative Offset Support)

```java
// Negative values = from right/bottom edge
int x = config.getX() < 0
    ? screenWidth + config.getX() - overlayWidth
    : config.getX();
int y = config.getY() < 0
    ? screenHeight + config.getY() - overlayHeight
    : config.getY();
```

### Rendering

```java
public void render(DrawContext context, float tickDelta) {
    // Apply fade if configured
    float alpha = calculateFadeAlpha();
    if (alpha <= 0) return;

    // Background (semi-transparent, NO BORDER)
    int bgColor = applyAlpha(BG_OVERLAY, alpha);
    context.fill(x, y, x + width, y + height, bgColor);

    // Header (optional, based on config)
    int contentStartY = y + PADDING;
    if (config.isShowHudHeader()) {
        int titleColor = applyAlpha(TEXT_PRIMARY, alpha);
        context.drawCenteredTextWithShadow(textRenderer, title,
            x + width / 2, y + PADDING, titleColor);
        contentStartY = y + PADDING + LINE_HEIGHT + PADDING;
    }

    // Content (NO scrollbar - use max items limit instead)
    int maxItems = config.getMaxVisibleItems();
    int itemsToShow = Math.min(items.size(), maxItems);

    for (int i = 0; i < itemsToShow; i++) {
        renderItem(context, items.get(i), x + PADDING, contentStartY + i * LINE_HEIGHT, alpha);
    }

    // Overflow indicator (if more items exist)
    int remaining = items.size() - itemsToShow;
    if (remaining > 0) {
        String overflow = "+" + remaining + " more";
        int overflowColor = applyAlpha(TEXT_MUTED, alpha);
        context.drawCenteredTextWithShadow(textRenderer, overflow,
            x + width / 2, contentStartY + itemsToShow * LINE_HEIGHT, overflowColor);
    }
}
```

### Fade Effect

```java
private long lastActivityTime = System.currentTimeMillis();

public void resetFadeTimer() {
    lastActivityTime = System.currentTimeMillis();
}

private float calculateFadeAlpha() {
    int timeout = config.getFadeTimeoutSeconds();
    if (timeout < 0) return 1.0f;  // Never fade

    long elapsed = System.currentTimeMillis() - lastActivityTime;
    long fadeStart = timeout * 1000L;
    long fadeDuration = 1000L;

    if (elapsed < fadeStart) return 1.0f;

    float progress = (elapsed - fadeStart) / (float) fadeDuration;
    return Math.max(0f, 1f - progress);
}

private int applyAlpha(int color, float alpha) {
    int a = (int) (((color >> 24) & 0xFF) * alpha);
    return (a << 24) | (color & 0x00FFFFFF);
}
```

---

## Pop-up Windows & Settings Screens

Pop-up windows (modals, settings, history views) follow Clockwork Config's export screen pattern: **left-aligned content within a centered column** matching the standard window width (300px).

### Scrollbar Requirement

**Pop-up windows that scroll MUST always show a scrollbar** (in addition to supporting mouse wheel scrolling). This provides visual feedback that more content exists and allows precise navigation.

### Layout Structure

```
Full Screen (dimmed background: 0x80000000)
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│                    ┌──────────────────────┬──┐                  │
│                    │   Settings Title     │  │  ← Centered title│
│                    │ Optional subtitle    │  │  ← Gray subtitle │
│                    ├──────────────────────┤▒▒│                  │
│                    │ □ Option 1           │▒▒│  ← Left-aligned  │
│                    │ □ Option 2           │▒▒│    in 300px col  │
│                    │ ▼ Group header       │  │                  │
│                    │   □ Sub-option 1     │  │  ← Indented 20px │
│                    │   □ Sub-option 2     │  │                  │
│                    │                      │  │                  │
│                    │ [Cancel]    [Done]   │  │  ← Buttons       │
│                    └──────────────────────┴──┘                  │
│                                                                 │
│                         ↑ 300px width    ↑ scrollbar (always)   │
└─────────────────────────────────────────────────────────────────┘
```

**Note:** Scrollbar is ALWAYS visible when content is scrollable. Use wheel scrolling AND scrollbar dragging.

### Layout Constants

```java
// From Clockwork Config export screen
public static final int COLUMN_WIDTH = 300;               // Central column width (matches STANDARD_WIDTH)
public static final int HEADER_HEIGHT = 52;               // Title + subtitle area
public static final int FOOTER_HEIGHT = 50;               // Button area
public static final int ROW_HEIGHT = 20;                  // Each option row
public static final int INDENT = 20;                      // Sub-item indentation
public static final int CHECKBOX_SIZE = 12;               // Checkbox dimensions
public static final int LEFT_MARGIN = 20;                 // Left margin within column
```

### Column Calculation

```java
@Override
protected void init() {
    int screenWidth = this.width;
    int screenHeight = this.height;

    // Center the column
    int columnX = (screenWidth - COLUMN_WIDTH) / 2;
    int columnY = HEADER_HEIGHT;
    int columnHeight = screenHeight - HEADER_HEIGHT - FOOTER_HEIGHT;

    // Content is left-aligned within the column
    int contentX = columnX + LEFT_MARGIN;
    int contentWidth = COLUMN_WIDTH - LEFT_MARGIN * 2;

    // ... initialize widgets
}
```

### Rendering Implementation

```java
@Override
public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    // 1. Dim the entire background
    context.fill(0, 0, this.width, this.height, SETTINGS_BG);

    // 2. Centered title
    context.drawCenteredTextWithShadow(this.textRenderer, this.title,
        this.width / 2, 16, TEXT_PRIMARY);

    // 3. Optional subtitle (gray)
    if (subtitle != null) {
        context.drawCenteredTextWithShadow(this.textRenderer, subtitle,
            this.width / 2, 30, TEXT_SECONDARY);
    }

    // 4. Calculate column position
    int columnX = (this.width - COLUMN_WIDTH) / 2;
    int contentY = HEADER_HEIGHT;
    int contentHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;

    // 5. Enable scissor for scrollable content
    context.enableScissor(columnX, contentY, columnX + COLUMN_WIDTH, contentY + contentHeight);

    // 6. Render content rows (left-aligned within column)
    renderContent(context, columnX + LEFT_MARGIN, contentY - scrollOffset, mouseX, mouseY);

    context.disableScissor();

    // 7. Render widgets (buttons at bottom, centered)
    super.render(context, mouseX, mouseY, delta);
}
```

### Content Row Rendering

```java
private void renderContent(DrawContext context, int x, int y, int mouseX, int mouseY) {
    int currentY = y;

    for (SettingsEntry entry : entries) {
        // Indentation for sub-items
        int entryX = x + (entry.isSubItem() ? INDENT : 0);

        // Checkbox
        int checkboxY = currentY + (ROW_HEIGHT - CHECKBOX_SIZE) / 2;
        drawCheckbox(context, entryX, checkboxY, entry.isSelected());

        // Label (left-aligned after checkbox)
        int textColor = entry.isSelected() ? TEXT_PRIMARY : SETTINGS_UNSELECTED;
        if (entry.isModified()) {
            textColor = SETTINGS_MODIFIED;
        }
        context.drawText(textRenderer, entry.getLabel(),
            entryX + CHECKBOX_SIZE + 8, currentY + 5, textColor, false);

        currentY += ROW_HEIGHT;
    }
}

private void drawCheckbox(DrawContext context, int x, int y, boolean checked) {
    // Outer border
    context.fill(x, y, x + CHECKBOX_SIZE, y + 1, SETTINGS_CHECKBOX_OUTLINE);
    context.fill(x, y + CHECKBOX_SIZE - 1, x + CHECKBOX_SIZE, y + CHECKBOX_SIZE, SETTINGS_CHECKBOX_OUTLINE);
    context.fill(x, y, x + 1, y + CHECKBOX_SIZE, SETTINGS_CHECKBOX_OUTLINE);
    context.fill(x + CHECKBOX_SIZE - 1, y, x + CHECKBOX_SIZE, y + CHECKBOX_SIZE, SETTINGS_CHECKBOX_OUTLINE);

    // Fill if checked
    if (checked) {
        context.fill(x + 2, y + 2, x + CHECKBOX_SIZE - 2, y + CHECKBOX_SIZE - 2, SETTINGS_CHECKBOX_CHECKED);
    }
}
```

### Button Layout

```java
// Buttons centered at bottom
int buttonY = this.height - FOOTER_HEIGHT + 15;
int buttonWidth = 80;
int buttonSpacing = 10;
int totalButtonWidth = buttonWidth * 2 + buttonSpacing;
int buttonStartX = (this.width - totalButtonWidth) / 2;

// Cancel button (left)
addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> close())
    .dimensions(buttonStartX, buttonY, buttonWidth, 20).build());

// Done button (right)
addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> save())
    .dimensions(buttonStartX + buttonWidth + buttonSpacing, buttonY, buttonWidth, 20).build());
```

---

## Common UI Components

### Border Helper

```java
public static void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
    context.fill(x, y, x + w, y + 1, color);           // Top
    context.fill(x, y + h - 1, x + w, y + h, color);   // Bottom
    context.fill(x, y, x + 1, y + h, color);           // Left
    context.fill(x + w - 1, y, x + w, y + h, color);   // Right
}
```

### Text Truncation

```java
public static String truncate(TextRenderer textRenderer, String text, int maxWidth) {
    if (textRenderer.getWidth(text) <= maxWidth) {
        return text;
    }
    while (text.length() > 0 && textRenderer.getWidth(text + "...") > maxWidth) {
        text = text.substring(0, text.length() - 1);
    }
    return text + "...";
}
```

### Text Wrapping

```java
public static List<String> wrapText(TextRenderer textRenderer, String text, int maxWidth) {
    List<String> lines = new ArrayList<>();
    if (text == null || text.isEmpty()) {
        lines.add("");
        return lines;
    }

    if (textRenderer.getWidth(text) <= maxWidth) {
        lines.add(text);
        return lines;
    }

    StringBuilder line = new StringBuilder();
    for (String word : text.split(" ")) {
        if (line.length() == 0) {
            if (textRenderer.getWidth(word) > maxWidth) {
                for (char c : word.toCharArray()) {
                    if (textRenderer.getWidth(line.toString() + c) > maxWidth && line.length() > 0) {
                        lines.add(line.toString());
                        line = new StringBuilder();
                    }
                    line.append(c);
                }
            } else {
                line.append(word);
            }
        } else if (textRenderer.getWidth(line + " " + word) > maxWidth) {
            lines.add(line.toString());
            line = new StringBuilder(word);
        } else {
            line.append(" ").append(word);
        }
    }
    if (line.length() > 0) {
        lines.add(line.toString());
    }
    return lines;
}
```

### Date Separator

```java
private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM d, yyyy");

public static void drawDateSeparator(DrawContext context, TextRenderer textRenderer,
                                      long timestamp, int x, int width, int y) {
    String text = "— " + DATE_FORMAT.format(new Date(timestamp)) + " —";
    int textWidth = textRenderer.getWidth(text);
    int textX = x + (width - textWidth) / 2;
    context.drawText(textRenderer, text, textX, y, COLOR_SEPARATOR, true);
}
```

### Scrollbar Rendering (HUD style)

```java
private void renderScrollbar(DrawContext context, int x, int y, int height, float alpha) {
    // Track (subtle)
    int trackColor = applyAlpha(SCROLLBAR_TRACK, alpha);
    context.fill(x, y, x + SCROLLBAR_WIDTH, y + height, trackColor);

    // Calculate thumb position and size
    float viewRatio = (float) visibleHeight / totalContentHeight;
    int thumbHeight = Math.max(SCROLLBAR_MIN_THUMB, (int) (height * viewRatio));
    int maxThumbY = height - thumbHeight;
    int thumbY = maxScrollOffset > 0
        ? (int) ((float) scrollOffset / maxScrollOffset * maxThumbY)
        : 0;

    // Thumb color (brighter on hover/drag)
    int thumbColor = isDragging ? SCROLLBAR_THUMB_HOVER : SCROLLBAR_THUMB;
    thumbColor = applyAlpha(thumbColor, alpha);

    // Draw thumb
    context.fill(x, y + thumbY, x + SCROLLBAR_WIDTH, y + thumbY + thumbHeight, thumbColor);
}
```

### Mouse Scroll Handling

```java
@Override
public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
    // Accumulate for macOS trackpad smooth scrolling
    accumulatedScroll += vAmount;
    int lines = (int) accumulatedScroll;
    if (lines != 0) {
        accumulatedScroll -= lines;
        scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset - lines * LINE_HEIGHT * 3));
    }
    return true;
}
```

---

## Implementation Reference

### TeeknoxStyle Constants Class

Include this class in each mod for consistent styling:

```java
package com.teeknox.common;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;

public final class TeeknoxStyle {
    private TeeknoxStyle() {}

    // === COLORS (Whisper Window style) ===

    // Backgrounds
    public static final int BG_OVERLAY = 0x90000000;      // HUD overlay
    public static final int BG_MODAL = 0xE0000000;        // Modal/maximized screens
    public static final int BG_DIM = 0x80000000;          // Settings screen dim

    // Text
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFF888888;
    public static final int TEXT_MUTED = 0xFF666666;

    // Semantic
    public static final int COLOR_SENT = 0xFF55FFFF;
    public static final int COLOR_RECEIVED = 0xFFFFFFFF;
    public static final int COLOR_SEPARATOR = 0xFF666666;

    // Scrollbar (HUD - subtle)
    public static final int SCROLLBAR_TRACK = 0x20FFFFFF;
    public static final int SCROLLBAR_THUMB = 0x40FFFFFF;
    public static final int SCROLLBAR_THUMB_HOVER = 0x60FFFFFF;

    // Scrollbar (Modal - more visible)
    public static final int SCROLLBAR_TRACK_MODAL = 0x40FFFFFF;
    public static final int SCROLLBAR_THUMB_MODAL = 0x80FFFFFF;
    public static final int SCROLLBAR_THUMB_MODAL_HOVER = 0xA0FFFFFF;

    // Settings screen
    public static final int SETTINGS_CHECKBOX_OUTLINE = 0xFFAAAAAA;
    public static final int SETTINGS_CHECKBOX_CHECKED = 0xFF00FF00;
    public static final int SETTINGS_MODIFIED = 0xFFFFFF00;
    public static final int SETTINGS_UNSELECTED = 0xFF666666;

    // === DIMENSIONS ===

    // Standard widths
    public static final int STANDARD_WIDTH = 300;         // Default window width
    public static final int MIN_WIDTH = 150;
    public static final int MAX_WIDTH = 500;              // Maximized screens

    // HUD overlay spacing
    public static final int HUD_PADDING = 4;
    public static final int HUD_LINE_HEIGHT = 10;
    public static final int HUD_SEPARATOR_HEIGHT = 12;

    // Modal/maximized spacing
    public static final int MODAL_PADDING = 8;
    public static final int MODAL_LINE_HEIGHT = 12;
    public static final int MODAL_SEPARATOR_HEIGHT = 14;
    public static final int MODAL_TITLE_HEIGHT = 20;
    public static final int MODAL_BUTTON_HEIGHT = 20;
    public static final int MODAL_BUTTON_WIDTH = 80;

    // Settings screen
    public static final int SETTINGS_HEADER_HEIGHT = 52;
    public static final int SETTINGS_FOOTER_HEIGHT = 50;
    public static final int SETTINGS_ROW_HEIGHT = 20;
    public static final int SETTINGS_INDENT = 20;
    public static final int SETTINGS_CHECKBOX_SIZE = 12;
    public static final int SETTINGS_LEFT_MARGIN = 20;

    // Scrollbar
    public static final int SCROLLBAR_WIDTH = 6;
    public static final int SCROLLBAR_WIDTH_MODAL = 8;
    public static final int SCROLLBAR_MIN_THUMB = 20;
    public static final int SCROLLBAR_MIN_THUMB_MODAL = 24;

    // === HELPERS ===

    public static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static String truncate(TextRenderer tr, String text, int maxWidth) {
        if (tr.getWidth(text) <= maxWidth) return text;
        while (text.length() > 0 && tr.getWidth(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    public static List<String> wrapText(TextRenderer tr, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) { lines.add(""); return lines; }
        if (tr.getWidth(text) <= maxWidth) { lines.add(text); return lines; }

        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() == 0) {
                if (tr.getWidth(word) > maxWidth) {
                    for (char c : word.toCharArray()) {
                        if (tr.getWidth(line.toString() + c) > maxWidth && line.length() > 0) {
                            lines.add(line.toString());
                            line = new StringBuilder();
                        }
                        line.append(c);
                    }
                } else {
                    line.append(word);
                }
            } else if (tr.getWidth(line + " " + word) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line.append(" ").append(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    /**
     * Calculate modal bounds centered on screen.
     * @return int[] {modalX, modalY, modalWidth, modalHeight}
     */
    public static int[] calculateModalBounds(int screenWidth, int screenHeight) {
        int modalWidth = Math.min(MAX_WIDTH, screenWidth - 40);
        int modalHeight = (int)(screenHeight * 0.85f);
        int modalX = (screenWidth - modalWidth) / 2;
        int modalY = (screenHeight - modalHeight) / 2;
        return new int[] { modalX, modalY, modalWidth, modalHeight };
    }
}
```

### Migration Checklist

For each mod with a HUD window:

- [ ] Update window width to use `STANDARD_WIDTH` (300px)
- [ ] Update background color to `BG_OVERLAY` (0x90000000)
- [ ] Remove any borders from HUD windows
- [ ] Remove any scrollbars from HUD windows (use overflow indicator instead)
- [ ] Add `showHudHeader` config option with toggle in settings
- [ ] Add `maxVisibleItems` config option if applicable

For each mod with a pop-up/settings screen:

- [ ] Update to use centered column layout (300px wide)
- [ ] Content left-aligned within the column
- [ ] Use `SETTINGS_BG` (0x80000000) for background dim
- [ ] Ensure scrollbar is ALWAYS visible when content scrolls
- [ ] Support both wheel scrolling AND scrollbar dragging
- [ ] Match Clockwork Config export screen styling

---

*This unified style guide ensures visual consistency across all Teeknox Minecraft mods.*
