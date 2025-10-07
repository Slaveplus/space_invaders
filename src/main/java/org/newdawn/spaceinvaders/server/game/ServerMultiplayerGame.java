package org.newdawn.spaceinvaders.server.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerSkillManager;
import org.newdawn.spaceinvaders.multyplay.entity.AlienEntity;
import org.newdawn.spaceinvaders.multyplay.entity.BossEntity;
import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot;
import org.newdawn.spaceinvaders.multyplay.entity.ExplosionEntity;
import org.newdawn.spaceinvaders.multyplay.entity.MissileEntity;
import org.newdawn.spaceinvaders.multyplay.entity.ShipEntity;
import org.newdawn.spaceinvaders.multyplay.entity.ShotEntity;
import org.newdawn.spaceinvaders.multyplay.net.GameSnapshot;
import org.newdawn.spaceinvaders.multyplay.net.PlayerInput;
import org.newdawn.spaceinvaders.multyplay.state.PlayerState;

/**
 * 서버에서 구동되는 멀티플레이 게임 월드.
 * 기존 클라이언트용 MultiplayerGameCanvas에서 렌더링/입력 의존성을 제거하여
 * 순수 게임 로직 및 스냅샷만 계산한다.
 */
public class ServerMultiplayerGame implements MultiplayerGameContext {
    private final MultiplayerGameStateManager gameStateManager = new MultiplayerGameStateManager();
    private final MultiplayerSkillManager skillManager = new MultiplayerSkillManager(this);
    private final Random rng;

    private Entity ship;
    private double moveSpeed = 300;
    private int alienCount;

    private String currentSpaceshipSkin = "sprites/ship.gif";
    private String currentWeaponSkin = "sprites/shot.gif";

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean firePressed;

    private String primaryPlayerId;
    private final Map<String, PlayerInput> lastInputs = new HashMap<>();

    public ServerMultiplayerGame(long seed) {
        rng = new Random(seed);
        gameStateManager.setWaitingForKeyPress(false);
        gameStateManager.setGameRunning(true);
    }

    public void registerPlayers(Collection<String> playerIds) {
        for (String id : playerIds) {
            gameStateManager.ensurePlayer(id);
        }
        if (primaryPlayerId == null && !playerIds.isEmpty()) {
            setPrimaryPlayerId(playerIds.iterator().next());
        }
    }

    public void setPrimaryPlayerId(String playerId) {
        this.primaryPlayerId = playerId;
        if (playerId != null) {
            gameStateManager.setLocalPlayerId(playerId);
            gameStateManager.ensurePlayer(playerId);
        }
    }

    public void startGame() {
        gameStateManager.startNewGame();
        skillManager.reset();
        initEntities();
    }

    public void applyInputs(Map<String, PlayerInput> inputs) {
        lastInputs.clear();
        lastInputs.putAll(inputs);
        leftPressed = rightPressed = firePressed = false;
        if (primaryPlayerId != null) {
            PlayerInput pi = inputs.get(primaryPlayerId);
            if (pi != null) {
                leftPressed = pi.left;
                rightPressed = pi.right;
                firePressed = pi.fire;
            }
        }
    }

    public void update(long delta) {
        if (!gameStateManager.isWaitingForKeyPress()
                && !gameStateManager.isShowingPauseMenu()
                && !gameStateManager.isShowingSkillMenu()) {
            skillManager.updateSkillEffects();
            ArrayList<Entity> entities = new ArrayList<>(gameStateManager.getEntities());
            for (Entity entity : entities) {
                entity.move(delta);
            }
            tryAlienFire();
        }

        if (ship != null
                && !gameStateManager.isShowingPauseMenu()
                && !gameStateManager.isShowingSkillMenu()) {
            ship.setHorizontalMovement(0);
            if (leftPressed && !rightPressed) {
                ship.setHorizontalMovement(-moveSpeed);
            } else if (rightPressed && !leftPressed) {
                ship.setHorizontalMovement(moveSpeed);
            }
            if (firePressed) {
                tryToFire();
            }
        }

        ArrayList<Entity> entities = gameStateManager.getEntities();
        if (!gameStateManager.isShowingPauseMenu()
                && !gameStateManager.isShowingSkillMenu()) {
            for (int i = 0; i < entities.size(); i++) {
                Entity e1 = entities.get(i);
                for (int j = i + 1; j < entities.size(); j++) {
                    Entity e2 = entities.get(j);
                    if (e1.collidesWith(e2)) {
                        e1.collidedWith(e2);
                        e2.collidedWith(e1);
                    }
                }
            }
            entities.removeAll(gameStateManager.getRemoveList());
            gameStateManager.getRemoveList().clear();
            if (gameStateManager.isLogicRequiredThisLoop()) {
                for (Entity e : entities) {
                    e.doLogic();
                }
                gameStateManager.setLogicRequiredThisLoop(false);
            }
        }
    }

