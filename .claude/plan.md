# Ore Oracle - Implementation Plan

## Overview

This plan outlines the complete implementation of Ore Oracle, a Minecraft Fabric mod that displays ore probability information based on the player's Y-level via a HUD overlay.

---

## Phase 1: Project Setup & Renaming

Rename the template project to Ore Oracle.

### Files to Modify

1. **gradle.properties**
   - Change `maven_group` to `com.teeknox.oreoracle`
   - Change `archives_base_name` to `ore-oracle`

2. **settings.gradle**
   - Change `rootProject.name` to `ore-oracle`

3. **build.gradle**
   - Update mod ID in `loom.mods` block to `ore-oracle`

4. **fabric.mod.json**
   - Update `id` to `ore-oracle`
   - Update `name` to `Ore Oracle`
   - Update `description`
   - Update entry point paths to `com.teeknox.oreoracle.*`
   - Update mixin config reference to `ore-oracle.client.mixins.json`
   - Update icon path

5. **Rename Java packages**
   - `com.teeknox.modtemplate` → `com.teeknox.oreoracle`
   - Rename all Java files and update imports

6. **Rename resource files**
   - `mod-template.client.mixins.json` → `ore-oracle.client.mixins.json`
   - `assets/mod-template/` → `assets/ore-oracle/`

7. **Language file**
   - Update translation keys in `en_us.json`

---

## Phase 2: Core Data Structures

### 2.1 Create `data/` Package

Create new files in `src/client/java/com/teeknox/oreoracle/data/`:

#### `ProbabilityTier.java`
```java
public enum ProbabilityTier {
    GREEN(0xFF55FF55),   // High probability
    YELLOW(0xFFFFFF55),  // Medium probability
    RED(0xFFFF5555),     // Low probability
    NONE(0xFF888888);    // No probability (gray)

    private final int color;

    ProbabilityTier(int color) { this.color = color; }
    public int getColor() { return color; }
}
```

#### `Dimension.java`
```java
public enum Dimension {
    OVERWORLD("minecraft:overworld"),
    NETHER("minecraft:the_nether"),
    END("minecraft:the_end");

    private final String id;
    // Constructor, getter, static lookup method
}
```

#### `Ore.java`
Enum representing each ore type with:
- `id` (internal identifier, e.g., "coal")
- `displayName` (localized name key)
- `dimension` (which dimension it spawns in)
- `minY`, `maxY` (spawn range)
- `peakY` (optimal Y-level, can be array for multi-peak ores)
- `hasPeakIndicator` (whether to show ★)
- `requiresBiomeCheck` (for emerald/gold)

**Ores to define:**
- Overworld: COAL, COPPER, IRON, GOLD, DIAMOND, REDSTONE, LAPIS, EMERALD
- Nether: NETHER_QUARTZ, NETHER_GOLD, ANCIENT_DEBRIS

#### `OreDistribution.java`
Class containing the tier calculation logic for each ore:
```java
public class OreDistribution {
    public static ProbabilityTier getTier(Ore ore, int y, @Nullable Identifier biome) {
        // Dispatch to ore-specific calculation
        return switch (ore) {
            case COAL -> getCoalTier(y);
            case IRON -> getIronTier(y);
            case GOLD -> getGoldTier(y, biome);
            case EMERALD -> getEmeraldTier(y, biome);
            // ... etc
        };
    }

    public static boolean isAtPeak(Ore ore, int y) {
        // Check if y is within ±1 of peak
    }

    // Private methods for each ore's tier logic
    private static ProbabilityTier getCoalTier(int y) {
        if (y >= 136 || (y >= 67 && y <= 125)) return GREEN;
        if ((y >= 40 && y < 67) || (y > 125 && y < 136)) return YELLOW;
        if (y >= 0 && y < 40) return RED;
        return NONE;
    }
    // ... similar for other ores
}
```

#### `BiomeChecker.java`
Utility class for biome detection:
```java
public class BiomeChecker {
    private static final Set<Identifier> MOUNTAIN_BIOMES = Set.of(
        Identifier.of("minecraft", "meadow"),
        Identifier.of("minecraft", "cherry_grove"),
        // ... etc
    );

    private static final Set<Identifier> BADLANDS_BIOMES = Set.of(
        Identifier.of("minecraft", "badlands"),
        // ... etc
    );

    public static boolean isMountainBiome(Identifier biome) { ... }
    public static boolean isBadlandsBiome(Identifier biome) { ... }

    public static @Nullable Identifier getCurrentBiome(MinecraftClient client) {
        // Get biome at player position
        if (client.player == null || client.world == null) return null;
        BlockPos pos = client.player.getBlockPos();
        RegistryEntry<Biome> biomeEntry = client.world.getBiome(pos);
        return biomeEntry.getKey().map(RegistryKey::getValue).orElse(null);
    }
}
```

