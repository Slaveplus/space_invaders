# Space Invaders Multiplayer Design

## 1. Goals
- Mirror single-player gameplay experience (mechanics, UI composition, content) while allowing multiple players to clear the same stage cooperatively.
- Avoid invasive changes to `org.newdawn.spaceinvaders.gameplay`; use cloning/adapters in the `multyplay` package.
- Provide a server-authoritative simulation that tolerates 150 ms round-trip latency with client-side smoothing.
- Maintain compatibility with existing lobby (`room`) and server (`server`) packages via adapters.

## 2. Non-Goals
- Competitive PvP balancing or divergent mechanics.
- Persisting multiplayer progression beyond the existing user inventory schemas.
- Cross-platform networking optimizations (UDP, NAT traversal).

## 3. Architecture Overview
```
Client JVM (per player)
 └─ SpaceInvadersApp
     └─ MainMenu → RoomList → RoomLobby → MultiGameCanvas (new)
         ├─ MultiGameRuntime
         │   ├─ MultiInputManager (local inputs → network)
         │   ├─ MultiGameController (simulation + interpolation)
         │   ├─ MultiUIRenderer, MultiBackgroundRenderer
         │   └─ MultiNetworkAdapter (wraps GameClient transport)
         └─ LocalLoopbackMultiAdapter (for offline testing)

Dedicated Game Server JVM
 └─ GameServer
     └─ MultiServerAdapter (bridges text protocol ↔ binary snapshots)
         ├─ MultiGameSession (per room simulation)
         │   ├─ MultiGameState (authoritative state copy)
         │   └─ SnapshotCodec (serialize snapshots/events)
         └─ TickScheduler (shared)
```

## 4. Package Layout
- `multyplay.core`
  - `MultiGameRuntime`: owns render loop integration, dispatches updates based on snapshots.
  - `MultiGameController`: authoritative state consumer; applies interpolation/extrapolation.
  - `MultiGameCanvas`: Swing canvas mirroring `gameplay.Game` drawing contract.
- `multyplay.system`
  - `MultiInputManager`: captures keyboard/mouse, builds `PlayerInputMsg` stream and prediction state.
  - `PredictionBuffer`, `InterpolationQueue`: utility classes to smooth remote entities.
- `multyplay.entity`
  - Cloned entities from `gameplay.entity` (`AlienEntity`, `ShipEntity`, etc.) updated to be deterministic and decoupled from singletons.
  - `EntitySnapshot`, `SnapshotDiff` for delta compression hooks.
- `multyplay.state`
  - `MultiGameState`: authoritative state mirror (per session).
  - `PlayerRuntimeState`: per-player scalars, cooldowns, score.
- `multyplay.net`
  - `MultiNetworkAdapter`: high-level API analogous to `gameplay.net.GameNetworkAdapter` but socket-aware.
  - `MultiClientAdapter`: wraps `room.GameClient`; handles snapshot/event channels.
  - `MultiServerAdapter`: plugs into `server.GameServer`; manages per-room sessions.
  - `LocalLoopbackMultiAdapter`: single-process deterministic test adapter.
  - Message records: `PlayerInputMsg`, `GameSnapshotMsg`, `GameEventMsg`.
  - `SnapshotCodec`: binary serialization helper (DataInput/DataOutput based).
- `multyplay.ui`
  - `MultiUIRenderer`, `MultiBackgroundRenderer`, `RemotePlayerPanel`, HUD widgets.

## 5. Networking Model
- **Authority**: Server owns physics and spawn logic. Clients send untrusted inputs, render interpolated snapshots.
- **Tick rate**: Server simulation at 60 Hz; snapshots broadcast at 20 Hz (every 3 ticks) with delta compression roadmap.
- **Messages**
  - `PlayerInputMsg`: `{roomId, playerId, localSeq, pressed, released, analogs, aimAngle, skillActivations}`
  - `GameSnapshotMsg`: `{roomId, tick, timestamp, entitySnapshots[], playerStates[], events[]}`
  - `GameEventMsg`: categorical notifications (chat, boss spawn, round change).
