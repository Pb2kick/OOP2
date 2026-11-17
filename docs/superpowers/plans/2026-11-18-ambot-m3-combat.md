# AMBOT M3 (Combat) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The player can be hurt and can fight back. The milestone adds:
- three hearts, knockback with about a second of blinking invincibility, hurt and death animations, and Game Over with Retry from the last checkpoint
- a sword hitbox, and crouching that ducks low shots
- two World 1 enemies (a patrolling Walker and a Shooter that fires at standing height), heart pickups, a HUD
- a playable "First Blood" fight level

It also adds an on-screen crash message in place of the silently frozen window.

**Architecture:**
- **Health:** `Creature` gains health through a new `Damageable` interface. `Player` turns damage into hurt, knockback, invincibility and death states.
- **Enemies:** an abstract `Enemy` (AI hook, contact damage, drops) with `Walker` and `Shooter` subclasses. Spawning comes from level objects through an `EnemyFactory`.
- **Combat resolution:** an `Actors` class owns the enemies, projectiles and pickups, and resolves every hit between them and the player each tick. That keeps `PlayingState` small: it owns hazards, pits, Game Over and Retry.
- **Art:** placeholders only. Enemies, bolts and hearts are drawn with Java2D shapes until M4 picks an itch.io pack (spec §5.10).

**Tech Stack:** Java (`--release 21`) on JDK 26 at `C:\jdk-26`, Swing/Java2D, JUnit 5 (console standalone 1.11.4, tests only), `build.ps1` through `build.cmd`.

**Spec:** [`docs/superpowers/specs/2026-11-17-ambot-restoration-design.md`](../specs/2026-11-17-ambot-restoration-design.md), mainly §5.4 (Player: health), §5.7 (Enemies), §5.3 (GameOver state) and §5.10 (placeholders). The milestone map is [`docs/ROADMAP.md`](../../ROADMAP.md), and the level format is [`docs/LEVELS.md`](../../LEVELS.md). This plan builds on `main` at `2bcfd85` (tag `m2`).

All paths are relative to the project root `C:\Users\Pb2kick\Desktop\Project Restorations\AMBOT (OOP2)`.

## Global Constraints

- **Toolchain:** `javac --release 21` with the JDK at `C:\jdk-26`. Never use the `java` on PATH (Java 8).
- **Libraries:** none at runtime beyond the JDK.
- **Build:** no Maven or Gradle. Every command goes through `.\build.cmd <task>`.
- **Canvas and loop:** 640×360 logical canvas, 16×16 tiles, 60 UPS.
- **Health rules** (spec §5.4):
  - The player starts with 3 hearts.
  - Touching an enemy or a hazard costs 1 heart, knocks the player back, and gives about 1 second of blinking invincibility.
  - Pits are instant death.
  - At 0 hearts, Game Over offers Retry, from the last checkpoint with full hearts, or Main Menu.
  - There are no lives.
- **Pickups:** heart refills only (spec §5.6). There are no collectibles.
- **OOP shape** (spec §5.7):
  - `Entity` → `Creature` (implements `Damageable`) → `Player` | `Enemy` → `Walker`, `Shooter`.
  - Bosses are M4.
  - No `Collidable` interface yet: `Entity.hitbox()` already serves, and the spec asks for interfaces only where they make the code clearer.
- **World 1 theme** (spec §1): run, jump, sword attack and crouch. The Shooter's bolts fly at standing-body height, so crouching or jumping dodges them.
- **Art:** the team's sheet stays untouched. Hurt is row 8 columns 4–6, and death is row 8 columns 7–9. Spec §7 leaves this split to the builder; columns 0–3 are the sword stance, unused for now. Everything else is a Java2D placeholder.
- **Git:** work on branch `m3-combat`. Commit as `Pb2kick` with no co-author trailers, and credit no AI tool in any file or commit message. The repo stays private.

## Review Focus

