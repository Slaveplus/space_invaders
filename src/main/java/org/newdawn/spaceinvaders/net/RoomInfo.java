package org.newdawn.spaceinvaders.net;

public class RoomInfo {
    public String id;
    public String name;
    public int players;
    public int capacity;

    public RoomInfo() {}

    public RoomInfo(String id, String name, int players, int capacity) {
        this.id = id;
        this.name = name;
        this.players = players;
        this.capacity = capacity;
    }
}
