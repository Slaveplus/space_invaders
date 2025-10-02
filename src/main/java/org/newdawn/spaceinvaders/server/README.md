# Space Invaders Multiplayer Server (Draft)

This folder holds the server-side scaffolding for multiplayer.

Planned architecture:
- Java NIO based server (RoomManager + ClientHandler)
- Simple JSON/TEXT protocol
  - C->S: LIST_ROOMS, CREATE roomName, JOIN roomId, READY, START, INPUT {...}, PING
  - S->C: ROOMS [...], ROOM_CREATED id, JOINED roomId, STATE {...}, START, PONG, ERROR msg
- Rooms with up to 4 players; broadcast game state deltas at ~20 TPS

Current status:
- Client and server implementations are placeholders. The game still uses local gameplay for `startMultiplayerGame()`.
- UI flow in app:
  MainMenu → Multiplayer Connect → Room List → Lobby → Start Game → (uses local Game screen for now)

Next steps:
1) Implement server (Netty or NIO)
2) Implement client in `org.newdawn.spaceinvaders.multiplay` with reconnects and background IO thread
3) Define deterministic simulation / lockstep or server-authoritative model
4) Integrate networked game state into gameplay systems
