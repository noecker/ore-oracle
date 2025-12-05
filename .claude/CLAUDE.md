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

## Ore Distribution Data

*Data extracted from ore distribution charts for Java Edition 1.21.x. Standard and Deepslate variants are combined into single probability curves respecting Y-axis positions.*

### Distribution Types (from ore-chart)

- **Uniform:** Same chance of finding ore regardless of height within range
- **Triangular:** More likely to find ore at center of height range; halfway to edges = half as likely

### Overworld Ores

#### Coal (Combined: Coal Ore + Deepslate Coal Ore)

| Property | Value |
|----------|-------|
| **Total Range** | Y: 0 to 320 (Coal Ore: 136-320 uniform + 0-192 triangular) |
| **Peak Y** | ~96 (mountains) or ~48 (triangular peak) |
| **Peak Value** | ~650 per 100k blocks (combined) |
| **Distribution** | Two distributions: Upper (uniform 136-320), Lower (triangular 0-192 peak at 96) |
| **Green Zone** | Y: 40-136 (highest combined density) |
| **Notes** | Less terrain higher up for ores to generate; Deepslate Coal only exists Y: 0-8 (very small ~40 peak) |

**Approximate Combined Distribution Curve:**
- Y 0-8: ~40 (deepslate only)
- Y 8-20: ~100-300 (ramping up)
- Y 20-40: ~300-500
- Y 40-60: ~500-650 (peak zone)
- Y 60-96: ~450-650
- Y 96-136: ~400-200 (declining)
- Y 136-192: ~150-50 (upper uniform thinning)
- Y 192-320: ~20-5 (sparse)

#### Copper (Combined: Copper Ore + Deepslate Copper Ore)

| Property | Value |
|----------|-------|
| **Total Range** | Y: -16 to 112 |
| **Peak Y** | 48 |
| **Peak Value** | ~750 per 100k blocks (Copper Ore) + ~220 (Deepslate at Y=0) |
| **Distribution** | Triangular centered at Y=48 |
| **Green Zone** | Y: 32-64 |
| **Notes** | Large ore veins generate, mixed with granite above Y=0. Deepslate Copper only Y: -16 to 16 with peak ~220 at Y=0 |

**Approximate Combined Distribution Curve:**
- Y -16 to -8: ~50-100
- Y -8 to 0: ~100-220 (deepslate peak)
- Y 0-16: ~220-400 (transition zone, both variants)
- Y 16-32: ~400-600
- Y 32-48: ~600-750 (approaching peak)
- Y 48: ~750 (peak)
- Y 48-64: ~750-600
- Y 64-80: ~600-300
- Y 80-96: ~300-100
- Y 96-112: ~100-0

#### Iron (Combined: Iron Ore + Deepslate Iron Ore)

| Property | Value |
|----------|-------|
| **Total Range** | Y: -64 to 384 |
| **Peak Y** | 16 (main triangular), 256 (upper triangular) |
| **Peak Value** | ~560 (Iron Ore at Y=16) + ~330 (Deepslate at Y=-8) |
| **Distribution** | Three distributions: Upper (80-384 triangular), Middle (-24 to 56 triangular), Small (-64 to 72 uniform) |
| **Green Zone** | Y: -24 to 56 (main concentration) |
| **Notes** | More generates higher but less terrain. Large ore veins below Y=0 mixed with tuff. Mountains (Y=256) secondary peak |

**Approximate Combined Distribution Curve:**
- Y -64 to -56: ~80-90 (deepslate uniform)
- Y -56 to -40: ~90-100
- Y -40 to -24: ~100-150
- Y -24 to -8: ~150-330 (deepslate triangular peak)
- Y -8 to 0: ~330-400
- Y 0-16: ~400-560 (main peak zone)
- Y 16-32: ~560-400
- Y 32-56: ~400-150
- Y 56-80: ~150-50
- Y 80-120: ~30-20 (sparse)
- Y 120-256: ~10-20 (slight increase at mountains)
- Y 256+: tiny amounts in mountains

#### Gold (Combined: Gold Ore + Deepslate Gold Ore)

