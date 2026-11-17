# AMBOT (OOP2) — Analysis

_Run and analyzed 2026-11-17 on JDK 26 (`C:\jdk-26`). The project itself was not modified._

## What it is today

An Eclipse Java project (internal name "Game", JavaSE-21, no build tool) from an OOP2 group assignment, March 2025. It has 9 source files and about 580 lines, and is clearly a prototype. Running it opens a blank grey 1280×800 window with one animated character:

| Input | Effect |
|---|---|
| W A S D | Moves freely in 8 directions. No gravity, no jump. |
| Left click | Sword attack (plays once). |
| Ctrl | Crouch (broken, see below). |

It has no level, tiles, background, collision, enemies, HUD, menus, sound or win/lose condition.

The later version, with a menu, 3 worlds and a boss at the end of each world, is lost. The only other copy on the machine, `Downloads/Game.rar`, has the same March 2025 contents file for file.

Its structure follows the common Java Swing platformer tutorial layout: `MainClass` → `Game` (fixed-timestep loop, 120 FPS / 200 UPS) → `GamePanel` / `GameWindow`, plus `Entities`, `Inputs` and `Utils.Constants`.

## Runtime results

- Compiles with no errors on JDK 26, with only lint warnings.
- Steady 120 FPS / 200 UPS.
- No crashes. 400 forced run→crouch switches raised 0 exceptions.

## Bugs (all confirmed by running it)

1. **Crouch is broken** (`Player.java:27`). It shows nothing for 3 frames, then flashes a running frame. The crouch frame list points at cells (4,0) (5,0) (6,0) (0,1) of the *original* sheet. In the reworked sheet those cells are empty, and the crouch now lives in row 9.
2. **Only 7 of 11 sheet columns are loaded** (`Player.java:141`, `new BufferedImage[11][7]`). Rows 2, 4, 7 and 8 lose their last frames.
3. **The sprite is stretched about 18% wide**. 50×37 frames are drawn at 128×80 (`Player.java:60,62`).
4. **No facing direction.** Running left plays the right-facing run, so the character moonwalks.
5. **The game loop never sleeps** (`Game.java:58`). It uses about 1.08 CPU cores even when idle.
6. **The window opens off-centre**, because `setLocationRelativeTo(null)` runs before `pack()`. It also has no title.
7. Minor:
   - The character can leave the screen in any direction.
   - Unused constants: `WEPEQUIP`, `JUMP`, `Directions`.
   - Unused imports, and an empty `GamePanel.updateGame()`.
   - Typos: `PlayerConstats`, `resestAniTick`, `WindowFocustLost`.
   - Lowercase-convention violations in package names (`Entities`, `Inputs`, `Utils`).
   - A theoretical race between the render thread and the update thread on `playerAction`/`aniIndex`. It was never triggered in testing.

## Assets

The only asset family is rvros's free "Adventurer" sprite sheet (itch.io), with 50×37 frames.

| File | Size | Used? | What it is |
|---|---|---|---|
| `adventurer-Sheet.png` | 350×407 (7×11) | no | Original itch.io sheet, 72 frames |
| `Betteradventurer-Sheet.png` | 550×407 (11×11) | **yes** | The team's re-gridded copy, one animation per row |
| `MC.png` | 493×407 | no | Intermediate export (width isn't a multiple of 50) |
| `test.png` | 1409×982 | no | Blurry upscaled export |

A pixel comparison of `Betteradventurer-Sheet.png` against the original found:
- 66 frames are exact copies, many nudged 1–4 px vertically to line up.
- 4 frames were hand-altered. The biggest edit is cell (3,4).
- There is one stray pixel in cell (8,1).

Row layout of the reworked sheet:

| Row | Frames | Content |
|---|---|---|
| 0 | 4 | idle |
| 1 | 6 | run |
| 2 | 10 | crouch-ready → jump → somersault → fall |
| 3 | 5 | slide |
| 4 | 9 | ledge grab / climb up |
| 5 | 4 | sword-drawn idle |
| 6 | 7 | attack 1 |
| 7 | 10 | attack 2 + attack 3 |
| 8 | 10 | sword stance / hurt / death (exact split to verify) |
| 9 | 4 | crouch (original crouch frames, moved up 4 px) |

`bin/` is Eclipse's build output and holds copies of all of the above.
