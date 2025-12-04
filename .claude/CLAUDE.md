# Ore Oracle - Claude Context File

## Project Overview

**Mod Name:** Ore Oracle
**Package:** `com.teeknox.oreoracle`
**Mod ID:** `ore-oracle`
**Purpose:** Show the player what ores are common at their current Y-level via a HUD overlay with color-coded probability indicators.

## Target Versions

| Component | Version |
|-----------|---------|
| Minecraft | 1.21.10 |
| Fabric Loader | 0.18.1 |
| Fabric API | 0.138.3+1.21.10 |
| Java | 21 |
| Yarn Mappings | 1.21.10+build.2 |

## Core Feature Specification

### HUD Overlay

**Display:**
- Vertical layout running down the edge of the screen
- Position configurable: left or right side of screen
- Each ore entry shows: ore icon OR ore name (configurable) + color indicator
- Updates in real-time as player moves up/down (Y-level changes)
- Uses absolute Y coordinates (bedrock at ~-64)

**Color Coding (Probability Tiers):**
- **Green:** High probability (>70% of peak spawn rate at this Y-level, OR within designated "peak zones")
- **Yellow:** Medium probability (30-70% of peak spawn rate)
- **Red:** Low probability (<30% of peak spawn rate, but still spawns)
- **Gray:** No probability at this Y-level (only shown if player has specifically tracked that ore)

**Visibility Rules:**
- Ores with no probability at current Y-level are HIDDEN by default
- Exception: If player has specifically selected/tracked an ore, show it in gray when probability is zero
- Selecting a color tier is EXCLUSIVE and CUMULATIVE:
  - Green filter: shows only green ores
  - Yellow filter: shows green AND yellow ores
  - Red filter: shows green, yellow, AND red ores (all spawning ores)

### Ore Selection System

**Access Methods:**
1. Keybind to open ore/probability selector screen
2. Command to open the selector screen (e.g., `/oreoracle` or `/oo`)

**Selection Modes:**
1. **Individual ore tracking:** Select specific ores to always show (even when gray/no probability)
2. **Probability tier filter:** Show all ores at a certain probability level or higher

**Persistence:**
- Selections persist across game sessions
- Each server/world has its own saved selection (per-server data pattern from CONTEXT.md)

### Dimension Support

- **Overworld:** Full ore distribution data (coal, copper, iron, gold, redstone, emerald, lapis, diamond)
- **Nether:** Nether-specific ores (nether gold, nether quartz, ancient debris)
- **End:** Minimal/no mineable ores (may show empty or disabled state)

### Ore Distribution Data

**Data Source:** Images, charts, and wiki pages provided by user - to be hardcoded
**Minecraft Version:** 1.18+ ore distribution (modern world generation)
**Scope:** Vanilla ores only (no modded ore support initially)

**Vanilla Ores to Support:**

*Overworld:*
- Coal Ore
- Copper Ore
- Iron Ore
- Gold Ore (including deepslate variant distribution)
- Redstone Ore
- Emerald Ore (mountain biomes only - may need special handling)
- Lapis Lazuli Ore
- Diamond Ore

*Nether:*
- Nether Gold Ore
- Nether Quartz Ore
- Ancient Debris

*End:*
- None (dimension exists but no ores)

### Configuration Options

**HUD Settings:**
- `enabled` - Toggle HUD on/off
- `showHudHeader` - Show/hide header (per Teeknox style guide)
- `hudPosition` - Left or right side of screen
- `overlayX` / `overlayY` - Fine-tune position (negative = from opposite edge)
- `displayMode` - Show ore icon OR ore name
- `maxVisibleOres` - Maximum ores to display before overflow indicator

**Tracking Settings:**
- `selectedOres` - List of individually tracked ore IDs
- `probabilityFilter` - Current tier filter (GREEN, YELLOW, RED, or NONE)

### Data Persistence

**Config File:** `.minecraft/config/ore-oracle-config.json`
- Global settings (HUD position, display mode, etc.)

**Per-Server Data:** `.minecraft/config/ore-oracle-data/<server-id>.json`
- Selected ores
- Probability filter
- Server identifier uses sanitized server address or `singleplayer_<worldname>`

## Technical Implementation Notes

### Ore Distribution Data Structure

