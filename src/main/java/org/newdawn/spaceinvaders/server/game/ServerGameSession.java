package org.newdawn.spaceinvaders.server.game;

import static org.newdawn.spaceinvaders.server.MessageType.GAME_EVENT;
import static org.newdawn.spaceinvaders.server.MessageType.GAME_INIT;
import static org.newdawn.spaceinvaders.server.MessageType.GAME_STATE;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private final ScheduledExecutorService scheduler; // 생성자에서 room 할당 후 초기화

    private volatile boolean running;
    private long tickCounter;
    private long lastTickTimestamp;
    

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
        game.startGame();
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
        // Placeholder for skill activations or other discrete commands.
    }

    public void handlePlayerLeft(PlayerSession session) {
        if (session == null) return;
        readyPlayers.remove(session.getId());
        latestInputs.remove(session.getId());
        players.remove(session.getId());
        if (players.isEmpty()) {
            shutdown();
        }
    }

    private void startLoop() {
        if (running) return;
        running = true;
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
        GameSnapshot snapshot = game.createSnapshot(tickCounter, now, delta);
        broadcastSnapshot(snapshot);
    }

    private void broadcastSnapshot(GameSnapshot snapshot) {
        String entities = GameSnapshotCodec.encodeEntities(snapshot.entities);
        String playersPayload = GameSnapshotCodec.encodePlayers(snapshot.players);
        String line = TextMessage.builder(GAME_STATE)
                .put(ProtocolKeys.ROOM_ID, room.getId())
                .put(ProtocolKeys.TICK, snapshot.tick)
                .put(ProtocolKeys.DELTA, snapshot.deltaMillis)
                .put(ProtocolKeys.SERVER_TIME, snapshot.serverTime)
                .put(ProtocolKeys.ROUND, snapshot.round)
                .put(ProtocolKeys.ENTITIES, entities)
                .put(ProtocolKeys.PLAYERS, playersPayload)
                .toLine();
        sendToAll(line);

        // Events are currently empty; placeholder for future use.
        String eventsPayload = GameEventCodec.encode(Collections.<GameEvent>emptyList());
        if (!eventsPayload.isEmpty()) {
            String eventLine = TextMessage.builder(GAME_EVENT)
                    .put(ProtocolKeys.ROOM_ID, room.getId())
                    .put(ProtocolKeys.TICK, snapshot.tick)
                    .put(ProtocolKeys.EVENTS, eventsPayload)
                    .toLine();
            sendToAll(eventLine);
        }
    }

    private void sendToAll(String line) {
        for (PlayerSession ps : players.values()) {
            ps.getOut().println(line);
        }
    }

    public void shutdown() {
        running = false;
        scheduler.shutdownNow();
    }
}
