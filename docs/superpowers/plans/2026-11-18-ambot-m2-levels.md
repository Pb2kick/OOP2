# AMBOT M2 (Levels) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Levels come from Tiled `.tmx` files instead of a hard-coded room. M2 adds one-way platforms, hazards, a scrolling camera, parallax backgrounds, checkpoints and a level exit. A validator and a preview tool check every level, and three placeholder levels can be played from spawn to exit, one after another.

**Architecture:**
- **Level data:** a `TmxLoader` (`javax.xml`, CSV layers only) builds an immutable `Level`. The level holds its tile layers, its tileset, its objects and a collision `TileMap` derived from the `solid`, `oneway` and `hazard` layers.
- **Where levels come from:** they're written as ASCII art in `levels-src/`. A dev tool (`AsciiTmx`, test folder only) turns each one into a Tiled-compatible `.tmx` in `resources/levels/`, and tests reuse the same tool to build small levels in memory.
- **Playing:** `PlayingState` owns the camera, the respawn point and the exit check. Drawing is split between a `LevelRenderer`, which the preview tool reuses, and a `ParallaxBackground`.

**Tech Stack:** Java (`--release 21`) on JDK 26 at `C:\jdk-26`, Swing/Java2D, `javax.xml.parsers` (DOM), JUnit 5 (console standalone 1.11.4, tests only), `build.ps1` through `build.cmd`.

**Spec:** [`docs/superpowers/specs/2026-11-17-ambot-restoration-design.md`](../specs/2026-11-17-ambot-restoration-design.md), mainly §5.6 (Levels), §5.10 (placeholders) and §5.12 (testing). The milestone map is [`docs/ROADMAP.md`](../../ROADMAP.md). This plan builds on the M1 engine as merged at `main` `1173080`.

All paths are relative to the project root `C:\Users\Pb2kick\Desktop\Project Restorations\AMBOT (OOP2)`.

## Global Constraints

- **Compiler:** `javac --release 21` with the JDK at `C:\jdk-26` (`AMBOT_JDK` overrides it). Never use the `java` on PATH, which is Java 8.
- **Libraries:** none at runtime beyond the JDK. TMX parsing uses `javax.xml` only (spec §3).
- **Build:** no Maven or Gradle. Every command goes through `.\build.cmd <task>`. Dev tools live under `test/ambot/tools/` and never ship.
- **Canvas and loop:** 640×360 logical canvas, 16×16 tiles, 60 UPS. Physics constants stay as M1 tuned them.
- **Tiled format** (spec §5.6):
  - orthogonal, finite maps with 16×16 tiles, CSV-encoded layers and one embedded tileset
  - tile layers named `background`, `solid`, `oneway`, `hazard` and `foreground`
  - one object layer named `entities`, holding object types `player_spawn`, `checkpoint`, `exit`, `enemy` (property `type`), `pickup` (property `type`) and `boss_arena`
  - map properties `title`, `world`, and `boss` (`true` for a boss level)
- **Placeholder art only** (spec §5.10): flat-coloured tiles and procedurally drawn hills. No itch.io packs until M4. Placeholder art is recorded in `docs/ASSETS.md` as own work.
- **Level design limits** (from M1 physics: a full jump rises about 51 px, about 3 tiles, and covers about 68 px, about 4 tiles, on flat ground):
  - gaps at most 3 tiles wide
  - steps up at most 3 tiles
  - one-way platforms at most 3 tiles apart vertically
- **Damage:** hearts don't exist until M3. In M2, touching a hazard or falling out of the map sends the player back to the respawn point at once.
- **Not in M2:**
  - `enemy` and `pickup` objects are parsed and validated, but spawning them is M3.
  - Locking the camera to the boss arena comes with Boss 1 in M4.
  - Real tilesets and backgrounds also come in M4.
- **Git:** work on branch `m2-levels`. Commit as `Pb2kick` with no co-author trailers, and credit no AI tool in any file or commit message. The repo stays private (asset license).

## Review Focus

1. **One-way platforms at speed or from below.**
   - Falling at full speed (`MAX_FALL` 6) lands the player on top.
   - Jumping up from underneath passes through.
   - A body whose feet are partway inside a one-way tile is never "on the ground" and never gets stuck there.
   - Tested in Task 1: `landsOnOneWayAtMaxFall`, `risesThroughOneWay`, `notGroundedInsideOneWay`.
2. **Pressing ↓ when only part of the player is over a one-way platform,** or when standing on solid ground. The player doesn't drop through solid ground and crouches as in M1. Tested in Task 1: `noDropWhenPartlyOnSolid`, `crouchOnSolidUnchanged`.
3. **A level saved by real Tiled.**
   - CRLF line endings, spaces after commas, flip bits on tile ids, and the newer `class` attribute in place of `type` all load.
   - A non-CSV encoding or an external `.tsx` tileset fails with a message that names the file and the fix.
   - Tested in Task 2: `csvWithCrLfAndSpacesParses`, `flipFlagsAreStripped`, `classAttributeActsAsType`, `base64EncodingIsRejectedWithHint`, `externalTilesetIsRejectedWithHint`.
4. **A respawn point that is itself dangerous.** A spawn or checkpoint that overlaps hazard or solid tiles is flagged, so the player can never respawn into an endless death loop. Tested in Task 3: `flagsSpawnInsideSolid`, `flagsCheckpointOverHazard`.
5. **A level smaller than the screen, or the camera at a level's edge.** The camera centres a small level, the renderer never reads tiles outside the map, and nothing throws. Tested in Task 5 (`centresLevelSmallerThanView`) and Task 7 (`drawsViewLargerThanMapWithoutError`).

