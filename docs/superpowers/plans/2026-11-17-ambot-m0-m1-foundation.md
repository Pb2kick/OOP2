# AMBOT M0–M1 (Restored Prototype + Engine Core) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Put AMBOT under its own git repo with a script build. Fix the six prototype bugs and tag the result `v0.0`. Then replace the prototype with a new engine core: a scaled pixel canvas, a sleeping 60 UPS loop, buffered input, a state stack with menus, a data-driven animation system, solid-tile collision, and a player who runs, jumps, crouches and attacks in a test room.

**Architecture:** Plain Java and Swing, with no runtime dependencies. One game-loop thread updates the game and renders into an off-screen 640×360 image. The Swing event thread only collects input and copies the finished frame to the window. Game logic (fixed timestep, input, states, menus, animation, collision, player physics) lives in plain classes, so JUnit tests can run it without a window.

**Tech Stack:** Java (`--release 21`) on JDK 26 at `C:\jdk-26`, Swing/Java2D, JUnit 5 (console standalone 1.11.4, tests only), and the PowerShell build script `build.ps1` with its `build.cmd` wrapper. No Maven or Gradle.

**Spec:** [`docs/superpowers/specs/2026-11-17-ambot-restoration-design.md`](../specs/2026-11-17-ambot-restoration-design.md). The current state is in [`docs/ANALYSIS.md`](../../ANALYSIS.md) and the milestone map in [`docs/ROADMAP.md`](../../ROADMAP.md). This plan covers milestones M0 and M1 only. M2–M6 get their own plans.

All paths are relative to the project root `C:\Users\Pb2kick\Desktop\Project Restorations\AMBOT (OOP2)`.

## Global Constraints

- Compile with `javac --release 21`. The JDK is `C:\jdk-26`, which the `AMBOT_JDK` environment variable can override. The `java` on PATH is Java 8: never use it.
- No runtime libraries beyond the JDK. `lib/junit-platform-console-standalone-1.11.4.jar` is for tests only. It's git-ignored and never shipped.
- No Maven or Gradle. The Eclipse layout (`src/`, `resources/`, `test/`) must keep compiling and running in Eclipse.
- Every build command goes through `.\build.cmd <task>`. It runs PowerShell with `-ExecutionPolicy Bypass` for that one call. Never change the system execution policy.
- Logical canvas 640×360 at whole-number scale, default 2×. Tiles 16×16. 60 updates per second (M1 onward).
- Player art is the team's `Betteradventurer-Sheet.png`, with 50×37 cells. Never replace or redraw it, apart from removing the one stray pixel in cell (8,1) in Task 8.
- New code goes in lowercase packages under `ambot.`. The window title is exactly `AMBOT`.
- Default controls (spec §5.5): A/D or ←/→ move. Space, W or ↑ jump. S or ↓ crouch. J or left mouse attack. Esc pause/back. Enter or Space confirm. F11 fullscreen. F3 debug overlay.
- Git: the repo lives in the project folder itself (nested inside the home-folder repo, which must never receive these commits). Commit with the existing identity `Pb2kick` and no co-author trailers.

## Review Focus

1. **Alt-tab mid-run or mid-jump.** Every held input is released and the game pauses. No key is "stuck" on return. Tested in Task 6 (`releaseAllClearsHeldActions`) and Task 11 (`focusLostPushesPause`).
2. **Unusual window or screen sizes** (1366×768, or a window smaller than 640×360). The canvas scales by a whole number ≥ 1, is centred, and nothing throws. Tested in Task 5 (`fits1366x768Centered`, `tinyWindowKeepsScaleOne`).
3. **A long stall** (debugger pause, slow first frame). No burst of fast-forwarded updates afterward. Tested in Task 5 (`capsCatchUpAndDropsBacklog`).
4. **A key tapped faster than one tick, and key auto-repeat.** The tap still registers, and auto-repeat doesn't re-trigger an attack. Tested in Task 6 (`tapBetweenPollsIsNotLost`, `autoRepeatDoesNotRetrigger`) and Task 10 (`tapJumpStillLeavesGround`).
5. **The player at the map edge or falling into a pit.** No out-of-bounds crash, side edges act as walls, and falling out of the map respawns the player. Tested in Task 9 (`outOfBoundsRules`) and Task 11 (`respawnsAfterFallingOutOfMap`).

---

# Milestone M0 — Restored prototype (`v0.0`)

### Task 1: Own git repo with the untouched original

**Files:**
- Create: `.gitignore`, `docs/ASSETS.md`
- Commit (unchanged): `src/**`, `resources/**`, `.classpath`, `.project`, `.settings/**`, `docs/**`

**Interfaces:** Produces: a git repo at the project root with branch `main`.

- [ ] **Step 1: Check git identity and that no repo exists yet.**
  Run: `git -C . rev-parse --show-toplevel` (from the project root).
  Expected: prints `C:/Users/Pb2kick`, the home repo. That's why a nested repo is needed. Then run `git config user.name` and expect `Pb2kick`.

- [ ] **Step 2: Initialise the repo and add `.gitignore`.**
  Run `git init -b main`. Create `.gitignore` with:
  ```
  bin/
  build/
  lib/*.jar
  ```

- [ ] **Step 3: Commit the original prototype exactly as found** (docs excluded).
  ```bash
  git add .gitignore .classpath .project .settings src resources
  git commit -m "Original OOP2 prototype (March 2025), as found"
  ```
  Expected: `git show --stat HEAD` lists 9 `.java` files, 4 PNGs and the 4 Eclipse files.

- [ ] **Step 4: Start `docs/ASSETS.md`.**
  Add a table with columns `File | Pack | Author | URL | License | Notes` and one row for `resources/Betteradventurer-Sheet.png`. Open the author rvros's itch.io page for the Adventurer sprite and copy the exact pack title, URL and license wording from it. If the page states no license, write `not stated on page (free download)`. Note that the file is the team's re-gridded copy of `adventurer-Sheet.png`.

- [ ] **Step 5: Commit the docs.**
  ```bash
  git add docs
  git commit -m "docs: analysis, restoration design, roadmap, M0-M1 plan, asset credits"
  ```

- [ ] **Step 6: Ask the user whether to add a private GitHub remote** (spec §3). This is user-gated. Only on a yes, run `gh repo create Pb2kick/AMBOT --private --source . --push`. Otherwise skip.

### Task 2: Build script, JUnit and the first test

**Files:**
- Create: `build.ps1`, `build.cmd`, `test/Entities/PlayerFramesTest.java`
- Modify: `.classpath` (add the `test` source folder and the JUnit jar)

**Interfaces:**
- Produces: `.\build.cmd <task> [-Select <fully.qualified.TestClass>]` with these tasks:
  - `deps`: get the JUnit jar
  - `compile`: build into `build/classes`
  - `test`: build into `build/test-classes` and run
  - `run`: start the game
  - `clean`: delete `build/`
- The variable `$MainClass` in `build.ps1` (value `main.MainClass` for now).

- [ ] **Step 1: Write `build.cmd`**, exactly:
  ```bat
  @powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1" %*
  ```