---

## Phase 3: Configuration System

### 3.1 Global Config (`config/ModConfig.java`)

Expand the existing ModConfig with:

```java
// HUD Settings
private boolean enabled = true;
private boolean showHudHeader = true;
private HudPosition hudPosition = HudPosition.RIGHT;  // enum: LEFT, RIGHT
private int overlayX = -10;  // Negative = from right edge
private int overlayY = 10;
private DisplayMode displayMode = DisplayMode.NAME;  // enum: ICON, NAME
private int maxVisibleOres = 8;

// enum HudPosition { LEFT, RIGHT }
// enum DisplayMode { ICON, NAME }
```

### 3.2 Per-Server Data (`config/ServerDataManager.java`)

Pattern from Whisper Window for per-server persistence:

```java
public class ServerDataManager {
    private static final Path DATA_DIR = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("ore-oracle-data");

    private static String currentServerId = null;
    private static ServerData currentData = null;

    // ServerData class contains:
    // - Set<Ore> trackedOres (individually selected ores)
    // - ProbabilityTier filterTier (GREEN/YELLOW/RED/null for no filter)

    public static void updateCurrentServer() { ... }
    public static ServerData getData() { ... }
    public static void save() { ... }

    private static String getServerIdentifier(MinecraftClient client) {
        // "singleplayer_<worldname>" or sanitized server address
    }
}
```

---

## Phase 4: HUD Overlay

### 4.1 Create `gui/OreOracleOverlay.java`

Following STYLE_GUIDE.md patterns:

```java
public class OreOracleOverlay {
    // Style constants from TeeknoxStyle
    private static final int BG_OVERLAY = 0x90000000;
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 12;
    private static final int STANDARD_WIDTH = 120;  // Narrower than 300 for ore list

    // State
    private int cachedY = Integer.MIN_VALUE;
    private Identifier cachedBiome = null;
    private List<OreEntry> cachedEntries = new ArrayList<>();

    public void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ModConfig config = ModConfig.getInstance();

        if (!config.isEnabled() || client.player == null) return;

        // Get current Y and biome
        int currentY = (int) client.player.getY();
        Identifier currentBiome = BiomeChecker.getCurrentBiome(client);
        Dimension dimension = getCurrentDimension(client);

        // Recalculate if Y or biome changed
        if (currentY != cachedY || !Objects.equals(currentBiome, cachedBiome)) {
            cachedY = currentY;
            cachedBiome = currentBiome;
            recalculateEntries(dimension, currentY, currentBiome);
        }

        // Render
        renderOverlay(context, client, config);
    }

    private void recalculateEntries(Dimension dim, int y, Identifier biome) {
        cachedEntries.clear();
        ServerData serverData = ServerDataManager.getData();

        for (Ore ore : Ore.values()) {
            if (ore.getDimension() != dim) continue;

            ProbabilityTier tier = OreDistribution.getTier(ore, y, biome);
            boolean isTracked = serverData.getTrackedOres().contains(ore);
            boolean matchesFilter = matchesFilter(tier, serverData.getFilterTier());
            boolean isAtPeak = OreDistribution.isAtPeak(ore, y);

            // Show if: matches filter OR (is tracked AND tier != NONE means gray but tracked)
            if (matchesFilter || (isTracked && tier == NONE)) {
                cachedEntries.add(new OreEntry(ore, tier, isAtPeak));
            }
        }

        // Sort: GREEN first, then YELLOW, RED, NONE
        cachedEntries.sort(Comparator.comparing(e -> e.tier().ordinal()));
    }

    private boolean matchesFilter(ProbabilityTier tier, @Nullable ProbabilityTier filter) {
        if (filter == null) return tier != ProbabilityTier.NONE;
        return tier.ordinal() <= filter.ordinal() && tier != ProbabilityTier.NONE;
    }

    private void renderOverlay(DrawContext context, MinecraftClient client, ModConfig config) {
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Calculate dimensions
        int contentHeight = cachedEntries.size() * LINE_HEIGHT;
        int headerHeight = config.isShowHudHeader() ? LINE_HEIGHT + PADDING : 0;
        int boxHeight = PADDING * 2 + headerHeight + contentHeight;
        int boxWidth = STANDARD_WIDTH;

        // Calculate position
        int x = config.calculateActualX(screenWidth, boxWidth);
        int y = config.calculateActualY(screenHeight, boxHeight);

        // Draw background (NO border per style guide)
        context.fill(x, y, x + boxWidth, y + boxHeight, BG_OVERLAY);

        int currentY = y + PADDING;

        // Header (optional)
        if (config.isShowHudHeader()) {
            String header = "Y: " + cachedY;
            context.drawCenteredTextWithShadow(textRenderer, header,
                x + boxWidth / 2, currentY, 0xFFFFFFFF);
            currentY += LINE_HEIGHT + PADDING;
        }

        // Ore entries
        int maxToShow = Math.min(cachedEntries.size(), config.getMaxVisibleOres());
        for (int i = 0; i < maxToShow; i++) {
            OreEntry entry = cachedEntries.get(i);
            renderOreEntry(context, textRenderer, entry, x + PADDING, currentY, config);
            currentY += LINE_HEIGHT;
        }

        // Overflow indicator
        int remaining = cachedEntries.size() - maxToShow;
        if (remaining > 0) {
            String overflow = "+" + remaining + " more";
            context.drawCenteredTextWithShadow(textRenderer, overflow,
                x + boxWidth / 2, currentY, 0xFF666666);
        }
    }

    private void renderOreEntry(DrawContext ctx, TextRenderer tr, OreEntry entry,
                                 int x, int y, ModConfig config) {
        String text = entry.ore().getDisplayName();
        if (entry.isAtPeak()) {
            text += " ★";
        }

        // Color indicator dot
        ctx.fill(x, y + 2, x + 6, y + 8, entry.tier().getColor());

        // Ore name
        ctx.drawText(tr, text, x + 10, y, 0xFFFFFFFF, true);
    }

    record OreEntry(Ore ore, ProbabilityTier tier, boolean isAtPeak) {}
}
```

