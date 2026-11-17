# AMBOT Restoration — Design

_Status: approved by the user 2026-11-17 as a working basis, with changes expected after they've seen it in play · Current state: [`docs/ANALYSIS.md`](../../ANALYSIS.md)_

## 1. Intent

AMBOT was an OOP2 group project: a Java Swing 2D side-scrolling platformer built with free itch.io assets, some of which the team re-drew in GIMP and Aseprite. The team's last version had a main menu, 3 worlds and a boss at the end of each world. That version is lost. What survives is an early prototype: one animated character on a blank screen.

The goal is to **rebuild the game the team intended, and finish it this time**. Option B was chosen: **build one world at a time**. The engine, the menu and World 1 with its boss come first and are finished to release quality. At that point AMBOT is already a complete short game. Worlds 2 and 3 are then added as content on the same engine.

**Identity twist (proposed by Claude, accepted with route B):** each world is built around moves that already exist in the team's reworked character sheet, and each world's boss tests those moves.

| World | Moves it introduces | Boss tests |
|---|---|---|
| 1 | run, jump, basic sword attack, crouch | timing and spacing |
| 2 | slide, ledge-grab and climb | movement under pressure (slide under attacks, climb to reach weak points) |
| 3 | sword combos (attack 1 → 2 → 3) | sustained damage windows, the final fight |

## 2. Goals, non-goals, anti-goals

**Goals**
- A complete, finishable game after each world. Every world milestone ends with a playable release.
- The original structure: main menu, 3 worlds, a boss at the end of each world.
- Keep and use the team's reworked character sheet (`Betteradventurer-Sheet.png`) as the player's art.
- Clean, readable object-oriented code that is still recognisably an OOP2 project: a meaningful class hierarchy, interfaces and polymorphism, not one giant class.
- Runs anywhere with one command or one double-click, with no Eclipse needed.

**Non-goals**
- Multiplayer, online features, leaderboards, mobile ports, controller support (it could be added later; not planned).
- An in-game level editor. Levels are made in Tiled, a free external map editor.
- Switching engines (libGDX, Unity, Godot). It stays Java and Swing.
- Recovering or matching the lost version's exact levels. It's gone. This is a rebuild in its spirit.

**Anti-goals (failure even if it "works")**
- Another half-finished build, with three worlds started and none done. World N+1 doesn't start until World N is released.
- Using assets whose license doesn't allow redistribution in a free game, or forgetting to credit their creators.
- Losing the team's hand-edited art, or quietly swapping the character for a different asset pack.
- Hiding everything in a few god classes, so the project no longer reads as an OOP project.

## 3. Constraints

- **Language and runtime:** Java, compiled with `--release 21` (matching the original Eclipse setting). Develop and run with `C:\jdk-26`. The `java` on PATH is Java 8, which is too old.
- **Libraries:** standard JDK only at runtime: Swing/Java2D for graphics, `javax.sound.sampled` for audio, `javax.xml` for parsing Tiled maps. JUnit 5 is used for tests only.
- **Build:** no build tool (user decision). The original Eclipse project layout stays (`src/`, `resources/`, plus a new `test/` folder) and still opens and runs in Eclipse. A PowerShell script, `build.ps1`, does the command-line work with plain JDK tools: compile (`javac`), run tests, run the game and, from M4, make the jar and package it (`jar`, `jpackage`). The JUnit console jar lives in `lib/` and is used for tests only. It never ships with the game.
- **Repository:** the project gets its own git repo inside `AMBOT (OOP2)`. The first commit is the untouched original prototype, so the starting point is preserved. Commit as `Pb2kick` only. Whether to add a GitHub remote is the user's call when M0 starts (FitCampus uses a private one).
- **Assets:** sourced from itch.io (or drawn by the user), and only packs whose license allows use in a freely distributed game. Every asset is recorded in `docs/ASSETS.md` with file, pack, author, URL and license, and credited on the in-game Credits screen.
- **Platform:** Windows is the primary target. Releases are a **portable zip** made with `jpackage --type app-image`: a folder with `AMBOT.exe` and a bundled runtime, so there's no installer and no WiX Toolset needed. _(Claude's call.)_ Since it's plain Java, it should also run on macOS and Linux, but those aren't tested targets.

## 4. Approaches considered

