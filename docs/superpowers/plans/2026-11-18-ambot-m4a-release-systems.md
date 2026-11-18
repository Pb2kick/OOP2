# AMBOT M4a (v0.1 Release Systems) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Everything v0.1 needs around the levels:
- progress and settings saved to disk
- a main menu with Continue and New Game, plus World Select, Options and Credits screens
- music and sound effects with volume control
- a crash log
- a double-click Windows build

Real World 1 art, levels and Boss 1 are M4b.

**Architecture:**
- **Saving:** an `ambot.save` package keeps a `SaveData` value and a `SaveStore`, and `Progress` saves after every change.
- **Worlds:** an `ambot.level.Campaign` describes the worlds and their levels, with unlock rules derived from `SaveData`.
- **Audio:** an `ambot.audio` package hides `javax.sound.sampled` behind an `Audio` interface that falls back to silence. Gameplay sounds come from a `SoundCues` observer that compares the play state before and after each tick, so entities stay free of audio code.
- **Packaging:** `build.ps1` gains `jar` and `package` tasks; `package` uses `jpackage --type app-image` plus a zip.

**Tech Stack:** Java (`--release 21`) on JDK 26 at `C:\jdk-26`, Swing/Java2D, `javax.sound.sampled`, `java.util.Properties`, the JDK tools `jar`/`jpackage`, JUnit 5 (tests only), `build.ps1` through `build.cmd`.

**Spec:** [`docs/superpowers/specs/2026-11-17-ambot-restoration-design.md`](../specs/2026-11-17-ambot-restoration-design.md), mainly:
- §5.3 states and menus
- §5.8 audio
- §5.9 saving
- §5.10 credits
- §3 the portable zip release

The milestone map is [`docs/ROADMAP.md`](../../ROADMAP.md). This plan builds on `main` at `e6b807e` (tag `m3`).

All paths are relative to the project root `C:\Users\Pb2kick\Desktop\Project Restorations\AMBOT (OOP2)`.

## Global Constraints

- **Toolchain:** `javac --release 21` with the JDK at `C:\jdk-26`. Never use the `java` on PATH (Java 8).
- **Libraries:** none at runtime beyond the JDK. Audio uses `javax.sound.sampled` with WAV files (spec §5.8).
- **Build:** no Maven or Gradle. Every command goes through `.\build.cmd <task>`.
- **Saving** (spec §5.9):
  - Progress (unlocked levels, bosses defeated) and settings are saved automatically on level completion and on settings changes.
  - They're stored in `%APPDATA%\AMBOT\save.properties`.
  - A missing or broken file falls back to a new game without crashing.
  - Checkpoints aren't saved.
- **Main menu** (spec §5.3): Continue (when a save exists), New Game, World Select, Options, Credits, Quit.
- **World select:** a simple screen of the 3 worlds and their levels, with locked levels greyed out. It isn't an overworld map.
- **Options:** music volume, sound-effects volume, fullscreen, and a view of the controls.
- **Release** (spec §3): a portable zip made with `jpackage --type app-image`, containing a folder with `AMBOT.exe` and a bundled runtime. There's no installer.
- **World content until M4b:** World 1 is the four placeholder levels in their current order. Worlds 2 and 3 show as "coming soon".
- **Placeholder audio:** generated tones, recorded in `docs/ASSETS.md` as own work. M4b swaps in free-licensed packs.
- **Tests never touch real user files:** they use temporary folders or in-memory stores. The game honours `-Dambot.save=<path>`, and the smoke run uses it.
- **Git:** work on branch `m4a-release`. Commit as `Pb2kick` with no co-author trailers, and credit no AI tool in any file or commit message. The repo stays private.

## Review Focus

