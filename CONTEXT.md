# Teeknox Mods - Knowledge Base & Context

This document contains all valuable knowledge extracted from the three Teeknox mods:
- **Clockwork Config**: Keybinding synchronization via pre-launch hooks
- **Grindstone Goals**: Task list overlay with persistent storage
- **Whisper Window**: Chat message interception with per-server history

Use this as a reference when building new mods following Teeknox conventions.

---

## Table of Contents

1. [Version Information](#version-information)
2. [**CRITICAL: Minecraft 1.21.9+ Breaking Changes**](#critical-minecraft-1219-breaking-changes)
3. [Project Structure](#project-structure)
4. [Gradle Configuration](#gradle-configuration)
5. [Entry Points](#entry-points)
6. [Keybinding System](#keybinding-system)
7. [Configuration Management](#configuration-management)
8. [HUD Overlay Rendering](#hud-overlay-rendering)
9. [Data Persistence](#data-persistence)
10. [Mixin System](#mixin-system)
11. [Chat Message Detection](#chat-message-detection)
12. [Mod Menu Integration](#mod-menu-integration)
13. [Screen & GUI Patterns](#screen--gui-patterns)
14. [Logging Conventions](#logging-conventions)
15. [Localization](#localization)
16. [Common Patterns](#common-patterns)
17. [API Reference](#api-reference)

---

## Version Information

All Teeknox mods target the same versions for consistency:

| Component | Version | Notes |
|-----------|---------|-------|
| **Minecraft** | 1.21.10 | Latest stable at time of writing |
| **Fabric Loader** | 0.18.1 | Mod loading framework |
| **Fabric API** | 0.138.3+1.21.10 | Common utilities and events |
| **Fabric Loom** | 1.13-SNAPSHOT | Gradle build plugin |
| **Java** | 21 | LTS release, required by Minecraft |
| **Gradle** | 8.14 | Via gradle wrapper |
| **Yarn Mappings** | 1.21.10+build.2 | Deobfuscation mappings |
| **Mod Menu** | 14.0.0-rc.2 | Optional config screen integration |

### Where to Check for Updates

- Fabric versions: https://fabricmc.net/develop/
- Fabric API: https://modrinth.com/mod/fabric-api/versions
- Mod Menu: https://modrinth.com/mod/modmenu/versions
- Yarn Mappings: https://maven.fabricmc.net/net/fabricmc/yarn/

---

## CRITICAL: Minecraft 1.21.9+ Breaking Changes

**⚠️ This section documents API changes that caused frequent issues during development. These are the most common pitfalls when using outdated method signatures.**

### Screen Class Method Signature Changes

#### ❌ `keyPressed()` - Signature Changed

The method signature changed from `(int keyCode, int scanCode, int modifiers)` to using a `KeyInput` parameter.

**Problem**: Overriding `keyPressed` in Screen subclasses may not compile or work correctly.

**Solution**: Avoid overriding `keyPressed`. Use custom handler methods or rely on default behavior (Escape closes screens automatically).

#### ❌ `mouseClicked()` - Signature Changed

The method signature changed from `(double mouseX, double mouseY, int button)` to `(Click, boolean)`.

**Problem**: Overriding `mouseClicked` in Screen subclasses may not compile or work correctly.

**Solution**: Avoid overriding `mouseClicked`. Use `ButtonWidget` instances for click handling instead, or use GLFW polling in `tick()`.

#### ❌ `mouseReleased()` and `mouseDragged()` - Signatures Changed

Similar to `mouseClicked`, these methods have new signatures.

**Solution**: Use GLFW polling in `tick()` for custom mouse handling:

```java
private boolean mouseWasDown = false;

@Override
public void tick() {
    super.tick();
    if (this.client != null) {
        long window = this.client.getWindow().getHandle();
        boolean mouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        // Convert raw pixel coordinates to GUI coordinates
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        GLFW.glfwGetCursorPos(window, xpos, ypos);
        double rawWidth = this.client.getWindow().getWidth();
        double rawHeight = this.client.getWindow().getHeight();
        double mouseX = xpos[0] * this.width / rawWidth;
        double mouseY = ypos[0] * this.height / rawHeight;

        boolean mouseJustClicked = mouseDown && !mouseWasDown;
        boolean mouseJustReleased = !mouseDown && mouseWasDown;

        if (mouseJustClicked) {
            // Handle mouse down
        } else if (mouseDown) {
            // Handle drag
        } else if (mouseJustReleased) {
            // Handle mouse up
        }

        mouseWasDown = mouseDown;
    }
}
```

#### ✅ `mouseScrolled()` - Still Works

The `mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)` method still works with the same signature.

**macOS Trackpad Issue**: macOS trackpads send fractional scroll values (0.1, 0.2) while Windows mice send whole numbers (1.0, -1.0). Casting directly to `int` breaks macOS scrolling.

**Solution**: Accumulate fractional values:

```java
private double accumulatedScroll = 0;

@Override
public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    accumulatedScroll += verticalAmount;
    int scrollLines = (int) accumulatedScroll;
    if (scrollLines != 0) {
        accumulatedScroll -= scrollLines;
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - scrollLines));
    }
    return true;
}
```

### DrawContext Changes

#### ❌ `drawBorder()` - REMOVED

```java
// ❌ This no longer exists in 1.21.9+
context.drawBorder(x, y, width, height, color);
```

**Solution**: Create a helper method using `fill()`:

```java
// ✅ Manual border drawing helper
private static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
    context.fill(x, y, x + width, y + 1, color);                    // Top
    context.fill(x, y + height - 1, x + width, y + height, color);  // Bottom
    context.fill(x, y, x + 1, y + height, color);                   // Left
    context.fill(x + width - 1, y, x + width, y + height, color);   // Right
}
```

### HudRenderCallback Changes

#### ⚠️ `getTickDelta()` - Behavior Changed

```java
// ⚠️ Old approach (may not work correctly)
HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
    overlay.render(drawContext, tickDelta.getTickDelta(false));
});
```

**Solution**: Use fixed delta or new API:

```java
// ✅ Recommended: Fixed delta for simple HUD rendering
HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
    overlay.render(drawContext, 1.0f);
});

// ✅ Or use the new API if smooth animation is needed
HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
    float delta = renderTickCounter.getTickDelta(false);
    overlay.render(drawContext, delta);
});
```

### Widget Focus Issues

#### ⚠️ TextField Focus Lost After Button Click

When a button is clicked, Minecraft's widget system changes focus away from text fields.

**Solution**: Use deferred focus in `tick()`:

```java
private boolean shouldRefocusTextField = false;

// In button callback:
ButtonWidget.builder(Text.literal("+Add"), button -> {
    doSomething();
    shouldRefocusTextField = true;  // Schedule refocus
}).dimensions(...).build();

// In tick():
@Override
public void tick() {
    super.tick();
    if (shouldRefocusTextField) {
        shouldRefocusTextField = false;
        myTextField.setFocused(true);
        this.setFocused(myTextField);
    }
}
```

### Summary: Methods to Avoid Overriding in 1.21.9+

| Method | Status | Alternative |
|--------|--------|-------------|
| `keyPressed()` | ⚠️ Changed | Use default behavior or custom handlers |
| `mouseClicked()` | ⚠️ Changed | Use `ButtonWidget` or GLFW polling |
| `mouseReleased()` | ⚠️ Changed | Use GLFW polling in `tick()` |
| `mouseDragged()` | ⚠️ Changed | Use GLFW polling in `tick()` |
| `mouseScrolled()` | ✅ Works | Accumulate fractional values for macOS |
| `DrawContext.drawBorder()` | ❌ Removed | Use `fill()` helper method |

---

## Project Structure

Standard Fabric mod structure with split source sets:

```
mod-name/
├── build.gradle                           # Build configuration
├── settings.gradle                        # Plugin repositories
├── gradle.properties                      # Version properties
├── gradlew                                # Gradle wrapper (Unix)
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── src/
│   ├── main/                              # Common/server-side code
│   │   ├── java/com/[group]/[mod]/
│   │   │   └── ModNameMod.java            # ModInitializer
│   │   └── resources/
│   │       ├── fabric.mod.json            # Mod metadata
│   │       └── assets/[modid]/
│   │           └── lang/en_us.json        # Translations
│   └── client/                            # Client-only code
│       ├── java/com/[group]/[mod]/
│       │   ├── ModNameClient.java         # ClientModInitializer
│       │   ├── config/                    # Configuration classes
│       │   ├── gui/                       # Screens and overlays
│       │   └── mixin/                     # Mixin classes
│       └── resources/
│           └── modname.client.mixins.json # Mixin config
├── LICENSE                                # MIT License
└── README.md                              # Documentation
```

### Package Naming Conventions

- Main package: `com.[author].[modname]`
- Config classes: `com.[author].[modname].config`
- GUI classes: `com.[author].[modname].gui`
- Mixin classes: `com.[author].[modname].mixin`
- Command classes: `com.[author].[modname].command`
- Data/model classes: `com.[author].[modname].task` or similar

---

## Gradle Configuration

### build.gradle

```gradle
plugins {
    id 'fabric-loom' version '1.13-SNAPSHOT'
    id 'maven-publish'
    id 'java'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

repositories {
    mavenCentral()
    maven {
        name = "TerraformersMC"
        url = "https://maven.terraformersmc.com/"
    }
}

// Split source sets: main (common) and client (client-only)
loom {
    splitEnvironmentSourceSets()

    mods {
        "mod-id" {
            sourceSet sourceSets.main
            sourceSet sourceSets.client
        }
    }
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"

    // Mod Menu - use modCompileOnly for optional, modImplementation for required
    modCompileOnly "com.terraformersmc:modmenu:${project.modmenu_version}"
}

processResources {
    inputs.property "version", project.version
    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 21
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

jar {
    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}
```

### gradle.properties

```properties
# Fabric Properties
minecraft_version=1.21.10
yarn_mappings=1.21.10+build.2
loader_version=0.18.1

# Mod Properties
mod_version=1.0.0
maven_group=com.yourgroup.modname
archives_base_name=mod-name

# Dependencies
fabric_version=0.138.3+1.21.10
modmenu_version=14.0.0-rc.2
```

---

## Entry Points

### Main Entry Point (ModInitializer)

Runs on both client and dedicated server. Use for:
- Block/item registration
- Server-side commands
- Game rules

```java
package com.example.modname;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModNameMod implements ModInitializer {
    public static final String MOD_ID = "mod-name";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing {}", MOD_ID);
        // Register server-side components
    }
}
```

### Client Entry Point (ClientModInitializer)

Runs only on client. Use for:
- Keybindings
- HUD rendering
- Client tick events
- Screens and GUIs

```java
package com.example.modname;

import net.fabricmc.api.ClientModInitializer;

public class ModNameClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register client-side components
    }
}
```

### Pre-Launch Entry Point (PreLaunchEntrypoint)

Runs before Minecraft starts. Used by Clockwork Config for options sync:

```java
package com.example.modname;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class ModNamePreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        // Run before game initialization
    }
}
```

### fabric.mod.json Entry Points

```json
{
  "entrypoints": {
    "main": ["com.example.modname.ModNameMod"],
    "client": ["com.example.modname.ModNameClient"],
    "preLaunch": ["com.example.modname.ModNamePreLaunch"],
    "modmenu": ["com.example.modname.config.ModMenuIntegration"]
  }
}
```

---

## Keybinding System

### Registering Keybindings

```java
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

// Create custom category
private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(
    Identifier.of(MOD_ID, "category")
);

// Register keybinding
KeyBinding exampleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
    "key.mod-name.action",           // Translation key
    InputUtil.Type.KEYSYM,           // Input type (keyboard)
    GLFW.GLFW_KEY_P,                 // Default key
    CATEGORY                          // Category
));

// Unbound by default
KeyBinding unboundKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
    "key.mod-name.other",
    InputUtil.Type.KEYSYM,
    GLFW.GLFW_KEY_UNKNOWN,           // Unbound
    CATEGORY
));
```

### Processing Keybindings

```java
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    // wasPressed() returns true once per key press
    while (exampleKey.wasPressed()) {
        if (client.currentScreen == null) {
            // Handle key press when no screen is open
        }
    }
});
```

### Common GLFW Keys

```java
GLFW.GLFW_KEY_UNKNOWN      // Unbound
GLFW.GLFW_KEY_P            // P key
GLFW.GLFW_KEY_LEFT_BRACKET // [ key
GLFW.GLFW_KEY_RIGHT_BRACKET// ] key
GLFW.GLFW_KEY_BACKSLASH    // \ key
```

---

## Configuration Management

### Singleton Pattern with JSON Persistence

```java
package com.example.modname.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("mod-name-config.json");

    private static ModConfig instance;

    // Config properties with defaults
    private int overlayX = -10;  // Negative = from right edge
    private int overlayY = 10;
    private boolean enabled = true;

    private ModConfig() {}

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    // Getters with auto-save on change
    public int getOverlayX() { return overlayX; }
    public void setOverlayX(int x) { this.overlayX = x; save(); }

    // Position calculation (supports negative offsets)
    public int calculateActualX(int screenWidth, int boxWidth) {
        return overlayX >= 0 ? overlayX : screenWidth + overlayX - boxWidth;
    }

    private static ModConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            ModConfig config = new ModConfig();
            config.save();
            return config;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            // Copy data to config instance
            return config;
        } catch (IOException e) {
            return new ModConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                ConfigData data = new ConfigData();
                // Copy config to data
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            // Log error
        }
    }

    // Internal data class for serialization
    private static class ConfigData {
        int overlayX = -10;
        int overlayY = 10;
        boolean enabled = true;
    }
}
```

### Alternative: Static Manager Pattern

Used by Whisper Window for simpler config:

```java
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("mod-name-config.json");

    private static Config config;

    public static Config getConfig() {
        if (config == null) {
            config = load();
        }
        return config;
    }

    public static void save() {
        try {
            String json = GSON.toJson(config);
            Files.writeString(CONFIG_FILE, json);
        } catch (Exception e) {
            // Log error
        }
    }

    private static Config load() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                return GSON.fromJson(Files.readString(CONFIG_FILE), Config.class);
            }
        } catch (Exception e) {
            // Log error
        }
        return new Config();
    }
}
```

---

## HUD Overlay Rendering

**IMPORTANT: All color values MUST use 8-digit ARGB format (0xAARRGGBB).** Never use 6-digit RGB hex values. Minecraft's rendering APIs expect the alpha channel, and omitting it causes undefined behavior.

- `0xFF` prefix = fully opaque (255 alpha)
- `0x80` prefix = 50% transparent (128 alpha)
- `0x40` prefix = 25% transparent (64 alpha)

### Registering HUD Callback

```java
HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
    MinecraftClient client = MinecraftClient.getInstance();
    // Only render when no screen is open (optional)
    if (client.currentScreen == null) {
        overlay.render(drawContext, 1.0f);
    }
});
```

### Overlay Rendering (1.21.9+)

```java
public class TaskListOverlay {
    // Styling constants
    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 12;
    private static final int BG_COLOR = 0x80000000;       // Semi-transparent black
    private static final int BORDER_COLOR = 0xFF444444;   // Dark gray (ARGB)
    private static final int TEXT_COLOR = 0xFFFFFFFF;     // White
    private static final int HEADER_COLOR = 0xFFFFCC00;   // Gold

    public void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Calculate position and dimensions
        int boxWidth = 120;
        int boxHeight = 50;
        int x = screenWidth - boxWidth - 10;  // 10px from right
        int y = 10;                            // 10px from top

        // Draw background
        context.fill(x, y, x + boxWidth, y + boxHeight, BG_COLOR);

        // Draw border (built-in drawBorder removed in 1.21.9+)
        drawBorder(context, x, y, boxWidth, boxHeight, BORDER_COLOR);

        // Draw text
        context.drawText(textRenderer, "Hello!", x + PADDING, y + PADDING, TEXT_COLOR, true);
    }

    // Custom border helper (drawBorder was removed in 1.21.9+)
    private static void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);           // Top
        context.fill(x, y + h - 1, x + w, y + h, color);   // Bottom
        context.fill(x, y, x + 1, y + h, color);           // Left
        context.fill(x + w - 1, y, x + w, y + h, color);   // Right
    }
}
```

### Text Truncation

```java
String text = "Very long task text that needs truncation";
int maxWidth = 100;

if (textRenderer.getWidth(text) > maxWidth) {
    while (textRenderer.getWidth(text + "...") > maxWidth && text.length() > 0) {
        text = text.substring(0, text.length() - 1);
    }
    text += "...";
}
```

---

## Data Persistence

### JSON List Persistence Pattern (Grindstone Goals)

```java
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class TaskManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TASK_LIST_TYPE = new TypeToken<List<TaskData>>() {}.getType();

    private final List<Task> tasks = new ArrayList<>();
    private final Path savePath;

    public TaskManager() {
        this.savePath = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("mod-name-tasks.json");
    }

    public void load() {
        if (!Files.exists(savePath)) return;

        try (Reader reader = Files.newBufferedReader(savePath)) {
            List<TaskData> dataList = GSON.fromJson(reader, TASK_LIST_TYPE);
            if (dataList != null) {
                tasks.clear();
                for (TaskData data : dataList) {
                    tasks.add(new Task(data.id, data.text));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(savePath.getParent());
            try (Writer writer = Files.newBufferedWriter(savePath)) {
                List<TaskData> dataList = tasks.stream()
                    .map(t -> new TaskData(t.getId(), t.getText()))
                    .toList();
                GSON.toJson(dataList, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save", e);
        }
    }

    private static class TaskData {
        String id;
        String text;
        TaskData(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }
}
```

### Per-Server History Pattern (Whisper Window)

```java
public class WhisperManager {
    private static final Path HISTORY_DIR = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("whisper-window-history");

    private static String currentServerId = null;

    public static void updateCurrentServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        String newServerId = getServerIdentifier(client);

        if (!Objects.equals(newServerId, currentServerId)) {
            // Save current server's history
            if (currentServerId != null) {
                saveWhispers();
            }
            // Load new server's history
            currentServerId = newServerId;
            loadWhispers();
        }
    }

    private static String getServerIdentifier(MinecraftClient client) {
        if (client.isInSingleplayer()) {
            return "singleplayer_" + sanitizeFileName(client.getServer().getSaveProperties().getLevelName());
        }
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null) {
            return sanitizeFileName(serverInfo.address);
        }
        return "unknown";
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static Path getWhispersFile(String serverId) {
        return HISTORY_DIR.resolve(serverId + ".json");
    }
}
```

---

## Mixin System

### Mixin Configuration (mod-name.client.mixins.json)

```json
{
  "required": true,
  "package": "com.example.modname.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": [
    "ChatPacketMixin",
    "MouseMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

### Reference in fabric.mod.json

```json
{
  "mixins": [
    "mod-name.client.mixins.json"
  ]
}
```

### Chat Packet Interception Mixin

```java
@Mixin(value = ClientPlayNetworkHandler.class, priority = 100)
public class ChatPacketMixin {

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessageHead(GameMessageS2CPacket packet, CallbackInfo ci) {
        Text content = packet.content();
        // Process message
    }

    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void onChatMessageHead(ChatMessageS2CPacket packet, CallbackInfo ci) {
        // For signed messages, apply chat type decoration
        MessageType.Parameters params = packet.serializedParameters();
        Text bodyText = Text.literal(packet.body().content());
        Text decorated = params.applyChatDecoration(bodyText);
    }

    @Inject(method = "onProfilelessChatMessage", at = @At("HEAD"))
    private void onProfilelessChatMessageHead(ProfilelessChatMessageS2CPacket packet, CallbackInfo ci) {
        Text content = packet.message();
    }
}
```

### Mouse Mixin

```java
@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        // Handle mouse button events
        // ci.cancel() to consume the event
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        // Handle scroll events
    }
}
```

### Mixin Priority

- **100**: Run before most other mods (for early interception)
- **1000**: Default priority
- **1000+**: Run after most other mods

---

## Chat Message Detection

### Whisper Detection with Multiple Formats

```java
public class WhisperDetector {
    // Deduplication to prevent double-detection
    private static final Set<Integer> recentMessageHashes = new LinkedHashSet<>();
    private static final int MAX_RECENT = 50;

    public enum WhisperFormat {
        VANILLA("(?<sender>\\w+) whispers to you: (?<message>.+)"),
        ARROW("(?<sender>\\w+) -> You: (?<message>.+)"),
        FROM_TO("From (?<sender>\\w+): (?<message>.+)"),
        PM("PM from (?<sender>\\w+): (?<message>.+)"),
        MSG("\\[MSG\\] (?<sender>\\w+): (?<message>.+)"),
        TELLS("(?<sender>\\w+) tells you: (?<message>.+)");

        private final Pattern pattern;

        WhisperFormat(String regex) {
            // PREFIXED version handles timestamp prefixes like [12:34]
            this.pattern = Pattern.compile("^(?:\\[.*?\\]\\s*)?" + regex + "$");
        }
    }

    public static void detectWhisper(Text message) {
        String content = message.getString();
        int hash = content.hashCode();

        // Deduplication
        if (recentMessageHashes.contains(hash)) {
            return;
        }
        addRecentHash(hash);

        // Try each format
        for (WhisperFormat format : WhisperFormat.values()) {
            Matcher matcher = format.pattern.matcher(content);
            if (matcher.matches()) {
                String sender = matcher.group("sender");
                String msg = matcher.group("message");
                // Handle whisper
                return;
            }
        }
    }

    private static void addRecentHash(int hash) {
        recentMessageHashes.add(hash);
        while (recentMessageHashes.size() > MAX_RECENT) {
            recentMessageHashes.remove(recentMessageHashes.iterator().next());
        }
    }
}
```

### Chat Event Listeners

```java
// System messages (vanilla whispers via /msg)
ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
    WhisperDetector.detectWhisper(message);
});

// Chat messages (some servers route whispers here)
ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
    WhisperDetector.detectWhisper(message);
});
```

---

## Mod Menu Integration

### ModMenuIntegration Class

```java
package com.example.modname.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
```

### fabric.mod.json Configuration

```json
{
  "entrypoints": {
    "modmenu": ["com.example.modname.config.ModMenuIntegration"]
  },
  "suggests": {
    "modmenu": "*"
  }
}
```

### build.gradle Dependency

```gradle
// Use modCompileOnly for optional integration
modCompileOnly "com.terraformersmc:modmenu:${project.modmenu_version}"

// Use modImplementation if you need Mod Menu at runtime
modImplementation "com.terraformersmc:modmenu:${project.modmenu_version}"
```

---

## Screen & GUI Patterns

### Basic Screen Template

```java
public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("gui.mod-name.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Toggle button
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Enabled: ON"),
            button -> {
                // Toggle logic
                button.setMessage(Text.literal("Enabled: OFF"));
            }
        ).dimensions(centerX - 100, startY, 200, 20).build());

        // Done button
        addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.done"),
            button -> close()
        ).dimensions(centerX - 100, startY + 50, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
```

### Opening Screens

```java
// From keybinding
while (settingsKey.wasPressed()) {
    if (client.currentScreen == null) {
        client.setScreen(new ConfigScreen(null));
    }
}

// From another screen
client.setScreen(new OtherScreen(this));  // Pass current as parent
```

---

## Logging Conventions

### Logger Setup

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModNameMod {
    public static final String MOD_ID = "mod-name";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
}
```

### Log Levels

```java
LOGGER.info("Mod initialized");           // Normal events
LOGGER.debug("Saved {} items", count);    // Detailed info (hidden by default)
LOGGER.warn("Config not found, using defaults");  // Potential issues
LOGGER.error("Failed to save config", exception); // Errors with stack trace
```

---

## Localization

### Language File (assets/mod-name/lang/en_us.json)

```json
{
  "key.category.mod-name.category": "Mod Name",
  "key.mod-name.toggle": "Toggle Feature",
  "key.mod-name.settings": "Open Settings",
  "gui.mod-name.title": "Mod Name",
  "gui.mod-name.settings": "Settings",
  "gui.mod-name.enabled": "Enabled",
  "gui.mod-name.hint": "Press %s to open"
}
```

### Using Translations

```java
// Simple text
Text.translatable("gui.mod-name.title")

// With arguments
Text.translatable("gui.mod-name.hint", keyBinding.getBoundKeyLocalizedText().getString())
```

### Key Naming Conventions

- Keybinding categories: `key.category.mod-name.category`
- Keybindings: `key.mod-name.action`
- GUI elements: `gui.mod-name.element`
- Tooltips: `tooltip.mod-name.element`

---

## Common Patterns

### Immutable List Return

```java
public List<Task> getTasks() {
    return Collections.unmodifiableList(tasks);
}
```

### Bounds Validation

```java
public void setMaxItems(int max) {
    this.maxItems = Math.max(1, Math.min(100, max));  // Clamp to 1-100
    save();
}
```

### Index Management

```java
public void setSelectedIndex(int index) {
    if (index >= 0 && index < items.size()) {
        selectedIndex = index;
    } else if (items.isEmpty()) {
        selectedIndex = 0;
    } else {
        selectedIndex = Math.max(0, Math.min(index, items.size() - 1));
    }
}
```

### List Reordering

```java
public void moveUp(Item item) {
    int index = items.indexOf(item);
    if (index > 0) {
        Collections.swap(items, index, index - 1);
        save();
    }
}

public void moveToPosition(Item item, int newPosition) {
    int currentIndex = items.indexOf(item);
    if (currentIndex >= 0 && newPosition >= 0 && newPosition < items.size()) {
        items.remove(currentIndex);
        items.add(newPosition, item);
        save();
    }
}
```

### UUID Generation for Data Items

```java
public class Task {
    private final String id;
    private String text;

    public Task(String text) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
    }
}
```

---

## API Reference

### Fabric API Events

```java
// Client tick (runs every game tick, ~20/sec)
ClientTickEvents.END_CLIENT_TICK.register(client -> { });

// HUD rendering
HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> { });

// Chat messages
ClientReceiveMessageEvents.GAME.register((message, overlay) -> { });
ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> { });

// Commands (client-side)
ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> { });
```

### Keybinding Registration

```java
KeyBindingHelper.registerKeyBinding(new KeyBinding(
    translationKey,
    InputUtil.Type.KEYSYM,
    defaultKey,
    category
));
```

### Screen Utilities

```java
MinecraftClient client = MinecraftClient.getInstance();
client.setScreen(new MyScreen(null));
client.currentScreen  // Current open screen or null
```

### Window Dimensions

```java
int width = client.getWindow().getScaledWidth();
int height = client.getWindow().getScaledHeight();
double scale = client.getWindow().getScaleFactor();
```

### File Paths

```java
Path configDir = FabricLoader.getInstance().getConfigDir();
Path gameDir = FabricLoader.getInstance().getGameDir();
```

---

## Building & Running

### Build Commands

```bash
./gradlew build                    # Compile mod JAR
./gradlew runClient                # Run Minecraft with mod
./gradlew genSources               # Generate Minecraft sources (for IDE)
```

### Output

- Main JAR: `build/libs/mod-name-1.0.0.jar`
- Sources JAR: `build/libs/mod-name-1.0.0-sources.jar`

---

## Advanced Patterns (from Clockwork Config & Whisper Window)

### Pre-Launch Entry Points

Use `PreLaunchEntrypoint` for operations that must run before Minecraft initializes (e.g., file copying, options sync):

```java
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import java.nio.file.Files;
import java.nio.file.Path;

public class MyModPreLaunch implements PreLaunchEntrypoint {
    private static final String SOURCE_FILENAME = ".minecraft_options";

    @Override
    public void onPreLaunch() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path destFile = gameDir.resolve("options.txt");

        // Only copy if file doesn't exist
        if (Files.exists(destFile)) {
            System.out.println("[my-mod] options.txt exists, skipping");
            return;
        }

        // Cross-platform home directory
        String userHome = System.getProperty("user.home");
        Path sourceFile = Path.of(userHome, SOURCE_FILENAME);

        if (!Files.exists(sourceFile)) {
            System.out.println("[my-mod] No source file found");
            return;
        }

        try {
            Files.copy(sourceFile, destFile);
            System.out.println("[my-mod] Copied " + sourceFile + " to " + destFile);
        } catch (Exception e) {
            System.err.println("[my-mod] Failed to copy: " + e.getMessage());
        }
    }
}
```

Register in `fabric.mod.json`:
```json
{
  "entrypoints": {
    "preLaunch": ["com.example.mymod.MyModPreLaunch"]
  }
}
```

### Per-Server/Per-World Data Management

Store separate data files for each server or singleplayer world:

```java
public class DataManager {
    private static final Path DATA_DIR = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("my-mod-data");

    private static String currentServerId = null;
    private static final LinkedList<DataItem> items = new LinkedList<>();

    public static void updateCurrentServer() {
        String newServerId = getServerIdentifier();

        if (!Objects.equals(newServerId, currentServerId)) {
            // Save current before switching
            if (currentServerId != null) {
                saveToFile(getDataFile(currentServerId));
            }

            currentServerId = newServerId;
            loadFromFile(getDataFile(currentServerId));
        }
    }

    private static String getServerIdentifier() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;

        // Multiplayer: use server address
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null) {
            return sanitizeFileName(serverInfo.address);
        }

        // Singleplayer: use world name with prefix
        if (client.isInSingleplayer() && client.getServer() != null) {
            String worldName = client.getServer().getSaveProperties().getLevelName();
            return "singleplayer_" + sanitizeFileName(worldName);
        }

        return null;
    }

    private static String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    private static Path getDataFile(String serverId) {
        if (serverId == null) serverId = "unknown";
        return DATA_DIR.resolve(serverId + ".json");
    }
}
```

### Mouse Mixin for Overlay Interaction

Intercept mouse events for custom overlay interactions (scrolling, clicking) when no screen or chat screen is open:

```java
@Mixin(Mouse.class)
public class MouseMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow private double x;
    @Shadow private double y;

    @Unique private boolean wasLeftButtonDown = false;

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (this.client.currentScreen == null || this.client.currentScreen instanceof ChatScreen) {
            // Convert raw coordinates to scaled GUI coordinates
            double scaleFactor = this.client.getWindow().getScaleFactor();
            double scaledX = this.x / scaleFactor;
            double scaledY = this.y / scaleFactor;

            if (MyOverlay.handleScroll(scaledX, scaledY, vertical)) {
                ci.cancel();  // Consume event if handled
            }
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        if (this.client.currentScreen == null || this.client.currentScreen instanceof ChatScreen) {
            double scaleFactor = this.client.getWindow().getScaleFactor();
            double scaledX = x / scaleFactor;
            double scaledY = y / scaleFactor;

            boolean isLeftButtonDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

            // Detect press
            if (isLeftButtonDown && !wasLeftButtonDown) {
                MyOverlay.handleMouseDown(scaledX, scaledY);
            }

            // Detect release
            if (!isLeftButtonDown && wasLeftButtonDown) {
                MyOverlay.handleMouseUp(scaledX, scaledY);
            }

            // Handle drag
            if (isLeftButtonDown) {
                MyOverlay.handleMouseDrag(scaledX, scaledY);
            }

            wasLeftButtonDown = isLeftButtonDown;
        }
    }
}
```

**Key points:**
- Use `@Shadow` to access private fields from the target class
- Use `@Unique` for mixin-local state
- Always convert raw pixel coordinates using `scaleFactor`
- Use `ci.cancel()` to consume events
- Check for both `null` screen and `ChatScreen` for overlay interaction during chat

### Professional Scrollbar Implementation

Full scrollbar with drag, click-to-jump, and macOS trackpad support:

```java
public class ScrollableOverlay {
    // Scrollbar state
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private boolean isDraggingScrollbar = false;
    private double dragStartY = 0;
    private int dragStartScrollOffset = 0;
    private double accumulatedScroll = 0;  // For macOS trackpad

    // Cached bounds for hit testing
    private int cachedScrollbarX, cachedScrollbarY, cachedScrollbarHeight;
    private int cachedThumbY, cachedThumbHeight;

    private static final int SCROLLBAR_WIDTH = 6;
    private static final int MIN_THUMB_HEIGHT = 20;

    public void renderScrollbar(DrawContext context, int x, int y, int height, int contentHeight, int totalContentHeight) {
        if (totalContentHeight <= contentHeight) return;

        maxScrollOffset = totalContentHeight - contentHeight;

        // Cache bounds
        cachedScrollbarX = x;
        cachedScrollbarY = y;
        cachedScrollbarHeight = height;

        // Draw track
        context.fill(x, y, x + SCROLLBAR_WIDTH, y + height, 0xFF333333);

        // Calculate thumb
        float viewRatio = (float) contentHeight / totalContentHeight;
        int thumbHeight = Math.max(MIN_THUMB_HEIGHT, (int) (height * viewRatio));
        int maxThumbY = height - thumbHeight;
        int thumbY = maxScrollOffset > 0 ? (int) ((float) scrollOffset / maxScrollOffset * maxThumbY) : 0;

        cachedThumbY = y + thumbY;
        cachedThumbHeight = thumbHeight;

        // Draw thumb
        context.fill(x, y + thumbY, x + SCROLLBAR_WIDTH, y + thumbY + thumbHeight, 0xFF888888);
    }

    public boolean handleScroll(double mouseX, double mouseY, double amount) {
        // Accumulate for macOS trackpad (sends fractional values)
        accumulatedScroll += amount;
        int scrollLines = (int) accumulatedScroll;
        if (scrollLines != 0) {
            accumulatedScroll -= scrollLines;
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset - scrollLines * 36));
        }
        return true;
    }

    public boolean handleMouseDown(double mouseX, double mouseY) {
        // Click on thumb: start drag
        if (mouseX >= cachedScrollbarX && mouseX <= cachedScrollbarX + SCROLLBAR_WIDTH &&
            mouseY >= cachedThumbY && mouseY <= cachedThumbY + cachedThumbHeight) {
            isDraggingScrollbar = true;
            dragStartY = mouseY;
            dragStartScrollOffset = scrollOffset;
            return true;
        }

        // Click on track: jump to position
        if (mouseX >= cachedScrollbarX && mouseX <= cachedScrollbarX + SCROLLBAR_WIDTH &&
            mouseY >= cachedScrollbarY && mouseY <= cachedScrollbarY + cachedScrollbarHeight) {
            float clickRatio = (float) (mouseY - cachedScrollbarY) / cachedScrollbarHeight;
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, (int) (clickRatio * maxScrollOffset)));
            return true;
        }
        return false;
    }

    public boolean handleMouseDrag(double mouseX, double mouseY) {
        if (isDraggingScrollbar && maxScrollOffset > 0) {
            double deltaY = mouseY - dragStartY;
            int trackHeight = cachedScrollbarHeight - cachedThumbHeight;
            if (trackHeight > 0) {
                float scrollRatio = (float) deltaY / trackHeight;
                scrollOffset = Math.max(0, Math.min(maxScrollOffset,
                    dragStartScrollOffset + (int) (scrollRatio * maxScrollOffset)));
            }
            return true;
        }
        return false;
    }

    public boolean handleMouseUp(double mouseX, double mouseY) {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return false;
    }
}
```

### Text Wrapping Algorithm

Wrap text to fit within a given pixel width, breaking on word boundaries:

```java
private List<String> wrapText(TextRenderer textRenderer, String text, int maxWidth) {
    List<String> lines = new ArrayList<>();

    if (text == null || text.isEmpty()) {
        lines.add("");
        return lines;
    }

    // Fits on one line
    if (textRenderer.getWidth(text) <= maxWidth) {
        lines.add(text);
        return lines;
    }

    StringBuilder currentLine = new StringBuilder();
    String[] words = text.split(" ");

    for (String word : words) {
        if (currentLine.length() == 0) {
            // Word too long for line - break by character
            if (textRenderer.getWidth(word) > maxWidth) {
                for (char c : word.toCharArray()) {
                    if (textRenderer.getWidth(currentLine.toString() + c) > maxWidth && currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder();
                    }
                    currentLine.append(c);
                }
            } else {
                currentLine.append(word);
            }
        } else {
            String test = currentLine + " " + word;
            if (textRenderer.getWidth(test) > maxWidth) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();

                // Handle long word
                if (textRenderer.getWidth(word) > maxWidth) {
                    for (char c : word.toCharArray()) {
                        if (textRenderer.getWidth(currentLine.toString() + c) > maxWidth && currentLine.length() > 0) {
                            lines.add(currentLine.toString());
                            currentLine = new StringBuilder();
                        }
                        currentLine.append(c);
                    }
                } else {
                    currentLine.append(word);
                }
            } else {
                currentLine.append(" ").append(word);
            }
        }
    }

    if (currentLine.length() > 0) {
        lines.add(currentLine.toString());
    }

    return lines;
}
```

### Fade-Out Effect for Overlays

Auto-hide overlay after inactivity:

```java
private long lastActivityTime = System.currentTimeMillis();

public void resetFadeTimer() {
    lastActivityTime = System.currentTimeMillis();
}

private float calculateFadeAlpha(int fadeTimeoutSeconds) {
    if (fadeTimeoutSeconds < 0) return 1.0f;  // Never fade

    long elapsed = System.currentTimeMillis() - lastActivityTime;
    long fadeStartMs = fadeTimeoutSeconds * 1000L;
    long fadeDurationMs = 1000L;

    if (elapsed < fadeStartMs) return 1.0f;

    float fadeProgress = (elapsed - fadeStartMs) / (float) fadeDurationMs;
    return Math.max(0.0f, 1.0f - fadeProgress);
}

private int applyAlpha(int color, float alpha) {
    int originalAlpha = (color >> 24) & 0xFF;
    int newAlpha = (int) (originalAlpha * alpha);
    return (newAlpha << 24) | (color & 0x00FFFFFF);
}

// In render method:
float alpha = calculateFadeAlpha(config.getFadeTimeoutSeconds());
if (alpha <= 0) return;  // Fully faded, don't render

int bgColor = applyAlpha(0x80000000, alpha);
int textColor = applyAlpha(0xFFFFFFFF, alpha);
```

### Client-Side Commands with Reply Pattern

Register client commands that interact with server commands:

```java
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ReplyCommand {
    private static String lastWhisperer = null;

    public static void setLastWhisperer(String name) {
        lastWhisperer = name;
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("reply")
            .then(argument("message", StringArgumentType.greedyString())
                .executes(context -> {
                    if (lastWhisperer == null) {
                        context.getSource().sendError(Text.literal("No one to reply to"));
                        return 0;
                    }

                    String message = StringArgumentType.getString(context, "message");
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        // Send server command
                        client.player.networkHandler.sendChatCommand("msg " + lastWhisperer + " " + message);
                    }
                    return 1;
                })));
    }
}

// Register in ClientModInitializer:
ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
    ReplyCommand.register(dispatcher);
});
```

### Date Separators in Message Lists

Group messages by date with visual separators:

```java
private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM d, yyyy");

private boolean isSameDay(Calendar cal1, Calendar cal2) {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
}

// In render loop:
Calendar prevCal = null;
for (int i = 0; i < messages.size(); i++) {
    Message msg = messages.get(i);
    Calendar currentCal = Calendar.getInstance();
    currentCal.setTimeInMillis(msg.getTimestamp());

    // Draw date separator if day changed
    if (prevCal == null || !isSameDay(prevCal, currentCal)) {
        String dateText = "— " + DATE_FORMAT.format(new Date(msg.getTimestamp())) + " —";
        int dateWidth = textRenderer.getWidth(dateText);
        int dateX = x + (width - dateWidth) / 2;
        context.drawText(textRenderer, dateText, dateX, currentY, 0xFF888888, true);
        currentY += SEPARATOR_HEIGHT;
    }

    prevCal = currentCal;

    // Render message...
    currentY += messageHeight;
}
```

### Scissor-Based Content Clipping

Clip rendering to a specific area (for scrollable content):

```java
// Enable scissor before rendering clipped content
context.enableScissor(
    clipX,                    // Left edge
    clipY,                    // Top edge
    clipX + clipWidth,        // Right edge
    clipY + clipHeight        // Bottom edge
);

// Render content (anything outside scissor bounds is clipped)
for (Item item : items) {
    renderItem(context, item, x, currentY);
    currentY += ITEM_HEIGHT;
}

// Disable scissor when done
context.disableScissor();
```

---

## Quick Start Checklist

When creating a new mod from this template:

1. [ ] Update `gradle.properties`:
   - `mod_version`
   - `maven_group`
   - `archives_base_name`

2. [ ] Update `build.gradle`:
   - Mod ID in `loom.mods` block

3. [ ] Update `settings.gradle`:
   - `rootProject.name`

4. [ ] Update `fabric.mod.json`:
   - `id`, `name`, `description`
   - `authors`, `contact`
   - Entry point class paths

5. [ ] Rename Java packages:
   - Update package declarations
   - Update import statements

6. [ ] Update mixin config:
   - `package` path
   - Mixin class names

7. [ ] Update language file:
   - Translation keys

8. [ ] Add mod icon:
   - `assets/mod-name/icon.png` (128x128 recommended)

---

*Generated from Clockwork Config, Grindstone Goals, and Whisper Window mods.*