---

## Phase 5: Ore Selector Screen

### 5.1 Create `gui/OreSelectorScreen.java`

Following STYLE_GUIDE.md popup window pattern:

```java
public class OreSelectorScreen extends Screen {
    private static final int COLUMN_WIDTH = 300;
    private static final int HEADER_HEIGHT = 52;
    private static final int FOOTER_HEIGHT = 50;
    private static final int ROW_HEIGHT = 24;

    private final Screen parent;
    private ServerData serverData;

    // Scroll state
    private int scrollOffset = 0;
    private double accumulatedScroll = 0;

    public OreSelectorScreen(@Nullable Screen parent) {
        super(Text.translatable("gui.ore-oracle.selector"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        serverData = ServerDataManager.getData();

        // Filter buttons at top
        int buttonY = 40;
        int buttonWidth = 60;
        int startX = (width - (buttonWidth * 4 + 15)) / 2;

        addFilterButton(startX, buttonY, "All", null);
        addFilterButton(startX + buttonWidth + 5, buttonY, "Green", ProbabilityTier.GREEN);
        addFilterButton(startX + (buttonWidth + 5) * 2, buttonY, "Yellow", ProbabilityTier.YELLOW);
        addFilterButton(startX + (buttonWidth + 5) * 3, buttonY, "Red", ProbabilityTier.RED);

        // Done button at bottom
        addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.done"),
            btn -> close()
        ).dimensions(width / 2 - 50, height - 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dim background
        context.fill(0, 0, width, height, 0x80000000);

        // Title
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFFFF);

        // Subtitle showing current filter
        String subtitle = "Filter: " + (serverData.getFilterTier() == null ? "Show All" :
            serverData.getFilterTier().name());
        context.drawCenteredTextWithShadow(textRenderer, subtitle, width / 2, 30, 0xFF888888);

        // Content area with ore checkboxes
        int columnX = (width - COLUMN_WIDTH) / 2;
        int contentY = HEADER_HEIGHT;
        int contentHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;

        context.enableScissor(columnX, contentY, columnX + COLUMN_WIDTH, contentY + contentHeight);
        renderOreList(context, columnX + 20, contentY - scrollOffset, mouseX, mouseY);
        context.disableScissor();

        // Scrollbar (always visible per style guide)
        renderScrollbar(context, columnX + COLUMN_WIDTH - 10, contentY, contentHeight);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderOreList(DrawContext context, int x, int y, int mouseX, int mouseY) {
        Dimension currentDim = getCurrentDimension();

        // Section: Current Dimension Ores
        int currentY = y;
        context.drawText(textRenderer, "— " + currentDim.name() + " Ores —",
            x, currentY, 0xFF888888, true);
        currentY += ROW_HEIGHT;

        for (Ore ore : Ore.values()) {
            if (ore.getDimension() != currentDim) continue;

            boolean tracked = serverData.getTrackedOres().contains(ore);
            renderOreCheckbox(context, x, currentY, ore, tracked, mouseX, mouseY);
            currentY += ROW_HEIGHT;
        }

        // Other dimensions (collapsed or shown)
        for (Dimension dim : Dimension.values()) {
            if (dim == currentDim || dim == Dimension.END) continue;

            currentY += 10;  // Spacing
            context.drawText(textRenderer, "— " + dim.name() + " Ores —",
                x, currentY, 0xFF666666, true);
            currentY += ROW_HEIGHT;

            for (Ore ore : Ore.values()) {
                if (ore.getDimension() != dim) continue;
                boolean tracked = serverData.getTrackedOres().contains(ore);
                renderOreCheckbox(context, x, currentY, ore, tracked, mouseX, mouseY);
                currentY += ROW_HEIGHT;
            }
        }
    }

    private void renderOreCheckbox(DrawContext ctx, int x, int y, Ore ore,
                                    boolean checked, int mouseX, int mouseY) {
        // Checkbox box
        int checkSize = 12;
        ctx.fill(x, y, x + checkSize, y + 1, 0xFFAAAAAA);  // Top
        ctx.fill(x, y + checkSize - 1, x + checkSize, y + checkSize, 0xFFAAAAAA);  // Bottom
        ctx.fill(x, y, x + 1, y + checkSize, 0xFFAAAAAA);  // Left
        ctx.fill(x + checkSize - 1, y, x + checkSize, y + checkSize, 0xFFAAAAAA);  // Right

        if (checked) {
            ctx.fill(x + 2, y + 2, x + checkSize - 2, y + checkSize - 2, 0xFF55FF55);
        }

        // Ore name
        int textColor = checked ? 0xFFFFFFFF : 0xFF888888;
        ctx.drawText(textRenderer, ore.getDisplayName(), x + checkSize + 8, y + 2, textColor, true);
    }

    // Mouse handling for checkbox clicks (using GLFW polling in tick())
    // Scroll handling with mouseScrolled()
}
```