---

### Task 1: One-way platforms, hazard tiles and drop-through

**Files:**
- Modify: `src/ambot/level/TileMap.java`, `src/ambot/level/TileCollider.java`, `src/ambot/entities/Creature.java`, `src/ambot/entities/player/Player.java`, `src/ambot/entities/player/PlayerTuning.java`
- Test: `test/ambot/level/TileMapTest.java`, `test/ambot/level/TileColliderTest.java`, `test/ambot/entities/player/PlayerTest.java`

**Interfaces:**
- Produces:
  - In `TileMap`:
    - constants `ONEWAY = 2` and `HAZARD = 3`
    - `boolean isOneWay(int tx, int ty)` and `boolean isHazard(int tx, int ty)` (false out of bounds)
    - `boolean overlaps(Rect box, int kind)`
    - `boolean touchesHazard(Rect box)`: `overlaps` on the box shrunk by `HAZARD_INSET = 2` px on every side
    - in `fromAscii`, `'-'` means one-way and `'^'` means hazard
  - In `TileCollider`:
    - `moveY(Rect, double, TileMap, boolean ignoreOneWay)`
    - `onGround(Rect, TileMap, boolean ignoreOneWay)`
    - `onOneWayOnly(Rect, TileMap)`
    - the old three- and two-argument forms stay and pass `false`
  - In `Creature`: `applyGravityAndMove(TileMap map, boolean ignoreOneWay)`. The old form passes `false`.
  - In `PlayerTuning`: `DROP_THROUGH_TICKS = 6`.

- [ ] **Step 1: Write the failing tests.**
  - In `TileMapTest`:
    - `asciiReadsOneWayAndHazard`: a map with `-` and `^` gives `isOneWay`/`isHazard` true there, and `isSolid` false for both.
    - `overlapsChecksKind`: a box over one solid and one one-way tile overlaps both `SOLID` and `ONEWAY`, but not `HAZARD`.
    - `hazardInsetForgivesGrazing`: with a hazard tile at x 16..32, a box whose right edge is at x 17 doesn't touch, and one whose right edge is at x 19 does.
  - In `TileColliderTest`, on a map with a one-way row whose top is at y=64:
    - `landsOnOneWayAtMaxFall`: `moveY(box with bottom 60, +6)` gives `Move(64 - h, true)`.
    - `risesThroughOneWay`: `moveY(box with top 70, -6)` isn't blocked.
    - `oneWayNeverBlocksSideways`: `moveX` across a one-way tile isn't blocked.
    - `ignoreOneWayFallsThrough`: the max-fall case with `ignoreOneWay = true` isn't blocked.
    - `notGroundedInsideOneWay`: a box with bottom at y=66 isn't on the ground. With bottom at exactly 64 it is.
    - `onOneWayOnlyNeedsNoSolidUnderfoot`: true over one-way only, false when any column under the box is solid.
  - In `PlayerTest`:
    - `downOnOneWayDropsThrough`: standing on a one-way ledge, `press(DOWN)` then `held(DOWN)` for 10 ticks leaves `y` lower than before and the state `FALL`.
    - `noDropWhenPartlyOnSolid`: the same input while standing over one solid and one one-way column gives `CROUCH` and an unchanged `y`.
    - `crouchOnSolidUnchanged`: the M1 crouch behaviour on solid floor still holds.
    - `landsOnOneWayFromAbove`: falling from 3 tiles above a one-way ledge ends `onGround()` with feet at the ledge top.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test`
  Expected: compile errors for `isOneWay`, `ONEWAY` and the 4-argument `moveY`.

- [ ] **Step 3: Implement.**
  - **`TileCollider.moveY` going down:** a newly entered row blocks if any column under the box is `SOLID`, or `ONEWAY` when `!ignoreOneWay`. Rows the box's bottom edge newly enters were always below its old bottom, so no "was above" test is needed. Going up, and in `moveX`, only `SOLID` blocks.
  - **`TileCollider.onGround`:** solid below counts as before. One-way below counts only when `Math.abs(box.bottom() - row * T) < 0.02`.
  - **`Player.update`:** before step 1, set `dropTicks = DROP_THROUGH_TICKS` when `c.wasPressed(DOWN) && onGround && TileCollider.onOneWayOnly(hitbox(), map)`. Crouching additionally needs `dropTicks == 0`. Physics calls `applyGravityAndMove(map, dropTicks > 0)`, and `dropTicks` counts down to 0 afterwards. `respawn` resets it.

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all tests pass (85 from M1 plus the new ones).

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/level src/ambot/entities test/ambot/level test/ambot/entities
  git commit -m "feat(level): one-way platforms with drop-through, hazard tiles"
  ```

### Task 2: Tileset, level model and TMX loader

**Files:**
- Create in `src/ambot/level/`: `Tileset.java`, `TileLayer.java`, `LevelObject.java`, `Spot.java`, `Level.java`, `TmxLoader.java`, `LevelFormatException.java`
- Create: `test/ambot/tools/PlaceholderTiles.java`, `resources/tiles/placeholder.png` (generated)
- Test: `test/ambot/level/TmxLoaderTest.java`

