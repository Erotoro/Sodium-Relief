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

## Measured impact

The effect is reduced redundant work, not extra frames — so it is measured as tooltip rebuilds avoided rather than FPS. The numbers below come from an automated in-game test that opens a real inventory screen, hovers items, and reports how often a tooltip layout was reused instead of rebuilt:

![Tooltip rebuilds avoided: ~99.96% while resting on an item, ~91.6% while scanning the inventory, on 1.21.11 and 26.1.x](assets/benchmarks/benchmark-banner.png)

| Scenario                         | Tooltip-path calls | Rebuilds with Sodium Relief | Avoided  |
| -------------------------------- | ------------------ | --------------------------- | -------- |
| Resting on a single item         | ~2,400             | 1                           | ~99.96%  |
| Scanning across 36 distinct items | ~1,280            | ~107                        | ~91.6%   |

Numbers are near-identical on 1.21.11 and 26.1.2. Reproduce them yourself with `gradlew :sodium-relief-mc12111:runClientGameTest` (or `:sodium-relief-mc261x:runClientGameTest`); each run also writes a raw counter snapshot you can inspect. A snapshot can be exported in-game at any time from the config screen's *Export Benchmark* button.

<img src="assets/benchmarks/inventory-tooltip-gametest.png" width="480" alt="The inventory screen the automated test hovers, captured in-game" />

*The exact inventory screen the test hovers — captured in-game by the test itself, not staged.*

## Compatibility

Two jars are built from the same shared core:

| Jar                              | Minecraft | Loader | Java |
| -------------------------------- | --------- | ------ | ---- |
| `sodiumrelief-mc12111-<ver>.jar` | 1.21.11   | Fabric | 21   |
| `sodiumrelief-mc261x-<ver>.jar`  | 26.1.x    | Fabric | 25   |

Client-side only. Requires Fabric API. Sodium is recommended but not required. Mod Menu is optional.

## Configuration

Settings can be changed in-game, with no config file editing required:

- through Sodium's options page (1.21.11 only, when Sodium is installed),
- through Mod Menu, or
- through a button added to the vanilla Options screen.

Everything is on by default and safe to leave as-is. Available in English, Russian and Ukrainian.

## Installation

Pick the jar matching your Minecraft version and drop it into your `mods/` folder.

## Disclaimer

Sodium Relief is an unofficial, independent companion mod. It is **not affiliated with, endorsed by, or sponsored by CaffeineMC or the Sodium project**, and it does not include or modify any Sodium code.

Sodium is the work of [CaffeineMC](https://github.com/CaffeineMC). Sodium Relief is simply designed to work well alongside it. All product names, logos, and brands are property of their respective owners.
