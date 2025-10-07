package org.newdawn.spaceinvaders.server.game;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final Map<String, MultiplayerSkillManager> skillManagers = new HashMap<>();
    private final Random rng;

    private ShipEntity ship;
    private double moveSpeed = 300;
    private int alienCount;

    private String currentSpaceshipSkin = "sprites/ship.gif";
    private String currentWeaponSkin = "sprites/shot.gif";

    private String primaryPlayerId;
    private RoundTransition pendingTransition;
    private Runnable pendingRoundInitializer;
    private boolean inIntermission;
    private boolean gameStarted;

    public static class RoundTransition {
        public enum Type { WAVE_CLEARED, BOSS_DEFEATED, GAME_COMPLETED }
        public final Type type;
        public final int completedRound;
        public final int nextRound;
        public final boolean bossNext;
        public final String message;

        private RoundTransition(Type type, int completedRound, int nextRound, boolean bossNext, String message) {
            this.type = type;
            this.completedRound = completedRound;
            this.nextRound = nextRound;
            this.bossNext = bossNext;
            this.message = message;
        }
    }

    private static class PlayerRuntime {
        ShipEntity ship;
        boolean leftPressed;
        boolean rightPressed;
        boolean firePressed;
        long lastFire;
        @SuppressWarnings("unused")
        final Map<Integer, Integer> skillInventory = new HashMap<>(); // 향후 사용 예정
    }

    private final Map<String, PlayerRuntime> playerRuntimes = new LinkedHashMap<>();

    public ServerMultiplayerGame(long seed) {
        rng = new Random(seed);
        gameStateManager.setWaitingForKeyPress(false);
        gameStateManager.setGameRunning(true);
    }

    public void registerPlayers(Collection<String> playerIds) {
        boolean layoutChanged = false;
        for (String rawId : playerIds) {
            if (rawId == null) {
                continue;
            }
            String id = rawId.trim();
            if (id.isEmpty()) {
                continue;
            }
            gameStateManager.ensurePlayer(id);
            if (!playerRuntimes.containsKey(id)) {
                playerRuntimes.put(id, new PlayerRuntime());
                layoutChanged = true;
            }
            skillManagers.computeIfAbsent(id, k -> new MultiplayerSkillManager(this, k));
        }
        if (primaryPlayerId == null && !playerIds.isEmpty()) {
            String first = playerIds.iterator().next();
            if (first != null) {
                setPrimaryPlayerId(first.trim());
            }
        }
        if (gameStarted && layoutChanged && !inIntermission) {
            setupPlayerShips();
        }
    }

    public void setPrimaryPlayerId(String playerId) {
        String normalized = playerId != null ? playerId.trim() : null;
        if (normalized == null || normalized.isEmpty()) {
            this.primaryPlayerId = null;
            return;
        }
        this.primaryPlayerId = normalized;
        gameStateManager.setLocalPlayerId(normalized);
        gameStateManager.ensurePlayer(normalized);
        playerRuntimes.computeIfAbsent(normalized, k -> new PlayerRuntime());
    }

    public RoundTransition pollRoundTransition() {
        RoundTransition transition = pendingTransition;
        pendingTransition = null;
        return transition;
    }

    public boolean isInIntermission() {
        return inIntermission;
    }

    public void startPendingRound() {
        gameStateManager.getEntities().clear();
        setupPlayerShips();
        if (pendingRoundInitializer != null) {
            pendingRoundInitializer.run();
        }
        pendingRoundInitializer = null;
        inIntermission = false;
        gameStateManager.setRoundTransition(false);
        gameStateManager.setWaitingForKeyPress(false);
        gameStateManager.setMessage("");
    }

    public boolean hasPendingRoundStart() {
        return pendingRoundInitializer != null;
    }

    private void scheduleIntermission(RoundTransition transition, Runnable nextRoundInitializer) {
        this.pendingTransition = transition;
        this.pendingRoundInitializer = nextRoundInitializer;
        this.inIntermission = true;
        gameStateManager.setRoundTransition(true);
        gameStateManager.setWaitingForKeyPress(true);
        if (transition.message != null) {
            gameStateManager.setMessage(transition.message);
        }
    }

    // 플레이어별 스킬 매니저 접근
    private MultiplayerSkillManager skillManager(String playerId) {
        String resolved = playerId;
        if (resolved == null) {
            resolved = primaryPlayerId;
            if (resolved == null && !playerRuntimes.isEmpty()) {
                resolved = playerRuntimes.keySet().iterator().next();
            }
        }
        String mapKey = resolved != null ? resolved : "__default__";
        final String ownerForManager = resolved;
        return skillManagers.computeIfAbsent(mapKey, id -> new MultiplayerSkillManager(this, ownerForManager));
    }

    @SuppressWarnings("unused")
    private PlayerRuntime runtime(String playerId) {
        return playerRuntimes.computeIfAbsent(playerId, k -> new PlayerRuntime());
    }

    public void startGame() {
        gameStateManager.startNewGame();
        for (String id : playerRuntimes.keySet()) {
            skillManager(id).reset();
        }
        gameStateManager.getEntities().clear();
        setupPlayerShips();
        initEntities();
        gameStarted = true;
    }

    private void setupPlayerShips() {
        ArrayList<Entity> entities = gameStateManager.getEntities();
        gameStateManager.getRemoveList().removeIf(e -> e instanceof ShipEntity);

        playerRuntimes.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            return key == null || key.trim().isEmpty();
        });

        if (playerRuntimes.isEmpty()) {
            if (primaryPlayerId != null && !primaryPlayerId.isEmpty()) {
                playerRuntimes.putIfAbsent(primaryPlayerId, new PlayerRuntime());
            }
            if (playerRuntimes.isEmpty()) {
                ship = null;
                return;
            }
        }

        List<String> spawnOrder = new ArrayList<>(playerRuntimes.keySet());
        if (primaryPlayerId != null && spawnOrder.remove(primaryPlayerId)) {
            spawnOrder.add(0, primaryPlayerId);
        }

        for (PlayerState state : gameStateManager.getPlayerStates()) {
            String playerId = state.getPlayerId();
            if (playerId == null || playerId.trim().isEmpty()) {
                continue;
            }
            if (!playerRuntimes.containsKey(playerId)) {
                playerRuntimes.put(playerId, new PlayerRuntime());
                spawnOrder.add(playerId);
            } else if (!spawnOrder.contains(playerId)) {
                spawnOrder.add(playerId);
            }
        }

        entities.removeIf(e -> e instanceof ShipEntity);

        int playerCount = spawnOrder.size();
        if (playerCount == 0) {
            ship = null;
            return;
        }

        final int minX = 120;
        final int maxX = 680;
        final int spawnY = 550;
        int spacing = playerCount > 1 ? (maxX - minX) / (playerCount - 1) : 0;

        for (int index = 0; index < spawnOrder.size(); index++) {
            String playerId = spawnOrder.get(index);
            PlayerRuntime runtime = playerRuntimes.get(playerId);
            if (runtime == null) {
                runtime = new PlayerRuntime();
                playerRuntimes.put(playerId, runtime);
            }
            int spawnX = (playerCount == 1) ? 370 : minX + (index * spacing);

            gameStateManager.ensurePlayer(playerId);
            ShipEntity newShip = new ShipEntity(this, currentSpaceshipSkin, spawnX, spawnY);
            newShip.setOwnerId(playerId);
            runtime.ship = newShip;
            runtime.leftPressed = false;
            runtime.rightPressed = false;
            runtime.firePressed = false;
            runtime.lastFire = 0;
            entities.add(newShip);
        }

        pruneDuplicateShips(entities);

        ShipEntity fallbackShip = spawnOrder.stream()
                .map(playerRuntimes::get)
                .filter(Objects::nonNull)
                .map(runtime -> runtime.ship)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        ship = primaryPlayerId != null && playerRuntimes.containsKey(primaryPlayerId)
                ? playerRuntimes.get(primaryPlayerId).ship
                : fallbackShip;
    }

    private void pruneDuplicateShips(ArrayList<Entity> entities) {
        HashSet<String> seenOwners = new HashSet<>();
        entities.removeIf(entity -> {
            if (!(entity instanceof ShipEntity)) {
                return false;
            }
            ShipEntity shipEntity = (ShipEntity) entity;
            String owner = shipEntity.getOwnerId();
            if (owner == null || owner.trim().isEmpty()) {
                return true;
            }
            return !seenOwners.add(owner);
        });
    }

    public void applyInputs(Map<String, PlayerInput> inputs) {
        for (PlayerRuntime runtime : playerRuntimes.values()) {
            runtime.leftPressed = false;
            runtime.rightPressed = false;
            runtime.firePressed = false;
        }
        for (Map.Entry<String, PlayerInput> entry : inputs.entrySet()) {
            PlayerRuntime runtime = playerRuntimes.get(entry.getKey());
            if (runtime == null) continue;
            PlayerInput pi = entry.getValue();
            runtime.leftPressed = pi.left;
            runtime.rightPressed = pi.right;
            runtime.firePressed = pi.fire;
        }
    }

    public void update(long delta) {
        if (!gameStateManager.isWaitingForKeyPress()
                && !gameStateManager.isShowingPauseMenu()
                && !gameStateManager.isShowingSkillMenu()) {
            for (MultiplayerSkillManager mgr : skillManagers.values()) {
                mgr.updateSkillEffects();
            }
            ArrayList<Entity> entities = new ArrayList<>(gameStateManager.getEntities());
            for (Entity entity : entities) {
                entity.move(delta);
            }
            tryAlienFire();
        }

        if (!gameStateManager.isShowingPauseMenu()
                && !gameStateManager.isShowingSkillMenu()) {
            for (Map.Entry<String, PlayerRuntime> entry : playerRuntimes.entrySet()) {
                PlayerRuntime runtime = entry.getValue();
                ShipEntity playerShip = runtime.ship;
                if (playerShip == null) {
                    continue;
                }
                playerShip.setHorizontalMovement(0);
                if (runtime.leftPressed && !runtime.rightPressed) {
                    playerShip.setHorizontalMovement(-moveSpeed);
                } else if (runtime.rightPressed && !runtime.leftPressed) {
                    playerShip.setHorizontalMovement(moveSpeed);
                }
                if (runtime.firePressed) {
                    tryToFire(entry.getKey(), runtime);
                }
            }
        }

        ShipEntity fallbackShip = playerRuntimes.values().stream()
                .map(r -> r.ship)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        ship = primaryPlayerId != null && playerRuntimes.containsKey(primaryPlayerId)
                ? playerRuntimes.get(primaryPlayerId).ship
                : fallbackShip;

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

    public GameSnapshot createSnapshot(long tick,
                                       long serverTime,
                                       long delta,
                                       GameSnapshot.Phase phase,
                                       boolean waitingForPlayers,
                                       Map<String, Boolean> readyStates,
                                       String message) {
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
                players,
                phase,
                waitingForPlayers,
                readyStates,
                message);
    }

    // ===== 내부 로직 재사용 =====

    private void initEntities() {
        ship = primaryPlayerId != null && playerRuntimes.containsKey(primaryPlayerId)
                ? playerRuntimes.get(primaryPlayerId).ship
                : playerRuntimes.values().stream().map(r -> r.ship).findFirst().orElse(null);

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

    private boolean isBossRound(int round) {
        return round == 2 || round == 4 || round == 6 || (round > 6 && round % 2 == 0);
    }

    private void tryToFire(String playerId, PlayerRuntime runtime) {
        ShipEntity playerShip = runtime.ship;
        if (playerShip == null) {
            return;
        }
        PlayerState ps = gameStateManager.ensurePlayer(playerId);
        long firingInterval = (long) (gameStateManager.getFiringInterval() / ps.getAttackSpeed());
        long now = System.currentTimeMillis();
        if (now - runtime.lastFire < firingInterval) {
            return;
        }
        runtime.lastFire = now;

        MultiplayerSkillManager manager = skillManager(playerId);
        if (manager.hasTripleShot()) {
            ShotEntity shot1 = new ShotEntity(this, currentWeaponSkin, playerShip.getX()-5, playerShip.getY()-30);
            ShotEntity shot2 = new ShotEntity(this, currentWeaponSkin, playerShip.getX()+10, playerShip.getY()-30);
            ShotEntity shot3 = new ShotEntity(this, currentWeaponSkin, playerShip.getX()+25, playerShip.getY()-30);
            shot1.setOwnerId(playerId);
            shot2.setOwnerId(playerId);
            shot3.setOwnerId(playerId);
            gameStateManager.getEntities().add(shot1);
            gameStateManager.getEntities().add(shot2);
            gameStateManager.getEntities().add(shot3);
        } else {
            ShotEntity shot = new ShotEntity(this, currentWeaponSkin, playerShip.getX()+10, playerShip.getY()-30);
            shot.setOwnerId(playerId);
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
        ShipEntity previousShip = ship;
        ShipEntity target = findClosestShip(alien.getX(), alien.getY());
        if (target != null) {
            ship = target;
        }
        alien.tryToFire();
        ShipEntity fallbackShip = playerRuntimes.values().stream()
                .map(r -> r.ship)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (previousShip != null) {
            ship = previousShip;
        } else {
            ship = fallbackShip;
        }
        gameStateManager.setLastAlienFire(System.currentTimeMillis());
    }

    private ShipEntity findClosestShip(double x, double y) {
        ShipEntity closest = null;
        double best = Double.MAX_VALUE;
        for (PlayerRuntime runtime : playerRuntimes.values()) {
            ShipEntity candidate = runtime.ship;
            if (candidate == null) continue;
            double dist = Point2D.distance(x, y, candidate.getX(), candidate.getY());
            if (dist < best) {
                best = dist;
                closest = candidate;
            }
        }
        return closest;
    }

    // ===== MultiplayerGameContext 구현 =====

    @Override
    public MultiplayerGameStateManager getGameStateManager() {
        return gameStateManager;
    }

    @Override
    public MultiplayerSkillManager getSkillManager(String playerId) {
        return skillManager(playerId);
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
    public void addSkillToInventory(String playerId, int skillType, int skillValue) {
        skillManager(playerId).addSkillToInventory(skillType, skillValue);
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
    public void fireMissile(String playerId, double targetX, double targetY) {
        ShipEntity source = playerId != null ? (ShipEntity) getShip(playerId) : ship;
        if (source == null) {
            return;
        }
        MissileEntity missile = new MissileEntity(this, "sprites/Skill/Missile.png",
                (int) source.getX() + 15, (int) source.getY(), targetX, targetY);
        if (playerId != null) {
            missile.setOwnerId(playerId);
        }
        gameStateManager.getEntities().add(missile);
    }

    @Override
    public void notifyAlienKilled(String killerPlayerId, double killX, double killY) {
        if (killerPlayerId != null) {
            MultiplayerSkillManager manager = skillManager(killerPlayerId);
            int earned = manager.getRandomSkillPoints(gameStateManager.getCurrentRound());
            addSkillPoints(killerPlayerId, earned);
            double dropChance = manager.getSkillDropChance(gameStateManager.getCurrentRound());
            if (rng.nextDouble() < dropChance) {
                manager.dropSkill(gameStateManager.getCurrentRound(), killX, killY);
            }
        }
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
    public void notifyBossDefeated(String killerPlayerId) {
        if (inIntermission || pendingRoundInitializer != null) {
            return;
        }
        int completedRound = gameStateManager.getCurrentRound();
        boolean roundAdvanced = gameStateManager.advanceRound();
        if (roundAdvanced) {
            int nextRound = gameStateManager.getCurrentRound();
            boolean bossNext = isBossRound(nextRound);
            Runnable initializer = () -> {
                if (bossNext) {
                    spawnBoss(false);
                } else {
                    initEntities();
                }
            };
            scheduleIntermission(
                    new RoundTransition(
                            RoundTransition.Type.BOSS_DEFEATED,
                            completedRound,
                            nextRound,
                            bossNext,
                            "보스를 처치했습니다! 다음 라운드를 준비하세요."),
                    initializer);
        } else {
            scheduleIntermission(
                    new RoundTransition(
                            RoundTransition.Type.GAME_COMPLETED,
                            completedRound,
                            completedRound,
                            false,
                            "모든 보스를 물리쳤습니다!"),
                    null);
        }
    }

    @Override
    public void notifyDeath(String playerId) {
        if (playerId != null && isPlayerInvincible(playerId)) {
            return;
        }
        String targetId = playerId != null ? playerId : gameStateManager.getLocalPlayerId();
        gameStateManager.takeDamage(targetId);
        PlayerState ps = gameStateManager.ensurePlayer(targetId);
        if (ps.isDead()) {
            gameStateManager.setMessage("Player down!");
            gameStateManager.setWaitingForKeyPress(true);
        }
    }

    private void notifyWin() {
        if (inIntermission || pendingRoundInitializer != null) {
            return;
        }
        int completedRound = gameStateManager.getCurrentRound();
        boolean roundAdvanced = gameStateManager.advanceRound();
        if (roundAdvanced) {
            int nextRound = gameStateManager.getCurrentRound();
            boolean bossNext = isBossRound(nextRound);
            Runnable initializer = () -> {
                if (bossNext) {
                    spawnBoss(false);
                } else {
                    initEntities();
                }
            };
            scheduleIntermission(
                    new RoundTransition(
                            RoundTransition.Type.WAVE_CLEARED,
                            completedRound,
                            nextRound,
                            bossNext,
                            "라운드 " + completedRound + " 클리어! 모든 플레이어 준비 후 다음 라운드가 시작됩니다."),
                    initializer);
        } else {
            scheduleIntermission(
                    new RoundTransition(
                            RoundTransition.Type.GAME_COMPLETED,
                            completedRound,
                            completedRound,
                            false,
                            "축하합니다! 모든 라운드를 클리어했습니다."),
                    null);
        }
    }

    @Override
    public int getCurrentRound() {
        return gameStateManager.getCurrentRound();
    }

    @Override
    public int getPlayerAttackPower(String playerId) {
        if (playerId == null) {
            return gameStateManager.getAttackPower();
        }
        PlayerState ps = gameStateManager.ensurePlayer(playerId);
        return ps.getAttackPower();
    }

    @Override
    public boolean hasPiercingShots(String playerId) {
        return skillManager(playerId).hasPiercing();
    }

    @Override
    public boolean isPlayerInvincible(String playerId) {
        return skillManager(playerId).isInvincible();
    }

    @Override
    public Entity getShip(String playerId) {
        if (playerId == null) {
            return ship;
        }
        PlayerRuntime runtime = playerRuntimes.get(playerId);
        return runtime != null ? runtime.ship : null;
    }

    @Override
    public int getShipX(String playerId) {
        Entity target = getShip(playerId);
        return target != null ? target.getX() : 370;
    }

    @Override
    public int getShipY(String playerId) {
        Entity target = getShip(playerId);
        return target != null ? target.getY() : 550;
    }

    @Override
    public java.util.List<Entity> getEntities() {
        return gameStateManager.getEntities();
    }

    @Override
    public void addScore(String playerId, int points) {
        // 점수 시스템이 필요하다면 구현
    }

    @Override
    public void addSkillPoints(String playerId, int points) {
        if (playerId == null) {
            int current = gameStateManager.getSkillPoints();
            gameStateManager.setSkillPoints(current + points);
            return;
        }
        PlayerState ps = gameStateManager.ensurePlayer(playerId);
        ps.addSkillPoints(points);
    }

    public void removePlayer(String playerId) {
        PlayerRuntime runtime = playerRuntimes.remove(playerId);
        if (runtime != null && runtime.ship != null) {
            gameStateManager.getEntities().remove(runtime.ship);
            if (ship == runtime.ship) {
                ship = null;
            }
        }
        skillManagers.remove(playerId);
        if (primaryPlayerId != null && primaryPlayerId.equals(playerId)) {
            primaryPlayerId = playerRuntimes.keySet().stream().findFirst().orElse(null);
        }
        if (primaryPlayerId != null && playerRuntimes.containsKey(primaryPlayerId)) {
            ship = playerRuntimes.get(primaryPlayerId).ship;
        } else {
            ship = playerRuntimes.values().stream().map(r -> r.ship).findFirst().orElse(null);
        }
        if (gameStarted && !inIntermission) {
            setupPlayerShips();
        }
    }

    private void spawnBoss(boolean announce) {
        BossEntity boss = new BossEntity(this, 400, 120, gameStateManager.getCurrentRound());
        gameStateManager.getEntities().add(boss);
        AlienEntity leftAlien = new AlienEntity(this, 200, 120);
        AlienEntity rightAlien = new AlienEntity(this, 600, 120);
        gameStateManager.getEntities().add(leftAlien);
        gameStateManager.getEntities().add(rightAlien);
        if (announce) {
            gameStateManager.setMessage("BOSS APPEARED!");
            gameStateManager.setWaitingForKeyPress(true);
        }
    }
}
