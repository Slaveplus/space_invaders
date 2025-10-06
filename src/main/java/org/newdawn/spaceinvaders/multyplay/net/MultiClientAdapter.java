package org.newdawn.spaceinvaders.multyplay.net;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.newdawn.spaceinvaders.multyplay.net.msg.GameEventMsg;
import org.newdawn.spaceinvaders.multyplay.net.msg.GameSnapshotMsg;
import org.newdawn.spaceinvaders.multyplay.net.msg.PlayerInputMsg;
import org.newdawn.spaceinvaders.multyplay.net.GameEventCodec;
import org.newdawn.spaceinvaders.multyplay.net.PlayerInputCodec;
import org.newdawn.spaceinvaders.multyplay.net.SnapshotCodec;
import org.newdawn.spaceinvaders.room.GameClient;
import org.newdawn.spaceinvaders.room.GameClientListener;
import org.newdawn.spaceinvaders.room.GameClientMultiListener;

/**
 * Bridges the existing text-based GameClient with multiplayer messages.
 * Serialization hooks will be implemented once the protocol is finalized.
 */
public class MultiClientAdapter implements MultiNetworkAdapter, GameClientMultiListener, GameClientListener {
    private final GameClient client;
    private final String roomId;
    private final Queue<GameSnapshotMsg> snapshots = new ConcurrentLinkedQueue<>();
    private final Queue<GameEventMsg> events = new ConcurrentLinkedQueue<>();

    public MultiClientAdapter(GameClient client, String roomId) {
        this.client = client;
        this.roomId = roomId;
        if (client != null) {
            client.setMultiListener(this);
            client.addListener(this);
        }
    }

    @Override
    public Mode getMode() {
        return Mode.CLIENT;
    }

    @Override
    public void sendInput(PlayerInputMsg input) {
        if (client == null || input == null) return;
        try {
            byte[] payload = PlayerInputCodec.encode(input);
            client.sendMultiInput(roomId, payload);
        } catch (IOException ignored) {
        }
    }

    @Override
    public GameSnapshotMsg pollSnapshot() {
        return snapshots.poll();
    }

    @Override
    public void tick(long nowMillis) {
        // Placeholder until we have real network events to consume.
    }

    @Override
    public void shutdown() {
        snapshots.clear();
        events.clear();
        if (client != null) {
            client.setMultiListener(null);
            client.removeListener(this);
        }
    }

    @Override
    public void sendEvent(GameEventMsg event) {
        if (client == null || event == null) return;
        try {
            byte[] payload = GameEventCodec.encode(event);
            client.sendMultiEvent(roomId, payload);
        } catch (IOException ignored) {
        }
    }

    @Override
    public GameEventMsg pollEvent() {
        return events.poll();
    }

    @Override
    public boolean isConnected() {
        return client != null && client.isRunning();
    }

    @Override
    public void onSnapshot(String roomId, byte[] payload) {
        if (payload == null) return;
        try {
            GameSnapshotMsg snapshot = SnapshotCodec.decode(payload);
            snapshots.add(snapshot);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void onEvent(String roomId, byte[] payload) {
        if (payload == null) return;
        try {
            events.add(GameEventCodec.decode(payload));
        } catch (IOException ignored) {
        }
    }

    private String resolveRoomId(String incoming) {
        if (incoming != null && !incoming.isEmpty()) {
            return incoming;
        }
        if (roomId != null && !roomId.isEmpty()) {
            return roomId;
        }
        if (client != null) {
            String current = client.getCurrentRoomId();
            if (current != null && !current.isEmpty()) {
                return current;
            }
        }
        return "";
    }

    private void enqueueLocalEvent(String type, String payload) {
        events.add(new GameEventMsg(type, payload == null ? "" : payload, System.currentTimeMillis()));
    }

    @Override
    public void onHostLeft(String roomId) {
        enqueueLocalEvent("HOST_LEFT", resolveRoomId(roomId));
    }

    @Override
    public void onError(String msg) {
        enqueueLocalEvent("ERROR", msg);
    }

    @Override
    public void onInfo(String msg) {
        if ("LEFT_ROOM".equals(msg)) {
            enqueueLocalEvent("LEFT_ROOM", resolveRoomId(null));
        }
    }
}