- **Transport**: Reuse text TCP connection from `GameClient` by introducing base64 payloads prefixed with new commands (`MULTI_INPUT`, `MULTI_SNAPSHOT`, etc.). Server adapter handles translation.
- **Prediction**: Clients keep ring buffer of un-acked inputs; when snapshot arrives with `lastProcessedSeq`, controller replays predicted inputs (client-side replay).
- **Interpolation**: Entities maintain two most recent snapshots; render positions/time-smoothed with `alpha = (now - snapshotA.time)/(snapshotB.time - snapshotA.time)` capped to 100 ms.

## 6. Client Runtime Flow
1. `SpaceInvadersApp.showRoomLobby` receives `ROOM_STATE` with `single=0`; once `GAME_START` arrives, navigator instantiates `MultiGameCanvas`.
2. `MultiGameCanvas` creates `MultiGameRuntime` with `MultiClientAdapter` configured from active `GameClient` and room metadata.
3. Runtime registers listeners, starts a dedicated update thread (or piggybacks on app loop) that:
   - Polls inputs from `MultiInputManager` every frame, sends `PlayerInputMsg`.
   - Advances `MultiGameController` with latest `GameSnapshotMsg`.
   - Requests repaint; render path draws background, entities, UI identical to single-player.
4. When connection drops or server issues `HOST_LEFT`, runtime signals navigator to show lobby/main menu.

## 7. Server Runtime Flow
1. `MultiServerAdapter` subscribes to `GameServer` events (`ROOM_STATE`, `START_GAME`). When a room flagged as multi starts, adapter creates `MultiGameSession`.
2. `MultiGameSession` spawns deterministic clones of single-player state (`MultiGameState`, entity set) and executes ticks via shared `TickScheduler`.
3. Player inputs forwarded from `ClientConnection` as `PlayerInputMsg` are queued into the session; processing occurs next tick.
4. After each tick, session produces `GameSnapshotMsg` with current state and pushes to all room participants via server's `sendToRoom` using compact serialization (base64 string field `payload`).
5. Session handles disconnects; if host leaves or all players exit, session shuts down, freeing resources.

## 8. Data Conversion Strategy
- Clone entity logic from `gameplay.entity` into `multyplay.entity`, replacing direct references to `Game` with interfaces (`EntityContext`) that expose minimal callbacks (spawn projectile, play sound, broadcast event).
- Resource loading (sprites, sounds) reuses same paths; caches shared through static loaders.
- `EntitySnapshot` records `{id, type, x, y, vx, vy, hp, stateFlags}`; mapping table maintained in `SnapshotCodec`.
- `MultiGameState` reuses `GameStateManager` semantics but ensures deterministic RNG via seeded `Random` per session.

## 9. Lobby & Navigation Integration
- Extend `RoomLobbyCanvas` to show ready-state plus connection status for multi rooms.
- Upon `GAME_START` for multi, call `navigator.startMultiGame(roomId)` (new method) which bootstraps `MultiGameCanvas`.
- Provide fallback to single-player start for rooms flagged `single` to avoid regression.

## 10. Testing Plan
- **Unit**: Snapshot codec round-trip, input serialization, entity deterministic updates given fixed RNG seed.
- **Local loopback**: `LocalLoopbackMultiAdapter` simulating 2 players in-process; verify stage completion, boss fights.
- **Integration**: Scripted harness launching server + two clients exchanging canned inputs (use `run.sh` variant).
- **Load**: Simulate 4 clients using headless `MultiBot` driver to ensure server tick remains under 16 ms.
- **Regression**: Ensure single-player flow untouched by default (no new dependencies, `Game` still uses `LocalLoopbackNetworkAdapter`).

## 11. Open Questions
- How to handle inventory-based skill unlocks in multi (shared loot? instanced?). Default: loot replicated to all players.
- Voice chat / advanced social features: out of scope.
- Should we allow dynamic join-in-progress? Initially no; sessions lock on start.

