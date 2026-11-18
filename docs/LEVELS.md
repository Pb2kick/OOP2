# AMBOT levels

Levels are [Tiled](https://www.mapeditor.org/) `.tmx` maps in `resources/levels/`. You don't need Tiled to make them: each level is drawn as ASCII art in `levels-src/`, and a build task turns it into a `.tmx` that Tiled can still open.

## Workflow

1. Edit or add `levels-src/<folder>/<name>.txt`.
2. Run `.\build.cmd levels`. This writes `resources/levels/<folder>/<name>.tmx` and refreshes the placeholder tileset.
3. Run `.\build.cmd test`. `LevelFilesTest` validates every level and fails if a `.tmx` no longer matches what its `.txt` generates. `RouteTest` replays each level's `.route` and checks it can be finished.
4. Run `.\build.cmd preview`. This writes a whole-level PNG for every level to `build\preview\`.

The generated `.tmx` files open in Tiled. Changes saved in Tiled are overwritten the next time `levels` runs, unless you delete that level's `.txt` first. The `.tmx` then becomes the source.

## ASCII format

```
title: First Steps
world: placeholder
---
........................
..P..............C...E..
########..####..########
```

The lines above `---` are map properties, written as `key: value`:
- `title`: the name shown on the Level Complete screen
- `world`: picks the parallax background
- `boss`: `true` for a boss level

Every row below `---` must be the same length. One character is one 16×16 tile.

| Char | Meaning |
|---|---|
| `#` | Solid ground. It gets a grass top where the tile above is not `#`. |
| `-` | One-way platform: jump up through it, stand on it, press ↓ to drop down |
| `^` | Hazard (spikes): touching it costs a heart and knocks you back |
| `b` | Background decoration (drawn behind, no collision) |
| `f` | Foreground decoration (drawn in front of the player, no collision) |
| `P` | Player spawn: feet on the bottom of this cell. Exactly one per level. |
| `C` | Checkpoint: 1 tile wide, 2 tall, bottom on this cell's bottom |
| `E` | Exit: same size as a checkpoint. Exactly one per level. |
| `w` | Walker enemy: patrols, turning at walls and ledges. Two sword hits. |
| `W` | Walker that drops a heart when defeated |
| `s` | Shooter enemy: a turret that fires bolts at standing height (crouch or jump them). Two sword hits. |
| `h` | Heart pickup: heals one heart, and only when you're hurt |
| `.` | Empty |

Falling below the bottom of the map counts as a pit: instant death, then Game Over with Retry from the last checkpoint. Running out of hearts ends the same way.

Enemies, hearts and the player stand with their feet on the bottom of their cell. A walker on a platform one tile high can be fought from the ground beside it: it turns at the ledge inside your sword's reach, but can't touch you.

## Tiled conventions

These are the rules the loader enforces:
- Orthogonal, finite maps with 16×16 tiles.
- Tile layers stored as CSV (Map Properties → Tile Layer Format → CSV).
- One tileset, embedded in the map (not an external `.tsx`).
- Tile layers named `background`, `solid`, `oneway`, `hazard` and `foreground`. Only `solid` is required.
- One object layer named `entities`, holding objects of these types: `player_spawn`, `checkpoint`, `exit`, `enemy` (with a `type` property of `walker` or `shooter`, and an optional `drop` property of `heart`), `pickup` (with a `type` property; `heart` is the only one) and `boss_arena`.

`LevelValidator` also checks for:
- exactly one spawn and one exit
- unknown object, enemy, pickup or tile types
- a spawn or checkpoint inside solid tiles or on a hazard
- objects outside the map
- a boss level without a `boss_arena`, or an arena in a level that isn't a boss level

## Design limits

These come from the player's physics. A full jump rises 48.4 px (just over 3 tiles) and carries about 68 px (about 4 tiles) on flat ground.

- Gaps are at most **3 tiles** wide.
- Steps up are at most **2 tiles**. A 3-tile step needs a pixel-perfect jump, so don't use one.
- One-way platforms are at most **2 tiles** apart vertically, for the same reason.
