package org.newdawn.spaceinvaders.server;

import com.google.gson.Gson;
import org.newdawn.spaceinvaders.net.RoomInfo;
import org.newdawn.spaceinvaders.net.Snapshot;
import org.newdawn.spaceinvaders.gameplay.core.Rules;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 최소 기능 멀티플레이 서버 (유지보수 쉬운 단일 파일)
 * - 텍스트 라인 기반 JSON 프로토콜
 * - 명령: LIST, CREATE name, JOIN id, READY, START, QUIT
 * - 브로드캐스트: ROOMS [...], JOINED {roomId}, START
 */
public class Server {
    private final int port;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final List<Client> clients = new CopyOnWriteArrayList<>();
    private final Gson gson = new Gson();

    public Server(int port) { this.port = port; }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] Listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                Client client = new Client(socket);
                clients.add(client);
                new Thread(client, "Client-" + socket.getPort()).start();
            }
        }
    }

    private class Room {
        String id = UUID.randomUUID().toString();
        String name;
        int capacity = 4;
        List<Client> members = new CopyOnWriteArrayList<>();
        volatile boolean running = false;
        Thread loopThread;
        // 상태: 플레이어/적/총알/파워업 + 라운드/사격 타이밍
        final Map<String, Snapshot.PlayerState> playerStates = new ConcurrentHashMap<>();
        final List<Entity> aliens = new CopyOnWriteArrayList<>();
        final List<Entity> bullets = new CopyOnWriteArrayList<>();
    // 파워업 시스템 비활성화
        int round = 1;
        final int maxRound = 5;
        // 싱글 규칙: 글로벌 쿨다운 기반(근접 외계인 중 1기 선택) 사격 게이팅
        long lastAlienFireMs = 0L;
    long alienFiringIntervalMs = 1500L; // 라운드에 따라 조정
    final long baseAlienFiringIntervalMs = 1500L;
        final List<Snapshot.Event> eventsBuf = new CopyOnWriteArrayList<>();
    }

    private static class Entity {
        String id = UUID.randomUUID().toString();
        float x, y;
        float vx, vy;
        String type; // "alien" | "bullet"
        String ownerId; // bullet 소유자 id (스코어 계산)
        int pierce = 0; // 관통 가능 횟수 (0이면 관통 없음)
        // alien stats
        int hp = 1;
        // 싱글 규칙: 에일리언 개별 이동/사격 파라미터
        long lastDirectionChange = 0L;
        long directionChangeInterval = 2000L; // ms
        long lastFire = 0L;
        long firingInterval = 2000L; // ms, 라운드 기반 조정 + 랜덤 팩터
    }

    // 클래식 모드에선 파워업 사용 안 함
    // 파워업 타입 제거됨

    private class Client implements Runnable {
        final Socket socket;
        Room room;
        BufferedReader in;
        PrintWriter out;
    volatile boolean ready = false;
    final String playerId = UUID.randomUUID().toString();
    float inputDx = 0; // 좌우 입력만 간소화 (-1,0,1)
    long lastShootAt = 0;
    // 일시적 버프(파워업) 비활성화에 따라 관련 타이머 제거

        Client(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            sendRooms();
        }

        void send(String type, Object payload) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", type);
            msg.put("payload", payload);
            out.println(gson.toJson(msg));
        }

        void sendRooms() {
            List<RoomInfo> list = new ArrayList<>();
            for (Room r : rooms.values()) {
                list.add(new RoomInfo(r.id, r.name, r.members.size(), r.capacity));
            }
            send("ROOMS", list);
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    handleCommand(line.trim());
                }
            } catch (IOException ignored) {
            } finally {
                leave();
                try { socket.close(); } catch (IOException ignored) {}
                clients.remove(this);
            }
        }

        void broadcastRooms() {
            for (Client c : clients) c.sendRooms();
        }

        void handleCommand(String cmd) {
            if (cmd.isEmpty()) return;
            if (cmd.startsWith("LIST")) {
                sendRooms();
            } else if (cmd.startsWith("CREATE")) {
                String name = cmd.length() > 6 ? cmd.substring(6).trim() : ("Room-" + rooms.size());
                Room r = new Room();
                r.name = name.isEmpty() ? ("Room-" + rooms.size()) : name;
                rooms.put(r.id, r);
                send("ROOM_CREATED", Collections.singletonMap("id", r.id));
                broadcastRooms();
            } else if (cmd.startsWith("JOIN")) {
                String id = cmd.length() > 4 ? cmd.substring(4).trim() : "";
                Room r = rooms.get(id);
                if (r != null && r.members.size() < r.capacity) {
                    leave();
                    r.members.add(this);
                    room = r;
                    ready = false;
                    // 플레이어 상태 초기화
                    Snapshot.PlayerState ps = new Snapshot.PlayerState();
                    ps.id = playerId;
                    ps.x = 400; ps.y = 550; ps.hp = 3; ps.maxHp = 3;
                    ps.score = 0; ps.skillPoints = 0; ps.attackPower = 1; ps.attackSpeed = 1.0;
                    ps.costAtk = 2; ps.costAspd = 2; ps.costHp = 8;
                    r.playerStates.put(playerId, ps);
                    send("JOINED", Collections.singletonMap("roomId", r.id));
                    send("PLAYER", Collections.singletonMap("id", playerId));
                    startRoomIfReady(r);
                } else {
                    send("ERROR", Collections.singletonMap("msg", "JOIN_FAILED"));
                }
            } else if (cmd.startsWith("READY")) {
                ready = true;
            } else if (cmd.startsWith("START")) {
                if (room != null) {
                    boolean allReady = true;
                    for (Client c : room.members) if (!c.ready) { allReady = false; break; }
                    if (allReady && room.members.size() >= 1) {
                        for (Client c : room.members) c.send("START", null);
                        startRoomIfReady(room);
                    }
                }
            } else if (cmd.startsWith("INPUT")) {
                // INPUT dx:-1|0|1
                if (room != null) {
                    try {
                        String[] sp = cmd.split(" ",2);
                        if (sp.length==2 && sp[1].startsWith("dx:")) {
                            inputDx = Float.parseFloat(sp[1].substring(3));
                        }
                    } catch (Exception ignored) {}
                }
            } else if (cmd.startsWith("KEY")) {
                if (room != null) {
                    String[] sp = cmd.split(" ");
                    if (sp.length >= 3) {
                        boolean down = "DOWN".equals(sp[1]);
                        String key = sp[2];
                        if ("LEFT".equals(key)) {
                            inputDx = down ? -1 : (inputDx == -1 ? 0 : inputDx);
                        } else if ("RIGHT".equals(key)) {
                            inputDx = down ? 1 : (inputDx == 1 ? 0 : inputDx);
                        } else if ("FIRE".equals(key)) {
                            // 키다운 시 즉시 SHOOT 트리거(싱글은 키홀드 연사 X)
                            if (down) handleCommand("SHOOT");
                        }
                    }
                }
            } else if (cmd.startsWith("SHOOT")) {
                if (room != null) {
                    Snapshot.PlayerState ps = room.playerStates.get(playerId);
                    if (ps != null) {
                        long now = System.currentTimeMillis();
                        // 싱글과 동일한 기본 쿨다운(공속 반영)
                        long cd = (long)(Rules.PLAYER_BASE_FIRING_INTERVAL_MS / Math.max(0.1, ps.attackSpeed));
                        if (now - lastShootAt < cd) return; // 쿨다운
                        lastShootAt = now;
                        // 단일 탄환 발사(싱글 기본과 동일). 관통/3연사 등은 멀티 파워업 비활성화 상태에서는 사용하지 않음
                        Entity b = new Entity();
                        b.type = "bullet";
                        b.x = ps.x; b.y = ps.y - 15;
                        b.vx = 0; b.vy = Rules.BULLET_PLAYER_SPEED_Y; // px/sec 위로
                        b.ownerId = playerId;
                        b.pierce = 0;
                        room.bullets.add(b);
                        Snapshot.Event ev = new Snapshot.Event(); ev.type = "shoot"; ev.x = b.x; ev.y = b.y; room.eventsBuf.add(ev);
                    }
                }
            } else if (cmd.startsWith("UPGRADE")) {
                if (room != null) {
                    try {
                        int idx = Integer.parseInt(cmd.substring("UPGRADE".length()).trim());
                        Snapshot.PlayerState ps = room.playerStates.get(playerId);
                        if (ps != null) {
                            if (idx == 0) { // 공격력 +1
                                if (ps.skillPoints >= ps.costAtk) {
                                    ps.skillPoints -= ps.costAtk; ps.attackPower += 1; ps.costAtk += 1;
                                }
                            } else if (idx == 1) { // 공격속도 +0.2x
                                if (ps.skillPoints >= ps.costAspd) {
                                    ps.skillPoints -= ps.costAspd; ps.attackSpeed = Math.round((ps.attackSpeed + 0.2)*10)/10.0; ps.costAspd += 1;
                                }
                            } else if (idx == 2) { // 최대HP +3 및 풀회복
                                if (ps.skillPoints >= ps.costHp) {
                                    ps.skillPoints -= ps.costHp; ps.maxHp += 3; ps.hp = ps.maxHp; ps.costHp += 5;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            } else if (cmd.startsWith("QUIT")) {
                leave();
            }
        }

        void leave() {
            if (room != null) {
                room.members.remove(this);
                room.playerStates.remove(playerId);
                if (room.members.isEmpty()) {
                    rooms.remove(room.id);
                    broadcastRooms();
                }
                room = null;
            }
        }
    }

    private void startRoomIfReady(Room r) {
        if (r.running) return;
        boolean any = !r.members.isEmpty();
        if (!any) return;
        r.running = true;
        r.round = 1;
        // 라운드별 글로벌 에일리언 사격 간격 설정(싱글 GameStateManager와 동일 규칙)
        r.alienFiringIntervalMs = computeAlienFiringInterval(r.round, r.baseAlienFiringIntervalMs);
        spawnRoundGrid(r, r.round);
        r.loopThread = new Thread(() -> runRoomLoop(r), "RoomLoop-"+r.id);
        r.loopThread.setDaemon(true);
        r.loopThread.start();
    }

    private void runRoomLoop(Room r) {
        long last = System.currentTimeMillis();
        while (r.running && rooms.containsKey(r.id)) {
            long now = System.currentTimeMillis();
            float dt = (now - last) / 1000f;
            last = now;
            // 플레이어 이동: 싱글플레이와 동일 속도
            for (Client c : r.members) {
                Snapshot.PlayerState ps = r.playerStates.get(c.playerId);
                if (ps != null) {
                    ps.x += c.inputDx * Rules.PLAYER_MOVE_SPEED * dt; // px/sec
                    // 싱글은 좌상단 기준, 멀티는 중심 좌표이므로 절반 폭을 감안하여 클램프
                    float minX = Rules.PLAYER_HALF_WIDTH;
                    float maxX = Rules.WIDTH - Rules.PLAYER_HALF_WIDTH;
                    if (ps.x < minX) ps.x = minX; if (ps.x > maxX) ps.x = maxX;
                }
            }
            // 외계인 개별 이동/방향 전환(싱글 규칙 유사)
            if (!r.aliens.isEmpty()) {
                for (Entity a : r.aliens) {
                    // 이동
                    a.x += a.vx * dt;
                    a.y += a.vy * dt;
                    a.lastDirectionChange += (long)(dt * 1000);
                    // 경계 체크 및 방향 전환
                    if (a.x < Rules.ALIEN_MIN_X) { a.x = Rules.ALIEN_MIN_X; a.vx = Math.abs(a.vx); }
                    else if (a.x > Rules.ALIEN_MAX_X) { a.x = Rules.ALIEN_MAX_X; a.vx = -Math.abs(a.vx); }
                    if (a.y < Rules.ALIEN_MIN_Y) { a.y = Rules.ALIEN_MIN_Y; a.vy = Math.abs(a.vy); }
                    else if (a.y > Rules.ALIEN_MAX_Y) { a.y = Rules.ALIEN_MAX_Y; a.vy = -Math.abs(a.vy); }
                    // 랜덤 방향 전환 (10%/주기)
                    if (a.lastDirectionChange > a.directionChangeInterval) {
                        if (Math.random() < 0.1) { a.vx = -a.vx; }
                        a.lastDirectionChange = 0L;
                    }
                }
            }
            // 외계인 사격(싱글과 동일: 글로벌 쿨다운 + 근접 외계인 1기 선택)
            if (!r.aliens.isEmpty() && !r.playerStates.isEmpty()) {
                long nowMs = System.currentTimeMillis();
                if (nowMs - r.lastAlienFireMs >= r.alienFiringIntervalMs) {
                    // 근접 외계인 수집(플레이어 Y와의 차이 200 내)
                    float playerY = Rules.PLAYER_Y;
                    List<Entity> nearAliens = new ArrayList<>();
                    for (Entity e : r.aliens) {
                        if (Math.abs(e.y - playerY) < 200) nearAliens.add(e);
                    }
                    if (!nearAliens.isEmpty()) {
                        Entity shooter = nearAliens.get((int)(Math.random() * nearAliens.size()));
                        // 싱글 AlienEntity.tryToFire 조건 모사 + 개별 쿨다운
                        // 발사 여부 추적 불필요
                        if (nowMs - shooter.lastFire >= shooter.firingInterval) {
                            int round = r.round;
                            float dy = Math.abs(shooter.y - playerY);
                            boolean shouldFire = false;
                            if (round <= 2) {
                                if (dy < 350) shouldFire = Math.random() < 0.6;
                            } else if (round <= 4) {
                                if (dy < 450) shouldFire = Math.random() < 0.8;
                            } else {
                                if (dy < 500) shouldFire = Math.random() < 0.9;
                                if (dy < 150) shouldFire = true;
                            }
                            if (round >= 5 && Math.random() < 0.05) shouldFire = true;
                            if (shouldFire) {
                                // 타겟 플레이어(X 거리 최소)
                                Snapshot.PlayerState target = null;
                                double best = Double.MAX_VALUE;
                                for (Snapshot.PlayerState ps : r.playerStates.values()) {
                                    double d = Math.abs(ps.x - shooter.x);
                                    if (d < best) { best = d; target = ps; }
                                }
                                float spawnX = shooter.x;
                                if (target != null) {
                                    float aimOffset = Rules.aimedOffsetX(shooter.x, target.x);
                                    spawnX = shooter.x + aimOffset;
                                }
                                Entity b = new Entity();
                                b.type = "bullet"; b.x = spawnX; b.y = shooter.y + 30; b.vx = 0; b.vy = 300; b.ownerId = null;
                                r.bullets.add(b);
                                shooter.lastFire = nowMs;
                                // 발사 수행됨
                            }
                        }
                        // 싱글과 동일: 근접 외계인이 있을 때에만 글로벌 타이머 초기화
                        r.lastAlienFireMs = nowMs;
                    }
                }
            }
            // 총알 이동
            List<Entity> bulletsToRemove = new ArrayList<>();
            for (Entity b : r.bullets) {
                b.x += b.vx * dt; b.y += b.vy * dt;
                // 화면 밖으로 나간 탄 제거 (상/하/좌/우)
                if (b.y < -20 || b.y > 620 || b.x < -20 || b.x > 820) bulletsToRemove.add(b);
            }
            if (!bulletsToRemove.isEmpty()) r.bullets.removeAll(bulletsToRemove);
            // 충돌 판정 (총알 vs 외계인 / 적 탄 vs 플레이어)
            float hitR2 = 15*15;
            List<Entity> bulletsHit = new ArrayList<>();
            List<Entity> aliensHit = new ArrayList<>();
            List<Snapshot.Event> tickEvents = new ArrayList<>();
            for (Entity b : r.bullets) {
                if (b.ownerId != null) {
                    // 플레이어 탄: 외계인과 충돌
                    for (Entity a : r.aliens) {
                        float dx = a.x - b.x, dy = a.y - b.y;
                        if (dx*dx + dy*dy <= hitR2) {
                            // 플레이어 공격력 적용
                            Snapshot.PlayerState owner = r.playerStates.get(b.ownerId);
                            int dmg = (owner != null ? Math.max(1, owner.attackPower) : 1);
                            a.hp -= dmg;
                            if (a.hp <= 0) aliensHit.add(a);
                            if (b.pierce > 0) { b.pierce -= 1; } else { bulletsHit.add(b); }
                            // 스코어/스킬포인트 가산
                            if (owner != null) {
                                owner.score += 10;
                                // 처치 시 소량의 스킬 포인트 확률 지급 (싱글플레이 SkillManager의 확률을 단순화)
                                if (Math.random() < 0.25) owner.skillPoints += 1;
                            }
                            // 싱글과의 체감 일치를 위해 임시 파워업 드랍은 사용하지 않음
                            Snapshot.Event ev = new Snapshot.Event(); ev.type = "explode"; ev.x = a.x; ev.y = a.y; tickEvents.add(ev);
                            break;
                        }
                    }
                } else {
                    // 적 탄: 플레이어와 충돌 (무적 체크)
                    for (Client c : r.members) {
                        Snapshot.PlayerState ps = r.playerStates.get(c.playerId);
                        if (ps == null) continue;
                        float dx = ps.x - b.x, dy = ps.y - b.y;
                        if (dx*dx + dy*dy <= hitR2) {
                            // 싱글 모드와 유사하게 단순 피격 처리(무적/버프 제거)
                            if (ps.hp > 0) ps.hp -= 1;
                            bulletsHit.add(b);
                            Snapshot.Event ev = new Snapshot.Event(); ev.type = "hit"; ev.x = ps.x; ev.y = ps.y; tickEvents.add(ev);
                            break;
                        }
                    }
                }
            }
            if (!aliensHit.isEmpty()) {
                r.aliens.removeAll(aliensHit);
                // 남은 외계인 속도 증가: 싱글 notifyAlienKilled와 동일(1.5% + 라운드*1%)
                float mult = (float) Rules.remainingAlienSpeedMultiplier(r.round);
                for (Entity a : r.aliens) { a.vx *= mult; a.vy *= mult; }
            }
            if (!bulletsHit.isEmpty()) r.bullets.removeAll(bulletsHit);
            // 파워업 시스템 비활성화(싱글과 동일 체감 유지)
            // 라운드 클리어 및 진행
            if (r.aliens.isEmpty()) {
                if (r.round < r.maxRound) {
                    r.round++;
                    spawnRoundGrid(r, r.round);
                    // 라운드 진입 시 글로벌 사격 간격 갱신
                    r.alienFiringIntervalMs = computeAlienFiringInterval(r.round, r.baseAlienFiringIntervalMs);
                    // 라운드 보상: 모든 플레이어에 스킬 포인트 지급
                    for (Snapshot.PlayerState ps : r.playerStates.values()) {
                        ps.skillPoints += 2; // 기본 2포인트 보상
                    }
                    Snapshot.Event ev = new Snapshot.Event(); ev.type = "round_advanced"; ev.x = 0; ev.y = r.round;
                    r.eventsBuf.add(ev);
                } else {
                    Snapshot.Event ev = new Snapshot.Event(); ev.type = "game_completed"; ev.x = 0; ev.y = 0;
                    r.eventsBuf.add(ev);
                    r.running = false;
                }
            }

            // 스냅샷 생성
            Snapshot snap = new Snapshot();
            snap.tick = now;
            snap.players = new ArrayList<>(r.playerStates.values());
            snap.wave = r.round;
            // aliens
            List<Snapshot.EntityState> aliens = new ArrayList<>();
            for (Entity a : r.aliens) {
                Snapshot.EntityState es = new Snapshot.EntityState();
                es.id = a.id; es.x = a.x; es.y = a.y; es.type = a.type;
                aliens.add(es);
            }
            snap.aliens = aliens;
            // bullets
            List<Snapshot.EntityState> bullets = new ArrayList<>();
            for (Entity b : r.bullets) {
                Snapshot.EntityState es = new Snapshot.EntityState();
                es.id = b.id; es.x = b.x; es.y = b.y; es.type = (b.ownerId != null ? "bullet_player" : "bullet_enemy");
                bullets.add(es);
            }
            snap.bullets = bullets;
            // 클래스식 모드: 파워업/이벤트 없음
            snap.powerups = Collections.emptyList();
            snap.events = Collections.emptyList();
            Map<String,Object> msg = new HashMap<>();
            msg.put("type", "SNAPSHOT");
            msg.put("payload", snap);
            String line = gson.toJson(msg);
            for (Client c : r.members) c.out.println(line);
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }
    }

    private long computeAlienFiringInterval(int round, long base) {
        return Rules.globalAlienFiringIntervalMs(round, base);
    }

    private void spawnRoundGrid(Room r, int round) {
        // 싱글 규칙: 라운드별 그리드 및 좌표
        int rows, cols;
        switch (round) {
            case 1: rows = 3; cols = 6; break;
            case 2: rows = 3; cols = 7; break;
            case 3: rows = 4; cols = 7; break;
            case 4: rows = 4; cols = 8; break;
            case 5: rows = 5; cols = 8; break;
            default: rows = 3; cols = 6; break;
        }
        r.aliens.clear();
        for (int rowi = 0; rowi < rows; rowi++) {
            for (int xi = 0; xi < cols; xi++) {
                Entity a = new Entity();
                a.type = "alien";
                a.x = 120 + (xi * 70);
                a.y = 60 + rowi * 35;
                // 싱글 AlienEntity 스탯 스케일링과 동일
                a.hp = Rules.alienHpForRound(round);
                float moveSpeed = Rules.alienMoveSpeedForRound(round);
                a.vx = (Math.random() < 0.5 ? -moveSpeed : moveSpeed);
                a.vy = (Math.random() < 0.5 ? -1 : 1) * (moveSpeed * 0.3f);
                // 방향 전환 주기(개별)
                a.directionChangeInterval = 2000L;
                a.lastDirectionChange = 0L;
                // 사격 간격(라운드 기반 + 랜덤 ±20%)
                a.firingInterval = Rules.alienIndividualFiringIntervalMs(round);
                r.aliens.add(a);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 5057;
        new Server(port).start();
    }
}
