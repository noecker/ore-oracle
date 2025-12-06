# Ore Oracle

**Stop guessing. Start mining smarter.**

Ore Oracle adds a minimal HUD overlay that shows you exactly which ores spawn at your current Y-level — updated in real-time as you move. No more alt-tabbing to the wiki or memorizing complex ore charts.

---

## How It Works

A compact overlay displays ores with **color-coded probability indicators**:

| Color | Meaning |
|-------|---------|
| **Green** | High spawn rate — you're in the sweet spot |
| **Yellow** | Medium spawn rate — ores exist here, but it's not ideal |
| **Red** | Low spawn rate — possible but rare |
| **Gray** | No spawns at this level (only for tracked ores) |

**Peak indicator (★)** — When you hit the exact optimal Y-level for an ore, a star appears next to it. Diamond at Y=-59? You'll know.

---

## Features

**Dimension & Biome Aware**
- Automatically switches ore lists when you enter the Nether or End
- Emerald only shows in mountain biomes
- Gold shows bonus spawns when you're in Badlands

**Customizable Display**
- Position the HUD on the left or right side of your screen
- Show ore names or compact icon abbreviations
- Filter by probability tier (Green only, Green+Yellow, or all)
- Track specific ores to always show them, even outside their spawn range

**Per-Server Settings**
- Your ore selections save separately for each server and world
- Jump between servers without losing your preferences

**Lightweight**
- Client-side only — works on any server
- Minimal performance impact with smart Y-level caching
- Clean, unobtrusive design that stays out of your way

---

## Quick Start

1. Install the mod and join a world
2. The HUD appears automatically showing ores for your dimension
3. Use `/oreoracle` (or `/oo`) to open the ore selector
4. Toggle individual ores or set a probability filter

That's it. Mine efficiently.

---

## Optimal Mining Levels

For quick reference, here are the best Y-levels for each ore:

| Ore | Best Y | Green Zone |
|-----|--------|------------|
| **Diamond** | -59 | -64 to -48 |
| **Redstone** | -59 | -64 to -32 |
| **Gold** | -16 | -30 to -5 |
| **Iron** | 16 | -8 to 32 |
| **Lapis Lazuli** | 0 | -16 to 16 |
| **Copper** | 48 | 32 to 64 |
| **Coal** | 96 | 67 to 136+ |
| **Emerald** | 100 | 64 to 136 *(mountains)* |
| **Ancient Debris** | 15 | 8 to 22 |
| **Nether Quartz** | 14 / 114 | 10-22, 105-117 |
| **Nether Gold** | 14 / 114 | 10-22, 105-117 |

---

## Configuration

Access settings via **Mod Menu** or edit `config/ore-oracle-config.json`:

- `enabled` — Toggle HUD on/off
- `showHudHeader` — Display current Y-level in header
- `hudPosition` — LEFT or RIGHT side of screen
- `overlayX` / `overlayY` — Fine-tune position
- `displayMode` — ICON or NAME
- `maxVisibleOres` — Limit visible entries (overflow shows "+N more")

---

## Requirements

- **Minecraft** 1.21.10
- **Fabric Loader** 0.18.0+
- **Fabric API**

**Optional:** [Mod Menu](https://modrinth.com/mod/modmenu) for in-game settings

---

## Links

- [GitHub](https://github.com/teeknox/ore-oracle) — Source code & issues
- [Wiki](https://github.com/teeknox/ore-oracle/wiki) — Documentation

---

*Ore Oracle is client-side only and safe to use on any server. It does not modify gameplay or provide unfair advantages — just surfaces information that's already available on the Minecraft Wiki.*
