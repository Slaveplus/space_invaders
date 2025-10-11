# Project Handoff for Next Codex

## Repository Snapshot (2025-02-15)
- Branch: `merged` (tracking `origin/merged`)
- Build status: `mvn -q -DskipTests compile` passes after the latest changes.
- Uncommitted files:
  - `src/main/java/org/newdawn/spaceinvaders/gameplay/Game.java`
  - `src/main/java/org/newdawn/spaceinvaders/mainmenu/MainMenu.java` (legacy edits from previous tasks, untouched in the latest change)
  - `docs/multiplayer_alignment.md`
  - `docs/nextllm.md` (this handoff note)

## Recent Work Overview

### Shared Gameplay Abstraction
- Introduced `gameplay/core/GameplayContext` and `SharedGameplayCoordinator` to factor round / spawn / reward logic that both single-player and multiplayer will eventually share.
- `Game` now implements `GameplayContext` and delegates several responsibilities to the coordinator (round initialization, alien kill bookkeeping, near-monster transitions). No behavioural changes expected in single-player; integration remains in-progress for multiplayer.
- The coordinator currently manages:
  - Round initialization (`startNewGame`, `initializeRound`).
  - Handling alien kills (rewarding skill points, scheduling drops, speed-ups, and triggering wave clears).
  - Handling near-monster wave completion extending into boss waves.
  - Handling round clears (rewards, background changes, game completion messaging).

### Multiplayer Visual Parity
- Added `multyplay/entity/NearEntity` plus supporting shot handling so near-wave phases mirror the single-player flow.
- `MultiplayerGameCanvas` and `ServerMultiplayerGame` now spawn near waves/bosses using the updated sprites, advance to all 8 rounds, and update backgrounds/messages in lockstep.
- Multiplayer HUD adopts the Kostar font styling to align with the refreshed single-player UI.
- Introduced `SharedMultiplayerRoundCoordinator` so both the canvas and server share near/boss spawning and background updates.
- Coordinator now handles near/boss rewards (skill points, drop checks, alien counts) to keep client and server outcomes aligned.
- Server intermission descriptors now come from the shared coordinator so player-facing messaging is consistent across modes.
- Multiplayer HUD mirrors single-player coin popups and snapshot payloads now include per-player coin totals for remote clients.

### What Has *Not* Been Changed Yet
- `Game` still contains substantial gameplay logic (skill usage, coin display, entity creation). Only the entry points for rounds & kill flow were moved.
- Multiplayer runtime (`multyplay/core/MultiplayerGameCanvas`, server implementation, etc.) still references its own state/skill management. No shared coordinator usage yet.
- Many helper methods in `Game` remain untouched and duplicate logic (e.g. `spawnNearMonsters`, `spawnBoss`) even though the coordinator now does similar work.

## Known Issues / Follow-Ups

1. **Duplicate logic remains in `Game`:**
   - `spawnNearMonsters()`, `spawnBoss()`, `notifyWin()`, `notifyBossDefeated()` etc. currently coexist with the coordinator, leading to dead code or redundant behaviour. Need to remove or redirect these methods to the coordinator to avoid double-spawning or diverging behaviour.
   - Some methods still manipulate entities directly (`gameStateManager.getEntities().add(...)`) while the coordinator does similar work.

2. **GameplayContext coverage:**
   - `GameplayContext` currently exposes factory helpers returning generic `Entity`. For clarity, consider returning concrete typed classes (`NearEntity`, `BossEntity`, etc.) or update the coordinator factories to accept context-specific builders.
   - `GameplayContext` now has `getActiveEntities()` / `getPendingRemovals()` rather than direct `ArrayList` access. Ensure any future migrations respect the interface to avoid leaking implementation details.

3. **SkillManager coupling:**
   - `SkillManager` still calls `game.getEntities()` (now the context method). Need to audit to ensure multiplayer adaptation will not assume `ArrayList` specifics. Type inference uses raw class; consider generalizing the skill manager to accept `GameplayContext` as well or to operate on lists returned by the new interface.