| Property | Value |
|----------|-------|
| **Total Range** | Y: -64 to 32 (normal), 32-256 (badlands only) |
| **Peak Y** | -16 |
| **Peak Value** | ~170 (Deepslate) + ~80 (Gold Ore) = ~250 combined at Y=-16 |
| **Distribution** | Triangular -64 to 32, 50% buried. Additional uniform in badlands 32-256 |
| **Green Zone** | Y: -48 to 0 |
| **Notes** | Best layer Y=-16. Badlands biome has extra gold 32-256 uniform. Gold ore (non-deepslate) only Y: 0-32 |

**Approximate Combined Distribution Curve:**
- Y -64: ~0
- Y -56 to -48: ~60-100
- Y -48 to -32: ~100-150
- Y -32 to -16: ~150-250 (peak)
- Y -16 to 0: ~250-170
- Y 0-16: ~80-40 (gold ore only)
- Y 16-32: ~40-10
- Y 32+: 0 (except badlands: ~8 uniform to Y=256)

#### Diamond (Combined: Diamond Ore + Deepslate Diamond Ore)

| Property | Value |
|----------|-------|
| **Total Range** | Y: -64 to 16 |
| **Peak Y** | -64 (highest at bottom, reduced by bedrock) |
| **Peak Value** | ~225 (Deepslate Diamond at Y=-59) |
| **Distribution** | Triangular -64 to 16, buried (50% small, 100% medium, 70% large) |
| **Green Zone** | Y: -64 to -48 |
| **Notes** | Best Y=-59 (above bedrock). Mine at -53 to avoid lava pools. Diamond Ore (stone) only at Y: 0-16 (tiny ~13 peak at Y=8) |

**Approximate Combined Distribution Curve:**
- Y -64 to -59: ~200-225 (bedrock interference)
- Y -59: ~225 (effective peak, best level)
- Y -59 to -48: ~225-160
- Y -48 to -32: ~160-120
- Y -32 to -16: ~120-100
- Y -16 to 0: ~100-40
- Y 0-8: ~15-13 (diamond ore only)
- Y 8-16: ~13-0

#### Redstone (Combined: Redstone Ore + Deepslate Redstone Ore)

| Property | Value |
|----------|-------|
| **Total Range** | Y: -64 to 15 |
| **Peak Y** | -59 (Deepslate) and 8 (Redstone Ore) |
| **Peak Value** | ~510 (Deepslate at Y=-59) + ~90 (Redstone Ore at Y=8) |
| **Distribution** | Two: Lower triangular (-96 to -32, peak -59) + Upper uniform (-64 to 15) |
| **Green Zone** | Y: -64 to -32 |
| **Notes** | Mine at -53 to avoid lava pools. Mine above 8 to avoid deepslate. Redstone Ore (non-deepslate) Y: 0-15 with peak ~90 |

**Approximate Combined Distribution Curve:**
- Y -64 to -59: ~450-510 (deepslate peak zone)
- Y -59: ~510 (peak)
- Y -59 to -48: ~510-400
- Y -48 to -32: ~400-200
- Y -32 to -16: ~100-90 (uniform portion)
- Y -16 to 0: ~90-100
- Y 0-8: ~90-100 (redstone ore)
- Y 8-15: ~90-0

#### Lapis Lazuli (Combined: Lapis Lazuli Ore + Deepslate Lapis Lazuli Ore)

| Property | Value |
|----------|-------|
| **Total Range** | Y: -64 to 64 |
| **Peak Y** | 0 |
| **Peak Value** | ~130 (Deepslate at Y=0) + ~115 (Lapis Ore at Y=0) = ~245 combined |
| **Distribution** | Two: Triangular -32 to 32 (exposed) + Uniform -64 to 64 (100% buried/hidden) |
| **Green Zone** | Y: -16 to 16 |
| **Notes** | Best at Y=0. Mining below 64 yields good amounts. Hidden/buried lapis uniform throughout. Deepslate Lapis: -64 to 8, peak ~130 at Y=0. Regular Lapis: 0-64 with triangular peak ~115 at Y=0 |

**Approximate Combined Distribution Curve:**
- Y -64 to -56: ~55-57 (deepslate uniform buried)
- Y -56 to -32: ~57-70
- Y -32 to -16: ~70-100
- Y -16 to 0: ~100-245 (peak zone)
- Y 0: ~245 (peak, both variants)
- Y 0-16: ~245-150
- Y 16-32: ~100-60
- Y 32-48: ~50-30
- Y 48-64: ~30-0

#### Emerald (Combined: Emerald Ore + Deepslate Emerald Ore)