1. **Several damage sources in one tick, or while invincible.** An enemy, a spike and a bolt touching at once cost exactly 1 heart, and nothing more lands until invincibility ends. Tested in Task 1 (`invincibleIgnoresFurtherDamage`) and Task 6 (`twoSourcesSameTickCostOneHeart`).
2. **Falling into a pit while invincible, hurt or already dying.** Game Over opens exactly once and `deaths()` goes up by exactly 1. Tested in Task 7: `pitWhileInvincibleIsStillGameOver`, `gameOverOpensOnce`.
3. **Retry leaves no leftovers.** After Retry the player has full hearts and is neither hurt, invincible nor dead. All enemies are back, bolts and dropped hearts are gone, and the camera is on the checkpoint. Tested in Task 7: `retryRestoresHeartsEnemiesAndClearsBolts`.
4. **One sword swing is one hit.** An enemy that overlaps the blade for the whole active window takes 1 damage, an enemy behind the player takes none, and a fresh swing can hit again. Tested in Task 2 (`attackBoxOnlyInActiveFramesAndInFront`) and Task 6 (`swingHitsEachEnemyOnce`, `swingMissesEnemyBehind`).
5. **Space is both Jump and Confirm.** A jump press that lands on the tick Game Over or Level Complete opens doesn't pick a menu item or skip the screen (the M2 deferred minor). Tested in Task 7: `confirmIgnoredDuringGrace`.

---

### Task 1: Damageable creatures, and the player's hurt, invincibility and death

**Files:**
- Create: `src/ambot/entities/Damageable.java`
- Modify: `src/ambot/entities/Creature.java`, `src/ambot/entities/player/Player.java`, `PlayerState.java`, `PlayerTuning.java`, `PlayerAnimations.java`
- Test: `test/ambot/entities/player/PlayerCombatTest.java`, `test/ambot/entities/player/PlayerAnimationsTest.java`

**Interfaces:**
- Produces:
  - **`Damageable`:**
    - `int health()`, `int maxHealth()`, `boolean isDead()`
    - `boolean damage(int amount, double fromX)`: returns false and changes nothing when the target is dead or invincible
    - `void heal(int amount)`: capped at `maxHealth`
  - **`Creature`:**
    - implements `Damageable`, with the constructor `Creature(double x, double y, double w, double h, int maxHealth)` (the old 4-argument form passes 1)
    - `protected boolean isInvincible()` (default false)
    - `protected abstract void onDamaged(double fromX)`, called after health drops
  - **`PlayerState`:** adds `HURT` and `DEAD`.
  - **`PlayerTuning`:**
    - `MAX_HEARTS = 3`, `HURT_TICKS = 18`, `INVINCIBLE_TICKS = 60`, `KNOCKBACK_X = 2.5`, `KNOCKBACK_Y = -4.0`, `DEATH_TICKS = 60`, `BLINK_TICKS = 4`
  - **`PlayerAnimations`:**
    - `HURT = ("HURT", 8, 4, 3, 6, false, 36)` and `DEATH = ("DEATH", 8, 7, 3, 10, false, 36)`, both added to `ALL` and to the state map
  - **`Player` methods:**
    - `boolean isInvincible()`
    - `void kill()`: instant death for pits; sets health to 0 and the state to `DEAD`, even while invincible
    - `boolean deathFinished()`: true once `DEATH_TICKS` have passed since death
    - `void restoreHealth()`
    - `respawn(x, y)` also clears hurt, invincibility and death