4. **Multiplayer integration pending:**
   - `MultiplayerGameCanvas` and `ServerMultiplayerGame` still use `MultiplayerGameStateManager` + custom logic. Next steps involve creating an adapter that implements `GameplayContext` for multiplayer and wiring the coordinator there.
   - Need to double-check remote snapshots, per-player state, and commands so that they play nicely with the shared logic.

5. **MainMenu pending edits:**
   - The branch still has earlier modifications for the gameplay submenu. Those files remain staged/uncommitted but were not touched in the latest change.

## Next Steps (Suggested Order)

1. **Single-player cleanup:**
   - Replace direct round-spawn methods (`spawnNearMonsters`, `spawnBoss`, `notifyWin`, `notifyBossDefeated`, `checkAllNearMonstersDefeated`) with calls to the coordinator (or remove them when redundant).
   - Ensure `Game` is only responsible for UI and input while the coordinator handles all entity management.
   - Update tests or add manual verification for near/boss transitions to confirm no regressions.

2. **Remote reward visuals (completed):**
   - Coin popup events now propagate over the network so non-host clients see the same floating indicators.
   - Follow-up: add regression coverage (manual or automated) to confirm popups stay aligned under latency or packet loss.

3. **SkillManager refactor (in progress):**
   - Shared drop table now feeds both single-player and multiplayer; piercing skill removed from multiplayer to mirror the single-player kit.
   - Remaining: push `SkillManager` fully behind a shared context-aware abstraction and clean up residual single-player-only helpers.

4. **Snapshot metadata:**
   - Audit snapshot payloads (near HP, boss phases, reward timers) so clients render the same state received from the server.
   - Extend descriptors as needed to cover new HUD cues once reward feedback is unified.

5. **Testing / Validation:**
   - When both modes share the coordinator, add regression checks (manual or automated) covering all 8 rounds, near/boss transitions, and skill/coin behaviour.

## Files of Interest

- `src/main/java/org/newdawn/spaceinvaders/gameplay/Game.java`
  - Entry point for the single-player loop and now an implementation of `GameplayContext`.
- `src/main/java/org/newdawn/spaceinvaders/gameplay/core/GameplayContext.java`
  - Minimal interface representing the environment for shared gameplay logic.
- `src/main/java/org/newdawn/spaceinvaders/gameplay/core/SharedGameplayCoordinator.java`
  - Houses the consolidated round/kill logic now used by `Game`.
- `src/main/java/org/newdawn/spaceinvaders/multyplay/core/MultiplayerGameCanvas.java`
  - Still contains its own logic; will need to adopt the coordinator in future work.
- `docs/multiplayer_alignment.md`
  - Tracks progress across phases; now includes Phase 2 notes.
- `docs/nextllm.md`
  - This hand-off note.

## Build / Test Commands

- Compile (current state): `mvn -q -DskipTests compile`
- Tests have not been run (`-DskipTests` used). Recommend a full test pass after the next phase of changes.

## Miscellaneous Notes

- The codebase relies heavily on mutable `ArrayList` references; when generalizing to the coordinator, prefer returning `List` to reduce coupling.
- Keep an eye on `MainMenu` staged changes; they may need to be either finished or cleaned up if unrelated to the gameplay refactor.
- Coordinate with any additional network or UI updates before refactoring multiplayer to ensure no conflicting assumptions.

Good luck! Feel free to ping if any context is missing.

## Challenges Encountered During Coordinator Integration
- Attempting to wire multiplayer directly to the shared coordinator exposed structural gaps (no NearEntity analogue, different spawn logic tied to networking).
- Adapting the coordinator requires either introducing multiplayer equivalents of single-player entities or generalising the coordinator interface further.
- To avoid breaking the current build, changes were reverted; follow-up work is summarised in README above.
