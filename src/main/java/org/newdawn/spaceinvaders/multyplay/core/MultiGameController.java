package org.newdawn.spaceinvaders.multyplay.core;

import org.newdawn.spaceinvaders.multyplay.entity.*;
import org.newdawn.spaceinvaders.multyplay.state.MultiGameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MultiGameRuntime 구현: 싱글 Game.java 로직을 멀티 구조에 맞게 단계적으로 이식.
 * 현재 MVP: 단일 플레이어 슬롯(0) + 기본 Alien 스폰/이동/충돌/사격.
 */
public class MultiGameController implements MultiGameRuntime {
    // --- 상태 ---
    private MultiGameState gameState; // 외부에서 주입 (라운드/스킬 공유)
    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> removeList = new ArrayList<>();
    private final Random random = new Random();

    // 플레이어
    private ShipEntity playerShip;

    // 라운드 & 진행
    // (이전 필드 제거 → gameState 로 이관)

    // 사격 쿨다운
    private long lastFire = 0; private long firingInterval = 400; // ms (기본)
    private long lastAlienFire = 0; private long alienFiringInterval = 900;

    // 메시지 & UI 상태 (간단)
    private String message = ""; private boolean waitingForKeyPress = false;

    // 보스 여부는 gameState로 대체

    // Invincible / Piercing 임시 값
    private boolean invincible = false; private boolean piercing = false; private boolean tripleShot = false;

    public void attachState(MultiGameState state){ this.gameState = state; }

    public void startNewGame() {
    entities.clear(); removeList.clear(); message=""; waitingForKeyPress=false;
        if(gameState!=null){ gameState.newGame(); }
        spawnPlayer(); spawnRoundAliens();
    }

    private void spawnPlayer() { playerShip = new ShipEntity(this, "sprites/ship.gif", 370, 550); entities.add(playerShip); }

    private void spawnRoundAliens() {
    int round = getCurrentRound();
    int rows, cols; switch (round) { case 1: rows=2; cols=5; break; case 2: rows=3; cols=5; break; case 3: rows=3; cols=6; break; case 4: rows=3; cols=7; break; case 5: rows=4; cols=7; break; default: rows=2; cols=5; }
        int screenWidth=800, margin=50; int usableWidth = screenWidth - (2*margin); int spacingX = usableWidth / (cols + 1); int spacingY=80;
        for(int r=0;r<rows;r++){ for(int c=0;c<cols;c++){ int posX = margin + spacingX*(c+1); int posY = 80 + (r*spacingY); entities.add(new AlienEntity(this,posX,posY)); } }
    }

    // --- 틱 업데이트 ---
    public void tick(long deltaMs) { // 이동
        List<Entity> snapshot = new ArrayList<>(entities);
        for(Entity e: snapshot){ e.move(deltaMs); }
        // Alien 사격
        tryAlienFire();
        // 충돌
        for(int i=0;i<entities.size();i++){ Entity a=entities.get(i); for(int j=i+1;j<entities.size();j++){ Entity b=entities.get(j); if(a.collidesWith(b)){ a.collidedWith(b); b.collidedWith(a); } } }
        // 삭제 적용
        if(!removeList.isEmpty()){ entities.removeAll(removeList); removeList.clear(); }
        // 라운드 종료 검사
        checkRoundClear();
    }

    private void checkRoundClear(){
        boolean aliensLeft = entities.stream().anyMatch(e -> e instanceof AlienEntity); if(!aliensLeft && !waitingForKeyPress){ // 승리
            if(gameState!=null){
                if(gameState.tryAdvanceRound()){
                    if(gameState.isBossRound()) { spawnBoss(); }
                    else { entities.clear(); spawnPlayer(); spawnRoundAliens(); }
                } else { message = gameState.getMessage(); waitingForKeyPress = true; }
            } else {
                if(advanceRound()){
                    int round = getCurrentRound();
                    if(round==2 || round==4 || round==6){ spawnBoss(); } else { entities.clear(); spawnPlayer(); spawnRoundAliens(); }
                } else { message = "GAME COMPLETE"; waitingForKeyPress=true; }
            }
        }
    }

    private boolean advanceRound(){ return false; /* legacy fallback 제거 예정 */ }

    private void spawnBoss(){ int round = getCurrentRound(); BossEntity boss = new BossEntity(this,400,120,round); entities.add(boss); entities.add(new AlienEntity(this,200,120)); entities.add(new AlienEntity(this,600,120)); message = "BOSS APPEARED"; waitingForKeyPress=true; if(gameState!=null) gameState.onBossSpawn(); }

    private void tryAlienFire(){ if(System.currentTimeMillis() - lastAlienFire < alienFiringInterval) return; List<AlienEntity> aliens = new ArrayList<>(); for(Entity e: entities){ if(e instanceof AlienEntity) aliens.add((AlienEntity)e); } if(!aliens.isEmpty()){ AlienEntity shooter = aliens.get(random.nextInt(aliens.size())); shooter.tryToFire(); lastAlienFire = System.currentTimeMillis(); } }

