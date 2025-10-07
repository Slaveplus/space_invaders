package org.newdawn.spaceinvaders.multyplay.net.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.newdawn.spaceinvaders.multyplay.net.GameEvent;
import org.newdawn.spaceinvaders.multyplay.net.GameNetworkAdapter;
import org.newdawn.spaceinvaders.multyplay.net.GameSnapshot;
import org.newdawn.spaceinvaders.multyplay.net.PlayerInput;
import org.newdawn.spaceinvaders.room.GameClient;
import org.newdawn.spaceinvaders.room.GameClientListener;
import org.newdawn.spaceinvaders.room.GameEventPayload;
import org.newdawn.spaceinvaders.room.GameInitInfo;
import org.newdawn.spaceinvaders.room.GameStatePayload;

/**
 * GameNetworkAdapter 구현체로서 GameClient를 통해 서버와 통신한다.
 * 수신한 스냅샷은 최신 상태로 유지하고, 이벤트는 큐에 적재한다.
 */
public class RoomGameNetworkAdapter implements GameNetworkAdapter, GameClientListener {

    private final GameClient client;
    private final AtomicReference<GameSnapshot> latestSnapshot = new AtomicReference<>();
    private final ConcurrentLinkedQueue<GameEvent> eventQueue = new ConcurrentLinkedQueue<>();

    private volatile String roomId;
    private volatile long seed;
    private volatile long lastReceivedTick;
    private volatile long lastAckTick;
    private volatile boolean running = true;
    private boolean readySent = false;

    public RoomGameNetworkAdapter(GameClient client) {
        this.client = Objects.requireNonNull(client, "client");
        client.addListener(this);
    }

    @Override
    public Mode getMode() {
        return Mode.CLIENT;
    }

    @Override
    public void sendInput(PlayerInput input) {
        if (roomId == null || input == null) {
            return;
        }
        client.sendGameInput(roomId, input.sequence, input.mask, input.clientTime);
    }

    @Override
    public GameSnapshot pollLatestSnapshot() {
        return latestSnapshot.getAndSet(null);
    }

    @Override
    public void tick(long nowMillis) {
        if (!running || roomId == null) {
            return;
        }
        if (lastReceivedTick > 0 && lastReceivedTick > lastAckTick) {
            client.sendStateAck(roomId, lastReceivedTick);
            lastAckTick = lastReceivedTick;
        }
    }

    @Override
    public long getLastReceivedTick() {
        return lastReceivedTick;
    }

    @Override
    public void shutdown() {
        running = false;
        readySent = false;
        client.removeListener(this);
        latestSnapshot.set(null);
        eventQueue.clear();
    }

    @Override
    public void sendEvent(GameEvent event) {
        if (roomId == null || event == null) {
            return;
        }
        String action = event.type.name();
        String data = event.message;
        client.sendGameAction(roomId, action, data);
    }

    @Override
    public List<GameEvent> drainEvents() {
        List<GameEvent> list = new ArrayList<>();
        GameEvent ev;
        while ((ev = eventQueue.poll()) != null) {
            list.add(ev);
        }
        return list;
    }

    // -------- GameClientListener callbacks --------

    @Override
    public void onGameInit(GameInitInfo info) {
        if (info == null) {
            return;
        }
        this.roomId = info.roomId;
        this.seed = info.seed;
        this.lastReceivedTick = info.initialTick;
        this.lastAckTick = 0;
        if (!readySent) {
            client.sendGameReady(info.roomId, info.seed);
            readySent = true;
        }
    }

    @Override
    public void onGameState(GameStatePayload payload) {
        if (payload == null || payload.snapshot == null) {
            return;
        }
        latestSnapshot.set(payload.snapshot);
        lastReceivedTick = payload.snapshot.tick;
    }

    @Override
    public void onGameEvent(GameEventPayload payload) {
        if (payload == null || payload.events == null) {
            return;
        }
        for (GameEvent ev : payload.events) {
            if (ev != null) {
                eventQueue.add(ev);
            }
        }
    }

    @Override
    public void onHostLeft(String leftRoomId) {
        if (leftRoomId != null && leftRoomId.equals(roomId)) {
            shutdown();
        }
    }

    public long getSeed() {
        return seed;
    }
}