**Interfaces:**
- Consumes: `TileMap(int w, int h, int[] tiles)` and the `SOLID`/`ONEWAY`/`HAZARD` constants (Task 1).
- Produces:
  - **`Tileset`:**
    - `Tileset(int firstGid, int tileCount, int columns, BufferedImage image)`
    - `boolean contains(int gid)`
    - `BufferedImage tile(int gid)`: a cached 16×16 sub-image; throws `IllegalArgumentException` when `!contains`
    - `int firstGid()`, `int tileCount()`
  - **`TileLayer`:** `TileLayer(String name, int width, int height, int[] gids)`, with `name()`, `width()`, `height()`, and `int gid(int tx, int ty)` (0 out of bounds). It keeps a defensive copy of the gids.
  - **`Spot`:** `record Spot(double x, double y)`, a point where feet stand: x is the centre, y is the bottom.
  - **`LevelObject`:**
    - `record LevelObject(String type, Rect bounds, Map<String,String> properties)`, with `String property(String key)` (null when absent)
    - type constants `PLAYER_SPAWN`, `CHECKPOINT`, `EXIT`, `ENEMY`, `PICKUP`, `BOSS_ARENA`, holding the spec's strings
  - **`Level`:**
    - constructor: `Level(String name, Map<String,String> properties, Tileset tileset, int widthTiles, int heightTiles, List<TileLayer> layers, List<LevelObject> objects)`
    - layer-name constants `BACKGROUND`, `SOLID`, `ONEWAY`, `HAZARD`, `FOREGROUND`, plus `ENTITIES = "entities"`
    - metadata: `name()`; `title()` (the `title` property, or else `name`); `world()` (`world`, or else `"placeholder"`); `isBossLevel()` (true when `boss` is `true`)
    - tiles: `tileset()`; `TileMap collision()` (solid wins over one-way, which wins over hazard; missing layers count as empty); `Optional<TileLayer> layer(String)`; `widthPx()`, `heightPx()`
    - objects: `List<LevelObject> objects()` and `objects(String type)`
    - shortcuts: `Spot spawn()` (from the first `player_spawn` point; `IllegalStateException` if there is none); `Rect exit()`; `List<Rect> checkpoints()`; `Optional<Rect> bossArena()`
  - **`TmxLoader`:**
    - `static Level load(String resourcePath)`, a classpath path such as `"/levels/placeholder/p1.tmx"`
    - `static Level parse(InputStream in, String resourcePath)`
    - every failure is a `LevelFormatException extends RuntimeException`, with a message that starts with the resource path
  - **`PlaceholderTiles` (tool):**
    - gid constants `GROUND = 1`, `GROUND_TOP = 2`, `ONEWAY = 3`, `SPIKES = 4`, `BACK_WALL = 5`, `GRASS = 6`, `TILE_COUNT = 6`
    - `static BufferedImage draw()`, which returns a 96×16 ARGB image
    - `main` writes `resources/tiles/placeholder.png`

- [ ] **Step 1: Write the placeholder tileset tool and generate the PNG.**
  Each tile is a flat PICO-8 colour with a simple shape:
  - ground: `0x5F574F` with darker speckles at fixed positions
  - ground top: the same, with a 3-px `0x00E436` grass band
  - one-way platform: a 4-px `0xAB5236` plank at the top, transparent below
  - spikes: three `0xC2C3C7` triangles on a transparent background
  - back wall: `0x1D2B53` bricks
  - grass tuft: `0x008751` blades, transparent

  Run it once with the test classpath: `.\build.cmd test`, then `C:\jdk-26\bin\java -cp "build\classes;build\test-classes" ambot.tools.PlaceholderTiles`.
  Expected: `resources/tiles/placeholder.png` exists and is 96×16.

- [ ] **Step 2: Write the failing loader tests** in `TmxLoaderTest`.
  - Fixtures: TMX strings built inside the test. They use `<image source="../../tiles/placeholder.png" width="96" height="16"/>` and are parsed with the resource path `/levels/test/t.tmx`.
  - Loading and resolving:
    - `parsesSizesLayersAndProperties`: a 4×3 map with `solid` and `oneway` CSV layers and `title`/`world`/`boss` properties gives the right `widthPx()`, `title()`, `world()` and `isBossLevel()`, and `collision().isOneWay(1,1)`.
    - `solidWinsOverOneWayAndHazard`: a cell set in all three layers is `SOLID`.
    - `missingOptionalLayersAreEmpty`: a map with only `solid` loads, and `layer("foreground")` is empty.
    - `resolvesTilesetImageRelativeToFile`: `tileset().tile(2)` is 16×16.
  - Objects:
    - `readsObjects`:
      - a point `player_spawn` at (40,48) gives `spawn()` = `Spot(40,48)`
      - a 16×32 `exit` rectangle comes back from `exit()`
      - an `enemy` with property `type=walker` keeps its property
  - Files saved by real Tiled:
    - `csvWithCrLfAndSpacesParses`
    - `flipFlagsAreStripped`: gid `0x80000002` reads as 2
    - `classAttributeActsAsType`
  - Rejections. Each throws `LevelFormatException`, and the message contains the text given:
    - `base64EncodingIsRejectedWithHint`: "CSV"
    - `externalTilesetIsRejectedWithHint`: "embed"
    - `wrongTileSizeIsRejected`
    - `csvCountMismatchIsRejected`
    - `missingSolidLayerIsRejected`
    - `unknownLayerNameIsRejected`: names the layer
    - `malformedXmlIsRejected`
    - `missingImageIsRejected`

