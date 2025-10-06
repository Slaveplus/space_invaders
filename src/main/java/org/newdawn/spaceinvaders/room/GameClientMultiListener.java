package org.newdawn.spaceinvaders.room;

/**
 * Low-level listener for multiplayer payloads transported via GameClient.
 */
public interface GameClientMultiListener {
    default void onSnapshot(String roomId, byte[] payload) {}
    default void onEvent(String roomId, byte[] payload) {}
}