    public GameSnapshot createSnapshot(long tick, long serverTime, long delta) {
        ArrayList<EntitySnapshot> snaps = new ArrayList<>();
        for (Entity e : gameStateManager.getEntities()) {
            snaps.add(e.toSnapshot());
        }
        Map<String, GameSnapshot.PlayerScalarState> players = new LinkedHashMap<>();
        for (PlayerState ps : gameStateManager.getPlayerStates()) {
            players.put(ps.getPlayerId(),
                    new GameSnapshot.PlayerScalarState(
                            ps.getCurrentHP(),
                            ps.getMaxHP(),
                            ps.getAttackPower(),
                            ps.getAttackSpeed(),
                            ps.getSkillPoints()));
        }
        return new GameSnapshot(tick, serverTime, delta,
                gameStateManager.getCurrentRound(),
                snaps,
                players);
    }

    // ===== 내부 로직 재사용 =====

    private void initEntities() {
        ship = new ShipEntity(this, currentSpaceshipSkin, 370, 550);
        if (primaryPlayerId != null) {
            ship.setOwnerId(primaryPlayerId);
        }
        gameStateManager.getEntities().add(ship);

        alienCount = 0;
        int rows, cols;
        switch (gameStateManager.getCurrentRound()) {
            case 1: rows = 2; cols = 5; break;
            case 2: rows = 3; cols = 5; break;
            case 3: rows = 3; cols = 6; break;
            case 4: rows = 3; cols = 7; break;
            case 5: rows = 4; cols = 7; break;
            default: rows = 2; cols = 5; break;
        }

        int screenWidth = 800;
        int margin = 50;
        int usableWidth = screenWidth - (2 * margin);
        int spacingX = usableWidth / (cols + 1);
        int spacingY = 80;

        for (int row=0; row<rows; row++) {
            for (int x=0; x<cols; x++) {
                int posX = margin + spacingX * (x + 1);
                int posY = 80 + (row * spacingY);
                Entity alien = new AlienEntity(this, posX, posY);
                gameStateManager.getEntities().add(alien);
                alienCount++;
            }
        }

        gameStateManager.setAlienCount(alienCount);
    }

    private void tryToFire() {
        long currentFiringInterval = (long) (gameStateManager.getFiringInterval() / gameStateManager.getAttackSpeed());
        if (System.currentTimeMillis() - gameStateManager.getLastFire() < currentFiringInterval) {
            return;
        }
        gameStateManager.setLastFire(System.currentTimeMillis());

        if (skillManager.hasTripleShot()) {
            ShotEntity shot1 = new ShotEntity(this, currentWeaponSkin, ship.getX()-5, ship.getY()-30);
            ShotEntity shot2 = new ShotEntity(this, currentWeaponSkin, ship.getX()+10, ship.getY()-30);
            ShotEntity shot3 = new ShotEntity(this, currentWeaponSkin, ship.getX()+25, ship.getY()-30);
            if (primaryPlayerId != null) {
                shot1.setOwnerId(primaryPlayerId);
                shot2.setOwnerId(primaryPlayerId);
                shot3.setOwnerId(primaryPlayerId);
            }
            gameStateManager.getEntities().add(shot1);
            gameStateManager.getEntities().add(shot2);
            gameStateManager.getEntities().add(shot3);
        } else {
            ShotEntity shot = new ShotEntity(this, currentWeaponSkin, ship.getX()+10, ship.getY()-30);
            if (primaryPlayerId != null) {
                shot.setOwnerId(primaryPlayerId);
            }
            gameStateManager.getEntities().add(shot);
        }
    }

    private void tryAlienFire() {
        if (System.currentTimeMillis() - gameStateManager.getLastAlienFire()
                < gameStateManager.getAlienFiringInterval()) {
            return;
        }

        ArrayList<AlienEntity> aliens = new ArrayList<>();
        for (Entity entity : gameStateManager.getEntities()) {
            if (entity instanceof AlienEntity) {
                aliens.add((AlienEntity) entity);
            }
        }
        if (aliens.isEmpty()) return;

        AlienEntity alien = aliens.get(rng.nextInt(aliens.size()));
        alien.tryToFire();
        gameStateManager.setLastAlienFire(System.currentTimeMillis());
    }

    // ===== MultiplayerGameContext 구현 =====

    @Override
    public MultiplayerGameStateManager getGameStateManager() {
        return gameStateManager;
    }

    @Override
    public MultiplayerSkillManager getSkillManager() {
        return skillManager;
    }

    @Override
    public void addEntity(Entity entity) {
        if (entity != null) {
            gameStateManager.getEntities().add(entity);
        }
    }

    @Override
    public void removeEntity(Entity entity) {
        if (entity != null) {
            gameStateManager.getRemoveList().add(entity);
        }
    }

    @Override
    public void addAimedAlienShot(int x, int y, int alienX) {
        int playerX = ship != null ? ship.getX() + 10 : 400;
        int aimOffset = (int)((playerX - alienX) * 0.15);
        aimOffset = Math.max(-15, Math.min(15, aimOffset));
        ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", x + aimOffset, y, true);
        gameStateManager.getEntities().add(shot);
    }