- [ ] **Step 1: Write the failing tests** in `PlayerCombatTest`. Reuse `PlayerTest.ROOM`, `standing(x)` and the input helpers.
  - Damage and knockback:
    - `damageCostsOneHeartAndKnocksBackAway`: a hit from x = player centre + 10 leaves 2 hearts, state `HURT`, `vx() == -2.5` and `vy() < 0`.
    - `hurtIgnoresInputUntilItEnds`: holding RIGHT for `HURT_TICKS - 1` ticks after the hit leaves the state `HURT` and `facing()` unchanged. After the hurt ends, RIGHT moves the player right.
  - Invincibility:
    - `invincibleIgnoresFurtherDamage`: a second `damage` within 60 ticks returns false and leaves 2 hearts. At tick 61 it lands.
    - `blinksWhileInvincible`: render the player into an image every tick for 8 ticks after a hit. At least one frame is fully transparent where the sprite usually is, and at least one isn't.
  - Death:
    - `thirdHitKills`: after three hits spaced 61 ticks apart, `isDead()` is true and the state is `DEAD`. `deathFinished()` is false at tick 59 and true at tick 60. Input does nothing while dead.
    - `killIgnoresInvincibility`: `kill()` straight after a hit gives `health() == 0` and `DEAD`.
    - `respawnClearsHurtAndDeath`: after a kill, `respawn` plus `restoreHealth` gives 3 hearts, `IDLE`, and not invincible.
    - `healCapsAtMax`.
  - In `PlayerAnimationsTest`, add `hurtAndDeathClipsHaveFrames`: every cell in row 8 columns 4–9 has opaque pixels, using the same method the existing clip tests use.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test`
  Expected: compile errors for `damage`, `HURT` and `kill`.

- [ ] **Step 3: Implement.**
  - **Taking damage:** `Creature.damage` subtracts the amount, floors health at 0, then calls `onDamaged`.
  - **`Player.onDamaged`:**
    - at 0 hearts, it calls `kill()`
    - otherwise it sets `HURT`, `hurtTicks = HURT_TICKS` and `invincibleTicks = INVINCIBLE_TICKS`
    - the knockback is `vx = KNOCKBACK_X * sign(centreX - fromX)` (if the two are equal, opposite to `facing`) and `vy = KNOCKBACK_Y`
  - **`Player.update`:** while `HURT` or `DEAD` it skips steps 0–5 (drop, timers, attack, run, jump, cut), keeps `vx` (dead players stop: `vx = 0`), and still runs physics. Timers count down to 0. `HURT` ends when `hurtTicks` reaches 0.
  - **`render`:** skips drawing when `invincibleTicks > 0 && (invincibleTicks / BLINK_TICKS) % 2 == 1`.

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass (175 from M2 plus the new ones).

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/entities test/ambot/entities
  git commit -m "feat(entities): Damageable creatures; player hearts, knockback, invincibility, hurt and death"
  ```

### Task 2: Crouch hitbox and sword hitbox

**Files:**
- Modify: `src/ambot/entities/Entity.java`, `Creature.java`, `src/ambot/entities/player/Player.java`, `PlayerTuning.java`
- Test: `test/ambot/entities/player/PlayerCombatTest.java`

**Interfaces:**
- Consumes: `Player` states (Task 1).
- Produces:
  - **`Entity`:** `height` is no longer final.
  - **`Creature`:** `protected void setHeightKeepingFeet(double h)`.
  - **`PlayerTuning`:** `CROUCH_H = 18`, `ATTACK_REACH = 20`, `ATTACK_BOX_H = 20`, `ATTACK_FIRST_ACTIVE_FRAME = 2`, `ATTACK_LAST_ACTIVE_FRAME = 3`.
  - **`Player`:**
    - `Optional<Rect> attackBox()`: present only in `ATTACK` while the animator's frame index is 2 or 3. It's `ATTACK_REACH` wide and `ATTACK_BOX_H` tall, its top is the body's top + 4, and it sits against the body's front edge.
    - `int swingId()`: increases by one every time an attack starts.

- [ ] **Step 1: Write the failing tests.**
  - `crouchShrinksHitboxKeepingFeet`: after 3 ticks holding DOWN, `hitbox().h() == 18` and the bottom is unchanged. Releasing DOWN restores 28 with the bottom unchanged.
  - `attackBoxOnlyInActiveFramesAndInFront`:
    - facing right after `press(ATTACK)`, `attackBox()` is empty at ticks 0–9, present at ticks 10–19 with `x == hitbox().right()`, and empty at tick 20
    - facing left, the box's right edge equals `hitbox().x()`
  - `swingIdIncrementsPerAttack`.
  - `standingUpNeverEmbedsInCeiling`: in a corridor 2 tiles tall, crouch then stand. `hitbox()` doesn't overlap solid tiles. (Any space a crouched player can reach is at least 2 tiles tall, because crouching stops movement.)

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.entities.player.PlayerCombatTest`
  Expected: compile errors for `attackBox`.

- [ ] **Step 3: Implement.** Switch the hitbox height when the computed `crouching` flag changes, before physics runs.

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass. The M1 crouch and attack tests still pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/entities test/ambot/entities
  git commit -m "feat(player): crouch lowers the hitbox; sword hitbox during the swing frames"
  ```

### Task 3: Enemy base, Walker, projectiles, EnemyFactory and level characters

