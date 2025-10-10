# Project Status & Next Steps for Multiplayer Space Invaders

## High-Level Summary
- The codebase originally targeted single-player gameplay with local-only state management (`org.newdawn.spaceinvaders.gameplay`). Recent work introduced a multiplayer architecture that separates client and server logic under the `multyplay` and `server.game` packages.
- Current multiplayer flow: players enter a lobby (`room` package), receive `GAME_INIT`/`GAME_STATE` messages from the server, and render snapshots via `MultiplayerGameCanvas`. Server authoritative loop (`ServerMultiplayerGame`) handles enemy spawning, inputs, and snapshot broadcasting.
- Many gameplay systems (HP, skills, random drops, round transitions) still assume single-player semantics and require per-player refactors to behave identically to single-player UX/UI.

## Package Overview
- `org.newdawn.spaceinvaders.multyplay.core`
  - `MultiplayerGameCanvas`: client-side canvas for rendering + local input. Now supports remote snapshots but much logic (skills, HP, rewards) still only handles the local player.
  - `MultiplayerGameContext`: shared interface for server/client. Recently extended with player-aware overloads (e.g., `notifyAlienKilled(String playerId)`). Implementations need to honor these overloads.
  - `MultiplayerSkillManager`: manages skill buffs/inventory but still single-instance in client; server needs per-player managers.
- `org.newdawn.spaceinvaders.multyplay.entity`
  - Entities such as `ShotEntity`, `MissileEntity`, `AlienEntity`, `BossEntity` must set `ownerId` and use it when notifying the context. Some adjustments done for shots; missiles/boss still need player-aware damage/reward handling.
- `org.newdawn.spaceinvaders.multyplay.state`
  - `MultiplayerGameStateManager`: tracks game state, but many getters/setters (HP, skill points) still return local-player values. Needs player-aware methods.
- `org.newdawn.spaceinvaders.multyplay.net`
  - Snapshot DTOs + Room adapter. Already capable of sending entity metadata.
- `org.newdawn.spaceinvaders/server/game`
  - `ServerMultiplayerGame`: server authoritative simulation. Now supports per-player runtime map but still reuses single `MultiplayerSkillManager`, global HP, etc. Must expand for per-player skill/HP logic.
  - `ServerGameSession`: orchestrates sessions. Should register/deregister players and provide a round-ready state machine eventually.

## Recent Multiplayer Changes (already applied)
1. Server simulation clones the single-player gameplay into `ServerMultiplayerGame` and now spawns a ship per player, applying individual input, movement, and firing. Closest ship is targeted by alien fire.
2. Authoritative snapshots now carry metadata (owner/type). `MultiplayerGameCanvas` renders remote snapshots using helper entities (`RemoteShotEntity`, etc.) to get correct visuals.
3. `RoomLobbyCanvas` transitions to `MultiplayerGameCanvas` via `SpaceInvadersApp.startMultiplayerGame`, wiring `RoomGameNetworkAdapter` and handling readiness handshake.
4. Input streaming: client sends `PlayerInput` messages, server echoes state ticks.
5. Basic per-player structures exist (`PlayerRuntime` maps, skill inventory placeholders) but skill/HP logic is still global.

## Remaining Work (to match single-player UX/UI/Logic)
1. **Player-Scoped State & Skill Management**
   - Server (`ServerMultiplayerGame`): replace global `skillManager` with `skillManager(playerId)` helper (per-player `MultiplayerSkillManager`). Update start/reset to initialize each manager, and update loops (`update`, `tryToFire`, `notifyAlienKilled`, etc.) to use the correct manager via player ID.
   - Client (`MultiplayerGameCanvas`): use player ID aware context callbacks; ensure remote players do not alter local skill state. Extend HUD to read per-player data from snapshots for remote displays.
2. **Entity Ownership & Combat Rewards**
   - Ensure `ShotEntity`, `MissileEntity`, `BossEntity` set `ownerId` and pass it to `notifyAlienKilled(playerId)`, `notifyDeath(playerId)`, `addSkillPoints(playerId, …)`. Adjust collisions to differentiate between local/remote players.
   - For skills/drops: server `notifyAlienKilled` should reward the killer’s state (skill points, drop chance). Client should only show local popups but must accept drop entities from the server.
3. **HP & Skill Point Synchronization**
   - Extend `MultiplayerGameStateManager` with `takeDamage(String playerId)`, `addSkillPoints(String playerId)`, etc., and ensure snapshots carry per-player HP/skill values. `GameSnapshot` already has a `players` map.
   - Client HUD: render the local player’s HP/skills from the snapshot map; optional: list other players’ states.
4. **Round Transition Ready/Chat Overlay**
   - Design server state machine for “round complete” -> awaiting players -> next round. Add new protocol messages (e.g., `GAME_ROUND_READY`, `READY_TOGGLE`).
   - Client: overlay (semi-transparent) showing player list, ready status, chat (reuse lobby components). Start next round automatically when all ready.
5. **Consistency & Clean-up**
   - Remove lingering `@Override @Override` duplicates, ensure all new interface methods are implemented without local-only fallbacks.
   - Normalize spawn positions (single player center, multi evenly spaced) for consistent visual parity.
   - Sync boss kill rewards/HP logic: only local UI shows player messages, but server updates should match single-player behavior.
6. **Testing**
   - Test with two+ clients: each ship responds only to its own controls, HP decreases independently when hit, skill point increments match kills, random drops spawn near correct player. 
   - Confirm entity sizes/positions match single-player resources (no scaling issues).

## File-Level Notes (key files)
- `src/main/java/org/newdawn/spaceinvaders/multyplay/core/MultiplayerGameContext.java` – now contains player-aware overloads; ensure all implementations use them.
- `.../multyplay/core/MultiplayerGameCanvas.java` – client logic; adjust per-player rewards, HP, skills, remote entity rendering. Currently local-centric.
- `.../server/game/ServerMultiplayerGame.java` – authoritative server; must finalize per-player skill/HP, spawn, drop logic.
- `.../multyplay/entity/ShotEntity.java`, `MissileEntity.java`, `BossEntity.java` – need correct `ownerId` usage, call new context APIs.
- `.../multyplay/state/MultiplayerGameStateManager.java` – add per-player HP/skill mutators, align with server snapshots.
- `.../room/RoomLobbyCanvas.java`, `SpaceInvadersApp.java` – lobby flow and screen transitions ready for round-ready overlay extension.

## Implementation Guidance
- **Server first**: refactor `ServerMultiplayerGame` to use per-player managers and state. That includes health, skill points, inventory, drops, and death handling.
- **Client**: update `MultiplayerGameCanvas` to interpret snapshots and own-player adjustments correctly (HP, skills). Remote-only actions should not affect local state.
- **Networking**: consider new messages for ready states; extend `RoomGameNetworkAdapter` to handle them.
- **UI**: replicate single-player messaging & overlays (e.g., “Press any key” replaced by ready overlay). Use consistent fonts, colors from the single-player UI classes.

With these steps, multiplayer gameplay should mirror single-player UX/UI/logic while supporting multiple players synchronously.