- [ ] **Step 3: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.level.TmxLoaderTest`
  Expected: compile errors (no `TmxLoader`).

- [ ] **Step 4: Implement the model classes and `TmxLoader`.**
  - **Parser setup:** `DocumentBuilderFactory` with `disallow-doctype-decl` true and `setExpandEntityReferences(false)`.
  - **Map checks:** `orientation` must be `orthogonal`, `infinite` must not be `1`, and `tilewidth`/`tileheight` must be 16.
  - **Tileset:** there must be exactly one `<tileset>`, and it can't have a `source` attribute. The error says: "external tilesets (.tsx) are not supported; embed the tileset in the map".
  - **Tile layers:**
    - `<data encoding="csv">` with no `compression`; otherwise the error says: "set Tile Layer Format to CSV".
    - Values are split on commas and whitespace, parsed as `long`, then masked with `& 0x0FFFFFFFL`.
    - The count must be exactly `width*height`.
  - **Layer names:** only the five tile layers and the `entities` object group are allowed. Any other name is rejected.
  - **Objects:** an object's type is its `type` attribute, else its `class`. A `<point/>` child gives a 0×0 rectangle. `<properties>` become the property map.
  - **Image path:** resolved against the resource path's parent with `java.net.URI.resolve`, then loaded with `TmxLoader.class.getResourceAsStream`.

- [ ] **Step 5: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all tests pass.

- [ ] **Step 6: Commit.**
  ```bash
  git add src/ambot/level test/ambot/level/TmxLoaderTest.java test/ambot/tools/PlaceholderTiles.java resources/tiles
  git commit -m "feat(level): Tiled TMX loader, level model, placeholder tileset"
  ```

### Task 3: Level validator

**Files:**
- Create: `src/ambot/level/LevelValidator.java`
- Test: `test/ambot/level/LevelValidatorTest.java`

**Interfaces:**
- Consumes: `Level`, `LevelObject`, `TileMap.overlaps`, `TileMap.touchesHazard` (Tasks 1–2), and `PlayerTuning.HITBOX_W`/`HITBOX_H`.
- Produces:
  - `static List<String> check(Level level)`: an empty list means the level is valid.
  - Known-type sets: `KNOWN_ENEMY_TYPES = Set.of()` (M3 adds the World 1 enemies) and `KNOWN_PICKUP_TYPES = Set.of("heart")`.

- [ ] **Step 1: Write the failing tests.**
  - Fixtures: levels are built directly with the `Level` constructor, using a `Tileset` over a blank 96×16 image. Each test asserts that one problem contains a key phrase, and that a valid base level gives `List.of()`.
  - Spawn and exit counts:
    - `validLevelHasNoProblems`
    - `flagsMissingSpawn` ("player_spawn")
    - `flagsTwoExits` ("exit")
  - Unknown types:
    - `flagsUnknownObjectType` ("unknown object type 'chest'")
    - `flagsUnknownEnemyType` ("unknown enemy type 'dragon'")
    - `flagsEnemyWithoutType`
    - `flagsUnknownPickupType`
    - `flagsUnknownTileId`: gid 9 against a 6-tile set; the message names the layer and the tile position
  - Placement:
    - `flagsCheckpointInsideSolid`
    - `flagsSpawnInsideSolid`: the 14×28 box standing on the spawn overlaps solid tiles
    - `flagsCheckpointOverHazard`
    - `flagsSpawnOverHazard`
    - `flagsObjectOutsideMap`
  - Boss arenas:
    - `flagsBossLevelWithoutArena`
    - `flagsArenaInNonBossLevel`
  - Reporting:
    - `reportsEveryProblem`: a level with no spawn and an unknown object type yields both problems.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.level.LevelValidatorTest`
  Expected: compile error (no `LevelValidator`).

