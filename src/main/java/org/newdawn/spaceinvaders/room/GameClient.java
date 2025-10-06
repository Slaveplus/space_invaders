package org.newdawn.spaceinvaders.room;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.CopyOnWriteArrayList;

/** 간단 텍스트 프로토콜 클라이언트 */
public class GameClient implements Runnable {
    private final String host;
    private final int port;
    private final String username;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running;
    private Thread thread;
    private Thread heartbeatThread; // 주기적 PING 전송 스레드
    private final List<GameClientListener> listeners = new CopyOnWriteArrayList<>();
    private GameClientMultiListener multiListener;

    private String currentRoomId;
    private String currentHostId;
    private List<PlayerInfo> currentPlayers = new ArrayList<>();
    private List<RoomInfo> cachedRooms = new ArrayList<>();
    private String sessionId; // 서버가 부여한 고유 세션 식별자
    private boolean currentRoomSingle = false; // 현재 방이 싱글방인지

    public GameClient(String host, int port, String username) {
        this.host = host; this.port = port; this.username = username;
    }

    public void addListener(GameClientListener l) { if (l!=null) listeners.add(l); }
    public void removeListener(GameClientListener l) { listeners.remove(l); }
    public void setMultiListener(GameClientMultiListener multiListener) { this.multiListener = multiListener; }

    public void connect() throws IOException {
        if (running) {
            // 이미 실행 중이면 중복 접속 방지
            throw new IllegalStateException("GameClient already connected");
        }
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        running = true;
        thread = new Thread(this, "GameClient-Recv");
        thread.start();
        send("CONNECT|"+username);
        startHeartbeat();
    }

    public boolean isRunning() { return running; }
    public String getCurrentRoomId() { return currentRoomId; }
    public List<PlayerInfo> getCurrentPlayers() { return currentPlayers; }
    public List<RoomInfo> getCachedRooms() { return cachedRooms; }
    public boolean isHost() { return currentHostId != null && currentPlayers.stream().anyMatch(p -> p.host && p.id.equals(getSelfId())); }
    public String getSelfId() { return currentPlayers.stream().filter(p -> p.username.equals(username)).map(p -> p.id).findFirst().orElse(null); }
    public String getSessionId() { return sessionId; }
    public boolean isCurrentRoomSingle() { return currentRoomSingle; }

    public void requestRoomList() { send("LIST_ROOMS"); }
    public void createRoom(String name, boolean single, int max) { send("CREATE_ROOM|name="+escape(name)+"|type="+(single?"single":"multi")+"|max="+max); }
    public void joinRoom(String roomId) { send("JOIN_ROOM|"+roomId); }
    public void leaveRoom() { send("LEAVE_ROOM"); currentRoomId = null; currentPlayers = new ArrayList<>(); }
    public void toggleReady() { send("TOGGLE_READY"); }
    public void startGame() { send("START_GAME"); }
    public void chat(String msg) { send("CHAT|"+escape(msg)); }
    public void requestRoomState() { send("GET_ROOM_STATE"); }
    public void sendMultiInput(String roomId, byte[] payload) {
        if (roomId == null || payload == null) return;
        String encoded = Base64.getEncoder().encodeToString(payload);
        send("MULTI_INPUT|roomId=" + escape(roomId) + "|payload=" + encoded);
    }
    public void sendMultiEvent(String roomId, byte[] payload) {
        if (roomId == null || payload == null) return;
        String encoded = Base64.getEncoder().encodeToString(payload);
        send("MULTI_EVENT|roomId=" + escape(roomId) + "|payload=" + encoded);
    }

    private void send(String line) { if (out!=null) out.println(line); }