1. **A broken, old or hand-edited save file** (garbage, negative or huge volumes, unknown level ids, empty file). The game starts with defaults or clamped values and never crashes. Tested in Task 1: `brokenFileFallsBackToDefaults`, `outOfRangeValuesAreClamped`, `unknownLevelIdsAreKeptButHarmless`.
2. **Saving fails** (the folder can't be created, or a read-only file). Play continues with progress kept in memory, and no exception escapes. Tested in Task 1: `saveFailureDoesNotThrow`.
3. **No audio device, or a missing sound file.** The game runs silently and never crashes. Tested in Task 3: `missingResourceFallsBackToSilent`, `silentAudioAcceptsEverything`.
4. **New Game with existing progress.** It asks first; "No" keeps the progress. Tested in Task 5: `newGameWithProgressAsksFirst`, `answeringNoKeepsProgress`.
5. **Unlock order.** Finishing level 1 unlocks level 2 only. Completed levels can be replayed. Playing a level from World Select and quitting midway unlocks nothing. Tested in Task 2 (`completingOneLevelUnlocksOnlyTheNext`) and Task 5 (`quittingMidLevelUnlocksNothing`).

---

### Task 1: Save data, save store and progress

**Files:**
- Create: `src/ambot/save/SaveData.java`, `SaveStore.java`, `FileSaveStore.java`, `Progress.java`
- Create: `test/ambot/save/MemorySaveStore.java`
- Test: `test/ambot/save/FileSaveStoreTest.java`, `test/ambot/save/ProgressTest.java`

**Interfaces:**
- Produces:
  - **`SaveData`:** `record SaveData(Set<String> completed, int musicVolume, int sfxVolume, boolean fullscreen)`
    - `static SaveData defaults()`: empty, music 7, sfx 8, windowed
    - `withCompleted(String id)`, `withMusic(int)`, `withSfx(int)` and `withFullscreen(boolean)` return copies
    - volumes are clamped to `0..10` in the canonical constructor
    - `completed` is copied to an unmodifiable, sorted set
  - **`SaveStore`:** an interface with `SaveData load()` and `void save(SaveData d)`. `save` never throws: failures go to stderr.
  - **`FileSaveStore(Path file)`** implements `SaveStore`:
    - it stores `java.util.Properties` with the keys `version=1`, `completed` (comma-separated ids), `music`, `sfx` and `fullscreen`
    - a missing file, unreadable file or non-number gives defaults for the broken keys
    - the write goes to `<file>.tmp` and then moves over the file, creating parent folders as needed
    - **`static Path defaultFile()`:** `%APPDATA%\AMBOT\save.properties`; if `APPDATA` is unset, `<user.home>/.ambot/save.properties`. The `ambot.save` system property, when set, overrides both.
  - **`Progress(SaveStore store)`:**
    - loads once
    - `SaveData data()`
    - `boolean hasProgress()` (any completed level)
    - each of `markCompleted(String id)`, `setMusicVolume(int)`, `setSfxVolume(int)`, `setFullscreen(boolean)` and `reset()` saves at once. `reset()` clears `completed` and keeps the settings. A call that changes nothing doesn't save.
  - **`MemorySaveStore`** (tests only): holds the last saved `SaveData`, counts `saves()`, and can start from given data.

- [ ] **Step 1: Write the failing tests.**
  - In `FileSaveStoreTest`, each test uses `@TempDir`:
    - `roundTripsEveryField`.
    - `missingFileGivesDefaults`.
    - `brokenFileFallsBackToDefaults`: the file holds the bytes `\u0000garbage=`. Defaults come back, with no exception.
    - `outOfRangeValuesAreClamped`: `music=-4` and `sfx=99` load as 0 and 10.
    - `unknownLevelIdsAreKeptButHarmless`: `completed=w1-1,zz-9` round-trips both ids.
    - `saveFailureDoesNotThrow`: the store's parent "folder" is an existing file. `save` returns normally.
    - `noTempFileLeftBehind`.
    - `defaultFileHonoursSystemProperty`: set and then clear `ambot.save` in `try/finally`.
  - In `ProgressTest`, use a `MemorySaveStore`:
    - `markCompletedSavesOnce`: marking the same id twice saves once.
    - `settingsSaveImmediately`.
    - `resetKeepsSettings`.
    - `hasProgressOnlyAfterACompletion`.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test`
  Expected: compile errors (no `ambot.save` package).

- [ ] **Step 3: Implement the four classes and `MemorySaveStore`.**

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass (234 from M3 plus the new ones).

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/save test/ambot/save
  git commit -m "feat(save): save data in %APPDATA%, tolerant of broken files and failed writes"
  ```

### Task 2: Campaign, worlds and level ids

**Files:**
- Create: `src/ambot/level/Campaign.java`, `src/ambot/level/World.java`, `src/ambot/level/LevelRef.java`
- Modify: `src/ambot/level/LevelCatalog.java`
- Test: `test/ambot/level/CampaignTest.java`

**Interfaces:**
- Consumes: `SaveData` (Task 1), `LevelCatalog`.
- Produces:
  - **`LevelRef`:** `record LevelRef(String id, String title, String path)`.
  - **`World`:** `record World(int number, String name, List<LevelRef> levels, boolean available)`.
  - **`Campaign`:**
    - `static Campaign standard()`:
      - World 1 is "World 1" with `w1-1`…`w1-4` mapped to `PLACEHOLDER_PATHS` in order, with titles First Steps, Up and Through, Mind the Spikes and First Blood
      - World 2 and World 3 have no levels and `available = false`
    - `List<World> worlds()`, `World world(int number)`
    - `boolean isUnlocked(World w, int levelIndex, SaveData d)`: true if the world is available and either it's level 0 of World 1, it's level 0 of a world whose previous world's last level is completed, or the previous level in the same world is completed
    - `Optional<int[]> nextUnfinished(SaveData d)`: `{worldNumber, levelIndex}` of the first unlocked level that isn't completed; empty when everything available is done
    - `LevelCatalog catalog(World w)`: a catalog of the world's levels with their ids
    - `Optional<World> worldOf(LevelCatalog c)`: the world whose level ids match the catalog's ids
  - **`LevelCatalog`:**
    - `String id(int index)`
    - `static LevelCatalog files(List<String> paths, List<String> ids)`; the old `files(paths)` uses each file name without `.tmx` as its id
    - `of(Level...)` gives ids `"level-1"`, `"level-2"`, …

- [ ] **Step 1: Write the failing tests** in `CampaignTest`.
  - The worlds:
    - `standardHasThreeWorldsWithWorldOnePlayable`: 3 worlds, World 1 has 4 levels, Worlds 2 and 3 are unavailable.
  - Unlocking:
    - `onlyFirstLevelUnlockedOnANewGame`.
    - `completingOneLevelUnlocksOnlyTheNext`: completing `w1-1` unlocks index 1, and index 2 stays locked.
    - `completedLevelsStayPlayable`.
    - `unavailableWorldNeverUnlocks`, even when every World 1 level is completed.
  - Continue:
    - `nextUnfinishedFollowsProgress`: new game → `{1,0}`, after `w1-1` → `{1,1}`, after all four → empty.
  - Ids:
    - `catalogCarriesIds`: `catalog(world 1).id(2)` is `"w1-3"`, and `worldOf` that catalog is World 1.
    - `ofCatalogIds`: `LevelCatalog.of(a, b).id(1)` is `"level-2"`, and `worldOf` it is empty.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.level.CampaignTest`
  Expected: compile errors.

- [ ] **Step 3: Implement.**

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/level test/ambot/level/CampaignTest.java
  git commit -m "feat(level): campaign of worlds with unlock rules and level ids"
  ```

### Task 3: Audio engine and placeholder sounds

**Files:**
- Create: `src/ambot/audio/Audio.java`, `Sfx.java`, `Music.java`, `SampledAudio.java`, `SilentAudio.java`
- Create: `test/ambot/tools/PlaceholderAudio.java`, `test/ambot/audio/RecordingAudio.java`, and the generated `resources/audio/sfx/*.wav` and `resources/audio/music/*.wav`
- Modify: `build.ps1` (the `levels` task also runs `PlaceholderAudio`)
- Test: `test/ambot/audio/AudioTest.java`

**Interfaces:**
- Produces:
  - **`Sfx`:** an enum `JUMP, SWING, HIT, HURT, PICKUP, SHOOT, DEATH, LEVEL_CLEAR, MENU_MOVE, MENU_SELECT`, with `String path()` = `"/audio/sfx/" + name().toLowerCase() + ".wav"`.
  - **`Music`:** an enum `MENU, WORLD1`, with `path()` = `"/audio/music/" + name().toLowerCase() + ".wav"`.
  - **`Audio`:** an interface with `play(Sfx)`, `playMusic(Music)` (a no-op when that track is already playing), `stopMusic()`, `setMusicVolume(int 0..10)` and `setSfxVolume(int 0..10)`.
  - **`SilentAudio`:** does nothing.
  - **`SampledAudio`:**
    - **Creation:** `static Audio createOrSilent()` reads from the classpath, and the package-private `static Audio createOrSilentFrom(Function<String, InputStream> resources)` takes the resource source for tests. Each preloads every `Sfx` (3 `Clip`s each, round-robin) and opens `Music` lazily. Any `LineUnavailableException`, missing resource or unsupported format prints one warning and returns a `SilentAudio`.
    - **`static float gainDb(int level)`:** level 0 → `-80f`, 10 → `0f`, otherwise `20 * log10(level / 10.0)`.
  - **`RecordingAudio`** (tests only): records `played`, `musicNow`, and the volumes.
  - **`PlaceholderAudio`** (tool):
    - `static byte[] render(Sfx s)` and `static byte[] render(Music m)` produce complete, deterministic 16-bit mono 22050 Hz WAV files
    - sound effects: short square or sine blips under 0.4 s, each `Sfx` with its own pitch and envelope
    - music: two loops of about 8 s (MENU calm, WORLD1 upbeat) built from a fixed note list
    - `main(resourcesDir)` writes them all

- [ ] **Step 1: Write the failing tests** in `AudioTest`.
  - Volume:
    - `gainCurve`: 0 → -80, 10 → 0, 5 → about -6.02 (±0.01).
  - Fallbacks:
    - `missingResourceFallsBackToSilent`: `SampledAudio.createOrSilentFrom(name -> null)` is an instance of `SilentAudio`.
    - `silentAudioAcceptsEverything`: every method, with every enum value, doesn't throw.
  - The generated files:
    - `placeholderFilesDecode`: every `Sfx` and `Music` path exists on the classpath and opens with `AudioSystem.getAudioInputStream`. No playback, so this runs without a sound device.
    - `placeholderAudioIsCurrent`: for every enum value, `PlaceholderAudio.render(..)` equals the committed file's bytes.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.audio.AudioTest`
  Expected: compile errors.

- [ ] **Step 3: Implement, then generate the files** with `.\build.cmd levels` (which now also writes the audio).
  Expected: 10 `.wav` files in `resources/audio/sfx`, 2 in `resources/audio/music`, about 0.8 MB in total.

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/audio test/ambot/audio test/ambot/tools/PlaceholderAudio.java resources/audio build.ps1
  git commit -m "feat(audio): sampled audio with silent fallback, placeholder sounds and music"
  ```

### Task 4: Game context wiring, sound cues and saving completed levels

**Files:**
- Create: `src/ambot/states/SoundCues.java`
- Modify: `src/ambot/states/GameContext.java`, `test/ambot/states/FakeContext.java`, `src/ambot/core/Game.java`, `src/ambot/core/GameWindow.java`, `src/ambot/states/PlayingState.java`, `src/ambot/ui/Menu.java`, `test/ambot/tools/SmokeRun.java`
- Test: `test/ambot/states/SoundCuesTest.java`, `test/ambot/states/PlayingStateTest.java`

**Interfaces:**
- Consumes: `Progress` (Task 1), `Campaign` and `LevelCatalog.id` (Task 2), `Audio` and `Sfx` (Task 3).
- Produces:
  - **`GameContext`** gains `Progress progress()`, `Audio audio()`, `Campaign campaign()` and `void setFullscreen(boolean on)`.
  - **`FakeContext`:** a `MemorySaveStore`-backed `Progress`, a `RecordingAudio`, `Campaign.standard()`, and a `fullscreen` field.
  - **`GameWindow`:** `boolean isFullscreen()`.
  - **`Game`:**
    - builds `Progress` from `FileSaveStore(FileSaveStore.defaultFile())` and `SampledAudio.createOrSilent()`
    - applies the saved volumes, and saved fullscreen after the window opens
    - F11 also calls `progress().setFullscreen(window.isFullscreen())`
    - `setFullscreen(on)` toggles only when it differs, on the event thread
  - **`SoundCues`:** `static void observe(Snapshot before, Snapshot after, Audio audio)`, with `record Snapshot(int health, int swingId, boolean airborneUp, int enemyHealthTotal, int pickups, int projectiles, boolean dead)` and `static Snapshot of(PlayingState p)`.
    - JUMP: `airborneUp` turned true while health didn't drop
    - SWING: `swingId` went up
    - HURT: health dropped and the player isn't dead
    - DEATH: `dead` turned true
    - HIT: the enemy health total dropped
    - PICKUP: the pickups count dropped and health rose
    - SHOOT: the projectile count rose
  - **`PlayingState`:**
    - `update` takes a snapshot before and after the actor step and calls `SoundCues.observe`
    - `onEnter` plays `Music.WORLD1`
    - reaching the exit calls `ctx.progress().markCompleted(catalog.id(index))` and plays `LEVEL_CLEAR`, before pushing `LevelCompleteState`
  - **`Menu`:** `void update(Controls c, Audio audio)` plays MENU_MOVE when the selection changes and MENU_SELECT on confirm. The old `update(Controls)` stays.

- [ ] **Step 1: Write the failing tests.**
  - In `SoundCuesTest`, each test feeds two hand-made snapshots and checks `RecordingAudio.played`:
    - `jumpCue`
    - `swingCue`
    - `hurtCue`
    - `deathCueNotHurt`: health dropping to 0 with `dead` true gives DEATH only
    - `hitCue`
    - `pickupCue`
    - `shootCue`
    - `quietTickPlaysNothing`
  - In `PlayingStateTest`:
    - `reachingExitSavesCompletion`: `ctx.progress().data().completed()` contains `"level-1"`.
    - `enteringPlaysWorldMusic`.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test`
  Expected: compile errors.

- [ ] **Step 3: Implement.** `SmokeRun` sets `System.setProperty("ambot.save", "build/smoke/save.properties")` before creating `Game`.

- [ ] **Step 4: Run the whole suite and the smoke run.**
  - Run: `.\build.cmd test`. Expected: all pass, including the 4 `RouteTest` routes.
  - Run: `.\build.cmd smoke`. Expected: UPS 59–61, `build/smoke/save.properties` exists, and `%APPDATA%\AMBOT` is not created or changed.

- [ ] **Step 5: Commit.**
  ```bash
  git add src test
  git commit -m "feat(core): progress, audio and campaign in the game context; sound cues; completed levels saved"
  ```

### Task 5: Main menu, World Select, confirmation and World Clear

**Files:**
- Create: `src/ambot/states/WorldSelectState.java`, `ConfirmState.java`, `WorldClearState.java`
- Modify: `src/ambot/states/MainMenuState.java`, `src/ambot/states/LevelCompleteState.java`, `src/ambot/core/GameConfig.java`, `test/ambot/states/ScreensTest.java`
- Test: `test/ambot/states/MenuFlowTest.java`

**Interfaces:**
- Consumes: `GameContext.progress()`, `campaign()` and `audio()` (Task 4), `Campaign.worldOf` and `nextUnfinished` (Task 2).
- Produces:
  - **`GameConfig.VERSION = "0.1"`.**
  - **`MainMenuState`:**
    - items: "Continue" (enabled iff `progress.hasProgress()` and `nextUnfinished` is present), "New Game", "World Select", "Options" (still disabled until Task 6), "Credits", "Quit"
    - plays `Music.MENU` on enter and uses `menu.update(c, ctx.audio())`
    - draws `"v" + VERSION` at the bottom-right in `0x83769C`
    - **New Game:** with progress, it pushes `ConfirmState`; without progress, it plays w1-1 straight away
  - **`ConfirmState(GameContext ctx, String question, Runnable yes)`:** an overlay. Its menu is "No" (the default, which pops) and "Yes" (which runs `yes`). BACK equals No, and it has `InputGrace`. New Game's question is "Start a new game? Your progress will be erased."; its `yes` calls `progress.reset()` and starts w1-1.
  - **`WorldSelectState`:**
    - **Rows:** for each world, a disabled header row (`"World N"`, plus `" - coming soon"` when unavailable). Under each available world come its levels, as `"  N-k  Title"` with a `" *"` suffix when completed; a locked level is a disabled row. Then "Back".
    - **Choosing a level** does `replaceAll(new PlayingState(ctx, campaign.catalog(world), k))`.
    - BACK or "Back" pops.
  - **`WorldClearState(GameContext ctx, World w)`:** shows "WORLD N CLEAR" and "Enter: continue". After `InputGrace`, CONFIRM does `replaceAll(new MainMenuState(ctx))` and then pushes `WorldSelectState`.
  - **`LevelCompleteState`:** after the last level, `campaign.worldOf(catalog)` present → `WorldClearState`; otherwise `MainMenuState` as before.

- [ ] **Step 1: Write the failing tests** in `MenuFlowTest`, and update `ScreensTest` for the new menu order.
  - Continue and New Game:
    - `continueDisabledWithoutProgress`: the first selected item is "New Game" (index 1).
    - `continueResumesNextUnfinished`: with `w1-1` completed, CONFIRM on Continue gives a `PlayingState` whose `level().title()` is "Up and Through".
    - `newGameWithProgressAsksFirst`: `ConfirmState` is on top.
    - `answeringNoKeepsProgress`.
    - `answeringYesResetsAndStarts`.
  - World Select:
    - `worldSelectLocksLaterLevels`: on a new game only `1-1` is enabled among the level rows.
    - `quittingMidLevelUnlocksNothing`: start `1-1` from World Select, pause, choose Main Menu. `completed` stays empty.
  - Finishing a world:
    - `lastLevelOfWorldOpensWorldClear`: with a campaign catalog positioned at its last level, reaching the exit and confirming after the grace gives `WorldClearState`.
  - In `ScreensTest`, `quitSkipsDisabledOptions` becomes `quitIsLast`: UP from New Game wraps past the disabled Continue to Quit, then CONFIRM quits. `creditsBackReturnsToMenu` uses the DOWN presses that reach Credits.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test`
  Expected: compile errors.

- [ ] **Step 3: Implement.**

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src test
  git commit -m "feat(states): main menu with Continue/New Game, World Select, confirmation, World Clear"
  ```

### Task 6: Options and Controls screens

**Files:**
- Create: `src/ambot/states/OptionsState.java`, `src/ambot/states/ControlsState.java`
- Modify: `src/ambot/states/MainMenuState.java` (enables Options)
- Test: `test/ambot/states/OptionsStateTest.java`

**Interfaces:**
- Consumes: `progress()`, `audio()` and `setFullscreen` (Task 4).
- Produces:
  - **`OptionsState`:**
    - **Rows:** "Music  [##########]" (filled by volume), "Sound  [...]", "Fullscreen: On/Off", "Controls", "Back".
    - **Keys:** UP/DOWN select with wrapping. LEFT/RIGHT change a volume by 1 within `0..10` or toggle fullscreen. CONFIRM toggles fullscreen, opens Controls, or goes back. BACK pops.
    - **Applying changes:** each change calls the matching `Progress` setter (which saves) and applies it at once (`audio.setMusicVolume`, `ctx.setFullscreen`). Changing the sound volume also plays `Sfx.MENU_MOVE` as a sample.
  - **`ControlsState`:** lists the spec §5.5 controls, one per line ("Move  A / D or Left / Right", …, "Fullscreen  F11"). BACK or CONFIRM pops.

- [ ] **Step 1: Write the failing tests** in `OptionsStateTest`.
  - Volume:
    - `rightRaisesMusicAndSaves`: 7 → 8, saved, and `RecordingAudio` music volume is 8.
    - `volumeClampsAtBounds`: 15×RIGHT gives 10, and 15×LEFT on Sound gives 0.
  - Fullscreen:
    - `fullscreenToggles`: CONFIRM on the Fullscreen row sets `FakeContext.fullscreen` and the saved flag.
  - Navigation:
    - `controlsOpensAndCloses`.
    - `backReturnsToMenu`.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.states.OptionsStateTest`
  Expected: compile errors.

- [ ] **Step 3: Implement, and enable "Options" in the main menu.**

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/states test/ambot/states
  git commit -m "feat(states): options (volumes, fullscreen) and controls screens"
  ```

### Task 7: Credits from a resource file

**Files:**
- Create: `resources/credits.txt`
- Modify: `src/ambot/states/CreditsState.java`, `docs/ASSETS.md` (a placeholder-audio row)
- Test: `test/ambot/states/CreditsTest.java`

**Interfaces:**
- Produces:
  - **`CreditsState`:** reads `/credits.txt` (UTF-8) once; it shows "credits missing" if the file can't be read.
    - **Scrolling:** it scrolls upward at 0.5 px per tick when the lines don't fit, and loops.
    - **Keys:** UP/DOWN scroll by hand, and BACK or CONFIRM pops.
    - `static List<String> lines()` exposes the text for tests.
  - **`credits.txt` sections:**
    - "AMBOT v0.1"
    - "An OOP2 group project" with "Original team: names coming soon"
    - "Restoration: Pb2kick"
    - "Art", with one line per `docs/ASSETS.md` row's pack and author
    - "Sound"
    - "Made with Java and Swing"

- [ ] **Step 1: Write the failing tests** in `CreditsTest`.
  - `creditsListEveryAssetAuthor`: parse every data row of `docs/ASSETS.md`'s first table (skipping the header and the `|---|` separator) and check that every Author cell other than "own work (restoration)" appears in `CreditsState.lines()`.
  - `creditsMentionVersion`: a line contains `GameConfig.VERSION`.
  - `backPops`.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.states.CreditsTest`
  Expected: compile errors (no `lines()`).

- [ ] **Step 3: Implement, and add the placeholder-audio row to `ASSETS.md`** (own work, generated by `test/ambot/tools/PlaceholderAudio.java`, placeholder until M4b).

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add resources/credits.txt src/ambot/states/CreditsState.java docs/ASSETS.md test/ambot/states/CreditsTest.java
  git commit -m "feat(states): credits read from credits.txt and checked against ASSETS.md"
  ```

### Task 8: Crash log, jar and Windows package, README

**Files:**
- Create: `src/ambot/core/CrashLog.java`, `README.md`
- Modify: `src/ambot/core/Game.java`, `src/ambot/states/CrashState.java`, `build.ps1`, `docs/ROADMAP.md`
- Test: `test/ambot/core/CrashLogTest.java`, `test/ambot/states/CrashStateTest.java`

**Interfaces:**
- Produces:
  - **`CrashLog`:**
    - `static Path write(Path dir, Throwable e)` appends a timestamped stack trace to `dir/crash.log` and returns the path, or null if writing fails (it never throws)
    - `static Path defaultDir()` is the folder of `FileSaveStore.defaultFile()`
  - **`Game`'s crash handler** also writes the crash log, and passes the path to `CrashState`.
  - **`CrashState`:**
    - `CrashState(GameContext ctx, RuntimeException e, Path log)` adds a line "Details saved to <path>" when it's non-null
    - it gains an `InputGrace` (fixing the M3 deferred minor)
    - the old 2-argument constructor passes null
    - package-private `List<String> detailLines()` returns the lines it draws
  - **`build.ps1`:**
    - `jar` compiles, then runs `jar --create --file build\AMBOT.jar --main-class ambot.Main -C build\classes .`
    - `package` builds the jar, then copies it alone into `build\jpackage-input`
    - it then runs `jpackage --type app-image --name AMBOT --app-version 0.1 --input build\jpackage-input --main-jar AMBOT.jar --main-class ambot.Main --dest build\dist --add-modules java.desktop,java.xml --jlink-options "--strip-debug --no-header-files --no-man-pages"`
    - finally it zips `build\dist\AMBOT` into `build\AMBOT-v0.1-windows.zip`
    - it removes the old `build\dist` and the zip first, and adds both tasks to `ValidateSet` and the usage comment

- [ ] **Step 1: Write the failing tests.**
  - `CrashLogTest.writesStackTrace`: the file contains the exception message and "at ".
  - `CrashLogTest.unwritableDirReturnsNull`: the directory is an existing file.
  - `CrashStateTest.confirmIgnoredDuringGrace`.
  - `CrashStateTest.showsLogPath`: `detailLines()` contains the log's file name.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test`
  Expected: compile errors.

- [ ] **Step 3: Implement `CrashLog`, the `CrashState` and `Game` changes, and the build tasks.** Write `README.md` covering:
  - what AMBOT is, and that it's a restoration of an OOP2 group project
  - how to run it: `.\build.cmd run`
  - how to test: `.\build.cmd test`
  - how to package it: `.\build.cmd package`, with output in `build\AMBOT-v0.1-windows.zip`
  - the controls table
  - links to `docs/ROADMAP.md`, `docs/LEVELS.md` and `docs/ASSETS.md`
  - a note that the repo is private because of the sprite license

  Update `ROADMAP.md`: M4 is split into M4a (this plan, done) and M4b (World 1 content: art packs, 3 levels, Boss 1, music; to write).

- [ ] **Step 4: Verify.**
  - Run: `.\build.cmd test`. Expected: all pass.
  - Run: `.\build.cmd package`. Expected: `build\dist\AMBOT\AMBOT.exe` exists, and `build\AMBOT-v0.1-windows.zip` is under 80 MB.
  - Start `build\dist\AMBOT\AMBOT.exe` with `$env:JDK_JAVA_OPTIONS = "-Dambot.save=<temp>\save.properties"`, for this check only. Expected: after 5 s the process is still running, then it's stopped. No crash log appears in the temporary folder.
  - Run: `.\build.cmd smoke`. Expected: UPS 59–61, FPS ≥ 58, CPU < 0.5 cores.

- [ ] **Step 5: Commit.**
  ```bash
  git add src test build.ps1 README.md docs/ROADMAP.md
  git commit -m "feat(release): crash log, jar and portable Windows package; README; M4a complete"
  ```

- [ ] **Step 6: Hand off.** Tell the user the zip's path and how to try it (unzip, double-click `AMBOT.exe`).
