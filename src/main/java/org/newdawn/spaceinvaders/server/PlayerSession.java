package org.newdawn.spaceinvaders.server;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

/** 서버에 접속한 플레이어 세션 */
public class PlayerSession {
    private final String id = UUID.randomUUID().toString();
    private final Socket socket;
    private final PrintWriter out;
    private String username;
    private Room currentRoom;
    private boolean ready;
    private volatile long lastActivity = System.currentTimeMillis();

    public PlayerSession(Socket socket, PrintWriter out) {
        this.socket = socket;
        this.out = out;
    }
    public String getId() { return id; }
    public Socket getSocket() { return socket; }
    public PrintWriter getOut() { return out; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Room getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(Room currentRoom) { this.currentRoom = currentRoom; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public boolean isHost() { return currentRoom != null && currentRoom.getHost() == this; }
    public void touch() { lastActivity = System.currentTimeMillis(); }
    public long getLastActivity() { return lastActivity; }
}