    @Override
    public void addSkillToInventory(int skillType, int skillValue) {
        skillManager.addSkillToInventory(skillType, skillValue);
    }

    @Override
    public void createSkillDrop(int x, int y, int skillType, int skillValue) {
        ShotEntity skillDrop = new ShotEntity(this, "sprites/shot.gif", x, y,
                false, skillType, skillValue);
        gameStateManager.getEntities().add(skillDrop);
    }

    @Override
    public void createExplosion(int x, int y, double radius) {
        ExplosionEntity explosion = new ExplosionEntity(this, "sprites/Skill/Explosion.png", x, y, radius);
        gameStateManager.getEntities().add(explosion);
    }

    @Override
    public void fireMissile(double targetX, double targetY) {
        if (ship == null) return;
        MissileEntity missile = new MissileEntity(this, "sprites/Skill/Missile.png",
                (int) ship.getX() + 15, (int) ship.getY(), targetX, targetY);
        if (primaryPlayerId != null) {
            missile.setOwnerId(primaryPlayerId);
        }
        gameStateManager.getEntities().add(missile);
    }

    @Override
    public void notifyAlienKilled() {
        int remainingAliens = 0;
        ArrayList<Entity> entities = gameStateManager.getEntities();
        ArrayList<Entity> removeList = gameStateManager.getRemoveList();

        for (Entity entity : entities) {
            if (entity instanceof AlienEntity && !removeList.contains(entity)) {
                remainingAliens++;
            }
        }

        if (remainingAliens == 0) {
            notifyWin();
        }

        for (Entity entity : entities) {
            if (entity instanceof AlienEntity) {
                double speedMultiplier = 1.015 + (gameStateManager.getCurrentRound() * 0.01);
                entity.setHorizontalMovement(entity.getHorizontalMovement() * speedMultiplier);
                entity.setVerticalMovement(entity.getVerticalMovement() * speedMultiplier);
            }
        }
    }

    @Override
    public void notifyBossDefeated() {
        gameStateManager.setMessage("BOSS DEFEATED!");
        gameStateManager.setWaitingForKeyPress(true);
        boolean roundAdvanced = gameStateManager.advanceRound();
        if (roundAdvanced) {
            gameStateManager.getEntities().clear();
            initEntities();
        } else {
            gameStateManager.setMessage("GAME COMPLETED!");
            gameStateManager.setWaitingForKeyPress(true);
        }
    }

    @Override
    public void notifyDeath() {
        if (skillManager.isInvincible()) {
            return;
        }
        gameStateManager.takeDamage();
        if (gameStateManager.getCurrentHP() <= 0) {
            gameStateManager.setMessage("Player down!");
            gameStateManager.setWaitingForKeyPress(true);
        }
    }

    private void notifyWin() {
        boolean roundAdvanced = gameStateManager.advanceRound();
        if (roundAdvanced) {
            if (gameStateManager.getCurrentRound() == 2
                    || gameStateManager.getCurrentRound() == 4
                    || gameStateManager.getCurrentRound() == 6) {
                spawnBoss();
            } else {
                gameStateManager.getEntities().clear();
                initEntities();
            }
        } else {
            gameStateManager.setMessage("Well done! You Win!");
            gameStateManager.setWaitingForKeyPress(true);
        }
    }

    @Override
    public int getCurrentRound() {
        return gameStateManager.getCurrentRound();
    }

    @Override
    public int getPlayerAttackPower() {
        return gameStateManager.getAttackPower();
    }

    @Override
    public boolean hasPiercingShots() {
        return skillManager.hasPiercing();
    }

    @Override
    public boolean isPlayerInvincible() {
        return skillManager.isInvincible();
    }

    @Override
    public Entity getShip() {
        return ship;
    }

    @Override
    public int getShipX() {
        return ship != null ? ship.getX() : 370;
    }

    @Override
    public int getShipY() {
        return ship != null ? ship.getY() : 550;
    }

    @Override
    public java.util.List<Entity> getEntities() {
        return gameStateManager.getEntities();
    }

    @Override
    public void addScore(int points) {
        // 서버에서 점수 로직 필요 시 구현
    }

    @Override
    public void addSkillPoints(int points) {
        int currentPoints = gameStateManager.getSkillPoints();
        gameStateManager.setSkillPoints(currentPoints + points);
    }

    private void spawnBoss() {
        BossEntity boss = new BossEntity(this, 400, 120, gameStateManager.getCurrentRound());
        gameStateManager.getEntities().add(boss);
        AlienEntity leftAlien = new AlienEntity(this, 200, 120);
        AlienEntity rightAlien = new AlienEntity(this, 600, 120);
        gameStateManager.getEntities().add(leftAlien);
        gameStateManager.getEntities().add(rightAlien);
        gameStateManager.setMessage("BOSS APPEARED!");
        gameStateManager.setWaitingForKeyPress(true);
    }
}
