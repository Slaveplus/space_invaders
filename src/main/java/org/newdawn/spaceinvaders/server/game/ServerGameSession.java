package org.newdawn.spaceinvaders.server.game;

import static org.newdawn.spaceinvaders.server.MessageType.GAME_EVENT;
import static org.newdawn.spaceinvaders.server.MessageType.GAME_INIT;
import static org.newdawn.spaceinvaders.server.MessageType.GAME_STATE;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.newdawn.spaceinvaders.database.FirebaseConfig;
import org.newdawn.spaceinvaders.database.FirebaseDatabaseClient;
import org.newdawn.spaceinvaders.database.LeaderboardRecord;
import org.newdawn.spaceinvaders.database.LeaderboardRepository;
import org.newdawn.spaceinvaders.multyplay.net.GameEvent;
import org.newdawn.spaceinvaders.multyplay.net.GameSnapshot;
import org.newdawn.spaceinvaders.multyplay.net.PlayerInput;
import org.newdawn.spaceinvaders.multyplay.net.protocol.GameEventCodec;
import org.newdawn.spaceinvaders.multyplay.net.protocol.GameSnapshotCodec;
import org.newdawn.spaceinvaders.multyplay.net.protocol.ProtocolKeys;
import org.newdawn.spaceinvaders.multyplay.net.protocol.TextMessage;
import org.newdawn.spaceinvaders.server.GameServer;
import org.newdawn.spaceinvaders.server.PlayerSession;
import org.newdawn.spaceinvaders.server.Room;

/**
 * 룸별 멀티플레이 게임 세션. 추후 실제 게임 로직과 연동될 예정이며,
 * 현재는 프로토콜 핸드쉐이크/입력 수집/스냅샷 브로드캐스트의 뼈대를 담당한다.
 */
public class ServerGameSession implements Runnable {
    private static final long TICK_INTERVAL_MS = 16; // ~60fps

    private final GameServer server;
    private final Room room;
    private final long seed;
    private final ServerMultiplayerGame game;
    private final Map<String, PlayerSession> players = new LinkedHashMap<>();

    private final Map<String, PlayerInput> latestInputs = new ConcurrentHashMap<>();
    private final Set<String> readyPlayers = ConcurrentHashMap.newKeySet();
    private enum Phase { HANDSHAKE, ACTIVE, INTERMISSION, COMPLETED }
    private Phase phase = Phase.HANDSHAKE;
    private final Map<String, Boolean> roundReady = new ConcurrentHashMap<>();
    private ServerMultiplayerGame.RoundTransition currentTransition;
    private long intermissionStartedAt;

    private final ScheduledExecutorService scheduler; // 생성자에서 room 할당 후 초기화

    private volatile boolean running;
    private long tickCounter;
    private long lastTickTimestamp;
    private boolean leaderboardSaved = false;
    private final FirebaseDatabaseClient leaderboardDb =
            new FirebaseDatabaseClient(FirebaseConfig.DATABASE_URL);
    

