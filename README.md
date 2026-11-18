# AMBOT

A 2D side-scrolling platformer in Java and Swing. AMBOT started as an OOP2 group project that was never finished. This repository restores it and finishes it, one world at a time: a main menu, three worlds, and a boss at the end of each.

**Status: v0.1 in progress.**
- **Done:**
  - saving, menus, options, sound and the Windows build
  - combat with hearts, enemies and Game Over
  - levels made in Tiled
- **Still placeholder:** World 1's real art, its levels and Boss 1, which come next. See [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Play

- **From source:** `.\build.cmd run`. It needs JDK 21 or newer; it uses `C:\jdk-26` unless the `AMBOT_JDK` environment variable points elsewhere.
- **Windows build:** `.\build.cmd package` creates `build\AMBOT-v0.1-windows.zip`. Unzip it anywhere and double-click `AMBOT\AMBOT.exe`. It includes its own Java runtime, so nothing needs installing.

Progress and settings are saved in `%APPDATA%\AMBOT\save.properties`. If the game ever hits an error, it shows a message and writes the details to `%APPDATA%\AMBOT\crash.log`.

## Controls

| Action | Keys |
|---|---|
| Move | A / D or ← / → |
| Jump | Space, W or ↑ |
| Crouch (ducks low shots) | S or ↓ |
| Drop through a plank | S or ↓ while standing on it |
| Attack | J |
| Pause | Esc |
| Fullscreen | F11 |

## Build tasks

Run `.\build.cmd <task>`. It uses plain JDK tools, with no Maven or Gradle.

| Task | What it does |
|---|---|
| `run` | Compile and start the game |
| `test` | Run the JUnit tests |
| `smoke` | Start the real window, drive it with scripted input, save screenshots to `build\smoke` |
| `levels` | Turn the ASCII levels in `levels-src\` into Tiled `.tmx` files, and regenerate the placeholder tiles and sounds |
| `preview` | Render every level to a PNG in `build\preview` |
| `jar` | Build `build\AMBOT.jar` |
| `package` | Build the portable Windows zip |

## Docs

- [`docs/ROADMAP.md`](docs/ROADMAP.md): milestones and status
- [`docs/LEVELS.md`](docs/LEVELS.md): how levels are made
- [`docs/ASSETS.md`](docs/ASSETS.md): every asset, its author and license
- [`docs/superpowers/specs/2026-11-17-ambot-restoration-design.md`](docs/superpowers/specs/2026-11-17-ambot-restoration-design.md): the restoration design

## License note

The player sprite is rvros's "Animated Pixel Adventurer". Its license allows using it in the game, but not redistributing the raw sprite sheet, so this repository stays private. See [`docs/ASSETS.md`](docs/ASSETS.md).
