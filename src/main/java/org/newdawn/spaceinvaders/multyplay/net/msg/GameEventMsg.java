package org.newdawn.spaceinvaders.multyplay.net.msg;

import java.util.Objects;

public final class GameEventMsg {
    private final String type;
    private final String payload;
    private final long serverTimestamp;

    public GameEventMsg(String type, String payload, long serverTimestamp) {
        this.type = Objects.requireNonNull(type, "type");
        this.payload = payload == null ? "" : payload;
        this.serverTimestamp = serverTimestamp;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public long getServerTimestamp() {
        return serverTimestamp;
    }
}