---

## Phase 6: Commands & Keybindings

### 6.1 Update `OreOracleClient.java`

```java
public class OreOracleClient implements ClientModInitializer {
    public static final String MOD_ID = "ore-oracle";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(
        Identifier.of(MOD_ID, "category")
    );

    private static KeyBinding openSelectorKey;
    private static KeyBinding toggleHudKey;

    private static OreOracleOverlay overlay;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {} client", MOD_ID);

        // Initialize overlay
        overlay = new OreOracleOverlay();

        // Register keybindings (unbound by default)
        openSelectorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.ore-oracle.open_selector",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
        ));

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.ore-oracle.toggle_hud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
        ));

        // Register tick handler for keybindings
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSelectorKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new OreSelectorScreen(null));
                }
            }
            while (toggleHudKey.wasPressed()) {
                ModConfig config = ModConfig.getInstance();
                config.setEnabled(!config.isEnabled());
            }

            // Update server data on world/server change
            ServerDataManager.updateCurrentServer();
        });

        // Register HUD render callback
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen == null) {
                overlay.render(drawContext, 1.0f);
            }
        });

        // Register commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            OreOracleCommand.register(dispatcher);
        });

        LOGGER.info("{} client initialized successfully", MOD_ID);
    }
}
```

### 6.2 Create `command/OreOracleCommand.java`

```java
public class OreOracleCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("oreoracle")
            .executes(ctx -> openSelector())
            .then(literal("toggle")
                .executes(ctx -> toggleHud()))
        );

        // Alias /oo
        dispatcher.register(literal("oo")
            .executes(ctx -> openSelector())
            .then(literal("toggle")
                .executes(ctx -> toggleHud()))
        );
    }

    private static int openSelector() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> client.setScreen(new OreSelectorScreen(null)));
        return 1;
    }

    private static int toggleHud() {
        ModConfig config = ModConfig.getInstance();
        config.setEnabled(!config.isEnabled());
        return 1;
    }
}
```

---

## Phase 7: Config Screen & Mod Menu

### 7.1 Update `config/ConfigScreen.java`

Add settings for:
- HUD enabled toggle
- Show header toggle
- HUD position (left/right)
- Display mode (icon/name)
- Max visible ores slider
- X/Y offset inputs

### 7.2 Update `config/ModMenuIntegration.java`

Already exists in template, just ensure path is correct.

---

## Phase 8: Localization

### 8.1 Update `assets/ore-oracle/lang/en_us.json`

