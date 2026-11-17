# AMBOT Restoration — Roadmap

Route B: rebuild the lost version (menu, 3 worlds, a boss per world) one world at a time on a shared engine.
Design: [`superpowers/specs/2026-11-17-ambot-restoration-design.md`](superpowers/specs/2026-11-17-ambot-restoration-design.md) · Current state: [`ANALYSIS.md`](ANALYSIS.md)

Each milestone gets its own detailed plan when it starts, because later plans build on the real code from earlier ones.

| # | Milestone | Ends with | Plan | Status |
|---|---|---|---|---|
| M0 | Repo, `build.ps1`, the six prototype bugs fixed in place | tag `v0.0`, a working restored prototype | [M0–M1 plan](superpowers/plans/2026-11-17-ambot-m0-m1-foundation.md) | **done 2026-11-17** (tag `v0.0`) |
| M1 | Engine core: 640×360 scaled canvas, sleeping 60 UPS loop, input, state stack, menus, sprite and animation system, solid-tile collision, player physics and state machine, test room | new engine replaces the prototype code | same plan | **done 2026-11-17** (tag `m1`; smoke: 61 UPS, 61 FPS, 0.21 CPU cores) |
| M2 | Levels: Tiled `.tmx` loader, `LevelValidator`, level preview renderer, one-way platforms, hazards, camera, parallax, checkpoints, exit, placeholder levels | levels load from files | to write at M2 | — |
| M3 | Combat: `Damageable`, hearts, knockback, invincibility, attack hitboxes, World 1 enemies, HUD, GameOver and retry | a fight in a placeholder level | to write at M3 | — |
| M4 | World 1: assets picked and credited, 3 levels and Boss 1, audio, saving, options, credits, `jar`/`package` build tasks | **v0.1**, a complete short game (portable zip) | to write at M4 | — |
| M5 | World 2: slide and ledge-grab, enemies, 3 levels, Boss 2 | **v0.2** | to write at M5 | — |
| M6 | World 3: attack combos, enemies, 3 levels, final boss, ending | **v1.0** | to write at M6 | — |

Rules carried across every milestone:
- World N+1 doesn't start until World N is released.
- Every asset goes into `docs/ASSETS.md`.
- Ask for the original team's names before v0.1.
- The user reviews the result of every milestone.