    public ServerGameSession(GameServer server, Room room) {
        this.server = server;
        this.room = room;
        // room이 초기화된 후 스케줄러 생성 (thread name에 room 사용)
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ServerGameSession-" + this.room.getId());
            t.setDaemon(true);
            return t;
        });
        synchronized (room) {
            for (PlayerSession player : room.getPlayers()) {
                players.put(player.getId(), player);
            }
        }
        this.seed = System.nanoTime();
        this.game = new ServerMultiplayerGame(seed);
        game.registerPlayers(players.keySet());
        PlayerSession host = room.getHost();
        if (host != null) {
            game.setPrimaryPlayerId(host.getId());
        } else if (!players.isEmpty()) {
            game.setPrimaryPlayerId(players.keySet().iterator().next());
        }
    }

    public void startHandshake() {
        broadcastInit();
    }

    private void broadcastInit() {
        String line = TextMessage.builder(GAME_INIT)
                .put(ProtocolKeys.ROOM_ID, room.getId())
                .put(ProtocolKeys.SEED, seed)
                .put(ProtocolKeys.TICK, tickCounter)
                .put(ProtocolKeys.PLAYERS, encodePlayerList())
                .toLine();
        sendToAll(line);
    }

    private String encodePlayerList() {
        StringBuilder sb = new StringBuilder();
        for (PlayerSession ps : players.values()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(ps.getId()).append(',')
              .append(ps.getUsername() == null ? "" : TextMessage.escapeComponent(ps.getUsername()));
        }
        return sb.toString();
    }

    public void handleReady(PlayerSession session, String seedAck) {
        if (session == null) return;
        readyPlayers.add(session.getId());
        game.registerPlayers(Collections.singleton(session.getId()));
        if (readyPlayers.size() == players.size()) {
            startLoop();
        }
    }

    public void handleInput(PlayerSession session, int sequence, int mask, long clientTime) {
        if (session == null) return;
        PlayerInput input = new PlayerInput(session.getId(),
                (mask & 1) != 0,
                (mask & (1 << 1)) != 0,
                (mask & (1 << 2)) != 0,
                clientTime,
                sequence);
        latestInputs.put(session.getId(), input);
    }

    public void handleStateAck(PlayerSession session, long tick) {
        // Placeholder for reliability/resend logic.
    }

    public void handleStateRequest(PlayerSession session, long fromTick) {
        // Placeholder: once replay buffer is implemented, send missed snapshots.
    }

    public void handleAction(PlayerSession session, String action, String data) {
        if (session == null || action == null) {
            return;
        }
			switch (action) {
				case "ROUND_READY":
					boolean ready = data == null || !data.equals("0");
					setPlayerReady(session.getId(), ready);
					break;
				case "CHAT":
					if (data != null && !data.isEmpty()) {
						GameEvent chatEvent = new GameEvent(GameEvent.Type.CHAT, session.getId(), data, System.currentTimeMillis());
						broadcastGameEvent(chatEvent);
					}
					break;
                case "SKILL_REQUEST": {
                    int skillType = safeParseInt(data, -1);
                    ServerMultiplayerGame.SkillActionResult result = game.handleSkillActivation(session.getId(), skillType);
                    if (result != null && result.message != null && !result.message.isEmpty()) {
                        sendSystemMessage(session.getId(), result.message);
                    }
                    break;
                }
                case "SKILL_UPGRADE": {
                    int upgradeType = safeParseInt(data, -1);
                    ServerMultiplayerGame.SkillActionResult result = game.handleSkillUpgrade(session.getId(), upgradeType);
                    if (result != null && result.message != null && !result.message.isEmpty()) {
                        sendSystemMessage(session.getId(), result.message);
                    }
                    break;
                }
                case "SKIN": {
                    // 스킨 정보 처리: "SKIN:playerId:skinPath" 형식
                    if (data != null && data.startsWith("SKIN:")) {
                        String[] parts = data.split(":", 3);
                        if (parts.length == 3) {
                            String playerId = parts[1];
                            String skinPath = parts[2];
                            game.setPlayerSkin(playerId, skinPath);
                            System.out.println("서버: 플레이어 " + playerId + " 스킨 설정됨: " + skinPath);
                            
                            // 스킨 변경을 다른 플레이어들에게 알림
                            GameEvent skinChangeEvent = new GameEvent(GameEvent.Type.SYSTEM, 
                                playerId, "SKIN_CHANGED:" + playerId + ":" + skinPath, System.currentTimeMillis());
                            broadcastGameEvent(skinChangeEvent);
                        }
                    }
                    break;
                }
				default:
					// other actions can be handled here later
					break;
			}
		}

    public void handlePlayerLeft(PlayerSession session) {
        if (session == null) return;
        readyPlayers.remove(session.getId());
        latestInputs.remove(session.getId());
        players.remove(session.getId());
        roundReady.remove(session.getId());
        game.removePlayer(session.getId());
        if (players.isEmpty()) {
            shutdown();
            return;
        }
        if (phase == Phase.INTERMISSION) {
            maybeStartNextRound();
        }
    }

    private void setPlayerReady(String playerId, boolean ready) {
        if (phase != Phase.INTERMISSION || playerId == null || !players.containsKey(playerId)) {
            return;
        }
        if (ready) {
            roundReady.put(playerId, true);
        } else {
            roundReady.remove(playerId);
        }
        maybeStartNextRound();
    }

    private void maybeStartNextRound() {
        if (phase != Phase.INTERMISSION) {
            return;
        }
        if (!game.hasPendingRoundStart()) {
            return;
        }
        if (players.isEmpty()) {
            return;
        }
        boolean allReady = true;
        for (String id : players.keySet()) {
            if (!Boolean.TRUE.equals(roundReady.get(id))) {
                allReady = false;
                break;
            }
        }
        if (allReady) {
            game.startPendingRound();
            roundReady.clear();
            currentTransition = null;
            intermissionStartedAt = 0L;
            phase = Phase.ACTIVE;
        }
    }

    private void startLoop() {
        if (running) return;
        game.startGame();
        running = true;
        phase = Phase.ACTIVE;
        currentTransition = null;
        roundReady.clear();
        intermissionStartedAt = 0L;
        lastTickTimestamp = System.currentTimeMillis();
        scheduler.scheduleAtFixedRate(this, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        if (!running) {
            return;
        }
        long now = System.currentTimeMillis();
        long delta = lastTickTimestamp == 0 ? TICK_INTERVAL_MS : Math.max(1, now - lastTickTimestamp);
        lastTickTimestamp = now;
        tickCounter++;

        game.applyInputs(new HashMap<>(latestInputs));
        game.update(delta);

        if (phase == Phase.INTERMISSION && !game.isInIntermission()) {
            phase = Phase.ACTIVE;
            currentTransition = null;
            roundReady.clear();
        }

        ServerMultiplayerGame.RoundTransition transition = game.pollRoundTransition();
        if (transition != null) {
            currentTransition = transition;
            intermissionStartedAt = now;
            roundReady.clear();
            if (transition.type == ServerMultiplayerGame.RoundTransition.Type.GAME_COMPLETED) {
                phase = Phase.COMPLETED;
                persistLeaderboardIfNeeded();
            } else {
                phase = Phase.INTERMISSION;
            }
        }

        boolean waitingForPlayers = phase == Phase.INTERMISSION && game.hasPendingRoundStart();
        Map<String, Boolean> readyStatesView = buildReadyStates();

        GameSnapshot snapshot = game.createSnapshot(tickCounter, now, delta,
                mapPhase(phase),
                waitingForPlayers,
                readyStatesView,
                currentTransition != null ? currentTransition.message : null);
        broadcastSnapshot(snapshot);
    }

    private void broadcastSnapshot(GameSnapshot snapshot) {
        String entities = GameSnapshotCodec.encodeEntities(snapshot.entities);
        String playersPayload = GameSnapshotCodec.encodePlayers(snapshot.players);
        String readyPayload = encodeReady(snapshot.readyStates);
        TextMessage.Builder builder = TextMessage.builder(GAME_STATE)
                .put(ProtocolKeys.ROOM_ID, room.getId())
                .put(ProtocolKeys.TICK, snapshot.tick)
                .put(ProtocolKeys.DELTA, snapshot.deltaMillis)
                .put(ProtocolKeys.SERVER_TIME, snapshot.serverTime)
                .put(ProtocolKeys.ROUND, snapshot.round)
                .put(ProtocolKeys.ENTITIES, entities)
                .put(ProtocolKeys.PLAYERS, playersPayload)
                .put(ProtocolKeys.PHASE, snapshot.phase.name())
                .put(ProtocolKeys.WAITING, snapshot.waitingForPlayers ? "1" : "0");
        if (!readyPayload.isEmpty()) {
            builder.put(ProtocolKeys.READY, readyPayload);
        }
        if (snapshot.message != null && !snapshot.message.isEmpty()) {
            builder.put(ProtocolKeys.MESSAGE, snapshot.message);
        }
        String line = builder.toLine();
        sendToAll(line);

        List<GameEvent> pendingEvents = game.drainPendingEvents();
        if (pendingEvents != null && !pendingEvents.isEmpty()) {
            String eventsPayload = GameEventCodec.encode(pendingEvents);
            if (!eventsPayload.isEmpty()) {
                String eventLine = TextMessage.builder(GAME_EVENT)
                        .put(ProtocolKeys.ROOM_ID, room.getId())
                        .put(ProtocolKeys.TICK, snapshot.tick)
                        .put(ProtocolKeys.EVENTS, eventsPayload)
                        .toLine();
                sendToAll(eventLine);
            }
        }
    }

    private void sendToAll(String line) {
        for (PlayerSession ps : players.values()) {
            ps.getOut().println(line);
        }
    }

    private void persistLeaderboardIfNeeded() {
        if (leaderboardSaved) {
            return;
        }
        try {
            long playTimeMs = game.getPlayTimeMs();
            List<String> names = new java.util.ArrayList<>();
            for (PlayerSession ps : players.values()) {
                if (ps == null) {
                    continue;
                }
                String name = ps.getUsername();
                if (name == null || name.trim().isEmpty()) {
                    name = ps.getId();
                }
                String trimmed = name != null ? name.trim() : "";
                if (!trimmed.isEmpty() && !names.contains(trimmed)) {
                    names.add(trimmed);
                }
            }
            if (names.isEmpty()) {
                names.add("UNKNOWN");
            }
            LeaderboardRecord record = new LeaderboardRecord(
                    LeaderboardRecord.Mode.MULTI,
                    names,
                    playTimeMs);
            boolean success = LeaderboardRepository.saveRecord(
                    leaderboardDb,
                    LeaderboardRecord.Mode.MULTI,
                    record);
            if (success) {
                System.out.println("[Server] Saved multiplayer leaderboard entry: " + record);
            } else {
                System.err.println("[Server] Failed to save multiplayer leaderboard entry");
            }
        } catch (Exception ex) {
            System.err.println("[Server] Error saving multiplayer leaderboard entry: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            leaderboardSaved = true;
        }
    }

    private void broadcastGameEvent(GameEvent event) {
        if (event == null) {
            return;
        }
        String payload = GameEventCodec.encode(Collections.singletonList(event));
        if (payload.isEmpty()) {
            return;
        }
        String line = TextMessage.builder(GAME_EVENT)
                .put(ProtocolKeys.ROOM_ID, room.getId())
                .put(ProtocolKeys.TICK, tickCounter)
                .put(ProtocolKeys.EVENTS, payload)
                .toLine();
        sendToAll(line);
    }

    private void sendGameEventToPlayer(String playerId, GameEvent event) {
        if (playerId == null || event == null) {
            return;
        }
        PlayerSession session = players.get(playerId);
        if (session == null) {
            return;
        }
        String payload = GameEventCodec.encode(Collections.singletonList(event));
        if (payload.isEmpty()) {
            return;
        }
        String line = TextMessage.builder(GAME_EVENT)
                .put(ProtocolKeys.ROOM_ID, room.getId())
                .put(ProtocolKeys.TICK, tickCounter)
                .put(ProtocolKeys.EVENTS, payload)
                .toLine();
        session.getOut().println(line);
    }

    private void sendSystemMessage(String playerId, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        GameEvent event = new GameEvent(GameEvent.Type.SYSTEM, playerId, message, System.currentTimeMillis());
        sendGameEventToPlayer(playerId, event);
    }

    private Map<String, Boolean> buildReadyStates() {
        Map<String, Boolean> view = new LinkedHashMap<>();
        for (String id : players.keySet()) {
            view.put(id, Boolean.TRUE.equals(roundReady.get(id)));
        }
        return view;
    }

    private String encodeReady(Map<String, Boolean> readyStates) {
        if (readyStates == null || readyStates.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        readyStates.forEach((id, ready) -> {
            if (sb.length() > 0) sb.append(';');
            sb.append(TextMessage.escapeComponent(id)).append('=').append(ready ? '1' : '0');
        });
        return sb.toString();
    }

    private GameSnapshot.Phase mapPhase(Phase p) {
        switch (p) {
            case INTERMISSION: return GameSnapshot.Phase.INTERMISSION;
            case COMPLETED: return GameSnapshot.Phase.COMPLETED;
            default: return GameSnapshot.Phase.ACTIVE;
        }
    }

    public void shutdown() {
        running = false;
        scheduler.shutdownNow();
    }

    private static int safeParseInt(String value, int def) {
        if (value == null) {
            return def;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