- [ ] **Step 3: Implement `check`.** It applies every rule above, and each message includes the object's or tile's pixel or tile position.

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all tests pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/level/LevelValidator.java test/ambot/level/LevelValidatorTest.java
  git commit -m "feat(level): LevelValidator for spawn/exit, types, placement and boss arenas"
  ```

### Task 4: ASCII level sources, TMX writer and the `levels` build task

**Files:**
- Create: `test/ambot/tools/AsciiTmx.java`, `test/ambot/level/TestLevels.java`, `docs/LEVELS.md`
- Modify: `build.ps1` (adds the `levels` task)
- Test: `test/ambot/tools/AsciiTmxTest.java`

**Interfaces:**
- Consumes: `TmxLoader.parse`, the gid constants in `PlaceholderTiles`, `Level` (Task 2), and `LevelValidator.check` (Task 3).
- Produces:
  - **`AsciiTmx` conversion:**
    - `static String toTmx(Map<String,String> properties, String... rows)`: TMX text
    - `static String convert(String sourceText)`: header lines `key: value`, a line `---`, then the rows
  - **`AsciiTmx.main(String[] args)`:** `args[0]` is the source folder (`levels-src`) and `args[1]` the resources folder (`resources`). It writes `resources/levels/<relative path>.tmx` for every `levels-src/**/*.txt`; a missing source folder converts nothing. It then rewrites `resources/tiles/placeholder.png` from `PlaceholderTiles.draw()`.
  - **`TestLevels`:** `static Level ascii(String... rows)` (with title `Test` and world `placeholder`, parsed as `/levels/test/test.tmx`) and `static Level ascii(Map<String,String> props, String... rows)`.

- [ ] **Step 1: Write the failing tests** in `AsciiTmxTest`. They convert ASCII, load it with `TmxLoader.parse`, and assert:
  - Solid tiles:
    - `hashIsSolidWithGrassOnTop`: `#` under `.` gets gid `GROUND_TOP`, and `#` under `#` gets `GROUND`. Both are `SOLID`.
  - Other tile layers:
    - `dashIsOneWayCaretIsHazard`
    - `bAndFGoToDecorationLayers`: these go to the `background` and `foreground` layers and don't collide.
  - Objects:
    - `pIsSpawnStandingOnCellBottom`: `P` at tile (2,3) gives `Spot(40, 64)`.
    - `cAndEAreTwoTileTallRects`: `C`/`E` at tile (5,3) gives `Rect(80, 32, 16, 32)`.
  - Header and errors:
    - `headerBecomesMapProperties`
    - `unknownCharFailsWithPosition`: `x` at (3,1) fails with a message that contains `3,1`.
    - `raggedRowsFail`
  - Output and the test helper:
    - `outputIsStableAcrossRuns`: the same input gives the same string.
    - `testLevelsHelperRoundTrips`: `TestLevels.ascii` gives a `Level` whose `LevelValidator.check` is empty for a level with one `P` and one `E`.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.tools.AsciiTmxTest`
  Expected: compile error.

- [ ] **Step 3: Implement `AsciiTmx` and `TestLevels`.**
  - **Map header:** `<map version="1.10" tiledversion="1.10.2" orientation="orthogonal" renderorder="right-down" ... infinite="0">`, with `\n` line endings.
  - **Tileset:** embedded, `firstgid="1" name="placeholder"`, with `tilecount` and `columns` set to `TILE_COUNT` and `<image source="../../tiles/placeholder.png" width="96" height="16"/>`. That path works for any level stored two folders deep under `resources/`.
  - **Layers:** written in the order background, solid, oneway, hazard, foreground, then the `entities` object group.
  - **Objects:** carry the `type` attribute, and object ids count up from 1.
  - **Determinism:** properties are sorted by key.

- [ ] **Step 4: Add the `levels` task to `build.ps1`.** It runs `[void](Invoke-CompileTests)`, then `java -cp "build\classes;build\test-classes" ambot.tools.AsciiTmx levels-src resources`. Add `levels` to `ValidateSet` and to the usage comment.
  Run: `.\build.cmd levels`
  Expected: exits 0. There are no level sources yet. The rewritten `placeholder.png` keeps the same pixels; `LevelFilesTest` (Task 8) pins that.

- [ ] **Step 5: Write `docs/LEVELS.md`.** It covers:
  - the ASCII dialect: `#` solid, `-` one-way, `^` hazard, `b` background, `f` foreground, `P` spawn, `C` checkpoint, `E` exit and `.` empty
  - the header keys
  - the Tiled layer and object conventions from Global Constraints
  - the design limits from Global Constraints
  - the workflow: edit `levels-src/…/*.txt`, then run `.\build.cmd levels`, `.\build.cmd test` and `.\build.cmd preview` (from Task 8)
  - a note that the generated `.tmx` files open in Tiled, and that hand edits made in Tiled are overwritten by `levels` unless the source `.txt` is deleted

- [ ] **Step 6: Run the whole suite, then commit.**
  Run: `.\build.cmd test`. Expected: all tests pass.
  ```bash
  git add test/ambot/tools/AsciiTmx.java test/ambot/tools/AsciiTmxTest.java test/ambot/level/TestLevels.java build.ps1 docs/LEVELS.md
  git commit -m "feat(tools): ASCII-to-TMX level writer and levels build task"
  ```

### Task 5: Camera

**Files:**
- Create: `src/ambot/level/Camera.java`
- Test: `test/ambot/level/CameraTest.java`

**Interfaces:**
- Produces:
  - **Construction:** `Camera(int viewW, int viewH)`, plus constants `DEAD_ZONE_W = 48` and `DEAD_ZONE_H = 72` (centred on the view).
  - **Movement:**
    - `void follow(Rect target, int levelW, int levelH)`: moves only as far as needed to keep the target's centre inside the dead zone, then clamps.
    - `void snapTo(Rect target, int levelW, int levelH)`: centres on the target, then clamps.
  - **Clamping:** on each axis, a level smaller than the view is centred (the offset `(level - view) / 2.0`, which is negative). Otherwise the camera is clamped to `[0, level - view]`.
  - **Reading it:** `int x()` and `int y()` (`Math.round`, so tiles never shimmer), and `Rect view()`.

- [ ] **Step 1: Write the failing tests.** All use a 640×360 view.
  - Snapping and clamping:
    - `snapCentresOnTarget`: in a 2000×1000 level, a target centred at (1000,500) puts the camera at (680,320).
    - `clampsAtLeftAndTopEdges`
    - `clampsAtRightAndBottomEdges`
    - `centresLevelSmallerThanView`: a 320×200 level gives (-160,-80).
  - Following:
    - `holdsStillInsideDeadZone`: moving the target 20 px right from centre leaves `x()` unchanged.
    - `followsPastDeadZone`: moving it 40 px right moves the camera 16 px.
    - `followsVerticallyPastDeadZone`
  - Rounding:
    - `roundsToWholePixels`

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.level.CameraTest`
  Expected: compile error.

- [ ] **Step 3: Implement `Camera`.**

- [ ] **Step 4: Run the suite.**
  Run: `.\build.cmd test`
  Expected: all tests pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/level/Camera.java test/ambot/level/CameraTest.java
  git commit -m "feat(level): dead-zone camera clamped to level bounds"
  ```