**Files:**
- Create in `src/ambot/entities/enemies/`: `Enemy.java`, `EnemyWorld.java`, `Walker.java`, `EnemyFactory.java`
- Create: `src/ambot/entities/Projectile.java`
- Modify: `src/ambot/level/LevelValidator.java`, `test/ambot/tools/AsciiTmx.java`
- Test: `test/ambot/entities/enemies/WalkerTest.java`, `test/ambot/entities/ProjectileTest.java`, `test/ambot/level/LevelValidatorTest.java`, `test/ambot/tools/AsciiTmxTest.java`

**Interfaces:**
- Consumes: `Creature(x, y, w, h, maxHealth)` and `damage` (Task 1), `TileCollider`, `Spot`, `LevelObject`, `TestLevels`.
- Produces:
  - **`Projectile`** (extends `Entity`):
    - `Projectile(double x, double y, double vx)`, 6×3
    - `void update(TileMap map)`: moves `vx` per tick; dies on entering a solid tile or after `MAX_DISTANCE = 320` px
    - `boolean alive()`, `void kill()`, `int damage()` (1)
    - drawn as an orange `0xFFA300` bar
  - **`EnemyWorld`:** `record EnemyWorld(Rect player, TileMap map, Consumer<Projectile> fire)`.
  - **`Enemy`** (abstract, extends `Creature`):
    - `Enemy(double x, double y, double w, double h, int maxHealth, String drop)`
    - `final void update(EnemyWorld w)`: runs `think(w)` unless stunned, then gravity and movement, then the timers
    - `protected abstract void think(EnemyWorld w)`
    - `int contactDamage()`: 1
    - `String drop()`: `"heart"` or null
    - `boolean hitBySwing(int swingId)`: true and remembered the first time a swing id hits; false after that
    - `boolean isRemovable()`: true `REMOVE_TICKS = 20` ticks after death
    - `onDamaged` stuns the enemy for `STUN_TICKS = 12` ticks and pushes it `1.5` px/tick away from `fromX`
    - it flashes white while stunned
    - `Creature` gains `protected boolean blockedX`, set by `applyGravityAndMove`
  - **`Walker`:**
    - 16×12, 2 health, patrol speed `0.6`, starts walking toward `Facing.LEFT`
    - turns around when `blockedX` or when the tile below its leading bottom corner is neither solid nor one-way
    - drawn as a green `0x00E436` blob with two eyes
  - **`EnemyFactory`:**
    - `KNOWN_TYPES = Set.of("walker", "shooter")`
    - `static Enemy create(LevelObject o)`: the enemy stands with its bottom-centre on the object's bottom-centre; `drop` comes from the object's `drop` property
    - `static Rect bodyAt(String type, Spot feet)`
    - `"shooter"` throws `UnsupportedOperationException` until Task 4
  - **`LevelValidator`:**
    - `KNOWN_ENEMY_TYPES = EnemyFactory.KNOWN_TYPES`
    - new rules: an enemy body overlapping solid tiles ("enemy at (x,y) overlaps solid tiles"), and a `drop` value other than `heart` ("unknown drop 'x'")
  - **New ASCII characters:**
    - `w` walker
    - `W` walker with `drop=heart`
    - `s` shooter
    - `h` heart pickup
    - each is a point object at `(tx*16+8, (ty+1)*16)`, with property `type` set to `walker`, `shooter` or `heart` (object types `enemy` or `pickup`)

- [ ] **Step 1: Write the failing tests.**
  - In `ProjectileTest`:
    - `fliesStraight`
    - `stopsAtWall`: fired toward a wall 3 tiles away, it's dead within 25 ticks
    - `expiresAfterMaxDistance`
  - In `WalkerTest`, use `TestLevels.ascii` maps with `w`:
    - `patrolsAndTurnsAtWall`: within 200 ticks, x goes down, then goes up after touching the left wall.
    - `turnsAtLedgeInsteadOfFalling`: on a 6-tile platform over a void, a walker stays on the platform for 600 ticks (`y` constant).
    - `twoSwingsKill`: `damage(1, ...)` twice, 13 ticks apart, makes `isDead()` true. `isRemovable()` becomes true 20 ticks later.
    - `stunnedWalkerIsPushedAway`: after a hit from its left, x increases for the next 12 ticks.
    - `hitBySwingOncePerSwing`: `hitBySwing(1)` is true, then false; `hitBySwing(2)` is true.
    - `factoryPlacesFeetOnSpot`: `create(point enemy type=walker at (40,64))` has hitbox bottom 64 and centre x 40, and `drop()` is null. With `drop=heart`, `drop()` is `"heart"`.
  - In `LevelValidatorTest`:
    - `walkerIsKnown` (no problems)
    - `flagsEnemyInsideSolid`
    - `flagsUnknownDrop`
    - the existing `flagsUnknownEnemyType` still uses `'dragon'`
  - In `AsciiTmxTest`: `enemyAndPickupChars` checks that `w`, `W`, `s` and `h` produce the objects above, and that only `W` carries `drop=heart`.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test`
  Expected: compile errors.

- [ ] **Step 3: Implement.**
  - `Walker.think` sets `vx = facing == RIGHT ? 0.6 : -0.6` and flips `facing` on a wall or a ledge.
  - Add the four characters to `AsciiTmx`.

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass, and `LevelFilesTest` is unchanged because no level uses the new characters yet.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/entities src/ambot/level/LevelValidator.java test/ambot/entities test/ambot/level/LevelValidatorTest.java test/ambot/tools
  git commit -m "feat(enemies): Enemy base with stun and drops, patrolling Walker, projectiles, enemy level characters"
  ```

