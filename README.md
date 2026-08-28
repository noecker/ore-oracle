# Ore Oracle

**Know exactly which ores spawn at your Y-level — at a glance.**

Ore Oracle is a client-side Fabric mod that displays a compact HUD overlay showing ore spawn probabilities based on your current Y-level. No more memorizing ore distribution charts or constantly checking the wiki — just mine smarter.

## Features

### Real-Time Ore Probability HUD

- **Color-coded indicators** show spawn probability at your current Y-level:
  - 🟢 **Green** — High probability (you're in a good zone)
  - 🟡 **Yellow** — Medium probability (ores spawn here, but not ideal)
  - 🔴 **Red** — Low probability (rare spawns at this level)
  - ⚫ **Gray** — No spawns at this Y-level (only shown for tracked ores)

- **Peak indicator (★)** appears when you're at the optimal Y-level for an ore

- **Dimension-aware** — automatically switches between Overworld, Nether, and End ore sets

- **Biome-aware** — correctly handles Emerald (mountain biomes only) and Gold (bonus spawns in Badlands)

### Supported Ores

**Overworld:**
- Coal, Copper, Iron, Gold, Diamond, Redstone, Lapis Lazuli, Emerald

**Nether:**
- Nether Quartz, Nether Gold, Ancient Debris

### Customization

- **HUD position** — Left or right side of screen with fine-tune offset controls
- **Display mode** — Show ore names or compact icons
- **Probability filter** — Show only green-tier ores, green+yellow, or all spawning ores
- **Individual ore tracking** — Pin specific ores to always display (even when gray)
- **Per-server settings** — Your ore selections persist separately for each server/world

## Usage

### Opening the Ore Selector

- **Command:** `/oreoracle` or `/oo`
- **Keybind:** Configurable (unbound by default)

From the selector screen, you can:
1. **Toggle individual ores** — Click to track/untrack specific ores
2. **Set probability filter** — Use the Green/Yellow/All buttons to filter by tier

### Configuration

Settings are accessible via [Mod Menu](https://modrinth.com/mod/modmenu) or by editing `config/ore-oracle-config.json`.

| Setting | Description | Default |
|---------|-------------|---------|
| `enabled` | Toggle HUD visibility | `true` |
| `showHudHeader` | Show Y-level in HUD header | `true` |
| `hudPosition` | Screen side (LEFT/RIGHT) | `LEFT` |
| `overlayX` / `overlayY` | Position offset | `4` / `4` |
| `displayMode` | ICON or NAME display | `NAME` |
| `maxVisibleOres` | Max entries before "+N more" | `8` |

## Requirements

| Component | Version |
|-----------|---------|
| Minecraft | 26.2 |
| Fabric Loader | ≥0.19.3 |
| Fabric API | Required |
| Java | ≥25 |

**Optional:**
- [Mod Menu](https://modrinth.com/mod/modmenu) — For in-game settings access

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.2
2. Download and install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download Ore Oracle and place in your `mods` folder
4. Launch the game!

## Quick Reference: Optimal Mining Levels

| Ore | Best Y-Level | Green Zone |
|-----|--------------|------------|
| Diamond | -59 | -64 to -48 |
| Redstone | -59 | -64 to -32 |
| Gold | -16 | -30 to -5 |
| Iron | 16 | -8 to 32 |
| Lapis | 0 | -16 to 16 |
| Copper | 48 | 32 to 64 |
| Coal | 96 | 67 to 136+ |
| Emerald | 100 | 64 to 136 (mountains only) |
| Ancient Debris | 15 | 8 to 22 |
| Nether Quartz | 14 or 114 | 10-22, 105-117 |
| Nether Gold | 14 or 114 | 10-22, 105-117 |

## License

MIT License — See [LICENSE](LICENSE) for details.

## Links

- [Modrinth](https://modrinth.com/mod/ore-oracle)
- [GitHub](https://github.com/teeknox/ore-oracle)
- [Issue Tracker](https://github.com/teeknox/ore-oracle/issues)