### Task 6: Parallax background with placeholder hills

**Files:**
- Create in `src/ambot/gfx/`: `ParallaxLayer.java`, `ParallaxBackground.java`, `PlaceholderBackgrounds.java`
- Test: `test/ambot/gfx/ParallaxTest.java`

**Interfaces:**
- Produces:
  - **`ParallaxLayer`:** `record ParallaxLayer(BufferedImage image, double factor, int y)`. Here `y` is the layer's top edge on the canvas when the camera is at the bottom of the level.
  - **`ParallaxBackground`:**
    - `ParallaxBackground(Color sky, List<ParallaxLayer> layers)`
    - `void render(Graphics2D g, int cameraX, int cameraAboveBottom)`
    - `static int wrapOffset(int cameraX, double factor, int imageW)`: returns a value in `(-imageW, 0]`
  - **`PlaceholderBackgrounds`:** `static ParallaxBackground forWorld(String world)`. Any world, known or not, gets the default: a far range with factor `0.2` and near hills with factor `0.5`. Each is a 320-px-wide image whose bottom reaches canvas y=360. M4 replaces these with art for each world.

- [ ] **Step 1: Write the failing tests.**
  - Offsets:
    - `wrapOffsetStaysInRange`: (0, 0.5, 320) gives 0; (100, 0.5, 320) gives -50; (700, 0.5, 320) gives -30; and a negative camera x (-160, 0.5, 320) gives -240.
  - Placeholder hills:
    - `placeholderHasTwoSlowerLayers`: the factors are strictly between 0 and 1 and increase from back to front.
    - `unknownWorldFallsBack`
  - Rendering:
    - `renderCoversWholeCanvas`: after rendering into a 640×360 ARGB image at camera x 1234, every pixel's alpha is 255.
    - `layersSinkWhenCameraRises`: the near layer's topmost opaque row in the centre column is lower with `cameraAboveBottom` 200 than with 0.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.gfx.ParallaxTest`
  Expected: compile error.

- [ ] **Step 3: Implement.**
  - **`render`:** fills the sky, then draws each layer at `wrapOffset` and repeats it to the right edge, at a y of `layer.y() + Math.round(cameraAboveBottom * factor)`.
  - **Placeholder images:** filled polygons made from a sum of two sine waves with fixed constants, so every run looks the same. They use PICO-8 colours `0x5F574F` (far) and `0x008751` (near), with a `0x29366F` sky.

- [ ] **Step 4: Run the suite.**
  Run: `.\build.cmd test`
  Expected: all tests pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/gfx test/ambot/gfx/ParallaxTest.java
  git commit -m "feat(gfx): parallax background with placeholder hills"
  ```

### Task 7: Level renderer and whole-level preview image

**Files:**
- Create: `src/ambot/level/LevelRenderer.java`, `test/ambot/tools/LevelPreview.java`
- Test: `test/ambot/level/LevelRendererTest.java`

**Interfaces:**
- Consumes: `Level`, `Tileset.tile`, `TestLevels.ascii` (Tasks 2 and 4).
- Produces:
  - **`LevelRenderer`:**
    - `LevelRenderer(Level level)`
    - `void drawBehind(Graphics2D g, Rect view)`: draws the background, solid, oneway and hazard layers, only for tiles inside `view` and clamped to the map
    - `void drawMarkers(Graphics2D g, Rect activeCheckpoint)`: checkpoints as a pole with a flag (grey `0x83769C`, or yellow `0xFFEC27` when its bounds equal `activeCheckpoint`, which may be null) and the exit as a `0x29ADFF` door frame
    - `void drawFront(Graphics2D g, Rect view)`: the foreground layer
    - Drawing is in level pixel coordinates; the caller translates by the camera.
  - **`LevelPreview.render(Level level)`:** a whole-level `BufferedImage` at 1×, sky `0x29366F`. It draws behind, markers and front, plus outlines: a green 14×28 box standing on the spawn, magenta 6×6 squares for `enemy`/`pickup`, and a red rectangle for `boss_arena`.

- [ ] **Step 1: Write the failing tests.**
  - `previewIsLevelSized`: `TestLevels.ascii` of a 10×4 level renders as 160×64.
  - `solidTileIsDrawn`: the pixel at the centre of a `#` tile isn't the sky colour.
  - `drawsViewLargerThanMapWithoutError`: `drawBehind` with the view `Rect(-200, -200, 1000, 1000)` doesn't throw.
  - `activeCheckpointLooksDifferent`: the flag pixel's colour differs between active and inactive.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.level.LevelRendererTest`
  Expected: compile error.

- [ ] **Step 3: Implement `LevelRenderer` and `LevelPreview.render`.**

