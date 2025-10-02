package org.newdawn.spaceinvaders.multiplay;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.newdawn.spaceinvaders.net.RoomInfo;
import org.newdawn.spaceinvaders.net.Snapshot;

import java.io.*;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 최소 기능 멀티플레이 클라이언트
 * - 텍스트 라인 기반 JSON
 * - 이벤트 리스너로 UI에 알림
 */
public class MultiplayerClient {
    public interface Listener {
        default void onRooms(List<RoomInfo> rooms) {}
        default void onJoined(String roomId) {}
        default void onStart() {}
        default void onError(String msg) {}
        default void onDisconnected() {}
        default void onSnapshot(Snapshot snapshot) {}
        default void onPlayerId(String playerId) {}
    }

    private final Gson gson = new Gson();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread ioThread;
    private volatile Snapshot latestSnapshot;
    private volatile String playerId;

    public void addListener(Listener l) { listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }

    public boolean isConnected() { return socket != null && socket.isConnected() && !socket.isClosed(); }

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            startReader();
            return true;
        } catch (IOException e) {
            disconnect();
            return false;
        }
    }

    private void startReader() {
        ioThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    handle(line);
                }
            } catch (SocketException se) {
                // connection reset
            } catch (IOException ignored) {
            } finally {
                for (Listener l : listeners) l.onDisconnected();
                disconnect();
            }
        }, "MP-Client-IO");
        ioThread.setDaemon(true);
        ioThread.start();
    }

    private void handle(String line) {
        java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> msg = gson.fromJson(line, mapType);
        String type = (String) msg.get("type");
        Object payload = msg.get("payload");
        if ("ROOMS".equals(type)) {
            Type listType = new TypeToken<List<RoomInfo>>(){}.getType();
            List<RoomInfo> rooms = gson.fromJson(gson.toJson(payload), listType);
            for (Listener l : listeners) l.onRooms(rooms);
        } else if ("JOINED".equals(type)) {
            Map<String, Object> p = gson.fromJson(gson.toJson(payload), mapType);
            String roomId = (String) p.get("roomId");
            for (Listener l : listeners) l.onJoined(roomId);
        } else if ("START".equals(type)) {
            for (Listener l : listeners) l.onStart();
        } else if ("ERROR".equals(type)) {
            Map<String, Object> p = gson.fromJson(gson.toJson(payload), mapType);
            String msgText = (String) p.get("msg");
            for (Listener l : listeners) l.onError(msgText);
        } else if ("SNAPSHOT".equals(type)) {
            Snapshot snap = gson.fromJson(gson.toJson(payload), Snapshot.class);
            latestSnapshot = snap;
            for (Listener l : listeners) l.onSnapshot(snap);
        } else if ("PLAYER".equals(type)) {
            Map<String, Object> p = gson.fromJson(gson.toJson(payload), mapType);
            playerId = (String) p.get("id");
            for (Listener l : listeners) l.onPlayerId(playerId);
        }
    }

    public void listRooms() { send("LIST"); }
    public void createRoom(String name) { send("CREATE " + (name == null ? "" : name)); }
    public void joinRoom(String id) { send("JOIN " + id); }
    public void ready() { send("READY"); }
    public void start() { send("START"); }
    public void inputDx(int dx) { send("INPUT dx:" + dx); }
    public void inputShoot() { send("SHOOT"); }
    public void upgrade(int idx) { send("UPGRADE " + idx); }

    // 키 입력 세분화 (싱글 체감 일치)
    public void keyDownLeft() { send("KEY DOWN LEFT"); }
    public void keyUpLeft() { send("KEY UP LEFT"); }
    public void keyDownRight() { send("KEY DOWN RIGHT"); }
    public void keyUpRight() { send("KEY UP RIGHT"); }
    public void keyDownFire() { send("KEY DOWN FIRE"); }
    public void keyUpFire() { send("KEY UP FIRE"); }

    private void send(String s) {
        if (out != null) out.println(s);
    }

    public void disconnect() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null; in = null; out = null;
    }

    public Snapshot getLatestSnapshot() { return latestSnapshot; }
    public String getPlayerId() { return playerId; }
}
