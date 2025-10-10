package org.newdawn.spaceinvaders.server;

import java.util.*;
import java.util.stream.Collectors;

/** 방 관리 */
public class ServerRoomManager {
    private final Map<String, Room> rooms = new HashMap<>();

    public synchronized Room createRoom(String name, boolean single, int maxPlayers) {
        Room r = new Room(name, single, maxPlayers);
        rooms.put(r.getId(), r);
        return r;
    }
    public synchronized Room getRoom(String id) { return rooms.get(id); }
    public synchronized void removeRoom(String id) { rooms.remove(id); }

    public synchronized List<Room> listVisibleRooms() {
        return rooms.values().stream()
                .filter(r -> !r.isSingle() && !r.isStarted())
                .collect(Collectors.toList());
    }

    // 유지보수 용 내부 맵 접근 (주의: 동기화 블록 내에서만 사용)
    synchronized Map<String, Room> getRoomsInternal() { return rooms; }
}