```json
{
  "key.category.ore-oracle.category": "Ore Oracle",
  "key.ore-oracle.open_selector": "Open Ore Selector",
  "key.ore-oracle.toggle_hud": "Toggle HUD",

  "gui.ore-oracle.settings": "Ore Oracle Settings",
  "gui.ore-oracle.selector": "Ore Selector",
  "gui.ore-oracle.enabled": "HUD Enabled",
  "gui.ore-oracle.show_header": "Show Header",
  "gui.ore-oracle.position": "Position",
  "gui.ore-oracle.position.left": "Left",
  "gui.ore-oracle.position.right": "Right",
  "gui.ore-oracle.display_mode": "Display Mode",
  "gui.ore-oracle.display_mode.icon": "Icon",
  "gui.ore-oracle.display_mode.name": "Name",
  "gui.ore-oracle.max_visible": "Max Visible Ores",

  "ore.ore-oracle.coal": "Coal",
  "ore.ore-oracle.copper": "Copper",
  "ore.ore-oracle.iron": "Iron",
  "ore.ore-oracle.gold": "Gold",
  "ore.ore-oracle.diamond": "Diamond",
  "ore.ore-oracle.redstone": "Redstone",
  "ore.ore-oracle.lapis": "Lapis",
  "ore.ore-oracle.emerald": "Emerald",
  "ore.ore-oracle.nether_quartz": "Nether Quartz",
  "ore.ore-oracle.nether_gold": "Nether Gold",
  "ore.ore-oracle.ancient_debris": "Ancient Debris"
}
```

---

## Implementation Order

1. **Phase 1**: Project setup & renaming (~15 min)
2. **Phase 2**: Core data structures - Ore enum, ProbabilityTier, OreDistribution (~1 hr)
3. **Phase 3**: Configuration system - ModConfig expansion, ServerDataManager (~30 min)
4. **Phase 4**: HUD Overlay - OreOracleOverlay (~1 hr)
5. **Phase 5**: Ore Selector Screen - OreSelectorScreen (~1 hr)
6. **Phase 6**: Commands & Keybindings - OreOracleCommand, client init (~30 min)
7. **Phase 7**: Config Screen - ConfigScreen updates (~30 min)
8. **Phase 8**: Localization - en_us.json (~15 min)

**Total estimated effort**: ~5-6 hours

---

## Testing Checklist

- [ ] HUD renders correctly in Overworld
- [ ] HUD renders correctly in Nether with Nether ores
- [ ] HUD shows nothing/disabled state in The End
- [ ] Y-level changes update HUD in real-time
- [ ] Emerald shows gray when not in mountain biome
- [ ] Emerald shows correctly in mountain biomes
- [ ] Gold shows green above Y=32 in badlands
- [ ] Peak indicator (★) appears at correct Y-levels
- [ ] Ore selector screen opens via keybind
- [ ] Ore selector screen opens via `/oreoracle` command
- [ ] `/oo` alias works
- [ ] Tier filter buttons work correctly
- [ ] Individual ore tracking persists across sessions
- [ ] Per-server data saves separately for different servers/worlds
- [ ] Config screen settings apply correctly
- [ ] HUD position (left/right) works
- [ ] Mod Menu integration works

---

## File Summary

### New Files to Create

```
src/client/java/com/teeknox/oreoracle/
├── OreOracleClient.java           # (rename from ModTemplateClient)
├── config/
│   ├── ModConfig.java             # (expand existing)
│   ├── ConfigScreen.java          # (expand existing)
│   ├── ModMenuIntegration.java    # (rename paths)
│   └── ServerDataManager.java     # NEW
├── data/
│   ├── ProbabilityTier.java       # NEW
│   ├── Dimension.java             # NEW
│   ├── Ore.java                   # NEW
│   ├── OreDistribution.java       # NEW
│   └── BiomeChecker.java          # NEW
├── gui/
│   ├── OreOracleOverlay.java      # NEW (replace ExampleOverlay)
│   └── OreSelectorScreen.java     # NEW
└── command/
    └── OreOracleCommand.java      # NEW

src/main/java/com/teeknox/oreoracle/
└── OreOracleMod.java              # (rename from ModTemplateMod)

src/main/resources/
├── fabric.mod.json                # (update)
└── assets/ore-oracle/
    ├── lang/en_us.json            # (update)
    └── icon.png                   # (provide)

src/client/resources/
└── ore-oracle.client.mixins.json  # (rename)
```

### Files to Delete

- `src/client/java/com/teeknox/modtemplate/gui/ExampleOverlay.java`
- `src/client/java/com/teeknox/modtemplate/mixin/ExampleMixin.java` (if not needed)