- [ ] **Step 4: Run the suite.**
  Run: `.\build.cmd test`
  Expected: all tests pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/level/LevelRenderer.java test/ambot/level/LevelRendererTest.java test/ambot/tools/LevelPreview.java
  git commit -m "feat(level): level renderer and whole-level preview image"
  ```

### Task 8: Placeholder levels, level catalog, level-file checks and the `preview` task

**Files:**
- Create: `levels-src/placeholder/p1.txt`, `p2.txt`, `p3.txt`; `resources/levels/placeholder/p1.tmx`, `p2.tmx`, `p3.tmx` (generated); `src/ambot/level/LevelCatalog.java`
- Modify: `test/ambot/tools/LevelPreview.java` (adds `main`), `build.ps1` (adds the `preview` task)
- Test: `test/ambot/level/LevelFilesTest.java`

**Interfaces:**
- Consumes: `AsciiTmx.convert`, `TmxLoader.load`, `LevelValidator.check`, `PlaceholderTiles.draw`, `LevelPreview.render` (Tasks 2–4 and 7).
- Produces:
  - **`LevelCatalog`:**
    - `PLACEHOLDER_PATHS = List.of("/levels/placeholder/p1.tmx", "/levels/placeholder/p2.tmx", "/levels/placeholder/p3.tmx")`
    - `static LevelCatalog placeholder()` and `static LevelCatalog of(Level... levels)`
    - `int size()`
    - `Level load(int index)`: loads and validates; problems throw `LevelFormatException` with the path and all problems joined by `"; "`
  - **`LevelPreview.main(args[0] = output folder)`:** writes `<name>.png` for every path in `PLACEHOLDER_PATHS`.
  - **`build.ps1` `preview` task:** `[void](Invoke-CompileTests)`, then `java -cp "build\classes;build\test-classes" ambot.tools.LevelPreview build\preview`. It's added to `ValidateSet` and the usage comment.

- [ ] **Step 1: Write the failing tests** in `LevelFilesTest`. Each walks the file system from the project root, the folder `build.ps1` runs in.
  - `everyLevelFileIsValid`: for every `resources/levels/**/*.tmx`, `LevelValidator.check(TmxLoader.load("/" + relative path))` is empty. The assertion message names the file.
  - `catalogListsEveryPlaceholderFile`: the set of `PLACEHOLDER_PATHS` equals the files found in `resources/levels/placeholder`.
  - `generatedFilesAreUpToDate`:
    - What it checks: for every `levels-src/**/*.txt`, `AsciiTmx.convert(source)` equals the matching `.tmx`.
    - How it compares: both sides are normalised to `\n`.
    - On failure, the message says "run .\build.cmd levels".
  - `placeholderTilesImageIsCurrent`: `resources/tiles/placeholder.png` matches `PlaceholderTiles.draw()` pixel for pixel.
  - `catalogLoadsEveryLevel`: `LevelCatalog.placeholder().load(i)` works for every `i`.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.level.LevelFilesTest`
  Expected: compile error (no `LevelCatalog`).

- [ ] **Step 3: Implement `LevelCatalog`, `LevelPreview.main` and the `preview` task, then design the three levels.** Each level is 23 tiles tall unless stated, keeps within the design limits in Global Constraints, and has a `title` and `world: placeholder` header.
  - **`p1.txt`, "First Steps"** (about 120×23): solid ground only, with `b`/`f` decoration. It has gaps of 1–3 tiles and steps up of 1–3 tiles, one checkpoint near the middle, and the exit at the far right. It teaches running, jumping and horizontal scrolling.
  - **`p2.txt`, "Up and Through"** (about 50 wide, 45 tall): a climb up a tower of one-way platforms, which exercises the vertical camera. It has a checkpoint at the top, and the exit is reached by dropping through a one-way floor onto a lower ledge, which teaches ↓ to drop.
  - **`p3.txt`, "Mind the Spikes"** (about 110×23):
    - spikes (`^`) in pit floors and on the ground between jumps
    - one 3-tile pit over the void (falling out respawns the player)
    - two checkpoints, none over a hazard
    - the exit at the far right

  Run: `.\build.cmd levels`, then `.\build.cmd preview`.
  Expected: three `.tmx` files in `resources/levels/placeholder/`, and three PNGs in `build/preview/`. Open each PNG and check the layout matches the description above.

- [ ] **Step 4: Run the suite.**
  Run: `.\build.cmd test`
  Expected: all tests pass, including `LevelFilesTest`.

- [ ] **Step 5: Commit.**
  ```bash
  git add levels-src resources/levels src/ambot/level/LevelCatalog.java test/ambot/level/LevelFilesTest.java test/ambot/tools/LevelPreview.java build.ps1
  git commit -m "feat(level): three placeholder levels, level catalog, level-file checks, preview task"
  ```

### Task 9: Playing real levels: camera, checkpoints, hazards, exit and level complete

**Files:**
- Modify: `src/ambot/states/PlayingState.java`, `test/ambot/states/ScreensTest.java`, `test/ambot/level/TileMapTest.java` (drop the `AsciiLevel` cases), `test/ambot/tools/SmokeRun.java`, `docs/ROADMAP.md`, `docs/ASSETS.md`
- Create: `src/ambot/states/LevelCompleteState.java`, `test/ambot/states/FakeContext.java`, `levels-src/placeholder/p1.route`, `p2.route`, `p3.route`
- Delete: `src/ambot/level/AsciiLevel.java`, plus `PlayingState.TEST_ROOM` and `buildTestRoom`
- Test: `test/ambot/states/PlayingStateTest.java`, `test/ambot/states/RouteTest.java`