| Property | Value |
|----------|-------|
| **Total Range** | Y: -16 to 320 |
| **Peak Y** | 232 (algorithm-based triangle peak) |
| **Distribution** | Triangular -16 to 320, peak at Y=232 |
| **Green Zone** | Y: 180-320 |
| **Notes** | **MOUNTAIN BIOMES ONLY!** Triangle distribution peaks at Y=232. Higher altitudes have higher spawn density per stone block. Deepslate Emerald only Y: -16 to 16. |

**Algorithm-Based Distribution (spawn chance per stone block):**

The Minecraft ore generation algorithm uses a triangle distribution from Y=-16 to Y=320, with the peak at Y=232 (the midpoint formula: (-16+320+160)/2 ≈ 232, confirmed by Minecraft Wiki).

**Important:** Previous "per 100k blocks" charts showed emerald peaking around Y=100. This was misleading because those charts sampled actual world generation, which includes air blocks. At high altitudes, mountains have mostly air and little stone, so the total emerald count is low even though the per-stone-block density is highest.

**For players:** When mining stone in mountain biomes, you will encounter MORE emeralds per block broken at Y=230 than at Y=100. The green zone reflects this algorithm-based reality.

- Y -16 to 100: RED (far from peak, low density per stone block)
- Y 100-180: YELLOW (transitional zone)
- Y 180-320: GREEN (near peak at 232, highest density per stone block)

### Nether Ores

#### Nether Quartz Ore

| Property | Value |
|----------|-------|
| **Total Range** | Y: 10 to 117 |
| **Peak Y** | None (uniform distribution) |
| **Distribution** | Uniform - equal spawn chance per netherrack block throughout range |
| **Green Zone** | Y: 10-117 (entire range) |
| **Notes** | Very common ore. 16 attempts per chunk (32 in basalt deltas). |

**Algorithm-Based Distribution (spawn chance per netherrack block):**

The Minecraft ore generation algorithm spawns Nether Quartz **uniformly** from Y=10 to Y=117. Every netherrack block in this range has an equal chance of being replaced with quartz ore.

**Important:** Previous charts showed "peaks" at floor (~Y=14) and ceiling (~Y=114). This was misleading because those charts sampled actual Nether generation. The Nether has more netherrack near the floor and ceiling (solid terrain) and less in the middle (lava lakes, open caverns). The "peaks" reflected netherrack availability, not the spawn algorithm.

**For players:** The entire Y=10-117 range is equally good for finding quartz per block mined. There is no "best" level - mine wherever is convenient.

#### Nether Gold Ore

| Property | Value |
|----------|-------|
| **Total Range** | Y: 10 to 117 |
| **Peak Y** | None (uniform distribution) |
| **Distribution** | Uniform - equal spawn chance per netherrack block throughout range |
| **Green Zone** | Y: 10-117 (entire range) |
| **Notes** | Found in all Nether biomes. 10 attempts per chunk (20 in basalt deltas). |

**Algorithm-Based Distribution (spawn chance per netherrack block):**

Same as Nether Quartz - the algorithm spawns Nether Gold **uniformly** from Y=10 to Y=117. The apparent "peaks" in world-sample charts reflected netherrack availability, not spawn probability.

**For players:** The entire Y=10-117 range is equally good for finding nether gold per block mined.

#### Ancient Debris

| Property | Value |
|----------|-------|
| **Total Range** | Y: 8 to 119 |
| **Peak Y** | 15 |
| **Peak Value** | ~50 per 100k blocks |
| **Distribution** | Sharp triangular peak at Y=15, tiny amounts elsewhere |
| **Green Zone** | Y: 8-22 |
| **Notes** | Very rare. Best level Y=15. Small secondary presence at ceiling (~3 at Y=115-119). Never exposed to air |

**Approximate Distribution Curve:**
- Y 8-12: ~0-20
- Y 12-15: ~20-50 (peak)
- Y 15: ~50 (peak)
- Y 15-22: ~50-10
- Y 22-100: ~1-2 (trace amounts)
- Y 100-119: ~2-3 (tiny ceiling presence)

### End Dimension

No mineable ores in The End.

---

## Implementation Notes for Distribution Data

### Data Structure Recommendation

Rather than storing full curves, use a simplified approach:

```java
public enum ProbabilityTier {
    GREEN,   // >70% of peak OR in green zone
    YELLOW,  // 30-70% of peak
    RED,     // <30% of peak but >0
    NONE     // 0% - outside spawn range
}

public class OreData {
    String id;              // "coal", "diamond", etc.
    String displayName;     // "Coal", "Diamond"
    int minY;               // Minimum spawn Y
    int maxY;               // Maximum spawn Y
    int[] greenZoneMin;     // Green zone lower bounds (can have multiple)
    int[] greenZoneMax;     // Green zone upper bounds
    int peakY;              // Y with highest spawn rate
    float peakValue;        // For relative calculations

    // Key Y-level thresholds for tier calculation
    int yellowThresholdLow;  // Below this Y, transitions from green->yellow
    int yellowThresholdHigh; // Above this Y, transitions from green->yellow
    int redThresholdLow;     // Below this Y, transitions from yellow->red
    int redThresholdHigh;    // Above this Y, transitions from yellow->red
}
```

### Simplified Tier Boundaries (Recommended Implementation)

Rather than calculating percentages, use pre-computed Y boundaries:

| Ore | Green Zone | Yellow Zone | Red Zone |
|-----|-----------|-------------|----------|
| **Coal** | 67-125, 136+ | 40-67, 125-136 | 0-40 |
| **Copper** | 32-64 | 0-32, 64-96 | -16-0, 96-112 |
| **Iron** | Lower: -8 to 32, Upper: 200-256 | Always yellow if Y < 72 and not green | -64 to -48, 72-200 (when not in green/yellow) |
| **Gold** | -30 to -5, -54 to -48 | -48 to -30, -5 to 8 | 8-32 |
| **Gold (Badlands)** | 32+ (any Y above 32) | N/A | N/A |
| **Diamond** | -64 to -48 | -48 to -16 | -16 to 16 |
| **Redstone** | -64 to -32 | -32 to 16 (jumps straight to yellow at Y=16+) | N/A |
| **Lapis** | -16 to 16 | -64 to -16, 16-64 (jumps straight to yellow outside green) | N/A |
| **Emerald** | 180-320 (mountain biomes only, peak at 232) | 100-180 | -16 to 100 |
| **Nether Quartz** | 10-117 (entire range, uniform) | N/A | N/A |
| **Nether Gold** | 10-117 (entire range, uniform) | N/A | N/A |
| **Ancient Debris** | 8-22 | N/A | 22-119 |

### Special Biome Handling

#### Mountain Biomes (Emerald Spawning)

Emerald ore ONLY spawns in these biomes. Show as GRAY in all other biomes:

**Biome IDs:**
- `minecraft:meadow`
- `minecraft:cherry_grove`
- `minecraft:grove`
- `minecraft:snowy_slopes`
- `minecraft:jagged_peaks`
- `minecraft:frozen_peaks`
- `minecraft:stony_peaks`
- `minecraft:windswept_hills`
- `minecraft:windswept_gravelly_hills`
- `minecraft:windswept_forest`

#### Badlands Biomes (Extra Gold Spawning)

Gold spawns at elevated rates up to Y=255 in badlands biomes. Show as GREEN for any Y > 32:

**Biome IDs:**
- `minecraft:badlands`
- `minecraft:wooded_badlands`
- `minecraft:eroded_badlands`

### Algorithm-Based Distribution Philosophy

**Important:** This mod uses algorithm-based ore distribution tiers, NOT world-sample-based data.

Many ore distribution charts online show "ores per 100,000 blocks sampled at each Y-level." These charts sample actual world generation, which includes air blocks. This is misleading for ores at high altitudes (like Emerald) or in areas with variable terrain (like Nether ores), because:

- At high altitudes, most sampled blocks are air, making ore counts appear low
- In the Nether, lava lakes create air pockets that dilute samples in the middle Y-range

**Our approach:** We show spawn density per replaceable block (stone/netherrack). This tells players: "If you mine a block here, what's the chance it contains ore?" This is what actually matters for mining efficiency.

### Ore-Specific Implementation Notes

#### Coal
- **Y >= 136:** Always GREEN (uniform distribution, consistent spawning)
- **Y 67-125:** GREEN (peak zone of triangular distribution)
- **Y 40-67 or 125-136:** YELLOW
- **Y 0-40:** RED