```java
public class OreDistribution {
    private final String oreId;           // e.g., "minecraft:diamond_ore"
    private final String displayName;      // e.g., "Diamond"
    private final int minY;               // Minimum spawn Y
    private final int maxY;               // Maximum spawn Y
    private final int peakY;              // Y-level with highest spawn rate
    private final int[] greenZone;        // Y-levels considered "green" (peak zones)
    // Distribution curve data for calculating probability at any Y
}
```

### Probability Calculation

For a given Y-level:
1. Check if Y is within ore's spawn range (minY to maxY)
2. If within designated "green zone" (peak areas) -> GREEN
3. Otherwise calculate relative spawn rate vs peak:
   - >70% of peak rate -> GREEN
   - 30-70% of peak rate -> YELLOW
   - <30% of peak rate -> RED
   - 0% (outside range) -> GRAY (hidden unless tracked)

### Performance Considerations

- Y-level check runs on HUD render (every frame when HUD visible)
- Cache current Y-level and only recalculate probabilities when Y changes
- Ore distribution data is static/hardcoded - no runtime loading overhead

## File Structure (Planned)

```
src/
├── main/
│   ├── java/com/teeknox/oreoracle/
│   │   └── OreOracleMod.java           # ModInitializer (minimal)
│   └── resources/
│       ├── fabric.mod.json
│       └── assets/ore-oracle/
│           ├── lang/en_us.json
│           └── icon.png
└── client/
    ├── java/com/teeknox/oreoracle/
    │   ├── OreOracleClient.java        # ClientModInitializer
    │   ├── config/
    │   │   ├── ModConfig.java          # Global config singleton
    │   │   ├── ConfigScreen.java       # Settings screen
    │   │   ├── ModMenuIntegration.java
    │   │   └── ServerDataManager.java  # Per-server ore selections
    │   ├── data/
    │   │   ├── Ore.java                # Ore enum/class
    │   │   ├── OreDistribution.java    # Distribution data per ore
    │   │   ├── Dimension.java          # Dimension enum
    │   │   └── OreRegistry.java        # Static registry of all ore data
    │   ├── gui/
    │   │   ├── OreOracleOverlay.java   # HUD renderer
    │   │   └── OreSelectorScreen.java  # Ore/probability selection screen
    │   ├── command/
    │   │   └── OreOracleCommand.java   # Client-side command registration
    │   └── mixin/
    │       └── (if needed)
    └── resources/
        └── ore-oracle.client.mixins.json
```

## Style Guide Compliance

Follow Teeknox STYLE_GUIDE.md:
- HUD uses `BG_OVERLAY` (0x90000000), NO border, NO scrollbar
- Colors in 8-digit ARGB format
- `showHudHeader` config option required
- Overflow indicator ("+N more") if content exceeds max items
- Pop-up screens use centered 300px column, left-aligned content
- Scrollbar always visible on scrollable screens

## Commands

**Client-side commands:**
- `/oreoracle` or `/oo` - Opens the ore selector screen
- Possibly: `/oreoracle toggle` - Quick toggle HUD visibility

## Keybindings

- **Open Ore Selector:** Unbound by default (GLFW_KEY_UNKNOWN)
- **Toggle HUD:** Unbound by default (optional, can use command)

## Future Considerations (Out of Scope for v1.0)

- Modded ore support via API/data packs
- Biome-specific ore variations (emerald in mountains)
- Y-level graph/visualization mode
- Integration with minimaps or other HUD mods

---

## Ore Distribution Data (To Be Filled In)

*This section will be populated with hardcoded ore distribution data from provided charts/images.*

### Overworld Ores

| Ore | Min Y | Max Y | Peak Y | Green Zones | Notes |
|-----|-------|-------|--------|-------------|-------|
| Coal | | | | | |
| Copper | | | | | |
| Iron | | | | | |
| Gold | | | | | |
| Redstone | | | | | |
| Emerald | | | | | Mountain biomes only |
| Lapis | | | | | |
| Diamond | | | | | |

### Nether Ores

| Ore | Min Y | Max Y | Peak Y | Green Zones | Notes |
|-----|-------|-------|--------|-------------|-------|
| Nether Gold | | | | | |
| Nether Quartz | | | | | |
| Ancient Debris | | | | | |

---

## Development Notes

- Reference CONTEXT.md for Fabric mod patterns and 1.21.9+ API changes
- Reference STYLE_GUIDE.md for visual styling
- Test in all three dimensions
- Verify Y-coordinate accuracy with F3 debug screen