    public void shutdown() {
        running = false;
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }
        try { if (socket!=null) socket.close(); } catch (IOException ignored) {}
    }

    @Override
    public void run() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                handle(line.trim());
            }
        } catch (IOException ignored) {
        } finally {
            running = false;
        }
    }

    private void handle(String line) {
        if (line.isEmpty()) return;
        String[] parts = line.split("\\|");
        String type = parts[0];
        switch (type) {
            case "INFO": {
                String msg = getValue(parts, "msg");
                if ("CONNECTED".equals(msg)) {
                    // sid 수신
                    String sid = getValue(parts, "sid");
                    if (sid != null && !sid.isEmpty()) this.sessionId = sid;
                }
                fireInfo(msg);
                break;
            }
            case "ERROR": fireError(getValue(parts, "msg")); break;
            case "ROOMS": handleRooms(parts); break;
            case "ROOM_JOINED": handleRoomJoined(parts); break;
            case "ROOM_STATE": handleRoomState(parts); break;
            case "CHAT": handleChat(parts); break;
            case "HOST_LEFT": fireHostLeft(getValue(parts, "roomId")); break;
            case "GAME_START": fireGameStart(getValue(parts, "roomId")); break;
            case "MULTI_SNAPSHOT":
                dispatchMultiPayload(parts, true);
                break;
            case "MULTI_EVENT":
                dispatchMultiPayload(parts, false);
                break;
            default: // ignore
        }
        if ("INFO".equals(type) && "CONNECTED".equals(getValue(parts, "msg"))) {
            fireConnected();
        }
    }

    private void handleRooms(String[] parts) {
        String list = getValue(parts, "list");
        List<RoomInfo> temp = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            String[] rs = list.split(";");
            for (String r : rs) {
                if (r.isEmpty()) continue;
                String[] f = r.split(",");
                if (f.length >= 5) {
                    String id = f[0];
                    String name = unescape(f[1]);
                    boolean single = "single".equalsIgnoreCase(f[2]);
                    int cur = parseIntSafe(f[3]);
                    int max = parseIntSafe(f[4]);
                    temp.add(new RoomInfo(id, name, single, cur, max));
                }
            }
        }
        cachedRooms = temp;
        for (GameClientListener l : listeners) l.onRoomsUpdated(Collections.unmodifiableList(cachedRooms));
    }

    private void handleRoomJoined(String[] parts) {
        currentRoomId = getValue(parts, "roomId");
        currentHostId = getValue(parts, "hostId");
        currentRoomSingle = "1".equals(getValue(parts, "single"));
        for (GameClientListener l : listeners) l.onJoinedRoom(currentRoomId, currentHostId);
    }

    private void handleRoomState(String[] parts) {
        String roomId = getValue(parts, "roomId");
        currentHostId = getValue(parts, "hostId");
        currentRoomSingle = "1".equals(getValue(parts, "single"));
        String plist = getValue(parts, "players");
        List<PlayerInfo> ps = new ArrayList<>();
        if (plist != null && !plist.isEmpty()) {
            String[] entries = plist.split(";");
            for (String e : entries) {
                String[] f = e.split(",");
                if (f.length >= 4) {
                    ps.add(new PlayerInfo(f[0], unescape(f[1]), "1".equals(f[2]), "1".equals(f[3])));
                }
            }
        }
        currentPlayers = ps;
        currentRoomId = roomId;
        for (GameClientListener l : listeners) l.onRoomState(roomId, currentHostId, Collections.unmodifiableList(currentPlayers));
    }

    private void handleChat(String[] parts) {
        String from = getValue(parts, "from");
        String msg = unescape(getValue(parts, "msg"));
        for (GameClientListener l : listeners) l.onChatMessage(from, msg);
    }

    private void dispatchMultiPayload(String[] parts, boolean snapshot) {
        if (multiListener == null) return;
        String roomId = getValue(parts, "roomId");
        String payload = getValue(parts, "payload");
        if (payload == null || payload.isEmpty()) return;
        try {
            byte[] decoded = Base64.getDecoder().decode(payload);
            if (snapshot) {
                multiListener.onSnapshot(roomId, decoded);
            } else {
                multiListener.onEvent(roomId, decoded);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private String getValue(String[] parts, String key) {
        for (int i=1;i<parts.length;i++) {
            String p = parts[i];
            int idx = p.indexOf('=');
            if (idx>0) {
                String k = p.substring(0, idx);
                if (k.equals(key)) return p.substring(idx+1);
            }
        }
        return null;
    }

    private int parseIntSafe(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }
    private String escape(String s) { return s==null?"":s.replace("|","%7C").replace(";","%3B"); }
    private String unescape(String s) { return s==null?"":s.replace("%7C","|").replace("%3B",";"); }

    private void fireConnected(){ for (GameClientListener l: listeners) l.onConnected(); }
    private void fireInfo(String m){ for (GameClientListener l: listeners) l.onInfo(m); }
    private void fireError(String m){ for (GameClientListener l: listeners) l.onError(m); }
    private void fireHostLeft(String roomId){ for (GameClientListener l: listeners) l.onHostLeft(roomId); }
    private void fireGameStart(String roomId){ for (GameClientListener l: listeners) l.onGameStart(roomId); }

    // ---------------- Heartbeat ----------------
    private static final long HEARTBEAT_INTERVAL_MS = 5000; // 5초마다 PING
    private void startHeartbeat() {
        heartbeatThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);
                } catch (InterruptedException e) {
                    break; // 종료
                }
                if (!running) break;
                // 활동이 없을 때도 세션이 살아있음을 알리기 위한 PING
                send("PING");
            }
        }, "GameClient-Heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }
}
