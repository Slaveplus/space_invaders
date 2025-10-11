# Multiplayer Gameplay Alignment – Phase 1

This document captures the current assessment needed before lifting the single-player gameplay loop into the multiplayer stack.

## Key Components To Unify
- **State management** – `GameStateManager` vs `MultiplayerGameStateManager`. Both track rounds, entities, and skill stats, but the multiplayer version still owns its own fields for the same data.
- **Skill orchestration** – `SkillManager` and `MultiplayerSkillManager` diverged (extra piercing skill, inventory bookkeeping). We intend to converge them on one implementation.
- **Round lifecycle** – Single-player handles near / boss rounds, rewards, and background changes inside `Game`. Multiplayer spreads this between `MultiplayerGameCanvas` and `ServerMultiplayerGame`.

## Phase 1 Deliverables
1. Introduce a tiny `GameplayContext` interface that exposes the minimal hooks a shared coordinator will need.  
2. Provide a placeholder `SharedGameplayCoordinator` that accepts the context. No behaviour changes yet.
3. Use this skeleton in future phases to migrate spawning, reward handling, and skill application without touching networking code prematurely.

This incremental approach keeps the first step safe: no runtime wiring has changed, but we now have a shared abstraction to target in upcoming phases.

## Phase 2 Progress
- Implemented `SharedGameplayCoordinator` with initial round/kill lifecycle logic.
- `Game` now implements `GameplayContext` and delegates start-up, round transitions, and alien-kill bookkeeping to the coordinator.
- Multiplayer code remains untouched for now; future phases will introduce an adapter so `MultiplayerGameCanvas` and the server reuse the same coordinator.
- Boss defeat rewards and subsequent round initialisation now flow through the coordinator, allowing the single-player `Game` to drop its bespoke spawn helpers.
- Multiplayer canvas/server now spawn near waves and bosses using the updated sprites, and round transitions mirror single-player messaging/backgrounds.
- Added `SharedMultiplayerRoundCoordinator` to consolidate round layout (near vs boss, background changes) across the multiplayer canvas and server implementations.
- Coordinator now owns near/boss reward bookkeeping (skill points, drops, alien count) so both multiplayer runtime paths stay in sync.
- Server intermission messaging is now derived from the shared coordinator, keeping wave/boss transitions aligned between authoritative and client runtimes.
- Multiplayer HUD now mirrors coin popups/total tracking from single-player, and snapshots include per-player coin totals for remote clients.
