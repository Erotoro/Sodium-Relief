# Sodium Relief

Sodium Relief is a lightweight, client-side companion for Sodium focused on one problem: reducing tooltip and hover stutter on inventory-like screens.

It targets GUI interactions where repeated per-frame work can feel uneven even when overall FPS is already fine — hovering across a chest, scanning an inventory, comparing items.

## What it does

- Reuses already-prepared tooltip layouts while the hovered item is unchanged, instead of rebuilding them every frame.
- Stabilizes hover handling so the same target isn't recomputed redundantly as the cursor settles.
- Caches text width measurements for repeated strings (such as item stack counts drawn on every visible slot).
- Stays out of the way: when in doubt about an item or screen, it falls back to vanilla behavior.

## Scope

Sodium Relief is intentionally narrow.

It is not a replacement for Sodium and not a general FPS mod. It will not raise your framerate in the open world — it smooths the *feel* of tooltip- and hover-heavy GUI usage. If you don't spend time in inventories, you won't notice it, and that's by design.

## Compatibility

Two jars are built from the same shared core:

| Jar                              | Minecraft | Loader | Java |
| -------------------------------- | --------- | ------ | ---- |
| `sodiumrelief-mc12111-<ver>.jar` | 1.21.11   | Fabric | 21   |
| `sodiumrelief-mc261x-<ver>.jar`  | 26.1.x    | Fabric | 25   |

Client-side only. Requires Fabric API. Sodium is recommended but not required. Mod Menu is optional.

## Configuration

Settings can be changed in-game, with no config file editing required:

- through Sodium's options page (where Sodium is installed),
- through Mod Menu, or
- through a button added to the vanilla Options screen.

Everything is on by default and safe to leave as-is. Available in English, Russian and Ukrainian.

## Installation

Pick the jar matching your Minecraft version and drop it into your `mods/` folder.