**Interfaces:**
- Consumes: `LevelCatalog`, `Camera`, `LevelRenderer`, `PlaceholderBackgrounds.forWorld`, `TileMap.touchesHazard`, `TestLevels.ascii`.
- Produces:
  - **`PlayingState` constructors:** `PlayingState(GameContext ctx, LevelCatalog catalog, int index)`, and `PlayingState(GameContext ctx)`, which equals `(ctx, LevelCatalog.placeholder(), 0)`.
  - **`PlayingState` accessors:** `player()`, `level()`, `camera()`, `Spot respawnPoint()`, `int deaths()`.
  - **`LevelCompleteState`:**
    - `LevelCompleteState(GameContext ctx, LevelCatalog catalog, int index)`, an overlay
    - on `CONFIRM` it calls `replaceAll` with the next level's `PlayingState`, or `MainMenuState` after the last level
  - **`FakeContext`:** the `ScreensTest` fake moved to its own file. It loads the sheet lazily, so any test class can use it.

- [ ] **Step 1: Write the failing tests.**
  - In `PlayingStateTest`, use `TestLevels.ascii` levels wrapped in `LevelCatalog.of(...)`.
  - Spawning and the camera:
    - `spawnsStandingOnSpawnSpot`: after 5 idle ticks the player is `onGround()`, with feet at `spawn().y()`.
    - `cameraFollowsPlayer`: after holding RIGHT for 120 ticks in a 100-tile-wide level, `camera().x() > 0`.
  - Checkpoints and dying:
    - `touchingCheckpointMovesRespawn`: `respawnPoint()` becomes the checkpoint's bottom-centre.
    - `hazardSendsBackToRespawnPoint`: the player is back at the respawn point and `deaths()` is 1.
    - `fallingOutOfMapRespawns` (replaces M1's `respawnsAfterFallingOutOfMap`).
  - The exit:
    - `exitOpensLevelComplete`: on the next tick the top state is `LevelCompleteState`, and `PlayingState` no longer updates.
    - `confirmLoadsNextLevel`: the new top is a `PlayingState` whose `level()` is the second level.
    - `confirmAfterLastLevelReturnsToMenu`
  - In `ScreensTest`, use `FakeContext`, update `spawnsStandingOnFloor` to `Spot`, and keep the pause and focus tests as they are.
  - **`RouteTest`, the playability check (spec §5.6):**
    - It's a `@TestFactory` with one dynamic test per `levels-src/placeholder/p<N>.route`, run against catalog index `N - 1`.
    - **Route format:** each line is `<ticks> <ACTION+ACTION | ->`, and `#` starts a comment.
    - **Driving the level:** the runner turns each line into `Controls`. An action counts as pressed on the tick it appears and as released on the tick it disappears.
    - It ticks a `PlayingState` for that level until the top state is `LevelCompleteState`.
    - **Asserts:** the level completes within the route, and `deaths() == 0`.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test`
  Expected: compile errors (no `LevelCompleteState` or `LevelCatalog` constructor on `PlayingState`).

- [ ] **Step 3: Implement.**
  - **Spawning:** the player spawns at `(spot.x - HITBOX_W/2, spot.y - HITBOX_H)`. The camera snaps to the player in the constructor and on every respawn.
  - **The order inside `update`:**
    1. pause check
    2. `player.update`
    3. checkpoints: overlapping one sets the respawn point and the active checkpoint
    4. hazard (`touchesHazard`) or `y > heightPx + 64`: respawn and `deaths++`
    5. exit: overlapping it pushes `LevelCompleteState` once
    6. `camera.follow`
  - **`render`:**
    1. `background.render(g, cam.x(), max(0, heightPx - 360 - cam.y()))`
    2. translate by `(-cam.x(), -cam.y())`
    3. `drawBehind`, then `drawMarkers`, then the player, then `drawFront`
    4. translate back, then the `Esc: pause` hint
  - **`LevelCompleteState.render`:** dims the canvas with `0x80000000` and draws "LEVEL COMPLETE", the level title, and "Enter: continue", centred with `MainMenuState.drawCentered`.
  - **Deletions:** delete `AsciiLevel` and the test-room code.

- [ ] **Step 4: Write the three routes.**
  - Find each one by playing the level (`.\build.cmd run`, noting the inputs) or by trial against `RouteTest`. Each route must finish its level with no deaths.
  - **Fix routes, not levels:** a level changes only if it breaks a Global Constraints design limit.
  - Run: `.\build.cmd test`
  - Expected: all tests pass, including 3 `RouteTest` cases.

- [ ] **Step 5: Smoke run.**
  - Change `SmokeRun`: after `03-run`, hold RIGHT for 2500 ms and take `03b-scrolled`.
  - Run: `.\build.cmd smoke`
  - Expected:
    - UPS 59–61, FPS ≥ 58 and CPU < 0.5 cores.
    - In `contact.png`, `03b-scrolled` shows the camera scrolled, with the hills offset from `02-room`.

- [ ] **Step 6: Update the docs.**
  - **`docs/ROADMAP.md`:** M2 becomes **done**, with the date.
  - **`docs/ASSETS.md`:** add one row for `resources/tiles/placeholder.png` and the procedural hills: "own work, generated by `test/ambot/tools/PlaceholderTiles.java` / `PlaceholderBackgrounds`, placeholder until M4".

- [ ] **Step 7: Commit.**
  ```bash
  git add -A src test levels-src docs
  git commit -m "feat(states): play TMX levels with camera, parallax, checkpoints, hazards and exit; M2 complete"
  ```

- [ ] **Step 8: Hand off for review.**
  The user plays all three levels from New Game (`.\build.cmd run`). This is the spec's play check (§5.12), so wait for their verdict before finishing the branch.
