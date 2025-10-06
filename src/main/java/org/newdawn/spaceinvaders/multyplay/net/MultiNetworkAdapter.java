package org.newdawn.spaceinvaders.multyplay.net;

import org.newdawn.spaceinvaders.multyplay.net.msg.GameEventMsg;
import org.newdawn.spaceinvaders.multyplay.net.msg.GameSnapshotMsg;
import org.newdawn.spaceinvaders.multyplay.net.msg.PlayerInputMsg;

/**
 * Abstraction for multiplayer client/server communication.
 */
public interface MultiNetworkAdapter {
    enum Mode { CLIENT, SERVER, LOOPBACK }

    Mode getMode();

    void sendInput(PlayerInputMsg input);

    GameSnapshotMsg pollSnapshot();

    void tick(long nowMillis);

    void shutdown();

    void sendEvent(GameEventMsg event);

    GameEventMsg pollEvent();

    boolean isConnected();
}