### Task 4: Shooter

**Files:**
- Create: `src/ambot/entities/enemies/Shooter.java`
- Modify: `src/ambot/entities/enemies/EnemyFactory.java`
- Test: `test/ambot/entities/enemies/ShooterTest.java`

**Interfaces:**
- Consumes: `Enemy`, `EnemyWorld`, `Projectile` (Task 3).
- Produces:
  - **`Shooter`:**
    - 14×22, 2 health, never walks
    - faces the player whenever the player is within `RANGE_X = 160` px horizontally and `RANGE_Y = 48` px vertically (centre to centre)
    - fires the first shot `FIRST_SHOT_DELAY = 60` ticks after the player comes into range, then one every `FIRE_INTERVAL = 120` ticks while in range; the timer resets when the player leaves
    - the bolt starts at the front edge with its top at `MUZZLE_HEIGHT = 22` px above the Shooter's feet, moving at ±`2.5`
    - drawn as a grey `0x83769C` box with a barrel on the facing side
  - **`EnemyFactory`:** creates `"shooter"`.

- [ ] **Step 1: Write the failing tests** in `ShooterTest`.
  - `firesAfterDelayWhileInRange`: with the player 100 px to the right, no bolt at tick 59, one bolt at tick 60, the second at tick 180.
  - `doesNotFireOutOfRange`: the player 200 px away, 300 ticks, no bolts.
  - `facesPlayer`.
  - `boltFliesAtStandingHeight`: the bolt's vertical span lies inside a standing player's body (28 tall, feet on the same floor) and above a crouched one (18 tall).
  - `factoryCreatesShooter`.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.entities.enemies.ShooterTest`
  Expected: compile errors.

- [ ] **Step 3: Implement `Shooter` and the factory case.**

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/entities test/ambot/entities
  git commit -m "feat(enemies): Shooter firing bolts at standing height"
  ```

### Task 5: Heart pickups, heart icon and HUD

**Files:**
- Create: `src/ambot/entities/HeartPickup.java`, `src/ambot/ui/HeartIcon.java`, `src/ambot/ui/Hud.java`
- Test: `test/ambot/ui/HudTest.java`, `test/ambot/entities/HeartPickupTest.java`

**Interfaces:**
- Consumes: `Damageable` (Task 1).
- Produces:
  - **`HeartIcon`:** `static void draw(Graphics2D g, int x, int y, boolean full)` draws a 9×8 pixel heart, red `0xFF004D` when full or an outline in `0x5F574F` when empty. Constants `W = 9`, `H = 8`.
  - **`HeartPickup`** (extends `Entity`):
    - `HeartPickup(Spot feet)`, 10×9, standing on the spot
    - `void update()` bobs it ±2 px over 60 ticks; the hitbox doesn't move
    - `boolean tryCollect(Damageable who)`: heals 1 and returns true only when `who.health() < who.maxHealth()`
    - `boolean collected()`
  - **`Hud`:** `static void render(Graphics2D g, Damageable player)` draws `maxHealth()` hearts at (8, 8), 12 px apart, with the first `health()` of them full.

