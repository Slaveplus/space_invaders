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
    private volatile boolean autoStartSingle = false;
    // 로컬 임베디드 모드(서버를 띄운 척)
    private volatile boolean localMode = false;
    private LocalSession local;

    public void addListener(Listener l) { listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }
    public void enableAutoStartSingle() { autoStartSingle = true; }
    public void enableLocalMode() {
        if (localMode) return;
        localMode = true;
        local = new LocalSession();
        local.init();
        this.playerId = local.playerId;
        // 가짜 플레이어 ID 통지
        for (Listener l : listeners) l.onPlayerId(playerId);
        // 초기 방 목록 통지
        for (Listener l : listeners) l.onRooms(java.util.Collections.singletonList(new RoomInfo(local.roomId, local.roomName, 0, 1)));
    }

    public boolean isConnected() { return socket != null && socket.isConnected() && !socket.isClosed(); }

    public boolean connect(String host, int port) {
        if (localMode) return true; // 로컬 모드에서는 소켓 연결 불필요
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
        } else if ("ROOM_CREATED".equals(type)) {
            java.lang.reflect.Type mapType2 = new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> p = gson.fromJson(gson.toJson(payload), mapType2);
            String id = (String) p.get("id");
            if (autoStartSingle && id != null) {
                joinRoom(id);
            }
        } else if ("JOINED".equals(type)) {
            Map<String, Object> p = gson.fromJson(gson.toJson(payload), mapType);
            String roomId = (String) p.get("roomId");
            for (Listener l : listeners) l.onJoined(roomId);
            if (autoStartSingle) {
                // 싱글 자동 시작: READY 후 START
                ready();
                start();
            }
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

    public void listRooms() { if (localMode && local != null) local.listRooms(); else send("LIST"); }
    public void createRoom(String name) { if (localMode && local != null) local.createRoom(name); else send("CREATE " + (name == null ? "" : name)); }
    public void joinRoom(String id) { if (localMode && local != null) local.join(id); else send("JOIN " + id); }
    public void ready() { if (localMode && local != null) local.ready(); else send("READY"); }
    public void start() { if (localMode && local != null) local.start(); else send("START"); }
    public void inputDx(int dx) { if (localMode && local != null) local.inputDx(dx); else send("INPUT dx:" + dx); }
    public void inputShoot() { if (localMode && local != null) local.shootOnce(); else send("SHOOT"); }
    public void upgrade(int idx) { if (localMode && local != null) local.upgrade(idx); else send("UPGRADE " + idx); }

    // 키 입력 세분화 (싱글 체감 일치)
    public void keyDownLeft() { if (localMode && local != null) local.keyDownLeft(); else send("KEY DOWN LEFT"); }
    public void keyUpLeft() { if (localMode && local != null) local.keyUpLeft(); else send("KEY UP LEFT"); }
    public void keyDownRight() { if (localMode && local != null) local.keyDownRight(); else send("KEY DOWN RIGHT"); }
    public void keyUpRight() { if (localMode && local != null) local.keyUpRight(); else send("KEY UP RIGHT"); }
    public void keyDownFire() { if (localMode && local != null) local.keyDownFire(); else send("KEY DOWN FIRE"); }
    public void keyUpFire() { if (localMode && local != null) local.keyUpFire(); else send("KEY UP FIRE"); }

    private void send(String s) {
        if (out != null) out.println(s);
    }

    public void disconnect() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null; in = null; out = null;
        if (localMode && local != null) local.stop();
    }

    public Snapshot getLatestSnapshot() { return latestSnapshot; }
    public String getPlayerId() { return playerId; }

    // ================= Local embedded session =================
    private class LocalSession {
        String roomId = "local-room";
        String roomName = "single";
        boolean joined = false;
    // ready flag not required in local mode (immediate start)
        boolean running = false;
        Thread loop;
        // 입력 상태
        boolean leftHeld=false, rightHeld=false, fireHeld=false;
        int dx = 0; // -1,0,1
        // 플레이어
        String playerId = java.util.UUID.randomUUID().toString();
        org.newdawn.spaceinvaders.net.Snapshot.PlayerState me;
        // 엔티티(간소화)
        java.util.List<LEntity> aliens = new java.util.concurrent.CopyOnWriteArrayList<>();
        java.util.List<LEntity> bullets = new java.util.concurrent.CopyOnWriteArrayList<>();
        int round = 1; final int maxRound = 5;
        long lastAlienFireMs = 0L; long alienFiringIntervalMs = 1500L; final long baseAlienFiringIntervalMs = 1500L;
        long lastShootAt = 0L;

        void init() { /* no-op */ }
        void listRooms() { for (Listener l : listeners) l.onRooms(java.util.Collections.singletonList(new RoomInfo(roomId, roomName, joined?1:0, 1))); }
        void createRoom(String name) { if (name != null && !name.isEmpty()) roomName = name; /* local only */ }
        void join(String id) {
            if (!roomId.equals(id)) return;
            joined = true;
            me = new org.newdawn.spaceinvaders.net.Snapshot.PlayerState();
            me.id = playerId; me.x = 400; me.y = (float)org.newdawn.spaceinvaders.gameplay.core.Rules.PLAYER_Y; me.hp = 3; me.maxHp = 3; me.score = 0;
            me.skillPoints = 0; me.attackPower = 1; me.attackSpeed = 1.0; me.costAtk=2; me.costAspd=2; me.costHp=8;
            for (Listener l : listeners) l.onJoined(roomId);
        }
    void ready() { /* no-op for local */ }
        void start() {
            if (running) return; running = true; round = 1; aliens.clear(); bullets.clear();
            alienFiringIntervalMs = org.newdawn.spaceinvaders.gameplay.core.Rules.globalAlienFiringIntervalMs(round, baseAlienFiringIntervalMs);
            spawnRound(); for (Listener l : listeners) l.onStart();
            loop = new Thread(this::runLoop, "LocalSessionLoop"); loop.setDaemon(true); loop.start();
        }
        void stop() { running = false; }
        void inputDx(int dx) { this.dx = dx; this.leftHeld = dx < 0; this.rightHeld = dx > 0; }
        void keyDownLeft() { leftHeld = true; recomputeDx(); }
        void keyUpLeft() { leftHeld = false; recomputeDx(); }
        void keyDownRight() { rightHeld = true; recomputeDx(); }
        void keyUpRight() { rightHeld = false; recomputeDx(); }
        void keyDownFire() { fireHeld = true; }
        void keyUpFire() { fireHeld = false; }
        void shootOnce() { tryShootOnce(System.currentTimeMillis()); }
        void upgrade(int idx) {
            if (idx==0 && me.skillPoints>=me.costAtk) { me.skillPoints-=me.costAtk; me.attackPower+=1; me.costAtk+=1; }
            else if (idx==1 && me.skillPoints>=me.costAspd) { me.skillPoints-=me.costAspd; me.attackSpeed = Math.round((me.attackSpeed+0.2)*10)/10.0; me.costAspd+=1; }
            else if (idx==2 && me.skillPoints>=me.costHp) { me.skillPoints-=me.costHp; me.maxHp+=3; me.hp=me.maxHp; me.costHp+=5; }
        }
        void recomputeDx() { if (leftHeld && !rightHeld) dx=-1; else if (rightHeld && !leftHeld) dx=1; else dx=0; }

        void tryShootOnce(long now) {
            long cd = (long)(org.newdawn.spaceinvaders.gameplay.core.Rules.PLAYER_BASE_FIRING_INTERVAL_MS / Math.max(0.1, me.attackSpeed));
            if (now - lastShootAt < cd) return;
            lastShootAt = now;
            LEntity b = new LEntity(); b.type="bullet"; b.x=me.x+10; b.y=me.y-30; b.vx=0; b.vy=org.newdawn.spaceinvaders.gameplay.core.Rules.BULLET_PLAYER_SPEED_Y; b.hp=1;
            bullets.add(b);
        }

        void spawnRound() {
            int rows, cols; switch (round){ case 1: rows=3; cols=6; break; case 2: rows=3; cols=7; break; case 3: rows=4; cols=7; break; case 4: rows=4; cols=8; break; case 5: rows=5; cols=8; break; default: rows=3; cols=6; }
            for (int row=0; row<rows; row++) for (int xi=0; xi<cols; xi++) {
                LEntity a = new LEntity(); a.type="alien"; a.x=120+(xi*70); a.y=60+row*35; a.hp=org.newdawn.spaceinvaders.gameplay.core.Rules.alienHpForRound(round);
                float ms = org.newdawn.spaceinvaders.gameplay.core.Rules.alienMoveSpeedForRound(round); a.vx=(Math.random()<0.5?-ms:ms); a.vy=(Math.random()<0.5?-1:1)*(ms*0.3f);
                a.dirInterval=2000L; a.timeSinceDir=0L; a.fireInterval=org.newdawn.spaceinvaders.gameplay.core.Rules.alienIndividualFiringIntervalMs(round); a.lastFire=0L;
                aliens.add(a);
            }
        }

        void runLoop() {
            long last = System.currentTimeMillis();
            while (running) {
                long now = System.currentTimeMillis(); float dt = (now-last)/1000f; last=now;
                // player move
                me.x += dx * org.newdawn.spaceinvaders.gameplay.core.Rules.PLAYER_MOVE_SPEED * dt;
                float minX = org.newdawn.spaceinvaders.gameplay.core.Rules.PLAYER_HALF_WIDTH; float maxX = org.newdawn.spaceinvaders.gameplay.core.Rules.WIDTH - org.newdawn.spaceinvaders.gameplay.core.Rules.PLAYER_HALF_WIDTH;
                if (me.x<minX) me.x=minX; if (me.x>maxX) me.x=maxX;
                // hold fire
                if (fireHeld) tryShootOnce(now);

                // aliens move and bounce
                for (LEntity a: aliens) {
                    a.x += a.vx*dt; a.y += a.vy*dt; a.timeSinceDir += (long)(dt*1000);
                    if (a.x < org.newdawn.spaceinvaders.gameplay.core.Rules.ALIEN_MIN_X) { a.x = org.newdawn.spaceinvaders.gameplay.core.Rules.ALIEN_MIN_X; a.vx=Math.abs(a.vx);} else if (a.x > org.newdawn.spaceinvaders.gameplay.core.Rules.ALIEN_MAX_X) { a.x = org.newdawn.spaceinvaders.gameplay.core.Rules.ALIEN_MAX_X; a.vx=-Math.abs(a.vx);} 
                    if (a.y < org.newdawn.spaceinvaders.gameplay.core.Rules.ALIEN_MIN_Y) { a.y = org.newdawn.spaceinvaders.gameplay.core.Rules.ALIEN_MIN_Y; a.vy=Math.abs(a.vy);} else if (a.y > org.newdawn.spaceinvaders.gameplay.core.Rules.ALIEN_MAX_Y) { a.y = org.newdawn.spaceinvaders.gameplay.core.Rules.ALIEN_MAX_Y; a.vy=-Math.abs(a.vy);} 
                    if (a.timeSinceDir > a.dirInterval) { if (Math.random()<0.1) { a.vx = -a.vx; } a.timeSinceDir=0L; }
                }
                // alien-alien separation
                float minD=24f, minD2=minD*minD; for (int i=0;i<aliens.size();i++){ LEntity a=aliens.get(i); for(int j=i+1;j<aliens.size();j++){ LEntity b=aliens.get(j); float dx=b.x-a.x, dy=b.y-a.y; float d2=dx*dx+dy*dy; if(d2<minD2){ a.vx=-a.vx; b.vx=-b.vx; if(Math.random()<0.3) a.vy=-a.vy; if(Math.random()<0.3) b.vy=-b.vy; float d=(float)Math.max(1e-3, Math.sqrt(d2)); float nx=dx/d, ny=dy/d; float push=(minD-d)*0.5f; a.x-=nx*push; a.y-=ny*push; b.x+=nx*push; b.y+=ny*push; }}}

                // alien firing (global gate + near filter)
                if (!aliens.isEmpty() && now - lastAlienFireMs >= alienFiringIntervalMs){
                    java.util.List<LEntity> near = new java.util.ArrayList<>(); for(LEntity a: aliens){ if (Math.abs(a.y - org.newdawn.spaceinvaders.gameplay.core.Rules.PLAYER_Y) < 200) near.add(a);} 
                    if (!near.isEmpty()){
                        LEntity shooter = near.get((int)(Math.random()*near.size())); if (now - shooter.lastFire >= shooter.fireInterval){
                            float spawnX = shooter.x + org.newdawn.spaceinvaders.gameplay.core.Rules.aimedOffsetX(shooter.x, me.x+10);
                            LEntity b=new LEntity(); b.type="bullet_enemy"; b.x=spawnX; b.y=shooter.y+30; b.vx=0; b.vy=org.newdawn.spaceinvaders.gameplay.core.Rules.BULLET_ENEMY_SPEED_Y; bullets.add(b); shooter.lastFire=now; }
                        lastAlienFireMs = now;
                    }
                }

                // bullets move and cleanup
                java.util.List<LEntity> rm = new java.util.ArrayList<>();
                for (LEntity b: bullets){ b.x+=b.vx*dt; b.y+=b.vy*dt; if (b.y<-20||b.y>620||b.x<-20||b.x>820) rm.add(b);} if(!rm.isEmpty()) bullets.removeAll(rm);

                // collisions (원작에 가까운 원형 히트박스 간략화)
                float hitR2 = 15*15; java.util.List<LEntity> bulletsHit=new java.util.ArrayList<>(); java.util.List<LEntity> aliensDead=new java.util.ArrayList<>();
                for (LEntity b: bullets){ if ("bullet_enemy".equals(b.type)){
                        float dx = me.x+10 - b.x, dy = me.y - b.y; if (dx*dx+dy*dy <= hitR2) { if (me.hp>0) me.hp-=1; bulletsHit.add(b);} 
                    } else { // player bullet vs aliens
                        for (LEntity a: aliens){ float dx=a.x-b.x, dy=a.y-b.y; if (dx*dx+dy*dy<=hitR2){ int dmg=Math.max(1, me.attackPower); a.hp-=dmg; bulletsHit.add(b); if(a.hp<=0) { aliensDead.add(a); me.score+=10; if (Math.random()<0.25) me.skillPoints+=1; } break; } }
                    }
                }
                if(!aliensDead.isEmpty()){ aliens.removeAll(aliensDead); double mult=org.newdawn.spaceinvaders.gameplay.core.Rules.remainingAlienSpeedMultiplier(round); for(LEntity a: aliens){ a.vx*=mult; a.vy*=mult; } }
                if(!bulletsHit.isEmpty()) bullets.removeAll(bulletsHit);

                // round advance
                if (aliens.isEmpty()){
                    if (round < maxRound){ round++; spawnRound(); alienFiringIntervalMs = org.newdawn.spaceinvaders.gameplay.core.Rules.globalAlienFiringIntervalMs(round, baseAlienFiringIntervalMs); me.skillPoints += 2; }
                    else { running = false; }
                }

                // snapshot publish
                Snapshot snap = new Snapshot(); snap.tick=now; snap.wave=round; snap.players = new java.util.ArrayList<>(); snap.players.add(me);
                java.util.List<Snapshot.EntityState> aes=new java.util.ArrayList<>(); for(LEntity a: aliens){ Snapshot.EntityState es=new Snapshot.EntityState(); es.id=a.id; es.x=a.x; es.y=a.y; es.type="alien"; aes.add(es);} snap.aliens=aes;
                java.util.List<Snapshot.EntityState> bes=new java.util.ArrayList<>(); for(LEntity b: bullets){ Snapshot.EntityState es=new Snapshot.EntityState(); es.id=b.id; es.x=b.x; es.y=b.y; es.type=b.type==null?"bullet_player":b.type; bes.add(es);} snap.bullets=bes; snap.powerups=java.util.Collections.emptyList(); snap.events=java.util.Collections.emptyList();
                latestSnapshot = snap; for (Listener l : listeners) l.onSnapshot(snap);

                try { Thread.sleep(10);} catch (InterruptedException ignored) {}
            }
        }
    }

    // 간소 엔티티 구조체
    private static class LEntity { String id=java.util.UUID.randomUUID().toString(); String type; float x,y,vx,vy; int hp=1; long timeSinceDir=0L, dirInterval=2000L; long lastFire=0L, fireInterval=2000L; }
}
