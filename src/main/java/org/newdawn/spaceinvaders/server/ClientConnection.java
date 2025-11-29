package org.newdawn.spaceinvaders.server;

import java.io.*;
import java.net.Socket;
import java.util.stream.Collectors;
import org.newdawn.spaceinvaders.server.game.ServerGameSession;
import static org.newdawn.spaceinvaders.server.MessageType.*;

/** 클라이언트 개별 처리 스레드 */
class ClientConnection implements Runnable {
    private final GameServer server;
    private final Socket socket;
    private final BufferedReader in;
    private final PlayerSession session;

    private static final String KEY_ROOM_ID = "roomId";
    private static final String KEY_HOST_ID = "hostId";
    private static final String KEY_SINGLE = "single";
    private static final String LOG_BY_USERNAME = ") by ";

    ClientConnection(GameServer server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        this.session = new PlayerSession(socket, new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true));
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[ClientConnection] Received: " + line + " from " + socket.getInetAddress() + ":" + socket.getPort());
                handle(line.trim());
            }
        } catch (IOException e) {
            System.out.println("[ClientConnection] IO error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        System.out.println("[ClientConnection] Cleaning up for " + socket.getInetAddress() + ":" + socket.getPort());
        leaveRoomInternal(true);
        server.removeConnection(this);
        try { socket.close(); } catch (IOException ignored) {}
    }

    PlayerSession getSession() { return session; }
    void forceClose() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    private void handle(String line) {
        if (line.isEmpty()) return;
        String[] parts = line.split("\\|");
        String type = parts[0];
        session.touch();
        switch (type) {
            case CONNECT: {
                String username = parts.length>1? parts[1] : "guest";
                session.setUsername(username);
                System.out.println("[ClientConnection] CONNECT: username=" + username + ", sid="+session.getId());
                session.getOut().println(INFO+"|msg=CONNECTED|sid="+session.getId());
                break;
            }
            case PING:
                session.getOut().println(PONG);
                break;
            case LIST_ROOMS:
                System.out.println("[ClientConnection] LIST_ROOMS from " + session.getUsername());
                sendRoomList();
                break;
            case CREATE_ROOM:
                System.out.println("[ClientConnection] CREATE_ROOM by " + session.getUsername());
                handleCreate(parts);
                break;
            case JOIN_ROOM:
                if (parts.length>1) {
                    System.out.println("[ClientConnection] JOIN_ROOM " + parts[1] + " by " + session.getUsername());
                    handleJoin(parts[1]);
                }
                break;
            case LEAVE_ROOM:
                System.out.println("[ClientConnection] LEAVE_ROOM by " + session.getUsername());
                leaveRoomInternal(false);
                break;
            case TOGGLE_READY:
                System.out.println("[ClientConnection] TOGGLE_READY by " + session.getUsername());
                toggleReady();
                break;
            case START_GAME:
                System.out.println("[ClientConnection] START_GAME by " + session.getUsername());
                startGame();
                break;
            case CHAT:
                System.out.println("[ClientConnection] CHAT by " + session.getUsername());
                handleChat(parts);
                break;
            case GET_ROOM_STATE:
                // 현재 속한 방 상태 재전송 (폴링용)
                Room cr = session.getCurrentRoom();
                if (cr != null) {
                    broadcastRoomState(cr, false); // 단일 세션에만 보내고 싶다면 개별 전송 로직 필요
                } else {
                    session.getOut().println(INFO+"|msg=NO_ROOM");
                }
                break;
            case GAME_READY:
                handleGameReady(parts);
                break;
            case GAME_INPUT:
                handleGameInput(parts);
                break;
            case STATE_ACK:
                handleStateAck(parts);
                break;
            case STATE_REQUEST:
                handleStateRequest(parts);
                break;
            case GAME_ACTION:
                handleGameAction(parts);
                break;
            default:
                System.out.println("[ClientConnection] UNKNOWN_TYPE: " + type + " from " + session.getUsername());
                session.getOut().println(ERROR+"|msg=UNKNOWN_TYPE");
                break;
        }
    }

    private void handleGameReady(String[] parts) {
        String roomId = getValue(parts, KEY_ROOM_ID);
        if (roomId == null) {
            Room room = session.getCurrentRoom();
            if (room != null) roomId = room.getId();
        }
        if (roomId == null) return;
        ServerGameSession gs = server.getGameManager().getSession(roomId);
        if (gs != null) {
            String seedAck = getValue(parts, "seedAck");
            gs.handleReady(session, seedAck);
        }
    }

    private void handleGameInput(String[] parts) {
        String roomId = getValue(parts, KEY_ROOM_ID);
        if (roomId == null) {
            Room room = session.getCurrentRoom();
            if (room != null) roomId = room.getId();
        }
        if (roomId == null) return;
        ServerGameSession gs = server.getGameManager().getSession(roomId);
        if (gs == null) return;
        int seq = parseIntSafe(getValue(parts, "seq"));
        int mask = parseIntSafe(getValue(parts, "mask"));
        long clientTime = parseLongSafe(getValue(parts, "clientTime"));
        gs.handleInput(session, seq, mask, clientTime);
    }

    private void handleStateAck(String[] parts) {
        String roomId = getValue(parts, KEY_ROOM_ID);
        if (roomId == null) {
            Room room = session.getCurrentRoom();
            if (room != null) roomId = room.getId();
        }
        if (roomId == null) return;
        ServerGameSession gs = server.getGameManager().getSession(roomId);
        if (gs != null) {
            long tick = parseLongSafe(getValue(parts, "tick"));
            gs.handleStateAck(session, tick);
        }
    }

    private void handleStateRequest(String[] parts) {
        String roomId = getValue(parts, KEY_ROOM_ID);
        if (roomId == null) {
            Room room = session.getCurrentRoom();
            if (room != null) roomId = room.getId();
        }
        if (roomId == null) return;
        ServerGameSession gs = server.getGameManager().getSession(roomId);
        if (gs != null) {
            long fromTick = parseLongSafe(getValue(parts, "fromTick"));
            gs.handleStateRequest(session, fromTick);
        }
    }

    private void handleGameAction(String[] parts) {
        String roomId = getValue(parts, KEY_ROOM_ID);
        if (roomId == null) {
            Room room = session.getCurrentRoom();
            if (room != null) roomId = room.getId();
        }
        if (roomId == null) return;
        ServerGameSession gs = server.getGameManager().getSession(roomId);
        if (gs != null) {
            String action = getValue(parts, "action");
            String data = getValue(parts, "data");
            gs.handleAction(session, action, data);
        }
    }

    private void sendRoomList() {
        java.util.List<Room> list = server.getRoomManager().listVisibleRooms();
        String payload = list.stream().map(r -> r.getId()+","+escape(r.getName())+","+(r.isSingle()?KEY_SINGLE:"multi")+","+r.getPlayers().size()+","+r.getMaxPlayers())
                .collect(Collectors.joining(";"));
        session.getOut().println(ROOMS+"|list="+payload);
        System.out.println("[ClientConnection] Sent room list to " + session.getUsername() + ": " + payload);
    }

    private void handleCreate(String[] parts) {
        // 기본값
        String name = "방";
        boolean single = false;
        int max = 4;
        for (int i=1;i<parts.length;i++) {
            String p = parts[i];
            if (p.startsWith("name=")) name = unescape(p.substring(5));
            else if (p.startsWith("type=")) single = p.substring(5).equalsIgnoreCase(KEY_SINGLE);
            else if (p.startsWith("max=")) { try { max = Integer.parseInt(p.substring(4)); } catch (Exception ignored) {} }
        }
        Room room = server.getRoomManager().createRoom(name, single, max);
        System.out.println("[ClientConnection] Room created: " + room.getId() + " (" + name + ", single=" + single + ", max=" + max + ")" + LOG_BY_USERNAME + session.getUsername());
        room.addPlayer(session);
        broadcastRoomState(room, true);
        session.getOut().println(ROOM_JOINED + "|" + KEY_ROOM_ID + "=" + room.getId() + "|" + KEY_HOST_ID + "=" + room.getHost().getId() + "|" + KEY_SINGLE + "=" + (room.isSingle() ? "1" : "0"));
        if (single) {
            // 싱글이면 바로 게임 시작 신호 주거나 로비 상태 한번 전송
            session.getOut().println(INFO+"|msg=SINGLE_ROOM_CREATED");
        }
    }

    private void handleJoin(String roomId) {
        Room room = server.getRoomManager().getRoom(roomId);
        // 고아/손상된 방 제거 로직
        if (room != null) {
            if (room.getPlayers().isEmpty() || room.getHost()==null) {
                server.getRoomManager().removeRoom(room.getId());
                System.out.println("[ClientConnection] Removed stale room " + room.getId());
                room = null;
            }
        }
        if (room == null || room.isStarted()) {
            System.out.println("[ClientConnection] JOIN_ROOM failed: not found or started (" + roomId + ")" + LOG_BY_USERNAME + session.getUsername());
            session.getOut().println(ERROR+"|msg=ROOM_NOT_FOUND");
            return;
        }
        if (!room.addPlayer(session)) {
            System.out.println("[ClientConnection] JOIN_ROOM failed: full (" + roomId + ")" + LOG_BY_USERNAME + session.getUsername());
            session.getOut().println(ERROR+"|msg=ROOM_FULL");
            return;
        }
        System.out.println("[ClientConnection] JOIN_ROOM success: " + roomId + LOG_BY_USERNAME + session.getUsername());
        broadcastRoomState(room, true);
        session.getOut().println(ROOM_JOINED + "|" + KEY_ROOM_ID + "=" + room.getId() + "|" + KEY_HOST_ID + "=" + room.getHost().getId() + "|" + KEY_SINGLE + "=" + (room.isSingle() ? "1" : "0"));
    }

    private void leaveRoomInternal(boolean disconnect) {
        Room room = session.getCurrentRoom();
        if (room == null) return;
        System.out.println("[ClientConnection] LEAVE_ROOM: " + room.getId() + LOG_BY_USERNAME + session.getUsername() + (disconnect?" (disconnect)":""));
        boolean wasHost;
        synchronized (room) {
            wasHost = room.getHost() == session;
            room.removePlayer(session);
            server.getGameManager().handlePlayerDeparture(session, room);
            // 호스트가 나갔거나 플레이어가 없어졌으면 방 제거 (요구사항: 호스트 나가면 즉시 삭제)
            if (wasHost || room.getPlayers().isEmpty()) {
                // 남아있는 인원에게 호스트/방 종료 알림
                for (PlayerSession ps : room.getPlayers()) {
                    ps.getOut().println(HOST_LEFT + "|" + KEY_ROOM_ID + "=" + room.getId());
                }
                server.getRoomManager().removeRoom(room.getId());
                server.getGameManager().removeSession(room.getId());
                System.out.println("[ClientConnection] Room forcibly removed (host left or empty): " + room.getId());
            } else if (!wasHost && !room.getPlayers().isEmpty()) {
                // 일반 플레이어 퇴장 -> 상태만 브로드캐스트
                broadcastRoomState(room, false);
            }
        }
        if (!disconnect) session.getOut().println(INFO+"|msg=LEFT_ROOM");
    }

    private void toggleReady() {
        Room room = session.getCurrentRoom();
        if (room == null) return;
        System.out.println("[ClientConnection] TOGGLE_READY: " + session.getUsername() + " in room " + room.getId() + " now " + (!session.isReady()));
        session.setReady(!session.isReady());
        broadcastRoomState(room, false);
    }

    private void startGame() {
        Room room = session.getCurrentRoom();
        if (room == null || room.getHost() != session) return;
        // 모든 참가자 준비 여부 확인 (싱글은 무시)
        boolean allReady = room.isSingle() || room.getPlayers().stream().allMatch(p -> p == room.getHost() || p.isReady());
        if (!allReady) { session.getOut().println(ERROR+"|msg=NOT_ALL_READY"); return; }
        room.setStarted(true);
        if (!room.isSingle()) {
            server.getGameManager().createSession(room);
        }
        server.sendToRoom(room, GAME_START + "|" + KEY_ROOM_ID + "=" + room.getId());
        System.out.println("[ClientConnection] START_GAME: " + room.getId() + LOG_BY_USERNAME + session.getUsername());
    }

    private void handleChat(String[] parts) {
        Room room = session.getCurrentRoom();
        if (room == null) return;
        String msg = parts.length>1? unescape(parts[1]) : "";
        server.sendToRoom(room, CHAT_MSG+"|from="+escape(session.getUsername())+"|msg="+escape(msg));
        System.out.println("[ClientConnection] CHAT: " + session.getUsername() + " in room " + room.getId() + " -> " + msg);
    }

    private void broadcastRoomState(Room room, boolean includeHostJoin) {
        String players = room.getPlayers().stream()
                .map(p -> p.getId()+","+escape(p.getUsername())+","+(p.isReady()?"1":"0")+","+(p==room.getHost()?"1":"0"))
                .collect(Collectors.joining(";"));
    server.sendToRoom(room, ROOM_STATE + "|" + KEY_ROOM_ID + "=" + room.getId() + "|" + KEY_HOST_ID + "=" + room.getHost().getId() + "|" + KEY_SINGLE + "=" + (room.isSingle() ? "1" : "0") + "|players=" + players);
        System.out.println("[ClientConnection] ROOM_STATE broadcasted for room " + room.getId() + ": " + players);
        if (includeHostJoin) {
            // 갱신 직후 방 목록 새로고침 힌트 용 (클라이언트가 LIST_ROOMS 재요청 가능)
        }
    }
    private int parseIntSafe(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }
    private long parseLongSafe(String s) { try { return Long.parseLong(s); } catch (Exception e) { return 0L; } }

    /** parts 배열에서 key에 해당하는 값을 반환 (key=value 형태) */
    private String getValue(String[] parts, String key) {
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            int idx = p.indexOf('=');
            if (idx > 0) {
                String k = p.substring(0, idx);
                if (k.equals(key)) return p.substring(idx + 1);
            }
        }
        return null;
    }

    private String escape(String s) { return s==null?"":s.replace("|","%7C").replace(";","%3B"); }
    private String unescape(String s) { return s==null?"":s.replace("%7C","|").replace("%3B",";"); }
}