| | Approach | Trade-off | Decision |
|---|---|---|---|
| A | Rebuild everything at once (all 3 worlds and bosses in parallel) | Matches the original plan, but there's no finishable game until the very end, and it repeats how the original stalled | rejected |
| **B** | **One world at a time on a shared engine** | **The full original vision eventually, and a complete game after World 1. Slightly more up-front engine work so later worlds are mostly content** | **chosen** |
| C | One polished world with 5–6 levels and one boss | Cheapest in art and most likely to be polished, but smaller than what the team planned | rejected (B can stop after World 1 if needed, which gives most of C's safety) |

Also rejected: porting to libGDX, which would mean a full rewrite and lose the OOP2/Swing character.

## 5. Design

### 5.1 Screen and rendering
- **Logical canvas 640×360** (16:9), rendered to an off-screen image and scaled up by a whole number: 2× = 1280×720 windowed by default, 3× = 1920×1080 in fullscreen. This keeps pixel art crisp and fixes the current stretched-sprite bug. _(Claude's call.)_
- The player's 50×37 frames are drawn at 1:1 on the canvas, so they appear 100×74 at 2×.
- **Tiles are 16×16** on the canvas, the most common size for itch.io tilesets. One screen is 40 × 22.5 tiles.
- The window is centred and titled "AMBOT". It can be resized by snapping to whole-number scales, or toggled to fullscreen (F11).
- Rendering happens on one thread, and game state is updated on the same loop thread. Swing painting only blits the finished frame, which removes the current render/update race.

### 5.2 Game loop
- The fixed-timestep loop is kept (updates at a fixed rate, rendering capped). It **sleeps between ticks** instead of spinning, to fix the CPU bug.
- Target: 60 updates per second, with physics constants tuned to that rate, and rendering at 60 FPS or the display rate. _(Claude's call. The original 200 UPS is unnecessary.)_
- If the window loses focus, the game pauses (open the pause menu, reset inputs).

### 5.3 Game states
Each screen is a state object behind a common `GameState` interface (`update`, `render`, input handlers).

```
Title ─► MainMenu ─┬─► WorldSelect ─► Playing ⇄ Paused
                   ├─► Options               │
                   ├─► Credits               ├─► LevelComplete ─► next level / WorldSelect
                   └─► Quit                  ├─► GameOver ─► retry from checkpoint / MainMenu
                                             └─► WorldClear (after boss) ─► next world / Ending (after World 3)
```

- **Main menu:** Continue (when a save exists), New Game, World Select, Options, Credits, Quit.
- **World select:** a simple screen of the 3 worlds and their levels. Locked levels are greyed out. Not an overworld map.
- **Options:** music volume, sound-effects volume, fullscreen, and view controls.

### 5.4 Player
- A **state machine** with states idle, run, jump, fall, crouch, slide, ledge-hang, climb, attack 1/2/3, hurt and dead. Animations come from the reworked sheet, loaded across all 11 columns. A data table (row, frame count, frame duration, loop yes/no) replaces the current hard-coded `GetSpriteAmount` and crouch special case. Crouch becomes row 9.
- **Facing:** the sprite flips horizontally when moving left.
- **Hitbox separate from sprite:** collision uses a small box around the body, not the 50×37 frame.
- **Physics:** gravity, variable jump height (releasing jump early cuts it short), coyote time and jump buffering (small grace windows so jumps feel fair). Exact numbers are the builder's to tune.
- **Abilities unlock per world:** slide and ledge-grab from World 2 on, attack combos from World 3 on. Once unlocked, they stay available everywhere, including when replaying earlier levels.
- **Health:** hearts, starting at 3 _(Claude's call, see open questions)_. Touching an enemy or hazard costs 1 heart, knocks the player back and gives about 1 second of invincibility with blinking. Pits are instant death. At 0 hearts → GameOver → retry from the last checkpoint with full hearts.

### 5.5 Controls (default)
| Action | Key |
|---|---|
| Move | A / D or ← / → |
| Jump | Space or W or ↑ |
| Crouch / drop through platform | S or ↓ |
| Slide (World 2+) | S/↓ while running |
| Attack | J or left mouse |
| Pause | Esc |
| Fullscreen | F11 |

The original used W to float upward and Ctrl to crouch; both change to the scheme above. Rebinding keys is not planned. _(Claude's call.)_

### 5.6 Levels
- Designed in **Tiled** and saved as `.tmx` with CSV-encoded layers, which the game parses with `javax.xml`. No library needed.
- Layers:
  - `background` (decoration)
  - `solid` (collision)
  - `oneway` (platforms you can jump up through)
  - `hazard` (spikes etc.)
  - `foreground` (drawn over the player)
  - An object layer `entities` with `player_spawn`, `checkpoint`, `exit`, `enemy` (property `type`), `pickup` (property `type`) and `boss_arena` (marks where the camera locks for the boss).
- **Camera:** follows the player with a small dead zone, clamped to level bounds. It locks to the arena during a boss fight.
- **Parallax background:** 2–3 layers per world, from the world's asset pack.
- **Collision:** box-against-tile-grid, with one-way platforms and hazard tiles.
- **Structure per world:** 3 regular levels, then a boss level. The boss level is a short approach and then an arena. That's 12 levels in total. _(Claude's call.)_
- **Pickups:** a heart refill. Collectibles such as coins or gems aren't planned.
- **Who designs levels:** Claude designs all 12 (user decision). The user doesn't need to use Tiled. The workflow:
  - Levels are written as Tiled-compatible `.tmx` files, by hand or with small generator scripts, so the user can still open them in Tiled later if they ever want to.
  - A `LevelValidator` (run as a JUnit test over every level file) checks each level for:
    - exactly one spawn and one exit
    - unknown enemy, pickup or tile types
    - checkpoints that sit inside solid tiles
    - a boss level with no arena
  - A preview tool renders a whole level to a PNG for review.
  - Playability is checked with the input-injection harness: route the player from spawn to exit.

### 5.7 Enemies and bosses (OOP structure)

```
Entity (abstract: position, hitbox, update/render)
 └─ Creature (abstract: health, physics, facing, animation, implements Damageable)
     ├─ Player
     └─ Enemy (abstract: AI behaviour, contact damage, drops)
         ├─ concrete enemies per world, e.g. Walker (patrols edges), Flyer (sine-wave/hover), Shooter (ranged)
         └─ Boss (abstract: phases, attack patterns, health bar, arena)
             ├─ World1Boss · World2Boss · World3Boss
```

- Interfaces: `Damageable`, `Collidable`, `GameState`, and others where they make the code clearer. They show inheritance, abstraction and polymorphism without forcing it.
- **2–3 enemy types per world.** World 2 and 3 enemies are placed so that the new moves matter, for example low projectiles you slide under or enemies on ledges.
- **Each boss** has a health bar, 2–3 phases with readable warning ("tell") animations, and patterns that test its world's moves. The specific pattern designs are left to the builder within that outline. The user sees each boss when its world is released and can ask for changes then. There's no approval step before it's built. _(Claude's reading of "you will be doing everything".)_
- Enemy and boss art comes from itch.io packs, chosen to fit the player's small pixel-art scale.

### 5.8 Audio
- `javax.sound.sampled`, using WAV for sound effects and looping music per world, plus a menu theme and boss themes.
- Volumes are set in Options and saved.
- Sources: itch.io (or other free-license libraries), recorded in `docs/ASSETS.md`.

### 5.9 Saving
- Progress (unlocked worlds and levels, abilities, bosses defeated) and settings are saved automatically on level completion and on settings changes.
- Stored in `%APPDATA%\AMBOT\save.properties`. If the file is missing or broken, the game falls back to a new game without crashing.
- Checkpoints within a level are not saved to disk. Quitting mid-level restarts that level.

### 5.10 Assets workflow
- Until real art is chosen, placeholders (flat coloured tiles and boxes) let the engine and levels be built without waiting on art.
- At the start of each world's content milestone, Claude picks the itch.io packs (tileset, background, enemies, boss, music). It favours packs whose pixel scale matches the 50×37 player, and only uses licenses that allow free redistribution. The picks are shown with the world's release, and the user can ask for swaps then. _(Claude's reading of "you will be doing everything".)_
- **Downloading:** pack downloads go through the user's browser or with the user's go-ahead, since itch.io pages sometimes need a click-through or a name-your-price step. Claude never pays for or buys a pack. If the best fit is a paid pack, it's flagged and the user decides.
- `docs/ASSETS.md` is the single source of truth for credits and licenses. The Credits screen lists the original team, asset creators and the restoration.
- **Original team names:** deferred by the user. Until they're supplied, the Credits screen shows "Original OOP2 team". Claude asks again before the v0.1 release.

### 5.11 Code organisation
Lowercase packages under one root, e.g. `ambot.core` (loop, window, canvas), `ambot.states`, `ambot.entities`, `ambot.entities.enemies`, `ambot.entities.bosses`, `ambot.level` (TMX loader, tiles, camera), `ambot.audio`, `ambot.ui`, `ambot.save` and `ambot.util`. The final split is the builder's call. The original classes are refactored into this structure, and git history keeps the originals.

### 5.12 Testing and verification
- **JUnit 5 tests** for logic that doesn't need a window: TMX parsing, tile collision resolution, the player state machine (which inputs lead to which states), animation frame sequencing, save/load round-trips (including a broken save file) and state transitions.
- **Visual and feel checks** at each milestone: run the game and drive it with the same kind of input-injection harness used in the analysis, take screenshots, plus play sessions for the user.

## 6. Milestones (to be detailed in the implementation plan)

| Milestone | Delivers | Release |
|---|---|---|
| M0 | Own git repo with the original snapshot, `build.ps1`, small in-place fixes for analysis bugs 1–6 (crouch row, all 11 columns, aspect ratio, facing flip, sleeping loop, centred window), `docs/ASSETS.md` started. M1 replaces most of this code, but the fixes are about 30 lines and give a working "restored prototype" tag (`v0.0`) before the rebuild. _(Claude's call.)_ | v0.0 |
| M1 | Engine core: scaled canvas, fixed loop with sleep, game states, menu skeleton, player state machine with gravity, jump, facing and crouch, solid-tile collision in a hard-coded test room | — |
| M2 | Levels: TMX loader, tiles, collision, one-way platforms, hazards, camera, parallax, checkpoints, exit, placeholder test levels | — |
| M3 | Combat: hearts, damage, knockback, invincibility, attack hitboxes, World 1 enemies, HUD, GameOver/retry | — |
| M4 | World 1 content: tileset, background, 3 levels, Boss 1, audio, saving, pause, options, credits, packaged .exe | **v0.1: complete short game** |
| M5 | World 2: slide and ledge-grab mechanics, enemies, 3 levels, Boss 2 | **v0.2** |
| M6 | World 3: combos, enemies, 3 levels, final boss, ending screen, final polish | **v1.0** |

## 7. Left to the builder
- Exact physics and feel constants, hitbox sizes and enemy stats.
- Enemy AI details and boss pattern specifics, within §5.7. Boss concepts still go to the user for approval.
- Internal package and class split beyond §5.11, and the exact UI layout of menus.
- Level layouts, within the per-world move themes (Claude designs all levels, per §5.6).
- Exact frame split of sheet row 8 (sword stance / hurt / death), verified against the original sheet.
- How one-way platform drop-through (S/↓) and crouch/slide share a key: drop wins when standing on a one-way platform.

## 8. Decisions log

| Question | Answer | By |
|---|---|---|
| Route | B: one world at a time | user |
| Who designs the levels | Claude designs all 12 | user |
| Original team credits | Deferred. Placeholder until supplied, asked again before v0.1 | user |
| Asset packs and boss concepts | Claude picks; reviewed at each world release | Claude (reading of "doing everything") |
| Build tooling | No Maven. Keep the Eclipse layout, add `build.ps1` and a JUnit jar in `lib/` | user |
| Release format | Portable zip (app-image), no installer | Claude |
| Old bugs in M0 | Fixed in place (~30 lines), tagged `v0.0` | Claude |
| Canvas and tiles | 640×360 canvas, whole-number scaling, 16×16 tiles | Claude |
| Update rate | 60 UPS with a sleeping loop | Claude |
| Health model | 3 hearts, checkpoints, no lives | Claude |
| Level count | 3 levels and a boss level per world (12) | Claude |
| Controls | A/D move, Space jump, S crouch/slide, J or click attack, Esc pause | Claude |
| Collectibles | None, heart refills only | Claude |
| GitHub remote | Asked at M0 | open |