#### Iron (Two Triangular Distributions)
- **Upper Triangle (136-320):** GREEN at 200-256 (mountain peaks)
- **Lower Triangle (-24 to 56):** GREEN at -8 to 32 (main ore concentration)
- **Y < 72:** Always at least YELLOW (never red) due to uniform small blob distribution
- **Y 72-200:** RED (sparse area between distributions)

#### Gold (Two Peaks)
- **Upper Peak (-16):** GREEN zone is -30 to -5
- **Lower Peak (~-50):** GREEN zone is -54 to -48 (small)
- **Y -48 to -30:** YELLOW (between peaks)
- **Y -5 to 8:** YELLOW (above upper peak)
- **Y 8-32:** RED
- **Badlands special:** GREEN for any Y > 32

#### Redstone
- **Y -64 to -32:** GREEN
- **Y -32 to 16:** YELLOW (jumps straight to yellow, no red zone)
- **Y > 16:** GRAY (no spawning)

#### Lapis Lazuli
- **Y -16 to 16:** GREEN
- **Y -64 to -16 or 16-64:** YELLOW (jumps straight to yellow, no red zone)
- **Y outside -64 to 64:** GRAY (no spawning)

#### Emerald (Algorithm-Based Triangle)
- **Triangle distribution** from Y=-16 to Y=320, peak at Y=232
- **Y >= 180:** GREEN (near peak, highest density per stone block)
- **Y 100-180:** YELLOW (transitional)
- **Y < 100:** RED (far from peak)
- **Mountain biomes only** - shows GRAY/NONE in other biomes

#### Nether Quartz (Uniform)
- **Uniform distribution** from Y=10 to Y=117
- **Entire range is GREEN** - no peaks, every Y-level equally good
- No peak indicator (★) since there's no single best level

#### Nether Gold (Uniform)
- **Uniform distribution** from Y=10 to Y=117
- **Entire range is GREEN** - same as Nether Quartz
- No peak indicator (★) since there's no single best level

---

## Peak Y-Level Indicator (★)

When the player is at or within ±1 of an ore's peak Y-level, display a star (★) next to the ore entry in the HUD. This indicates "you are at THE ideal level for this ore."

### Peak Indicator Implementation

- Display: Append `★` symbol next to ore name/icon when at peak
- Only shows when ore is GREEN tier (peak is always within green zone)
- Works in both icon and text display modes

### Peak Y-Levels (±1 range)

| Ore | Peak Y | Indicator Range |
|-----|--------|-----------------|
| **Coal** | 96 | 95-97 |
| **Copper** | 48 | 47-49 |
| **Iron (Lower)** | 16 | 15-17 |
| **Iron (Upper)** | 256 | NO INDICATOR (upper peak excluded) |
| **Gold** | -16 | -17 to -15 |
| **Diamond** | -59 | -60 to -58 |
| **Redstone** | -59 | -60 to -58 |
| **Lapis** | 0 | -1 to 1 |
| **Emerald** | 232 | 231-233 |
| **Nether Quartz** | N/A | NO INDICATOR (uniform distribution) |
| **Nether Gold** | N/A | NO INDICATOR (uniform distribution) |
| **Ancient Debris** | 15 | 14-16 |

### Notes
- Iron upper peak (Y=256) does NOT get the star indicator - only the lower/main peak at Y=16
- Nether Quartz and Nether Gold have uniform distributions - no peak, so no indicator. Entire range (10-117) is equally good.
- Coal's upper uniform zone (136+) is consistently good but has no single "peak" - only the triangular peak at 96 gets the star
- Emerald peak at Y=232 reflects the algorithm's triangle distribution, not world-sample data

---

## Development Notes

- Reference CONTEXT.md for Fabric mod patterns and 1.21.9+ API changes
- Reference STYLE_GUIDE.md for visual styling
- Test in all three dimensions
- Verify Y-coordinate accuracy with F3 debug screen
- Emerald: Show as GRAY unless player is in a mountain biome (requires biome detection)
- Gold: Check for badlands biome and apply special GREEN rule for Y > 32
- Iron: Has two separate peak zones; ensure Y < 72 is always at least YELLOW; only lower peak gets ★
- Redstone/Lapis: No red zone - jump from green directly to yellow
- Ancient Debris "green zone" is small but well-defined at Y=15
- Peak indicator (★) shows when player is within ±1 of optimal Y-level