- [ ] **Step 2: Write `build.ps1`** (must run on Windows PowerShell 5.1, so no `&&` and no ternary). Behaviour:
  - Start with `param([ValidateSet('deps','compile','test','run','clean')][string]$Task = 'run', [string]$Select)` and `$ErrorActionPreference = 'Stop'`.
  - Set `$Jdk` to `$env:AMBOT_JDK`, or `C:\jdk-26` if that's empty. Fail with a clear message if `$Jdk\bin\javac.exe` is missing.
  - **deps:** if `lib\junit-platform-console-standalone-1.11.4.jar` is missing, download it and its `.sha1` from `https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.4/`. Verify the SHA-1 with `Get-FileHash -Algorithm SHA1`, and delete the jar and fail if it doesn't match.
  - **compile:** write every `src\**\*.java` path to `build\sources.txt` and run `javac --release 21 -encoding UTF-8 -Xlint:all -d build\classes "@build\sources.txt"`. Then copy `resources\*` recursively into `build\classes`. Exit with javac's exit code if it fails.
  - **test:** run `deps` and `compile`. Compile `test\**\*.java` with `-cp "build\classes;<junit jar>"` into `build\test-classes`. Run:
    ```
    java -jar <junit jar> execute --disable-banner --details=tree --class-path "build\classes;build\test-classes"
    ```
    followed by `--select-class $Select` when `-Select` is given, otherwise `--scan-class-path`. Exit with JUnit's exit code.
  - **run:** run `compile`, then `java -cp build\classes $MainClass`.
  - **clean:** delete `build\`.

- [ ] **Step 3: Get the JUnit jar.** This downloads about 2.6 MB from Maven Central, so get the user's go-ahead first.
  Run: `.\build.cmd deps`
  Expected: `lib\junit-platform-console-standalone-1.11.4.jar` exists and the hash check passes.

- [ ] **Step 4: Write the first test, `test/Entities/PlayerFramesTest.java`**, against the prototype's current API:
  ```java
  package Entities;
  // imports: org.junit.jupiter.api.*, static Assertions.*, java.awt.image.BufferedImage, static Utils.Constants.PlayerConstats.*
  class PlayerFramesTest {
      static boolean hasPixels(BufferedImage img) { /* any pixel with alpha > 20 */ }

      @Test void idleFramesHavePixels() {
          Player p = new Player(0, 0);
          for (int i = 0; i < GetSpriteAmount(IDLE); i++) assertTrue(hasPixels(p.frameFor(IDLE, i)), "idle " + i);
      }
  }
  ```

- [ ] **Step 5: Run it and confirm it fails.**
  Run: `.\build.cmd test -Select Entities.PlayerFramesTest`
  Expected: FAIL, with a compile error `cannot find symbol ... frameFor`.

- [ ] **Step 6: Add `BufferedImage frameFor(int action, int index)` to `src/Entities/Player.java`** (package-private).
  It returns exactly the image `render` currently draws for that action and index. Change `render` to call it.

- [ ] **Step 7: Run the test and confirm it passes.**
  Run: `.\build.cmd test -Select Entities.PlayerFramesTest`
  Expected: `1 tests successful`.

- [ ] **Step 8: Update `.classpath` for Eclipse.**
  Add `<classpathentry kind="src" path="test"/>` and `<classpathentry kind="lib" path="lib/junit-platform-console-standalone-1.11.4.jar"/>`.

- [ ] **Step 9: Commit.**
  ```bash
  git add build.ps1 build.cmd .classpath test
  git commit -m "build: add build.ps1/build.cmd script build with JUnit console runner"
  ```

### Task 3: Fix crouch, sheet columns and aspect ratio (analysis bugs 1–3)

**Files:**
- Modify: `src/Entities/Player.java`, `src/Utils/Constants.java`
- Test: `test/Entities/PlayerFramesTest.java`

**Interfaces:**
- Produces: `Constants.PlayerConstats.CROUCH = 9` (a sheet row) with `GetSpriteAmount(CROUCH) == 4`.
- Produces: `Player.DRAW_WIDTH = 150` and `Player.DRAW_HEIGHT = 111` (public static final).

- [ ] **Step 1: Add the failing tests:**
  ```java
  @Test void everyFrameOfEveryUsedAnimationHasPixels() {
      Player p = new Player(0, 0);
      for (int action : new int[]{IDLE, RUNNING, ATTACK, CROUCH})
          for (int i = 0; i < GetSpriteAmount(action); i++)
              assertTrue(hasPixels(p.frameFor(action, i)), action + ":" + i);
  }
  @Test void allElevenColumnsAreLoaded() { assertTrue(hasPixels(new Player(0, 0).frameFor(2, 9))); }
  @Test void drawSizeKeepsSpriteAspect() { assertEquals(Player.DRAW_WIDTH * 37, Player.DRAW_HEIGHT * 50); }
  ```

- [ ] **Step 2: Run them and confirm they fail.**
  Run: `.\build.cmd test -Select Entities.PlayerFramesTest`
  Expected: FAIL. The crouch frames are blank, (2,9) is out of bounds, and `DRAW_WIDTH` doesn't exist.

- [ ] **Step 3: Implement.**
  - Set `CROUCH = 9`, and add `case CROUCH: return 4;` to `GetSpriteAmount`.
  - Delete `crouchFrames`, `WepEquipFrames` and the crouch special cases in `render` and `updateAnims`. Crouch is now an ordinary row.
  - Size `anims` from the image: `new BufferedImage[img.getHeight() / 37][img.getWidth() / 50]`.
  - Draw at `DRAW_WIDTH` × `DRAW_HEIGHT` (150×111, which is 3×).

- [ ] **Step 4: Run the tests and confirm they pass.**
  Run: `.\build.cmd test`
  Expected: `4 tests successful`.

- [ ] **Step 5: Commit.**
  ```bash
  git add src test
  git commit -m "fix: crouch uses reworked sheet row 9, load all 11 columns, draw at true 3x aspect"
  ```

### Task 4: Fix facing, the CPU-hungry loop and window placement (bugs 4–6), then tag `v0.0`

**Files:**
- Modify: `src/Entities/Player.java`, `src/main/Game.java`, `src/main/GameWindow.java`
- Test: `test/Entities/PlayerFramesTest.java`

**Interfaces:** Produces: `boolean Player.isFacingLeft()`.

- [ ] **Step 1: Add the failing test:**
  ```java
  @Test void facingFollowsLastHorizontalDirection() {
      Player p = new Player(0, 0);
      p.setLeft(true); p.update(); assertTrue(p.isFacingLeft());
      p.setLeft(false); p.setRight(true); p.update(); assertFalse(p.isFacingLeft());
      p.setRight(false); p.update(); assertFalse(p.isFacingLeft()); // keeps facing when stopped
  }
  ```

- [ ] **Step 2: Run it and confirm it fails.** `.\build.cmd test -Select Entities.PlayerFramesTest` should FAIL with `cannot find symbol isFacingLeft`.

- [ ] **Step 3: Implement.**
  - **Facing:** in `updatePos`, set `facingLeft = true` on left-only movement and `false` on right-only. When facing left, `render` draws the frame mirrored: `g.drawImage(img, x + DRAW_WIDTH, y, -DRAW_WIDTH, DRAW_HEIGHT, null)`.
  - **Loop:** in `Game.run`, after handling updates and frames, compute the nanoseconds until the next update or the next frame, whichever is sooner. If that's more than 2 ms, `Thread.sleep` for it minus 1 ms. Otherwise `Thread.onSpinWait()`.
  - **Window:** in `GameWindow`, call `jframe.setTitle("AMBOT")`, and move `setLocationRelativeTo(null)` to after `pack()`.

- [ ] **Step 4: Run the tests.** `.\build.cmd test` should report `5 tests successful`.

- [ ] **Step 5: Measure CPU and update rate.** Build with `.\build.cmd compile`, then run in PowerShell:
  ```powershell
  $p = Start-Process -PassThru -RedirectStandardOutput build\fps.txt C:\jdk-26\bin\java.exe '-cp','build\classes','main.MainClass'
  Start-Sleep 3; $t0 = $p.TotalProcessorTime; Start-Sleep 5; $p.Refresh()
  "cores: " + (($p.TotalProcessorTime - $t0).TotalSeconds / 5); Stop-Process $p -Force; Get-Content build\fps.txt -Tail 3
  ```
  Expected:
  - `cores` below 0.5 (it was 1.08).
  - The last FPS lines read `FPS: 118–122 | UPS :190–205`.
  - The window opens centred, titled `AMBOT`.

  If UPS drops below 190, sleep for the remaining time minus 2 ms instead, and measure again.

- [ ] **Step 6: Check visually.** Run `.\build.cmd run`, then:
  - Hold Ctrl: the crouch animates with no blank frames.
  - Hold A: the character faces left.
  - The character isn't stretched.

  Close the window.

- [ ] **Step 7: Commit and tag.**
  ```bash
  git add src test
  git commit -m "fix: facing flip, sleeping game loop, centred titled window"
  git tag -a v0.0 -m "Restored prototype: original OOP2 code with the six analysis bugs fixed"
  ```

---

# Milestone M1 — Engine core

From here on, the new code lives in `src/ambot/**` and `test/ambot/**`. The prototype packages keep compiling until Task 12 deletes them.

### Task 5: Fixed timestep and canvas scaling

**Files:**
- Create: `src/ambot/core/GameConfig.java`, `src/ambot/core/FixedStep.java`, `src/ambot/core/ScaleMath.java`
- Test: `test/ambot/core/FixedStepTest.java`, `test/ambot/core/ScaleMathTest.java`

**Interfaces:**
- Produces: `GameConfig` constants:
  - `LOGICAL_WIDTH = 640`, `LOGICAL_HEIGHT = 360`
  - `TILE = 16`
  - `UPS = 60`, `MAX_CATCH_UP = 5`
  - `DEFAULT_SCALE = 2`
  - `TITLE = "AMBOT"`
- Produces: `FixedStep(int updatesPerSecond, int maxStepsPerAdvance)` with:
  - `int advance(long elapsedNanos)`
  - `long nanosUntilNextStep()`
  - `long stepNanos()`
- Produces: `ScaleMath.Viewport` (record: `int scale, int offsetX, int offsetY, int width, int height`) and `static Viewport fit(int windowW, int windowH, int logicalW, int logicalH)`.

- [ ] **Step 1: Write the failing tests.**
  ```java
  class FixedStepTest {
      static final long STEP = 1_000_000_000L / 60; // 16_666_666
      @Test void oneStepPerStepTime()       { assertEquals(1, new FixedStep(60, 5).advance(STEP)); }
      @Test void accumulatesPartialTime()   { var f = new FixedStep(60, 5); assertEquals(0, f.advance(STEP / 2)); assertEquals(1, f.advance(STEP / 2 + 1)); }
      @Test void capsCatchUpAndDropsBacklog() { var f = new FixedStep(60, 5); assertEquals(5, f.advance(1_000_000_000L)); assertEquals(0, f.advance(0)); }
      @Test void reportsTimeUntilNextStep() { var f = new FixedStep(60, 5); f.advance(STEP / 4); assertEquals(STEP - STEP / 4, f.nanosUntilNextStep()); }
      @Test void negativeElapsedCountsAsZero() { assertEquals(0, new FixedStep(60, 5).advance(-5_000_000)); }
  }
  class ScaleMathTest {
      @Test void fits1280x720AtTwo()       { assertEquals(new ScaleMath.Viewport(2, 0, 0, 1280, 720), ScaleMath.fit(1280, 720, 640, 360)); }
      @Test void fits1080pAtThree()        { assertEquals(new ScaleMath.Viewport(3, 0, 0, 1920, 1080), ScaleMath.fit(1920, 1080, 640, 360)); }
      @Test void fits1366x768Centered()    { assertEquals(new ScaleMath.Viewport(2, 43, 24, 1280, 720), ScaleMath.fit(1366, 768, 640, 360)); }
      @Test void tinyWindowKeepsScaleOne() { assertEquals(new ScaleMath.Viewport(1, -70, -30, 640, 360), ScaleMath.fit(500, 300, 640, 360)); }
  }
  ```

- [ ] **Step 2: Run them and confirm they fail.** Run `.\build.cmd test -Select ambot.core.FixedStepTest` and then `-Select ambot.core.ScaleMathTest`. Both should FAIL with `cannot find symbol`.

- [ ] **Step 3: Implement.**
  - **FixedStep:** an accumulator in nanoseconds, with `stepNanos = 1_000_000_000L / ups`.
    - `advance` adds `max(0, elapsed)`, then counts whole steps.
    - If the count would exceed the cap, it returns the cap and zeroes the accumulator. Otherwise it keeps the remainder.
    - `nanosUntilNextStep = stepNanos - accumulator`.
  - **ScaleMath:**
    - `scale = max(1, min(windowW / logicalW, windowH / logicalH))`
    - `width = logicalW * scale`, `height = logicalH * scale`
    - `offset = (window - size) / 2`, using integer division.

- [ ] **Step 4: Run the tests and confirm they pass.** `.\build.cmd test` should show all green.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot test/ambot
  git commit -m "feat(core): fixed timestep and whole-number canvas scaling"
  ```

### Task 6: Buffered, thread-safe input

**Files:**
- Create: `src/ambot/input/Action.java`, `KeyBindings.java`, `InputState.java`, `Controls.java` and `SwingInputAdapter.java`, all in `src/ambot/input/`
- Test: `test/ambot/input/InputStateTest.java`

**Interfaces:**
- Produces: `enum Action { LEFT, RIGHT, UP, DOWN, JUMP, ATTACK, CONFIRM, BACK, PAUSE }`.
- Produces: `KeyBindings`, with `static KeyBindings defaults()` and `Set<Action> actionsFor(int keyCode)`. An unbound key returns an empty set.
- Produces: `InputState(KeyBindings)`, with all methods `synchronized`:
  - `void keyPressed(int keyCode)`, `void keyReleased(int keyCode)`
  - `void actionPressed(Action)`, `void actionReleased(Action)` (used for the mouse)
  - `void releaseAll()`
  - `Controls poll()`
- Produces: `Controls`, immutable:
  - `static Controls of(Set<Action> held, Set<Action> pressed, Set<Action> released)`
  - `static final Controls NONE`
  - `boolean isHeld(Action)`, `boolean wasPressed(Action)`, `boolean wasReleased(Action)`
- Produces: `SwingInputAdapter(InputState)`, which implements `KeyListener` and `MouseListener`. Left mouse maps to `ATTACK`.

- [ ] **Step 1: Write the failing tests** (`in = new InputState(KeyBindings.defaults())`):
  ```java
  @Test void tapBetweenPollsIsNotLost() { in.keyPressed(VK_SPACE); in.keyReleased(VK_SPACE); var c = in.poll();
      assertTrue(c.wasPressed(JUMP)); assertFalse(c.isHeld(JUMP)); assertTrue(c.wasReleased(JUMP)); }
  @Test void pressedReportedOnlyOnce() { in.keyPressed(VK_D); assertTrue(in.poll().wasPressed(RIGHT));
      var c = in.poll(); assertFalse(c.wasPressed(RIGHT)); assertTrue(c.isHeld(RIGHT)); }
  @Test void autoRepeatDoesNotRetrigger() { in.keyPressed(VK_J); in.poll(); in.keyPressed(VK_J);
      var c = in.poll(); assertFalse(c.wasPressed(ATTACK)); assertTrue(c.isHeld(ATTACK)); }
  @Test void actionHeldWhileAnyBoundKeyHeld() { in.keyPressed(VK_A); in.keyPressed(VK_LEFT); in.keyReleased(VK_A);
      assertTrue(in.poll().isHeld(LEFT)); }
  @Test void releaseAllClearsHeldActions() { in.keyPressed(VK_D); in.actionPressed(ATTACK); in.poll(); in.releaseAll();
      var c = in.poll(); assertFalse(c.isHeld(RIGHT)); assertFalse(c.isHeld(ATTACK)); }
  @Test void defaultBindings() { var b = KeyBindings.defaults();
      assertEquals(Set.of(UP, JUMP), b.actionsFor(VK_W)); assertEquals(Set.of(JUMP, CONFIRM), b.actionsFor(VK_SPACE));
      assertEquals(Set.of(PAUSE, BACK), b.actionsFor(VK_ESCAPE)); assertEquals(Set.of(), b.actionsFor(VK_F11)); }
  ```

- [ ] **Step 2: Run them and confirm they fail.** `.\build.cmd test -Select ambot.input.InputStateTest` should FAIL with `cannot find symbol`.

- [ ] **Step 3: Implement.**
  - Default bindings: A/← to LEFT. D/→ to RIGHT. W/↑ to UP+JUMP. S/↓ to DOWN. Space to JUMP+CONFIRM. Enter to CONFIRM. J to ATTACK. Esc to PAUSE+BACK.
  - `InputState` tracks the set of held key codes and the set of held mouse actions. An action counts as held if any held key or mouse action maps to it.
  - Record a "pressed" event only when an action goes from not held to held. That's what ignores auto-repeat.
  - Record a "released" event on the reverse transition.
  - `poll()` returns a snapshot and clears the pressed and released sets.
  - `releaseAll()` clears both held sets without recording releases.

- [ ] **Step 4: Run the tests and confirm they pass.** `.\build.cmd test` should show all green.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot test/ambot
  git commit -m "feat(input): buffered thread-safe input with default bindings"
  ```

### Task 7: State stack and menu

**Files:**
- Create: `src/ambot/states/GameState.java`, `src/ambot/states/StateManager.java`, `src/ambot/ui/Menu.java`, `src/ambot/ui/MenuItem.java`
- Test: `test/ambot/states/StateManagerTest.java`, `test/ambot/ui/MenuTest.java`

**Interfaces:**
- Consumes: `Controls`, `Action` (Task 6).
- Produces: `GameState`, with these members:
  - `void update(Controls c)`
  - `void render(Graphics2D g)`
  - `default void onEnter() {}`
  - `default void onFocusLost() {}`
  - `default boolean isOverlay() { return false; }`
- Produces: `StateManager`, with these members:
  - `void push(GameState)`, `void pop()`, `void replaceAll(GameState)`. All three are queued and applied in order at the start of the next `update`.
  - `void update(Controls)`
  - `void render(Graphics2D)`. It renders from the topmost non-overlay state upward.
  - `void focusLost()`. It calls the top state's `onFocusLost`.
  - `GameState top()`, which returns null when the stack is empty.
  - `int size()`
- Produces: `record MenuItem(String label, Runnable action, boolean enabled)` with `static MenuItem of(String, Runnable)` and `static MenuItem disabled(String)`.
- Produces: `Menu(List<MenuItem>)`, which throws IllegalArgumentException if no item is enabled, with these members:
  - `void update(Controls)`. UP/DOWN wrap around and skip disabled items. CONFIRM runs the selected item.
  - `int selectedIndex()`
  - `void render(Graphics2D g, int centerX, int topY)`

- [ ] **Step 1: Write the failing tests.**
  Use a `RecordingState` that appends `name:update`, `name:render`, `name:enter` and `name:focus` to a shared list and takes an `overlay` flag. The tests:
  - `pushIsDeferredUntilNextUpdate`: after `replaceAll(a); update(NONE)`, the log is `[a:enter, a:update]`. After `push(b)`, `top()` is still `a`. After the next `update`, the log ends with `b:enter, b:update`, and `a` was not updated.
  - `overlayRendersStateBelow`: `a` with overlay `b` on top renders as `[a:render, b:render]`.
  - `nonOverlayHidesStatesBelow`: renders only `b:render`.
  - `popReturnsToPrevious`: after `push(b)`, `update`, `pop()` and another `update`, the last log entry is `a:update`.
  - `poppingLastStateThrows`: after `replaceAll(a); update(NONE); pop();`, the next `update(NONE)` throws `IllegalStateException`.
  - `opsQueuedDuringUpdateApplyNextTick`: a state that calls `push(b)` inside its own `update` is still `top()` right after that update.
  - `focusLostGoesToTop`: it logs `b:focus` only.
  - `emptyManagerIsNoOp`: `update` and `render` on an empty manager don't throw.

  `MenuTest` (items: `New Game`, `disabled("Options")`, `Credits`, `Quit`):
  - `startsOnFirstEnabled`: index 0. With a disabled first item, the start index is 1.
  - `downSkipsDisabled`: one DOWN gives index 2.
  - `wrapsBothWays`: UP from 0 gives index 3. DOWN from 3 gives index 0.
  - `confirmRunsSelectedAction`: a counter increments.
  - `rejectsAllDisabled`: throws `IllegalArgumentException`.

  Feed input with `Controls.of(Set.of(), Set.of(DOWN), Set.of())` and the equivalents for other actions.

- [ ] **Step 2: Run them and confirm they fail.** `.\build.cmd test -Select ambot.states.StateManagerTest` should FAIL with `cannot find symbol`.

- [ ] **Step 3: Implement** `GameState`, `StateManager` (an `ArrayDeque`-backed stack plus a pending-operation queue), `MenuItem` and `Menu`.
  `Menu.render` draws each label in the built-in monospaced font and marks the selected item with `> `. Draw disabled items dimmed. The exact look is the builder's call.

- [ ] **Step 4: Run the tests and confirm they pass.** `.\build.cmd test` should show all green.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot test/ambot
  git commit -m "feat(states): deferred state stack with overlays; menu widget"
  ```

### Task 8: Sprite sheet, animation and the player animation table

**Files:**
- Create: `src/ambot/gfx/SpriteSheet.java`, `src/ambot/gfx/AnimationClip.java`, `src/ambot/gfx/Animator.java`, `src/ambot/entities/player/PlayerAnimations.java`, `src/ambot/entities/player/PlayerState.java`
- Modify: `resources/Betteradventurer-Sheet.png` (remove the single stray pixel in cell (8,1) only)
- Test: `test/ambot/gfx/SpriteSheetTest.java`, `test/ambot/gfx/AnimatorTest.java`, `test/ambot/entities/player/PlayerAnimationsTest.java`

**Interfaces:**
- Produces: `SpriteSheet`, with these members:
  - `SpriteSheet(BufferedImage image, int cellW, int cellH)`
  - `static SpriteSheet load(String resourcePath, int cellW, int cellH)`. It throws `IllegalStateException` with a message containing the path if the resource is missing.
  - `BufferedImage frame(int col, int row)`. Frames are cached, and an out-of-range cell throws `IndexOutOfBoundsException`.
  - `boolean isEmpty(int col, int row)`, `int columns()`, `int rows()`
- Produces: `record AnimationClip(String name, int row, int startCol, int frameCount, int ticksPerFrame, boolean loop, int footY)`. `footY` is the cell-local y just below the lowest opaque pixel across the clip's frames.
- Produces: `Animator`, with these members:
  - `void play(AnimationClip)`. Playing the clip that's already playing is a no-op. A different clip restarts.
  - `void restart()`, `void tick()`, `AnimationClip clip()`
  - `int frameIndex()`. With `elapsed` = ticks since play, `frameIndex` is `(elapsed / tpf) % n` for looping clips and `min(elapsed / tpf, n - 1)` otherwise.
  - `int column()`, which is `startCol + frameIndex`.
  - `boolean isFinished()`, which is `!loop && elapsed >= n * tpf`.
- Produces: `enum PlayerState { IDLE, RUN, JUMP, FALL, CROUCH, ATTACK }`.
- Produces: `PlayerAnimations`, with these members:
  - `SHEET_PATH = "/Betteradventurer-Sheet.png"`, `CELL_W = 50`, `CELL_H = 37`
  - `static AnimationClip forState(PlayerState)`
  - `static final List<AnimationClip> ALL`

  The clip table (`row, startCol, frames, ticksPerFrame, loop, footY`, all values measured on 2026-11-17):

  | Clip | row | startCol | frames | ticks/frame | loop | footY |
  |---|---|---|---|---|---|---|
  | IDLE | 0 | 0 | 4 | 10 | yes | 36 |
  | RUN | 1 | 0 | 6 | 6 | yes | 34 |
  | JUMP | 2 | 2 | 2 | 6 | no | 34 |
  | FALL | 2 | 8 | 2 | 8 | yes | 30 |
  | CROUCH | 9 | 0 | 4 | 10 | yes | 32 |
  | ATTACK | 6 | 0 | 7 | 5 | no | 36 |

- [ ] **Step 1: Write the failing tests.**
  - `SpriteSheetTest`:
    - `loadsReworkedSheetAsElevenByEleven`
    - `frameIsCellSized` (50×37)
    - `missingResourceNamesPath`: `load("/nope.png", 50, 37)` throws `IllegalStateException`, and the message contains `/nope.png`.
    - `outOfRangeFrameThrows`: `frame(11, 0)`.
    - `strayPixelRemoved`: `isEmpty(8, 1)`.
  - `AnimatorTest`, with a loop clip of 4 frames at 3 ticks and a one-shot clip of 3 frames at 2 ticks:
    - The loop clip shows frame 1 after 3 ticks, and wraps to 0 after 12 ticks.
    - `column()` equals `startCol + frameIndex`.
    - The one-shot clip isn't finished after 5 ticks (frame 2), is finished after 6, and still shows frame 2 after 10.
    - Calling `play(sameClip)` mid-way doesn't reset.
    - `play(otherClip)` resets to frame 0 and not finished.
  - `PlayerAnimationsTest`:
    - `everyClipFrameHasPixels`: for every clip in `ALL`, each `(startCol + i, row)` is not empty.
    - `footYMatchesArt`: for every clip, the lowest opaque y + 1 across its frames equals `footY`.
    - `everyStateHasAClip`.

- [ ] **Step 2: Run them and confirm they fail.** `.\build.cmd test -Select ambot.gfx.SpriteSheetTest` should FAIL with `cannot find symbol`.

- [ ] **Step 3: Implement the classes.**

- [ ] **Step 4: Remove the stray pixel.**
  Using a throwaway program in the scratchpad (not committed), set pixel (422, 73) of `resources/Betteradventurer-Sheet.png` to fully transparent and write the PNG back with ImageIO. That's cell (8,1), local (22,36), colour `ffab4343`.
  Verify against `git show HEAD:resources/Betteradventurer-Sheet.png`: exactly 1 pixel differs, and it's that one.

- [ ] **Step 5: Run the tests and confirm they pass.** `.\build.cmd test` should show all green, with the prototype tests still passing.

- [ ] **Step 6: Commit.**
  ```bash
  git add src/ambot test/ambot resources/Betteradventurer-Sheet.png
  git commit -m "feat(gfx): sprite sheet, animator, player clip table with measured foot lines; remove stray pixel"
  ```

### Task 9: Tiles and collision

**Files:**
- Create: `src/ambot/level/Rect.java`, `src/ambot/level/TileMap.java`, `src/ambot/level/AsciiLevel.java`, `src/ambot/level/TileCollider.java`
- Test: `test/ambot/level/TileMapTest.java`, `test/ambot/level/TileColliderTest.java`

**Interfaces:**
- Produces: `record Rect(double x, double y, double w, double h)` with `right()`, `bottom()`, `withX(double)`, `withY(double)` and `intersects(Rect)`.
- Produces: `TileMap`, with these members:
  - `TileMap(int widthTiles, int heightTiles, int[] tiles)`. Tile 0 is empty, 1 is solid.
  - `static TileMap fromAscii(String... rows)`. `#` is solid, and `.` and `P` are empty. Uneven rows or any other character throw IllegalArgumentException.
  - `boolean isSolid(int tx, int ty)`. Off the left or right edge is solid. Above or below the map is not solid.
  - `int tileAt(int tx, int ty)`
  - `int widthTiles()`, `int heightTiles()`, `int widthPx()`, `int heightPx()`
- Produces: `record AsciiLevel(TileMap map, double spawnX, double spawnY)` with `static AsciiLevel parse(String... rows)`. The spawn is the pixel top-left of the single `P` tile. Zero or several `P` tiles throw IllegalArgumentException.
- Produces: `TileCollider`, with these members:
  - `record Move(double position, boolean blocked)`
  - `static Move moveX(Rect box, double dx, TileMap map)` and `static Move moveY(Rect box, double dy, TileMap map)`. Each requires `|d| <= 16`, otherwise it throws IllegalArgumentException. A blocked move snaps flush to the tile edge.
  - `static boolean onGround(Rect box, TileMap map)`. It checks for a solid tile 0.01 px below the box.

- [ ] **Step 1: Write the failing tests.** The test map is 8×5 tiles, 128×80 px, with the floor top at y=64:
  ```
  "........",
  "........",
  "...##...",
  "........",
  "########"
  ```
  The tests:
  - `outOfBoundsRules`: `isSolid(-1,0)` and `isSolid(8,0)` are true. `isSolid(0,-1)` and `isSolid(0,5)` are false. `isSolid(3,2)` is true.
  - `rejectsRaggedRowsAndUnknownChars`
  - `asciiLevelFindsSingleSpawn`: `P` at column 2, row 3 gives a spawn of (32, 48). Missing or duplicate `P` throws.
  - `landsFlushOnFloor`: `moveY(new Rect(16,40,14,20), 6)` gives `Move(44, true)`.
  - `fallsFreelyAboveFloor`: `moveY(new Rect(16,30,14,20), 6)` gives `Move(36, false)`.
  - `bumpsCeiling`: `moveY(new Rect(48,50,14,20), -6)` gives `Move(48, true)`.
  - `stopsAtRightMapEdge`: `moveX(new Rect(110,40,14,20), 6)` gives `Move(114, true)`.
  - `stopsAtLeftMapEdge`: `moveX(new Rect(2,40,14,20), -6)` gives `Move(0, true)`.
  - `rejectsMovesLargerThanATile`: `dx = 16.5` throws.
  - `onGroundExactlyWhenTouching`: a box with bottom at 64 is on the ground. Bottom at 63 is not.
  - `noTunnelingAtMaxFall`: starting at y=0 with h=20, applying `dy=6` repeatedly ends with the bottom exactly at 64.
  - `fallsOutBottomThroughGap`: in a map with a gap in the floor, a box over the gap moves to y > `heightPx` without being blocked.

- [ ] **Step 2: Run them and confirm they fail.** `.\build.cmd test -Select ambot.level.TileColliderTest` should FAIL with `cannot find symbol`.

- [ ] **Step 3: Implement `TileCollider`** with this algorithm (`EPS = 1e-9`, `T = 16`):
  ```
  moveY(box, dy):
    if dy == 0: return (box.y, false)
    tx0 = floor(box.x / T); tx1 = floor((box.x + box.w - EPS) / T); newY = box.y + dy
    if dy > 0:
      rowOld = floor((box.y + box.h - EPS) / T); rowNew = floor((newY + box.h - EPS) / T)
      for row in rowOld+1 .. rowNew: if any isSolid(tx, row) for tx in tx0..tx1: return (row*T - box.h, true)
    else:
      rowOld = floor(box.y / T); rowNew = floor(newY / T)
      for row in rowOld-1 down to rowNew: if any isSolid(tx, row): return ((row+1)*T, true)
    return (newY, false)
  moveX: the same with axes swapped (columns crossed, rows ty0..ty1 spanned)
  ```
  Implement `TileMap`, `AsciiLevel` and `Rect` as specified.

- [ ] **Step 4: Run the tests and confirm they pass.** `.\build.cmd test` should show all green.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot test/ambot
  git commit -m "feat(level): tile map, ASCII test levels, axis-separated tile collision"
  ```

### Task 10: Entity hierarchy and the player

**Files:**
- Create: `src/ambot/entities/Entity.java`, `src/ambot/entities/Creature.java`, `src/ambot/entities/Facing.java`, `src/ambot/entities/Physics.java`, `src/ambot/entities/player/Player.java`, `src/ambot/entities/player/PlayerTuning.java`
- Test: `test/ambot/entities/player/PlayerTest.java`

**Interfaces:**
- Consumes:
  - `Controls`, `Action` (Task 6)
  - `SpriteSheet`, `Animator`, `PlayerAnimations`, `PlayerState` (Task 8)
  - `TileMap`, `Rect`, `TileCollider` (Task 9)
- Produces: `abstract class Entity`, with `protected double x, y` (hitbox top-left), `protected final double width, height`, `Rect hitbox()`, `double x()`, `double y()` and `abstract void render(Graphics2D g)`.
- Produces: `abstract class Creature extends Entity`, with these members:
  - `protected double vx, vy`
  - `protected Facing facing = Facing.RIGHT`
  - `protected boolean onGround`
  - `protected final Animator animator`
  - `protected void applyGravityAndMove(TileMap map)`. In order, it:
    1. adds `Physics.GRAVITY` to `vy` and caps it at `Physics.MAX_FALL`
    2. calls `moveX(vx)`, and sets `vx = 0` if blocked
    3. calls `moveY(vy)`, and sets `vy = 0` if blocked
    4. sets `onGround = TileCollider.onGround(...)`
  - Getters `vx()`, `vy()`, `facing()`, `onGround()`, `animator()`.
- Produces: `enum Facing { LEFT, RIGHT }`, and `Physics` with `GRAVITY = 0.35` and `MAX_FALL = 6.0` (px per tick).
- Produces: `PlayerTuning` with:
  - `RUN_SPEED = 2.0`
  - `JUMP_VELOCITY = -6.0`, `JUMP_CUT_VELOCITY = -2.0`
  - `COYOTE_TICKS = 6`, `JUMP_BUFFER_TICKS = 6`
  - `HITBOX_W = 14`, `HITBOX_H = 28`
  - `SPRITE_CENTER_X = 21` (the body centre inside the 50-px cell)
- Produces: `Player extends Creature`, with these members:
  - `Player(double x, double y, SpriteSheet sheet)`
  - `void update(Controls c, TileMap map)`
  - `PlayerState state()`
  - `void respawn(double x, double y)`. It resets velocity and state and puts the player at IDLE.
  - `void render(Graphics2D g)`

- [ ] **Step 1: Write the failing tests** (the real sheet loaded once with `@BeforeAll`):
  ```
  ROOM = "....................",  ×11 rows of dots, then
         "##########....######"   // 20×12; floor top y=176, gap at cols 10–13 (x 160–224)
  standing y = 176 - 28 = 148
  ```
  Helpers: `step(p, c, n)` and `held(Action...)`/`press(Action...)`, which build `Controls`.
  - `settlesIdleOnFloor`: `Player(32,148)` after 3×NONE has `onGround`, `IDLE` and `y == 148`.
  - `runsRightAndFacesRight`: holding RIGHT for 10 ticks gives `x == 52`, `RUN`, `Facing.RIGHT`.
  - `facesLeftWhenMovingLeft`
  - `heldJumpRisesAbout48px`: press and hold JUMP. The maximum rise is between 45 and 55 px. The state is `JUMP` while `vy < 0`, then `FALL`, then `IDLE` after landing at y=148.
  - `tapJumpStillLeavesGround`: press JUMP for one tick (pressed and released, not held). The rise is > 0 and < 10.
  - `crouchStopsHorizontalMovement`: holding DOWN+RIGHT gives `CROUCH` with x unchanged.
  - `coyoteJumpWithinGraceWindow`: run right from x=146 until `!onGround()`. After 3×NONE, pressing JUMP gives `vy < 0`. A fresh run with 6×NONE before the press keeps `vy > 0`.
  - `jumpBufferedBeforeLanding`: drop from y=100. Press JUMP (then hold it) once the bottom is within 12 px above 176. Within the next 6 ticks, some tick ends with `vy < -4`.
  - `attackLasts35Ticks`: press ATTACK. The state is `ATTACK` after 35 updates and `IDLE` after 36. Pressing ATTACK again at tick 10 doesn't extend it.
  - `groundAttackStopsRunning`: holding RIGHT and pressing ATTACK leaves x unchanged for the attack's duration.
  - `fallsIntoPitWithoutError`: from x=176, y=148 (over the gap), 60 NONE ticks end with `y > 192` and no exception.

- [ ] **Step 2: Run them and confirm they fail.** `.\build.cmd test -Select ambot.entities.player.PlayerTest` should FAIL with `cannot find symbol`.

- [ ] **Step 3: Implement `Player.update`** in this fixed order:
  1. **Timers:** if on the ground, `coyote = COYOTE_TICKS`, else `coyote--`. If JUMP was pressed, `buffer = JUMP_BUFFER_TICKS`, else `buffer--`.
  2. **Attack:** if ATTACK was pressed and the state isn't ATTACK, set the state to `ATTACK` and play the ATTACK clip.
  3. **Horizontal:** `dir = held(RIGHT) − held(LEFT)`. `crouching = held(DOWN) && onGround && state != ATTACK`.
     - If crouching, or attacking on the ground, `vx = 0`. Otherwise `vx = dir * RUN_SPEED`.
     - Update facing when `dir != 0` and the state isn't ATTACK.
  4. **Jump:** if `buffer > 0 && coyote > 0 && !crouching && state != ATTACK`, then `vy = JUMP_VELOCITY` and `buffer = coyote = 0`.
  5. **Cut:** if JUMP isn't held and `vy < JUMP_CUT_VELOCITY`, then `vy = JUMP_CUT_VELOCITY`.
  6. Call `applyGravityAndMove(map)`.
  7. **State:**
     - Stay in `ATTACK` until `animator.isFinished()`.
     - Otherwise: not on the ground means `JUMP` if `vy < 0`, else `FALL`. Crouching means `CROUCH`. `vx != 0` means `RUN`. Anything else is `IDLE`.
     - Then call `animator.play(PlayerAnimations.forState(state))` and `animator.tick()`.

  **Render:**
  - `drawX = round(x + width/2 − (facing == RIGHT ? SPRITE_CENTER_X : 50 − SPRITE_CENTER_X))`
  - `drawY = round(y + height − clip.footY())`
  - Draw `sheet.frame(animator.column(), clip.row())`, mirrored with negative width when facing left.

- [ ] **Step 4: Run the tests and confirm they pass.** `.\build.cmd test` should show all green.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot test/ambot
  git commit -m "feat(entities): Entity/Creature hierarchy; player physics and state machine"
  ```

### Task 11: Game shell, window and screens

**Files:**
- Create in `src/ambot/core/`: `Screen.java`, `GameWindow.java`, `GameLoop.java`, `Game.java`
- Create in `src/ambot/states/`: `GameContext.java`, `MainMenuState.java`, `CreditsState.java`, `PausedState.java`, `PlayingState.java`
- Create: `src/ambot/Main.java`
- Modify: `build.ps1` (`$MainClass = 'ambot.Main'`)
- Test: `test/ambot/states/ScreensTest.java`

**Interfaces:**
- Consumes: everything from Tasks 5–10.
- Produces: `interface GameContext { StateManager states(); SpriteSheet playerSheet(); void quit(); }`.
- Produces: `Screen extends JPanel`, with these members:
  - `Screen(int logicalW, int logicalH)`
  - `void present(BufferedImage frame)`. It stores the frame in a volatile field and calls `repaint()`.
  - `BufferedImage lastFrame()`
  - `paintComponent` fills black, then draws the last frame at `ScaleMath.fit(...)` with nearest-neighbour interpolation.
  - The preferred size is logical × `DEFAULT_SCALE`.
- Produces: `GameWindow(Screen, String title)`, with these members:
  - It packs, then centres with `setLocationRelativeTo(null)` after `pack`, and is resizable.
  - `void toggleFullscreen()`: borderless maximized, restoring the previous bounds on the second toggle.
  - `void onFocusLost(Runnable)`
- Produces: `GameLoop(FixedStep step, Runnable tick, Runnable render)`, with `start()` (a non-daemon thread named `game-loop`), `stop()`, `int ups()` and `int fps()` (counts over the last full second).
  - **Sleep:** if `nanosUntilNextStep() > 2_000_000`, `Thread.sleep` for it minus 1 ms. Otherwise `Thread.onSpinWait()`.
  - **Render:** only after an iteration that ran at least one tick.
- Produces: `Game implements GameContext`, with `Game()` (call on the event thread), `void start()`, `InputState input()`, `Screen screen()` and `GameLoop loop()`.
  - Tick, on the loop thread:
    1. Handle the pending focus-lost flag, an `AtomicBoolean` set from the event thread: call `input.releaseAll()` and `states.focusLost()`.
    2. `controls = input.poll()`, then `states.update(controls)`.
  - Render: draw `states.render` into alternating back buffers (two `TYPE_INT_RGB` 640×360 images). If F3 is toggled, overlay `UPS n  FPS n`. Then `screen.present(buffer)`.
  - A separate `KeyListener` on the screen handles F11 (`toggleFullscreen`) and F3 (debug overlay).
  - `quit()` stops the loop, disposes the window and calls `System.exit(0)`.
  - The first state is `MainMenuState`.
- Produces: `MainMenuState(GameContext)`. Its menu is:
  - `New Game` → `states().replaceAll(new PlayingState(ctx))`
  - `disabled("Options (coming soon)")`
  - `Credits` → `push(new CreditsState(ctx))`
  - `Quit` → `ctx.quit()`
- Produces: `CreditsState(GameContext)`. BACK or CONFIRM pops. Its exact lines, as `static final List<String> LINES`:
  ```
  "AMBOT", "", "Original game: Original OOP2 team (2025)", "Player sprite: Adventurer by rvros (itch.io)", "Restoration: 2026", "", "Esc / Enter: back"
  ```
- Produces: `PausedState(GameContext)`, an overlay with `isOverlay() == true`.
  - Menu: `Resume` → `pop()`, and `Main Menu` → `replaceAll(new MainMenuState(ctx))`.
  - PAUSE or BACK pops.
  - It renders a translucent black fill and `PAUSED`.
- Produces: `PlayingState`, with these members:
  - `PlayingState(GameContext)` and `PlayingState(GameContext, AsciiLevel)`
  - `Player player()`
  - `static final String[] TEST_ROOM`: 40×22 tiles. Constraints:
    - a solid floor and side walls
    - three ledges at 3, 5 and 8 tiles above the floor
    - one 4-tile pit, open at the bottom
    - `P` on the floor at the left
  - **Spawning:** the player's hitbox bottom-centre goes on the bottom-centre of the `P` tile, which is `x = spawnX + (16 − HITBOX_W) / 2` and `y = spawnY + 16 − HITBOX_H`. Respawn uses the same position. The hitbox (28 px) is taller than a tile, so placing it at the tile's top-left would sink it into the floor.
  - Update: PAUSE pressed → `push(new PausedState(ctx))`. `player.update(c, map)`. If `player.y() > map.heightPx() + 64`, respawn.
  - `onFocusLost` → push `PausedState`.
  - Render: a flat background, solid tiles as placeholder blocks, the player, and the hint `Esc: pause`.
- Produces: `ambot.Main.main`, which calls `SwingUtilities.invokeLater(() -> new Game().start())`.

- [ ] **Step 1: Write the failing tests** in `ScreensTest`, using a `FakeContext` with a real `StateManager`, the real sheet and a `quitCalled` flag:
  - `newGameStartsPlaying`: the menu with CONFIRM pressed, then one more update, gives a `PlayingState` on top.
  - `quitSkipsDisabledOptions`: DOWN, DOWN, CONFIRM sets `quitCalled`.
  - `creditsBackReturnsToMenu`
  - `escapePushesPause`: with `PlayingState` on top, PAUSE pressed then one update gives a `PausedState` on top, and `size() == 2`.
  - `pauseEscapeResumes`
  - `focusLostPushesPause`: `states.focusLost()`, then an update, gives a `PausedState` on top.
  - `spawnsStandingOnFloor`: in a fresh `PlayingState`, after 5 NONE updates, `player().onGround()` is true and `player().y() + 28` equals the floor top under `P`.
  - `respawnsAfterFallingOutOfMap`: `PlayingState(ctx, AsciiLevel.parse(<pit room>))` with the player placed over the pit. After 120 NONE updates, `player().y() < map.heightPx()`.
  - `pausedFreezesPlayer`: while paused, player x and y don't change across 10 updates with RIGHT held.

- [ ] **Step 2: Run them and confirm they fail.** `.\build.cmd test -Select ambot.states.ScreensTest` should FAIL with `cannot find symbol`.

- [ ] **Step 3: Implement** the classes above, and point `build.ps1` at `ambot.Main`.

- [ ] **Step 4: Run the tests and confirm they pass.** `.\build.cmd test` should show all green.

- [ ] **Step 5: Check by hand.** Run `.\build.cmd run`. Expected:
  - A centred `AMBOT` window, 1280×720, showing the menu.
  - Enter starts the test room. A/D runs with correct facing. Space jumps, and a tap gives a short hop. S crouches. J attacks.
  - Falling into the pit respawns the player.
  - Esc pauses and resumes. F11 toggles fullscreen and the pixels stay crisp. F3 shows `UPS 60`.
  - Alt-tab pauses the game.

- [ ] **Step 6: Commit.**
  ```bash
  git add src build.ps1 test
  git commit -m "feat(core): window, sleeping loop, menus, pause, credits, playable test room"
  ```

### Task 12: Smoke harness, acceptance and retiring the prototype

**Files:**
- Create: `test/ambot/tools/SmokeRun.java` (a plain `main`, not a JUnit test)
- Modify: `build.ps1` (add a `smoke` task), `src/ambot/entities/player/PlayerAnimations.java` (`SHEET_PATH`), `docs/ASSETS.md`, `docs/ROADMAP.md`
- Move: `resources/Betteradventurer-Sheet.png` to `resources/sprites/Betteradventurer-Sheet.png`
- Move: `resources/adventurer-Sheet.png`, `resources/MC.png` and `resources/test.png` to `art/source/` (kept, not shipped)
- Delete: `src/main/`, `src/Entities/`, `src/Inputs/`, `src/Utils/`, `test/Entities/`

**Interfaces:**
- Consumes: `Game.input()`, `Game.screen().lastFrame()`, `Game.loop().ups()/fps()` (Task 11).
- Produces: `.\build.cmd smoke`, which runs `ambot.tools.SmokeRun build\smoke` with `build\classes;build\test-classes` on the classpath.

- [ ] **Step 1: Move the files.**
  - Move the files listed above with `git mv`.
  - Set `PlayerAnimations.SHEET_PATH = "/sprites/Betteradventurer-Sheet.png"`.
  - Delete the prototype packages and their test.
  - Update `docs/ASSETS.md` paths.

- [ ] **Step 2: Run the tests.** `.\build.cmd test` should show all green, with no references to the old packages left.

- [ ] **Step 3: Write `SmokeRun`.** It creates `Game` on the event thread and calls `start()`. Then it drives input via `game.input().actionPressed/actionReleased`, and after each step saves `screen().lastFrame()` as a PNG to the output folder:
  1. menu, `01-menu.png`
  2. CONFIRM, room, `02-room.png`
  3. hold RIGHT 1 s, `03-run.png`
  4. JUMP press, then 250 ms, `04-air.png`
  5. land, hold DOWN, `05-crouch.png`
  6. ATTACK, with 6 frames at 100 ms saved as `06-attack-N.png`
  7. hold LEFT 300 ms, `07-left.png`
  8. PAUSE, `08-paused.png`

  Then it measures process CPU over 3 s while paused, using `com.sun.management.OperatingSystemMXBean.getProcessCpuTime()`, and prints:
  ```
  UPS <n>  FPS <n>  CPU <cores>
  ```
  Finally it calls `System.exit(0)`.

- [ ] **Step 4: Run it and check the result.**
  Run: `.\build.cmd smoke`
  Expected:
  - The output shows `UPS 59–61`, `FPS 59–61` and `CPU` below 0.30.
  - In the 13 PNGs, the character stands on the floor with feet on the tile line in every pose, isn't stretched, faces left in `07`, crouches in `05` and shows the sword arc in `06`. `08` shows the pause overlay.
  - Look at every image before continuing. If CPU is over 0.30, use sleep-minus-2 ms in `GameLoop` and measure again.

- [ ] **Step 5: Update `docs/ROADMAP.md`.** Mark M0 and M1 done, with dates.

- [ ] **Step 6: Commit and tag.**
  ```bash
  git add -A src test resources art build.ps1 docs
  git commit -m "chore: retire prototype code, organise art, smoke harness; M1 complete"
  git tag -a m1 -m "M1 engine core"
  ```

- [ ] **Step 7: Hand over for review.** Show the user the smoke screenshots, then ask them to run `.\build.cmd run` and play the test room (spec: "the user reviews the result of every milestone").