- [ ] **Step 1: Write the failing tests.**
  - `HudTest.drawsFullThenEmptyHearts`: rendering a fake `Damageable` with 2 of 3 health into a 640×360 image gives heart 1 and heart 2 a red pixel at their centres, and heart 3 none.
  - `HeartPickupTest.healsOnlyWhenHurt`: at full health `tryCollect` is false and the pickup isn't collected. After damage it's true, health goes up by 1, and the pickup is collected.
  - `HeartPickupTest.bobKeepsHitboxStill`.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test`
  Expected: compile errors.

- [ ] **Step 3: Implement.**

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/entities src/ambot/ui test/ambot/ui test/ambot/entities
  git commit -m "feat(ui): heart pickups, heart icon and hearts HUD"
  ```

### Task 6: Actors, the combat resolution

**Files:**
- Create: `src/ambot/entities/Actors.java`
- Test: `test/ambot/entities/ActorsTest.java`

**Interfaces:**
- Consumes: `Player.attackBox()` and `swingId()` (Task 2), `Enemy`, `EnemyFactory`, `EnemyWorld`, `Projectile` (Task 3), `Shooter` (Task 4), `HeartPickup` (Task 5), `Level.objects(...)`.
- Produces:
  - **Construction:** `Actors(Level level)` spawns an enemy for every `enemy` object and a `HeartPickup` for every `pickup` object whose type is `heart`.
  - **`void update(Player player, TileMap map)`** runs in this order each tick:
    1. enemies update (shots join the projectile list)
    2. projectiles update
    3. pickups update
    4. sword: every live enemy that overlaps `attackBox()` and returns true from `hitBySwing(swingId())` takes `damage(1, playerCentreX)`
    5. contact: every live enemy overlapping the player body calls `player.damage(contactDamage, enemyCentreX)`
    6. bolts: every live bolt overlapping the player body calls `player.damage(1, boltCentreX)`, and the bolt is killed even if the player is invincible
    7. pickups: `tryCollect(player)`
    8. removal: dead projectiles and collected pickups are removed; removable enemies are removed, and those with `drop == "heart"` leave a `HeartPickup` at their feet
  - **Rendering:** `void render(Graphics2D g, Rect view)` draws pickups, then enemies, then projectiles that intersect `view` (grown by 32 px).
  - **Accessors:** `List<Enemy> enemies()`, `List<Projectile> projectiles()`, `List<HeartPickup> pickups()`, all read-only views.

- [ ] **Step 1: Write the failing tests** in `ActorsTest`, using `TestLevels.ascii` levels with `w`, `W`, `s` and `h`, and drive a real `Player`.
  - Sword:
    - `swingHitsEachEnemyOnce`: a walker inside the attack box for all 10 active ticks loses exactly 1 health. A second swing takes it to 0.
    - `swingMissesEnemyBehind`.
  - Contact and bolts:
    - `contactDamagesPlayer`.
    - `twoSourcesSameTickCostOneHeart`: a walker and a bolt both overlapping the player on the same tick leave 2 hearts.
    - `boltHitsStandingPlayerAndIsRemoved`.
    - `crouchingPlayerDodgesBolt`: a player holding DOWN takes no damage from a shooter's bolt on flat ground, and the bolt flies on past.
  - Pickups and drops:
    - `pickupHealsHurtPlayerOnly`.
    - `deadEnemyWithHeartDropLeavesPickup`: the pickup appears where the enemy stood, after `REMOVE_TICKS`.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test -Select ambot.entities.ActorsTest`
  Expected: compile errors.

- [ ] **Step 3: Implement `Actors`.**

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/entities/Actors.java test/ambot/entities/ActorsTest.java
  git commit -m "feat(entities): Actors resolves sword, contact, bolt and pickup hits each tick"
  ```

### Task 7: Playing with health: hazards, pits, Game Over, Retry, HUD and input grace

**Files:**
- Create: `src/ambot/states/GameOverState.java`, `src/ambot/states/InputGrace.java`
- Modify: `src/ambot/states/PlayingState.java`, `src/ambot/states/LevelCompleteState.java`
- Test: `test/ambot/states/PlayingStateTest.java`, `test/ambot/states/GameOverStateTest.java`

