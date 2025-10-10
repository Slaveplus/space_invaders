package org.newdawn.spaceinvaders.room;

public class PlayerInfo {
    public final String id;
    public final String username;
    public final boolean ready;
    public final boolean host;
    public PlayerInfo(String id, String username, boolean ready, boolean host) {
        this.id = id; this.username = username; this.ready = ready; this.host = host;
    }
}
