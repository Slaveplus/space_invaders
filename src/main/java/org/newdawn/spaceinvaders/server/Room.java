package org.newdawn.spaceinvaders.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** 단순 방 모델 */
public class Room {
    private final String id = UUID.randomUUID().toString();
    private final String name;
    private final boolean single; // true면 싱글 방 (리스트 비표시)
    private final int maxPlayers;
    private final List<PlayerSession> players = new ArrayList<>();
    private PlayerSession host;
    private boolean started;

    public Room(String name, boolean single, int maxPlayers) {
        this.name = name;
        this.single = single;
        this.maxPlayers = Math.max(1, maxPlayers);
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isSingle() { return single; }
    public int getMaxPlayers() { return maxPlayers; }
    public synchronized List<PlayerSession> getPlayers() { return Collections.unmodifiableList(players); }
    public synchronized PlayerSession getHost() { return host; }
    public synchronized boolean isStarted() { return started; }
    public synchronized void setStarted(boolean started) { this.started = started; }

    public synchronized boolean addPlayer(PlayerSession p) {
        if (players.size() >= maxPlayers) return false;
        if (players.contains(p)) return true;
        players.add(p);
        if (host == null) host = p;
        p.setCurrentRoom(this);
        return true;
    }
    public synchronized void removePlayer(PlayerSession p) {
        players.remove(p);
        p.setCurrentRoom(null);
        p.setReady(false);
        if (p == host) {
            host = players.isEmpty()? null : players.get(0);
        }
    }
}