**Interfaces:**
- Consumes: `Actors` (Task 6), `Hud` (Task 5), and `Player.damage`, `kill`, `deathFinished`, `restoreHealth` (Task 1).
- Produces:
  - **`InputGrace`:** `InputGrace(int ticks)`, `boolean tick()` (true once the grace has passed), `GRACE_TICKS = 20`.
  - **`GameOverState`:**
    - `GameOverState(GameContext ctx, PlayingState playing)`, an overlay
    - its menu has "Retry" (`playing.retry()` then pop) and "Main Menu" (`replaceAll(new MainMenuState(ctx))`)
    - it ignores input for `GRACE_TICKS`
    - it draws "GAME OVER" in red `0xFF004D`
  - **`LevelCompleteState`:** ignores CONFIRM for `GRACE_TICKS`.
  - **`PlayingState`:**
    - `actors()` and `void retry()`
    - `deaths()` now counts Game Overs
    - `update` runs in this order:
      1. pause
      2. `player.update`
      3. `actors.update`
      4. checkpoints
      5. a hazard costs `player.damage(1, centreX + (facing == RIGHT ? 8 : -8))`, so the player is knocked backwards
      6. a pit (`y > heightPx + 64`) calls `player.kill()` if the player isn't dead yet
      7. when `player.deathFinished()` and no Game Over has opened: `deaths++` and push `GameOverState` once
      8. the exit pushes `LevelCompleteState`, only while the player is alive
      9. the camera follows
    - **`retry()`:**
      - `player.respawn` at the respawn point
      - `restoreHealth`
      - fresh `new Actors(level)`
      - camera snap, and the Game Over flag cleared
    - **`render`:** draws actors between the markers and the player, then `Hud.render` after translating back. `Esc: pause` moves to the top-right corner.

- [ ] **Step 1: Write the failing tests.**
  - In `PlayingStateTest`, these replace the M2 `hazardSendsBackToRespawnPoint` and `fallingOutOfMapRespawns`:
    - `hazardCostsOneHeartAndKnocksBack`: health is 2, the player isn't back at the spawn, and `deaths() == 0`.
    - `pitIsGameOver`: after walking into the pit, `GameOverState` is on top within `DEATH_TICKS + 30` ticks and `deaths() == 1`.
    - `pitWhileInvincibleIsStillGameOver`.
    - `gameOverOpensOnce`: 200 more ticks leave exactly one `GameOverState`, with `states().size() == 2`.
    - `retryRestoresHeartsEnemiesAndClearsBolts`: after damage and a pit death in a level with a walker, Retry gives 3 hearts, `IDLE`, not invincible, and the original number of enemies. No projectiles remain, and the player stands at the respawn point.
    - `exitIgnoredWhileDead`.
  - In `GameOverStateTest`:
    - `confirmIgnoredDuringGrace`: CONFIRM on the first tick does nothing. At tick 20 it retries.
    - `mainMenuOption`.
    - `levelCompleteHasGraceToo`.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test`
  Expected: compile errors, and the old M2 behaviour fails the new hazard and pit tests.

- [ ] **Step 3: Implement.**

- [ ] **Step 4: Run the whole suite.** `RouteTest` must still pass for p1–p3, because the routes take no damage.
  Run: `.\build.cmd test`
  Expected: all pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/states test/ambot/states
  git commit -m "feat(states): hearts in play - hazards hurt, pits kill, Game Over with Retry, HUD, input grace"
  ```

### Task 8: Crash screen in place of a frozen window

**Files:**
- Create: `src/ambot/states/CrashState.java`
- Modify: `src/ambot/core/GameLoop.java`, `src/ambot/core/Game.java`, `src/ambot/states/StateManager.java`
- Test: `test/ambot/core/GameLoopTest.java`, `test/ambot/states/CrashStateTest.java`

**Interfaces:**
- Produces:
  - **`GameLoop`:**
    - `GameLoop(FixedStep, Runnable tick, Runnable render, Consumer<RuntimeException> onCrash)`; the 3-argument form rethrows
    - a `RuntimeException` from `tick` or `render` goes to `onCrash` and the loop keeps running
    - a second exception within 1 second of the first stops the loop
  - **`StateManager`:** `void reset(GameState s)` clears the stack and any pending changes at once, then enters `s`.
  - **`CrashState`:**
    - `CrashState(GameContext ctx, RuntimeException e)`
    - it shows "Something went wrong.", the exception's simple class name, its message (wrapped at 70 characters, at most 3 lines), and "Enter: main menu   Esc: quit"
    - Enter calls `replaceAll(new MainMenuState(ctx))`; Esc or BACK calls `ctx.quit()`
  - **`Game`:** passes `e -> { e.printStackTrace(); states.reset(new CrashState(this, e)); }`.

