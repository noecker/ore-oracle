# MC Widgets Integration Guide

This guide explains how to integrate your Fabric mod with MC Widgets, allowing your HUD elements to be managed through MC Widgets' unified grid-based layout system.

---

## Changelog

### v1.1.0

**New Features:**

- **Reusable Widgets**: Widgets can now be placed multiple times with per-instance configuration. See [Reusable Widgets](#reusable-widgets).
  - New `ReusableWidgetRenderer` interface
  - New factory methods: `WidgetDefinition.reusable()` and `simpleReusable()`
  - Instance IDs use `base-id#N` format (e.g., `mymod:widget#1`)

- **Visibility Conditions API**: External mods can register custom visibility conditions. See [Custom Visibility Conditions](#custom-visibility-conditions).
  - New `VisibilityCondition` interface
  - New `VisibilityConditionRegistry` for registering condition types and categories

**API Changes:**

- `WidgetDefinition` record now includes an `isReusable` boolean field
- Existing code using the 5-parameter constructor remains compatible (defaults to non-reusable)

**Migration:** No changes required for existing integrations. New features are opt-in.

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Detailed Integration Steps](#detailed-integration-steps)
4. [Reusable Widgets](#reusable-widgets)
5. [Custom Visibility Conditions](#custom-visibility-conditions)
6. [API Reference](#api-reference)
7. [Best Practices](#best-practices)
8. [Example: Complete Integration](#example-complete-integration)
9. [Troubleshooting](#troubleshooting)

---

## Overview

### What MC Widgets Provides

MC Widgets is a HUD widget management system that:

- Divides the screen into a configurable grid
- Allows users to position and resize widgets via a visual config screen
- Manages rendering order and prevents widget overlap
- Persists widget layouts per-world

### Integration Model

MC Widgets uses a **soft dependency** model. Your mod:

1. Works independently when MC Widgets is not installed
2. Automatically registers widgets when MC Widgets is present
3. Delegates HUD positioning to MC Widgets when available

This ensures your mod works for all users, with enhanced functionality for those who have MC Widgets.

---

## Quick Start

### 1. Add Dependency

```gradle
// build.gradle
dependencies {
    // Compile-only: your mod builds against the API but doesn't require it at runtime
    modCompileOnly files('../mc-widgets/build/libs/mc-widgets-1.1.0.jar')

    // Or when published to Maven:
    // modCompileOnly "com.teeknox:mc-widgets:1.1.0"
}
```

### 2. Create Widget Provider

```java
package com.example.mymod;

import com.teeknox.mcwidgets.api.MCWidgetsProvider;
import com.teeknox.mcwidgets.api.WidgetDefinition;
import java.util.List;

public class MyModWidgetProvider implements MCWidgetsProvider {
    @Override
    public List<WidgetDefinition> getWidgets() {
        return List.of(
            WidgetDefinition.simple(
                "mymod:status",           // Unique ID: "modid:widget_name"
                "My Mod Status",          // Display name shown in MC Widgets config
                (context, bounds, delta) -> {
                    // Render your widget within bounds
                    context.fill(bounds.x(), bounds.y(),
                                 bounds.right(), bounds.bottom(),
                                 0x80000000);
                    context.drawText(textRenderer, "Status: OK",
                                     bounds.x() + 4, bounds.y() + 4,
                                     0xFFFFFFFF, true);
                }
            )
        );
    }
}
```

### 3. Register Entrypoint

```json
// fabric.mod.json
{
  "entrypoints": {
    "mc-widgets": ["com.example.mymod.MyModWidgetProvider"]
  },
  "suggests": {
    "mc-widgets": "*"
  }
}
```

### 4. Add Fallback Logic

```java
// In your ClientModInitializer
@Override
public void onInitializeClient() {
    if (!FabricLoader.getInstance().isModLoaded("mc-widgets")) {
        // MC Widgets not installed - render HUD ourselves
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            renderMyHud(context);
        });
    }
    // When MC Widgets is present, it will discover and render our widget
}
```

---

## Detailed Integration Steps

### Step 1: Add Build Dependency

Add MC Widgets as a compile-only dependency. This means:

- Your mod compiles against the MC Widgets API
- MC Widgets is NOT bundled with your mod
- Your mod works whether MC Widgets is installed or not

```gradle
dependencies {
    // During development, reference the local JAR
    modCompileOnly files('../mc-widgets/build/libs/mc-widgets-1.1.0.jar')
}
```

### Step 2: Create Your Widget Provider

Create a class that implements `MCWidgetsProvider`. This class is discovered by MC Widgets at startup via Fabric's entrypoint system.

```java
package com.example.mymod;

import com.teeknox.mcwidgets.api.MCWidgetsProvider;
import com.teeknox.mcwidgets.api.WidgetDefinition;
import com.teeknox.mcwidgets.api.WidgetBounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class MyModWidgetProvider implements MCWidgetsProvider {

    @Override
    public List<WidgetDefinition> getWidgets() {
        return List.of(
            new WidgetDefinition(
                "mymod:player-stats",    // Unique widget ID
                "Player Statistics",      // Display name for UI
                1,                        // Minimum columns (suggestion only)
                1,                        // Minimum rows (suggestion only)
                this::renderPlayerStats   // Render callback
            )
        );
    }

    private void renderPlayerStats(DrawContext context, WidgetBounds bounds, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Use the bounds provided by MC Widgets
        int x = bounds.x();
        int y = bounds.y();
        int width = bounds.width();
        int height = bounds.height();

        // Draw background
        context.fill(x, y, x + width, y + height, 0x80000000);

        // Draw content
        String healthText = "Health: " + (int) client.player.getHealth();
        context.drawText(client.textRenderer, healthText, x + 4, y + 4, 0xFFFFFFFF, true);
    }
}
```

### Step 3: Register the Entrypoint

Add the `mc-widgets` entrypoint to your `fabric.mod.json`:

```json
{
  "schemaVersion": 1,
  "id": "mymod",
  "version": "1.0.0",
  "name": "My Mod",
  "environment": "client",
  "entrypoints": {
    "client": ["com.example.mymod.MyModClient"],
    "mc-widgets": ["com.example.mymod.MyModWidgetProvider"]
  },
  "depends": {
    "fabricloader": ">=0.18.0",
    "minecraft": "~1.21.10",
    "fabric-api": "*"
  },
  "suggests": {
    "mc-widgets": "*"
  }
}
```

**Important Notes:**

- Use `suggests` (not `depends`) for MC Widgets to keep it optional
- The entrypoint class must be in the `client` source set
- Multiple provider classes can be listed if you have many widgets

### Step 4: Implement Fallback Rendering

Modify your client initializer to only register HUD callbacks when MC Widgets is absent:

```java
package com.example.mymod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;

public class MyModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Only register our own HUD callback if MC Widgets is not present
        if (!FabricLoader.getInstance().isModLoaded("mc-widgets")) {
            HudRenderCallback.EVENT.register((context, tickCounter) -> {
                renderStandaloneHud(context);
            });
        }
        // If MC Widgets is loaded, our MCWidgetsProvider entrypoint handles rendering
    }

    private void renderStandaloneHud(DrawContext context) {
        // Your original HUD rendering code here
        // Uses your own positioning logic
    }
}
```

### Step 5: Adapt Your Rendering Code

Your widget renderer receives a `WidgetBounds` object that defines where to render. Adapt your rendering code to work within these bounds:

```java
// Before: Fixed position rendering
public void renderHud(DrawContext context) {
    int x = 10;
    int y = 10;
    int width = 150;
    int height = 50;

    context.fill(x, y, x + width, y + height, 0x80000000);
    context.drawText(textRenderer, "Hello", x + 4, y + 4, 0xFFFFFFFF, true);
}

// After: Bounds-aware rendering
public void renderInBounds(DrawContext context, WidgetBounds bounds, float tickDelta) {
    int x = bounds.x();
    int y = bounds.y();
    int width = bounds.width();
    int height = bounds.height();

    context.fill(x, y, x + width, y + height, 0x80000000);
    context.drawText(textRenderer, "Hello", x + 4, y + 4, 0xFFFFFFFF, true);
}
```

---

## Reusable Widgets

Reusable widgets can be placed multiple times on the grid, each with unique per-instance configuration. This is useful for widgets like "Static Text" where users might want several instances with different content.

### Creating a Reusable Widget

Implement the `ReusableWidgetRenderer` interface:

```java
package com.example.mymod;

import com.google.gson.JsonObject;
import com.teeknox.mcwidgets.api.ReusableWidgetRenderer;
import com.teeknox.mcwidgets.api.WidgetBounds;
import com.teeknox.mcwidgets.api.WidgetDefinition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

public class CustomTextWidget implements ReusableWidgetRenderer {
    public static final String ID = "mymod:custom-text";
    public static final String DISPLAY_NAME = "Custom Text";

    /**
     * Create the widget definition for registration.
     */
    public static WidgetDefinition createDefinition() {
        return WidgetDefinition.simpleReusable(ID, DISPLAY_NAME, new CustomTextWidget());
    }

    @Override
    public void render(DrawContext context, WidgetBounds bounds, float tickDelta, JsonObject config) {
        // Read configuration
        String text = config.has("text") ? config.get("text").getAsString() : "Default";
        int color = config.has("color") ? config.get("color").getAsInt() : 0xFFFFFFFF;

        // Draw background
        context.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0x80000000);

        // Draw text
        MinecraftClient client = MinecraftClient.getInstance();
        context.drawText(client.textRenderer, text, bounds.x() + 4, bounds.y() + 4, color, true);
    }

    @Override
    public JsonObject getDefaultConfig() {
        JsonObject config = new JsonObject();
        config.addProperty("text", "Default Text");
        config.addProperty("color", 0xFFFFFFFF);
        return config;
    }

    @Override
    public Screen createConfigScreen(Screen parent, String instanceId) {
        // Return a screen for configuring this widget instance
        return new CustomTextConfigScreen(parent, instanceId);
    }
}
```

### Registering a Reusable Widget

```java
public class MyModWidgetProvider implements MCWidgetsProvider {
    @Override
    public List<WidgetDefinition> getWidgets() {
        return List.of(
            // Regular widget
            WidgetDefinition.simple("mymod:status", "Status", this::renderStatus),

            // Reusable widget
            CustomTextWidget.createDefinition()
        );
    }
}
```

### Instance IDs

When a reusable widget is placed, MC Widgets assigns it a unique instance ID in the format `base-id#N` (e.g., `mymod:custom-text#1`, `mymod:custom-text#2`). This ID is passed to `createConfigScreen()` and should be used to store/retrieve per-instance configuration.

---

## Custom Visibility Conditions

MC Widgets allows widgets to show/hide based on game state (health, game mode, dimension, etc.). External mods can register custom visibility conditions.

### Implementing a Custom Condition

Implement the `VisibilityCondition` interface:

```java
package com.example.mymod;

import com.google.gson.JsonObject;
import com.teeknox.mcwidgets.visibility.VisibilityCondition;
import net.minecraft.client.MinecraftClient;

public class MyCustomCondition implements VisibilityCondition {
    public static final String TYPE = "mymod:custom_state";

    private final int threshold;

    public MyCustomCondition(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean evaluate(MinecraftClient client) {
        if (client.player == null) return false;
        // Your custom evaluation logic
        return MyModState.getValue() > threshold;
    }

    @Override
    public String getDisplayName() {
        return "My state above " + threshold;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("threshold", threshold);
        return json;
    }

    /**
     * Factory method for creating from JSON (used by registry).
     */
    public static MyCustomCondition fromJson(JsonObject params) {
        int threshold = params.has("threshold") ? params.get("threshold").getAsInt() : 50;
        return new MyCustomCondition(threshold);
    }
}
```

### Registering Custom Conditions

Register conditions during your mod's client initialization, after MC Widgets has loaded:

```java
import com.teeknox.mcwidgets.visibility.VisibilityConditionRegistry;

public class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("mc-widgets")) {
            registerVisibilityConditions();
        }
    }

    private void registerVisibilityConditions() {
        VisibilityConditionRegistry registry = VisibilityConditionRegistry.getInstance();

        // Register a custom category (optional)
        registry.registerCategory("mymod", "My Mod", 100);

        // Register condition with parameters
        registry.register(
            MyCustomCondition.TYPE,      // Unique type ID
            "mymod",                      // Category ID
            MyCustomCondition::fromJson   // Factory function
        );

        // Register a simple condition with no parameters
        registry.registerSimple(
            "mymod:is_active",
            "mymod",
            () -> new SimpleCondition("mymod:is_active", "My Mod Active", MyModState::isActive)
        );
    }
}
```

### Built-in Condition Categories

MC Widgets provides these condition categories:

| Category ID | Display Name | Order |
|-------------|--------------|-------|
| `player` | Player State | 0 |
| `game_mode` | Game Mode | 1 |
| `environment` | Environment | 2 |
| `multiplayer` | Multiplayer | 3 |

You can add conditions to existing categories or create your own.

---

## API Reference

### MCWidgetsProvider

The main interface your mod implements to provide widgets.

```java
public interface MCWidgetsProvider {
    /**
     * Called during MC Widgets initialization to discover widgets.
     * Return all widgets your mod provides.
     *
     * @return List of widget definitions (never null, can be empty)
     */
    List<WidgetDefinition> getWidgets();
}
```

### WidgetDefinition

Defines a widget's metadata and renderer.

```java
public record WidgetDefinition(
    String id,              // Unique ID: "modid:widget_name"
    String displayName,     // Human-readable name for config UI
    int minColumns,         // Minimum width suggestion (1+)
    int minRows,            // Minimum height suggestion (1+)
    WidgetRenderer renderer, // Render callback
    boolean isReusable      // Whether this widget can be placed multiple times
) {
    // Constructor for regular (non-reusable) widgets (backward compatible)
    public WidgetDefinition(String id, String displayName, int minColumns, int minRows, WidgetRenderer renderer);

    // Factory for simple 1x1 regular widgets
    public static WidgetDefinition simple(String id, String displayName, WidgetRenderer renderer);

    // Factory for reusable widgets with custom size
    public static WidgetDefinition reusable(String id, String displayName, int minColumns, int minRows,
                                            ReusableWidgetRenderer renderer);

    // Factory for simple 1x1 reusable widgets
    public static WidgetDefinition simpleReusable(String id, String displayName, ReusableWidgetRenderer renderer);

    // Extract mod ID from widget ID
    public String modId();

    // Extract widget name from widget ID
    public String widgetName();
}
```

**Widget ID Format:**

- Must contain a colon: `"modid:widget_name"`
- Mod ID should match your fabric.mod.json id
- Widget name should be unique within your mod
- Examples: `"mymod:status"`, `"mymod:minimap"`, `"mymod:inventory-preview"`

### WidgetRenderer

Functional interface for rendering a widget.

```java
@FunctionalInterface
public interface WidgetRenderer {
    /**
     * Render the widget within the given bounds.
     *
     * @param context   DrawContext for rendering operations
     * @param bounds    Pixel bounds allocated to this widget
     * @param tickDelta Partial tick for smooth animations (0.0 to 1.0)
     */
    void render(DrawContext context, WidgetBounds bounds, float tickDelta);
}
```

### WidgetBounds

Represents the pixel area allocated to your widget.

```java
public record WidgetBounds(int x, int y, int width, int height) {
    // Check if a point is within bounds
    public boolean contains(int px, int py);

    // Right edge: x + width
    public int right();

    // Bottom edge: y + height
    public int bottom();

    // Center coordinates
    public int centerX();
    public int centerY();

    // Create bounds with padding applied
    public WidgetBounds withPadding(int padding);
}
```

### ReusableWidgetRenderer

Extended renderer interface for widgets that support multiple instances with per-instance configuration.

```java
public interface ReusableWidgetRenderer extends WidgetRenderer {
    /**
     * Render with instance-specific configuration.
     */
    void render(DrawContext context, WidgetBounds bounds, float tickDelta, JsonObject config);

    /**
     * Get the default configuration for new instances.
     */
    JsonObject getDefaultConfig();

    /**
     * Create the configuration screen for editing an instance.
     *
     * @param parent     The parent screen to return to when done
     * @param instanceId The instance ID (e.g., "mymod:widget#1")
     */
    Screen createConfigScreen(Screen parent, String instanceId);
}
```

### VisibilityCondition

Interface for implementing custom visibility conditions.

```java
public interface VisibilityCondition {
    /** Unique type identifier for serialization */
    String getType();

    /** Evaluate whether condition is currently met */
    boolean evaluate(MinecraftClient client);

    /** Display name for UI (e.g., "Health below 50%") */
    String getDisplayName();

    /** Serialize parameters to JSON */
    JsonObject toJson();
}
```

### VisibilityConditionRegistry

Registry for custom visibility condition types.

```java
public class VisibilityConditionRegistry {
    public static VisibilityConditionRegistry getInstance();

    /** Register a condition category for UI organization */
    void registerCategory(String id, String displayName, int order);

    /** Register a condition type with a JSON factory */
    void register(String type, String categoryId, ConditionFactory factory);

    /** Register a simple condition with no parameters */
    void registerSimple(String type, String categoryId, Supplier<VisibilityCondition> supplier);

    /** Check if a type is registered */
    boolean isRegistered(String type);

    /** Get all registered type IDs */
    Set<String> getRegisteredTypes();

    /** Get all categories sorted by order */
    List<ConditionCategory> getCategories();

    @FunctionalInterface
    interface ConditionFactory {
        VisibilityCondition create(JsonObject params);
    }
}
```

---

## Best Practices

### 1. Respect the Bounds

Always render within the provided bounds. Don't assume a fixed size.

```java
// Good: Adapts to available space
void render(DrawContext ctx, WidgetBounds bounds, float delta) {
    int availableWidth = bounds.width() - 8;  // Account for padding
    String text = truncateToFit(myText, availableWidth);
    ctx.drawText(textRenderer, text, bounds.x() + 4, bounds.y() + 4, 0xFFFFFFFF, true);
}

// Bad: Ignores bounds, may overflow
void render(DrawContext ctx, WidgetBounds bounds, float delta) {
    ctx.drawText(textRenderer, myVeryLongText, bounds.x(), bounds.y(), 0xFFFFFFFF, true);
}
```

### 2. Handle Dynamic Content

If your widget displays variable content (like a list), adapt to the available height:

```java
void render(DrawContext ctx, WidgetBounds bounds, float delta) {
    int lineHeight = 10;
    int padding = 4;
    int availableHeight = bounds.height() - padding * 2;
    int maxItems = availableHeight / lineHeight;

    List<String> items = getItems();
    int visibleItems = Math.min(items.size(), maxItems);

    for (int i = 0; i < visibleItems; i++) {
        int y = bounds.y() + padding + i * lineHeight;
        ctx.drawText(textRenderer, items.get(i), bounds.x() + padding, y, 0xFFFFFFFF, false);
    }

    // Show overflow indicator if needed
    if (items.size() > maxItems) {
        String more = "+" + (items.size() - maxItems) + " more";
        ctx.drawText(textRenderer, more, bounds.x() + padding,
                     bounds.y() + bounds.height() - lineHeight, 0xFF888888, false);
    }
}
```

### 3. Preserve Your Own Visibility Toggle

If your mod has its own visibility toggle, respect it in the widget renderer:

```java
public class MyModWidgetProvider implements MCWidgetsProvider {
    @Override
    public List<WidgetDefinition> getWidgets() {
        return List.of(
            WidgetDefinition.simple("mymod:status", "Status Display",
                (ctx, bounds, delta) -> {
                    // Check our own visibility setting
                    if (MyModClient.isHudVisible()) {
                        renderStatus(ctx, bounds, delta);
                    }
                }
            )
        );
    }
}
```

### 4. Use ARGB Colors

All color values must use 8-digit ARGB format:

```java
// Correct: 8-digit ARGB
int background = 0x80000000;  // 50% transparent black
int textColor = 0xFFFFFFFF;   // Fully opaque white

// Wrong: 6-digit RGB (will be invisible - 0 alpha!)
int badColor = 0x000000;      // This has 0 alpha!
```

### 5. Keep Fallback Code Synchronized

When updating your widget renderer, remember to update the fallback renderer too:

```java
// Shared rendering logic
private void renderContent(DrawContext ctx, int x, int y, int width, int height) {
    ctx.fill(x, y, x + width, y + height, 0x80000000);
    // ... common rendering code
}

// MC Widgets renderer
public void renderInBounds(DrawContext ctx, WidgetBounds bounds, float delta) {
    renderContent(ctx, bounds.x(), bounds.y(), bounds.width(), bounds.height());
}

// Fallback renderer
public void renderStandalone(DrawContext ctx) {
    int x = config.getX();
    int y = config.getY();
    renderContent(ctx, x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
}
```

---

## Example: Complete Integration

Here's a complete example of integrating a simple status widget:

### StatusWidgetProvider.java

```java
package com.example.statusmod;

import com.teeknox.mcwidgets.api.MCWidgetsProvider;
import com.teeknox.mcwidgets.api.WidgetBounds;
import com.teeknox.mcwidgets.api.WidgetDefinition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class StatusWidgetProvider implements MCWidgetsProvider {

    @Override
    public List<WidgetDefinition> getWidgets() {
        return List.of(
            WidgetDefinition.simple(
                "statusmod:player-status",
                "Player Status",
                this::renderStatus
            )
        );
    }

    private void renderStatus(DrawContext context, WidgetBounds bounds, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Check our own visibility toggle
        if (!StatusModClient.isStatusVisible()) return;

        TextRenderer textRenderer = client.textRenderer;

        int x = bounds.x();
        int y = bounds.y();
        int width = bounds.width();
        int height = bounds.height();

        // Background
        context.fill(x, y, x + width, y + height, 0x90000000);

        // Content
        int padding = 4;
        int lineHeight = 10;
        int contentY = y + padding;

        // Health
        String health = "❤ " + (int) client.player.getHealth() + "/" + (int) client.player.getMaxHealth();
        context.drawText(textRenderer, health, x + padding, contentY, 0xFFFF5555, true);
        contentY += lineHeight;

        // Food (if space allows)
        if (contentY + lineHeight <= y + height - padding) {
            String food = "🍖 " + client.player.getHungerManager().getFoodLevel() + "/20";
            context.drawText(textRenderer, food, x + padding, contentY, 0xFFFFAA00, true);
            contentY += lineHeight;
        }

        // Armor (if space allows)
        if (contentY + lineHeight <= y + height - padding) {
            String armor = "🛡 " + client.player.getArmor();
            context.drawText(textRenderer, armor, x + padding, contentY, 0xFF55FFFF, true);
        }
    }
}
```

### StatusModClient.java

```java
package com.example.statusmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class StatusModClient implements ClientModInitializer {

    private static boolean statusVisible = true;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        // Register toggle keybinding
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.statusmod.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.statusmod"
        ));

        // Handle key presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                statusVisible = !statusVisible;
            }
        });

        // Only register HUD callback if MC Widgets is not present
        if (!FabricLoader.getInstance().isModLoaded("mc-widgets")) {
            HudRenderCallback.EVENT.register((context, tickCounter) -> {
                if (statusVisible) {
                    renderStandaloneHud(context);
                }
            });
        }
    }

    private void renderStandaloneHud(DrawContext context) {
        // Fallback rendering when MC Widgets is not installed
        // Uses fixed position from config or defaults
        int x = 10;
        int y = 10;
        int width = 100;
        int height = 40;

        // Reuse same rendering logic...
    }

    public static boolean isStatusVisible() {
        return statusVisible;
    }
}
```

### fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "statusmod",
  "version": "1.0.0",
  "name": "Status Mod",
  "description": "Shows player status on the HUD",
  "environment": "client",
  "entrypoints": {
    "client": ["com.example.statusmod.StatusModClient"],
    "mc-widgets": ["com.example.statusmod.StatusWidgetProvider"]
  },
  "depends": {
    "fabricloader": ">=0.18.0",
    "minecraft": "~1.21.10",
    "fabric-api": "*"
  },
  "suggests": {
    "mc-widgets": "*"
  }
}
```

---

## Troubleshooting

### Widget doesn't appear in MC Widgets

1. **Check entrypoint registration**: Ensure `mc-widgets` entrypoint is in `fabric.mod.json`
2. **Check class path**: The provider class must be in the correct package
3. **Check logs**: Look for errors during MC Widgets initialization
4. **Verify widget ID format**: Must contain a colon (`modid:name`)

### Widget appears but doesn't render

1. **Check visibility conditions**: Is your visibility toggle enabled?
2. **Check for null player**: `MinecraftClient.getInstance().player` may be null
3. **Check bounds**: Are you rendering within the provided bounds?
4. **Check colors**: Are you using 8-digit ARGB colors?

### Both MC Widgets and fallback render (double rendering)

Ensure you check `isModLoaded("mc-widgets")` before registering your `HudRenderCallback`:

```java
if (!FabricLoader.getInstance().isModLoaded("mc-widgets")) {
    HudRenderCallback.EVENT.register(...);
}
```

### Compilation errors about missing API classes

Ensure MC Widgets JAR is built and the path in `build.gradle` is correct:

```gradle
modCompileOnly files('../mc-widgets/build/libs/mc-widgets-1.1.0.jar')
```

Run `./gradlew build` in the MC Widgets project first.

### Widget renders outside its bounds

Use the `WidgetBounds` helper methods:

```java
// Use bounds.right() and bounds.bottom() instead of calculating manually
context.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), color);

// Apply padding using the helper
WidgetBounds inner = bounds.withPadding(4);
```

### Reusable widget doesn't appear in the widget list

1. **Check renderer type**: Reusable widgets must use `ReusableWidgetRenderer`, not `WidgetRenderer`
2. **Check factory method**: Use `WidgetDefinition.reusable()` or `simpleReusable()`, not `simple()`
3. **Verify `isReusable` flag**: The definition must have `isReusable = true`

### Custom visibility condition doesn't show in the UI

1. **Check registration timing**: Register conditions after MC Widgets is loaded
2. **Check category exists**: Either use a built-in category or register your own first
3. **Check type uniqueness**: Condition types must be unique; use `modid:name` format
4. **Check logs**: Look for registration warnings in the console

---

## Summary

Integrating with MC Widgets involves:

1. Adding a compile-only dependency
2. Creating a provider class implementing `MCWidgetsProvider`
3. Registering the `mc-widgets` entrypoint
4. Adapting rendering to use `WidgetBounds`
5. Adding fallback logic for when MC Widgets is absent

**Optional advanced features:**

- **Reusable Widgets**: Implement `ReusableWidgetRenderer` for widgets that can be placed multiple times with per-instance configuration
- **Custom Visibility Conditions**: Register conditions via `VisibilityConditionRegistry` to let users control when widgets appear based on your mod's state

The result is a mod that works independently but gains unified HUD management when MC Widgets is installed
