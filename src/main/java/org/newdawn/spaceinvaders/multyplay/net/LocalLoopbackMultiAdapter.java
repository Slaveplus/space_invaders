package org.newdawn.spaceinvaders.multyplay.net;

import java.util.ArrayDeque;
import java.util.Queue;

import org.newdawn.spaceinvaders.multyplay.net.msg.GameEventMsg;
import org.newdawn.spaceinvaders.multyplay.net.msg.GameSnapshotMsg;
import org.newdawn.spaceinvaders.multyplay.net.msg.PlayerInputMsg;

/**
 * In-process adapter useful for harness tests before wiring real networking.
 */
public class LocalLoopbackMultiAdapter implements MultiNetworkAdapter {
    private final Queue<PlayerInputMsg> inputs = new ArrayDeque<>();
    private final Queue<GameSnapshotMsg> snapshots = new ArrayDeque<>();
    private final Queue<GameEventMsg> events = new ArrayDeque<>();

    @Override
    public Mode getMode() {
        return Mode.LOOPBACK;
    }

    @Override
    public void sendInput(PlayerInputMsg input) {
        inputs.add(input);
    }

    @Override
    public GameSnapshotMsg pollSnapshot() {
        return snapshots.poll();
    }

    @Override
    public void tick(long nowMillis) {
        // For loopback we immediately fabricate a snapshot from latest input count.
        PlayerInputMsg latest = inputs.peek();
        if (latest != null) {
            snapshots.add(new GameSnapshotMsg(
                    "loopback",
                    nowMillis,
                    nowMillis,
                    1,
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyMap(),
                    latest.getSequence(),
                    ""
            ));
            inputs.clear();
        }
    }

    @Override
    public void shutdown() {
        inputs.clear();
        snapshots.clear();
        events.clear();
    }

    @Override
    public void sendEvent(GameEventMsg event) {
        events.add(event);
    }

    @Override
    public GameEventMsg pollEvent() {
        return events.poll();
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}