- [ ] **Step 1: Write the failing tests.**
  - `GameLoopTest.crashGoesToHandlerAndLoopKeepsRendering`: a tick that throws once calls the handler once (checked with a `CountDownLatch`, 2-second timeout), and the render counter keeps increasing afterwards. Stop the loop at the end.
  - `GameLoopTest.secondCrashWithinASecondStopsLoop`.
  - `CrashStateTest.showsMessageAndReturnsToMenu`: rendering doesn't throw. CONFIRM leads to `MainMenuState`, and BACK calls `quit`.
  - `CrashStateTest.resetDropsPendingChanges`: pending pushes queued before `reset` never apply.

- [ ] **Step 2: Run them and watch them fail.**
  Run: `.\build.cmd test`
  Expected: compile errors.

- [ ] **Step 3: Implement.**

- [ ] **Step 4: Run the whole suite.**
  Run: `.\build.cmd test`
  Expected: all pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add src/ambot/core src/ambot/states test/ambot/core test/ambot/states
  git commit -m "feat(core): crash screen instead of a silently frozen window"
  ```

### Task 9: "First Blood" fight level, route, smoke run and docs

**Files:**
- Create: `levels-src/placeholder/p4.txt`, `levels-src/placeholder/p4.route`, `resources/levels/placeholder/p4.tmx` (generated)
- Modify: `src/ambot/level/LevelCatalog.java` (adds p4), `test/ambot/tools/SmokeRun.java`, `docs/LEVELS.md`, `docs/ROADMAP.md`, `docs/ASSETS.md`
- Test: the existing `LevelFilesTest` and `RouteTest` now cover p4

**Interfaces:**
- Consumes: everything above.
- Produces: `LevelCatalog.PLACEHOLDER_PATHS` gains `"/levels/placeholder/p4.tmx"` as the fourth level.

- [ ] **Step 1: Add p4 to the catalog and watch the level-file test fail.**
  Run: `.\build.cmd test -Select ambot.level.LevelFilesTest`
  Expected: FAIL, because `p4.tmx` isn't there yet.

- [ ] **Step 2: Design `p4.txt`, "First Blood".** It's about 100×23 with ground at row 20, and stays within the `LEVELS.md` design limits. In order:
  1. one walker on flat ground (attack it)
  2. a `W` on a raised 8-tile platform (attack from the platform; it drops a heart)
  3. a shooter at the end of a flat corridor about 8 tiles long (crouch or jump the bolts, then get close and attack)
  4. a checkpoint
  5. a 2-tile spike strip on the ground (now costs a heart, not a respawn)
  6. a heart pickup after the spikes
  7. two walkers patrolling a 10-tile area together
  8. the exit

  Then run `.\build.cmd levels` and `.\build.cmd preview`.
  Expected: `p4.png` matches this description.

- [ ] **Step 3: Write `p4.route`.** It must finish the level with `deaths() == 0`; losing hearts is allowed. Find it by computing positions (2 px per tick while running) and by reading `RouteTest`'s failure messages.
  Run: `.\build.cmd test`
  Expected: all pass, including 4 `RouteTest` cases and `LevelFilesTest`.

- [ ] **Step 4: Smoke run.**
  - Run: `.\build.cmd smoke`
  - Expected: UPS 59–61, FPS ≥ 58, CPU < 0.5 cores, and the HUD's three hearts visible on the playing frames in `contact.png`.

- [ ] **Step 5: Update the docs.**
  - `docs/LEVELS.md`: the four new characters, the enemy and pickup types, the `drop` property, and hazards now costing a heart while pits mean Game Over.
  - `docs/ASSETS.md`: a placeholder enemy, bolt and heart art row ("own work, Java2D, placeholder until M4").
  - `docs/ROADMAP.md`: M3 is **done**, with the date.

- [ ] **Step 6: Commit.**
  ```bash
  git add -A src test levels-src resources/levels docs
  git commit -m "feat(level): First Blood fight level; M3 complete"
  ```

- [ ] **Step 7: Hand off for review.** The user plays from New Game (`.\build.cmd run`): First Blood is the fourth level, after the three M2 levels. Wait for their verdict before finishing the branch.
