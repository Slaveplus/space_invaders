package org.newdawn.spaceinvaders.room;

import java.util.List;

public interface GameClientListener {
    default void onConnected() {}
    default void onRoomsUpdated(List<RoomInfo> rooms) {}
    default void onJoinedRoom(String roomId, String hostId) {}
    default void onRoomState(String roomId, String hostId, java.util.List<PlayerInfo> players) {}
    default void onChatMessage(String from, String msg) {}
    default void onHostLeft(String roomId) {}
    default void onGameStart(String roomId) {}
    default void onGameInit(GameInitInfo info) {}
    default void onGameState(GameStatePayload state) {}
    default void onGameEvent(GameEventPayload event) {}
    default void onInfo(String msg) {}
    default void onError(String msg) {}
}