    // --- 플레이어 입력 처리 (단일 플레이어) ---
    public void playerSetHorizontal(int dir){ if(playerShip==null) return; double speed=300; if(dir<0) playerShip.setHorizontalMovement(-speed); else if(dir>0) playerShip.setHorizontalMovement(speed); else playerShip.setHorizontalMovement(0); }
    public void playerTryFire(){ double atkSpeed = gameState!=null? gameState.getAttackSpeed() : 1.0; long interval=(long)(firingInterval / atkSpeed); if(System.currentTimeMillis()-lastFire < interval) return; lastFire=System.currentTimeMillis(); if(tripleShot){ addEntity(new ShotEntity(this,"sprites/shot.gif",playerShip.getX()-5,playerShip.getY()-30)); addEntity(new ShotEntity(this,"sprites/shot.gif",playerShip.getX()+10,playerShip.getY()-30)); addEntity(new ShotEntity(this,"sprites/shot.gif",playerShip.getX()+25,playerShip.getY()-30)); } else { addEntity(new ShotEntity(this,"sprites/shot.gif",playerShip.getX()+10,playerShip.getY()-30)); } }

    // --- MultiGameRuntime 구현 ---
    @Override public int getCurrentRound(){ return gameState!=null? gameState.getRound() : 1; }
    @Override public boolean isPlayerInvincible(){ return invincible; }
    @Override public void notifyDeath(){ if(invincible) return; if(gameState!=null){ /* TODO HP 관리 연동 */ } }
    @Override public void notifyAlienKilled(){ if(gameState!=null) gameState.onAlienKilled(); for(Entity e: entities){ if(e instanceof AlienEntity){ e.setHorizontalMovement(e.getHorizontalMovement()*1.01); e.setVerticalMovement(e.getVerticalMovement()*1.01); } } }
    @Override public void notifyBossDefeated(){ message="BOSS DOWN"; waitingForKeyPress=true; if(gameState!=null) gameState.onBossDefeated(); }
    @Override public void addAimedAlienShot(int x,int y,int sourceX){ addEntity(new ShotEntity(this,"sprites/shot.gif",x,y,true)); }
    @Override public void addEntity(Entity e){ entities.add(e); }
    @Override public void removeEntity(Entity e){ removeList.add(e); }
    @Override public void addSkillToInventory(int type,int value){ /* TODO: 인벤토리 시스템 */ }
    @Override public int getPlayerAttackPower(){ return gameState!=null? gameState.getAttackPower():1; }
    @Override public boolean hasPiercingShots(){ return piercing; }
    @Override public int getShipX(){ return playerShip!=null? playerShip.getX():370; }
    @Override public int getShipY(){ return playerShip!=null? playerShip.getY():550; }
    @Override public ShipEntity getShip(){ return playerShip; }
    @Override public void createExplosion(int x,int y,double radius){ addEntity(new ExplosionEntity(this,"sprites/Skill/Explosion.png",x,y,radius)); }
    @Override public void addScore(int score){ /* TODO: 점수 시스템 */ }
    @Override public void addSkillPoints(int points){ /* central skillPoints moved to gameState; distribution handled in onAlienKilled */ }
    @Override public List<Entity> getEntities(){ return (List<Entity>) entities; }

    // --- 접근자 (UI/디버깅용) ---
    public String getMessage(){ return message; }
    public boolean isWaitingForKeyPress(){ return waitingForKeyPress; }

    // --- 스냅샷 생성 (서버 or 로컬 루프백용) ---
    public org.newdawn.spaceinvaders.multyplay.net.GameSnapshotMsg buildSnapshot(long tick){
        org.newdawn.spaceinvaders.multyplay.net.GameSnapshotMsg snap = new org.newdawn.spaceinvaders.multyplay.net.GameSnapshotMsg();
        snap.tick = tick;
        // 단일 플레이어만 포함 (slot 0)
        if(playerShip != null){
            org.newdawn.spaceinvaders.multyplay.net.GameSnapshotMsg.PlayerStateSnapshot ps = new org.newdawn.spaceinvaders.multyplay.net.GameSnapshotMsg.PlayerStateSnapshot();
            ps.slot = 0;
            ps.x = playerShip.getX();
            ps.y = playerShip.getY();
            ps.hp = gameState!=null? gameState.getCurrentHP():5;
            ps.sp = 0; // TODO: per-player skill (gameState distribution) -> snapshot에 반영
            snap.players.add(ps);
        }
        return snap;
    }

    public java.util.List<org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot> buildEntitySnapshots(){
        java.util.List<org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot> list = new java.util.ArrayList<>();
        for(Entity e: entities){
            list.add(e.toSnapshot());
        }
        return list;
    }
}
