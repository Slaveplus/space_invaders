package org.newdawn.spaceinvaders.server.game;

import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;
import org.newdawn.spaceinvaders.multyplay.entity.AlienEntity;
import org.newdawn.spaceinvaders.multyplay.entity.BossEntity;
import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot;
import org.newdawn.spaceinvaders.multyplay.entity.ExplosionEntity;
import org.newdawn.spaceinvaders.multyplay.entity.MissileEntity;
import org.newdawn.spaceinvaders.multyplay.entity.NearEntity;
import org.newdawn.spaceinvaders.multyplay.entity.ShipEntity;
import org.newdawn.spaceinvaders.multyplay.entity.ShotEntity;
import org.newdawn.spaceinvaders.multyplay.net.GameEvent;
import org.newdawn.spaceinvaders.multyplay.net.GameSnapshot;
import org.newdawn.spaceinvaders.multyplay.net.PlayerInput;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerSkillManager;
import org.newdawn.spaceinvaders.multyplay.core.SharedMultiplayerRoundCoordinator;
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
    
    // 플레이어별 스킨 정보 저장
    private final Map<String, String> playerSkins = new HashMap<>();

    private String primaryPlayerId;
    private RoundTransition pendingTransition;
    private Runnable pendingRoundInitializer;
    private boolean inIntermission;
    private boolean gameStarted;
    private final Queue<GameEvent> pendingEvents = new ArrayDeque<>();

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

    public static class SkillActionResult {
        public final boolean success;
        public final String message;

        public SkillActionResult(boolean success, String message) {
            this.success = success;
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
        boolean spectating;
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
            PlayerRuntime runtime = playerRuntimes.get(id);
            if (runtime == null) {
                runtime = new PlayerRuntime();
                playerRuntimes.put(id, runtime);
                layoutChanged = true;
            }
            runtime.spectating = false;
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
        revivePlayersForNextRound();
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
        revivePlayersForNextRound();
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
            PlayerState state = gameStateManager.ensurePlayer(playerId);
            if (state.isDead()) {
                runtime.ship = null;
                runtime.spectating = true;
                continue;
            }
            int spawnX = (playerCount == 1) ? 370 : minX + (index * spacing);

            // 플레이어별 스킨 적용
            String playerSkin = getPlayerSkin(playerId);
            ShipEntity newShip = new ShipEntity(this, playerSkin, spawnX, spawnY);
            newShip.setOwnerId(playerId);
            runtime.ship = newShip;
            runtime.spectating = false;
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
        String playerId = ps.getPlayerId();
        MultiplayerSkillManager manager = skillManager(playerId);
        int invCount = manager != null ? manager.getInvincibleSkills() : 0;
        int tripleCount = manager != null ? manager.getTripleShotSkills() : 0;
        int missileCount = manager != null ? manager.getMissileSkills() : 0;
        long invRem = manager != null ? Math.max(0L, manager.getInvincibleEndTime() - serverTime) : 0L;
        long tripleRem = manager != null ? Math.max(0L, manager.getTripleShotEndTime() - serverTime) : 0L;
        int atkLevel = manager != null ? manager.getAttackPowerLevel() : 0;
        int aspdLevel = manager != null ? manager.getAttackSpeedLevel() : 0;
        int hpLevel = manager != null ? manager.getHpUpLevel() : 0;

        players.put(playerId,
            new GameSnapshot.PlayerScalarState(
                ps.getCurrentHP(),
                ps.getMaxHP(),
                ps.getAttackPower(),
                ps.getAttackSpeed(),
                ps.getSkillPoints(),
                invCount,
                0,
                tripleCount,
                missileCount,
                ps.getEarnedCoins(),
                invRem,
                0L,
                tripleRem,
                atkLevel,
                aspdLevel,
                hpLevel));
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

    public List<GameEvent> drainPendingEvents() {
        if (pendingEvents.isEmpty()) {
            return Collections.emptyList();
        }
        List<GameEvent> events = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    // ===== 내부 로직 재사용 =====

    private void initEntities() {
        ship = primaryPlayerId != null && playerRuntimes.containsKey(primaryPlayerId)
                ? playerRuntimes.get(primaryPlayerId).ship
                : playerRuntimes.values().stream().map(r -> r.ship).findFirst().orElse(null);

        int currentRound = gameStateManager.getCurrentRound();
        gameStateManager.getRemoveList().clear();

        SharedMultiplayerRoundCoordinator.setupRound(this, currentRound);
        alienCount = gameStateManager.getAlienCount();
    }

    private void tryToFire(String playerId, PlayerRuntime runtime) {
        ShipEntity playerShip = runtime.ship;
        if (playerShip == null || runtime.spectating) {
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

    public SkillActionResult handleSkillActivation(String playerId, int skillType) {
        if (playerId == null) {
            return new SkillActionResult(false, "플레이어 정보를 확인할 수 없습니다.");
        }
        MultiplayerSkillManager manager = skillManager(playerId);
        if (manager == null) {
            return new SkillActionResult(false, "스킬 정보를 불러올 수 없습니다.");
        }
        switch (skillType) {
            case 0:
                if (manager.getInvincibleSkills() <= 0) {
                    return new SkillActionResult(false, "무적 스킬이 부족합니다.");
                }
                manager.extendSkill(0, 5);
                return new SkillActionResult(true, null);
            case 2:
                if (manager.getTripleShotSkills() <= 0) {
                    return new SkillActionResult(false, "3줄 공격 스킬이 부족합니다.");
                }
                manager.extendSkill(2, 8);
                return new SkillActionResult(true, null);
            case 3:
                if (manager.getMissileSkills() <= 0) {
                    return new SkillActionResult(false, "미사일 스킬이 부족합니다.");
                }
                manager.activateSkill(3, 1);
                return new SkillActionResult(true, null);
            case 1:
                return new SkillActionResult(false, "관통 스킬은 더 이상 사용할 수 없습니다.");
            default:
                return new SkillActionResult(false, "알 수 없는 스킬입니다.");
        }
    }

    public SkillActionResult handleSkillUpgrade(String playerId, int upgradeType) {
        if (playerId == null) {
            return new SkillActionResult(false, "플레이어 정보를 확인할 수 없습니다.");
        }
        MultiplayerSkillManager manager = skillManager(playerId);
        PlayerState state = gameStateManager.ensurePlayer(playerId);
        if (manager == null || state == null) {
            return new SkillActionResult(false, "스킬 정보를 불러올 수 없습니다.");
        }

        int availablePoints = state.getSkillPoints();
        switch (upgradeType) {
            case 0: { // Attack Power
                int cost = manager.getAttackPowerCost();
                if (availablePoints < cost) {
                    return new SkillActionResult(false,
                            "스킬 포인트가 부족합니다! (필요: " + cost + ", 보유: " + availablePoints + ")");
                }
                state.setAttackPower(state.getAttackPower() + 1);
                state.setSkillPoints(availablePoints - cost);
                manager.increaseAttackPowerLevel();
                return new SkillActionResult(true,
                        "공격력이 증가했습니다! (현재: " + state.getAttackPower() + ", 레벨: " + manager.getAttackPowerLevel() + ")");
            }
            case 1: { // Attack Speed
                int cost = manager.getAttackSpeedCost();
                if (availablePoints < cost) {
                    return new SkillActionResult(false,
                            "스킬 포인트가 부족합니다! (필요: " + cost + ", 보유: " + availablePoints + ")");
                }
                state.setAttackSpeed(state.getAttackSpeed() + 0.2);
                state.setSkillPoints(availablePoints - cost);
                manager.increaseAttackSpeedLevel();
        return new SkillActionResult(true,
            "공격 속도가 증가했습니다! (현재: " + String.format(Locale.KOREA, "%.1f", state.getAttackSpeed())
                                + ", 레벨: " + manager.getAttackSpeedLevel() + ")");
            }
            case 2: { // HP Up & Heal
                int cost = manager.getHpUpCost();
                if (availablePoints < cost) {
                    return new SkillActionResult(false,
                            "스킬 포인트가 부족합니다! (필요: " + cost + ", 보유: " + availablePoints + ")");
                }
                state.setMaxHP(state.getMaxHP() + 3);
                state.setCurrentHP(state.getMaxHP());
                state.setSkillPoints(availablePoints - cost);
                manager.increaseHpUpLevel();
                return new SkillActionResult(true,
                        "최대 체력이 증가하고 체력이 회복되었습니다! (현재: " + state.getMaxHP() + ", 레벨: " + manager.getHpUpLevel() + ")");
            }
            default:
                return new SkillActionResult(false, "알 수 없는 강화 항목입니다.");
        }
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

    private boolean isBossAlive() {
        ArrayList<Entity> entities = gameStateManager.getEntities();
        ArrayList<Entity> removeList = gameStateManager.getRemoveList();
        for (Entity entity : entities) {
            if (entity instanceof BossEntity && !removeList.contains(entity)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void fireMissile(String playerId, double targetX, double targetY) {
        ShipEntity source = playerId != null ? getShip(playerId) : ship;
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
        boolean bossAlive = isBossAlive();
        if (remainingAliens == 0 && !bossAlive) {
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
        SharedMultiplayerRoundCoordinator.handleBossDefeated(this, killerPlayerId);
        alienCount = gameStateManager.getAlienCount();
        int completedRound = gameStateManager.getCurrentRound();
        boolean roundAdvanced = gameStateManager.advanceRound();
        if (roundAdvanced) {
            int nextRound = gameStateManager.getCurrentRound();
            SharedMultiplayerRoundCoordinator.TransitionDescriptor descriptor =
                    SharedMultiplayerRoundCoordinator.bossDefeatedTransition(completedRound, nextRound);
            Runnable initializer = () -> {
                SharedMultiplayerRoundCoordinator.setupRound(this, gameStateManager.getCurrentRound());
                alienCount = gameStateManager.getAlienCount();
            };
            scheduleIntermission(
                    new RoundTransition(
                            mapTransitionKind(descriptor.kind),
                            completedRound,
                            nextRound,
                            descriptor.bossNext,
                            descriptor.message),
                    initializer);
        } else {
            SharedMultiplayerRoundCoordinator.TransitionDescriptor descriptor =
                    SharedMultiplayerRoundCoordinator.gameCompletedTransition(completedRound);
            scheduleIntermission(
                    new RoundTransition(
                            mapTransitionKind(descriptor.kind),
                            completedRound,
                            completedRound,
                            descriptor.bossNext,
                            descriptor.message),
                    null);
        }
    }

    @Override
    public void notifyDeath(String playerId) {
        if (playerId != null && isPlayerInvincible(playerId)) {
            return;
        }
        String targetId = playerId != null ? playerId : gameStateManager.getLocalPlayerId();
        if (targetId == null) {
            return;
        }
        PlayerRuntime runtime = playerRuntimes.get(targetId);
        gameStateManager.takeDamage(targetId);
        PlayerState ps = gameStateManager.ensurePlayer(targetId);
        if (!ps.isDead()) {
            return;
        }
        handlePlayerElimination(targetId, runtime, ps);
    }

    @Override
    public boolean canEnemiesAttack() {
        long startTime = gameStateManager.getGameStartTime();
        if (startTime <= 0) {
            return false;
        }
        return System.currentTimeMillis() - startTime >= 3000;
    }

    @Override
    public void notifyPlayerDamaged(String playerId, int damage) {
        if (damage <= 0) {
            return;
        }
        if (playerId != null && isPlayerInvincible(playerId)) {
            return;
        }
        String targetId = playerId != null ? playerId : gameStateManager.getLocalPlayerId();
        if (targetId == null) {
            return;
        }

        PlayerRuntime runtime = playerRuntimes.get(targetId);
        PlayerState ps = gameStateManager.ensurePlayer(targetId);
        for (int i = 0; i < damage; i++) {
            gameStateManager.takeDamage(targetId);
            if (ps.isDead()) {
                handlePlayerElimination(targetId, runtime, ps);
                break;
            }
        }
    }

    private void handlePlayerElimination(String targetId, PlayerRuntime runtime, PlayerState ps) {
        if (!ps.isDead()) {
            return;
        }
        if (runtime != null && runtime.ship != null) {
            gameStateManager.getEntities().remove(runtime.ship);
            gameStateManager.getRemoveList().remove(runtime.ship);
            if (ship == runtime.ship) {
                ship = null;
            }
            runtime.ship = null;
        }
        if (runtime != null) {
            runtime.spectating = true;
        }

        if (isEveryoneEliminated()) {
            scheduleIntermission(
                    new RoundTransition(
                            RoundTransition.Type.GAME_COMPLETED,
                            gameStateManager.getCurrentRound(),
                            gameStateManager.getCurrentRound(),
                            false,
                            "모든 플레이어가 쓰러졌습니다."),
                    null);
            return;
        }

        gameStateManager.setWaitingForKeyPress(false);
        gameStateManager.setMessage("");
    }

    private void notifyWin() {
        if (inIntermission || pendingRoundInitializer != null) {
            return;
        }
        int completedRound = gameStateManager.getCurrentRound();
        boolean roundAdvanced = gameStateManager.advanceRound();
        if (roundAdvanced) {
            int nextRound = gameStateManager.getCurrentRound();
            SharedMultiplayerRoundCoordinator.TransitionDescriptor descriptor =
                    SharedMultiplayerRoundCoordinator.waveClearedTransition(completedRound, nextRound);
            Runnable initializer = () -> {
                SharedMultiplayerRoundCoordinator.setupRound(this, gameStateManager.getCurrentRound());
                alienCount = gameStateManager.getAlienCount();
            };
            scheduleIntermission(
                    new RoundTransition(
                            mapTransitionKind(descriptor.kind),
                            completedRound,
                            nextRound,
                            descriptor.bossNext,
                            descriptor.message),
                    initializer);
        } else {
            SharedMultiplayerRoundCoordinator.TransitionDescriptor descriptor =
                    SharedMultiplayerRoundCoordinator.gameCompletedTransition(completedRound);
            scheduleIntermission(
                    new RoundTransition(
                            mapTransitionKind(descriptor.kind),
                            completedRound,
                            completedRound,
                            descriptor.bossNext,
                            descriptor.message),
                    null);
        }
    }

    @Override
    public int getCurrentRound() {
        return gameStateManager.getCurrentRound();
    }

    public long getPlayTimeMs() {
        return gameStateManager.getPlayTimeMs();
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
    public boolean isPlayerInvincible(String playerId) {
        return skillManager(playerId).isInvincible();
    }

    @Override
    public ShipEntity getShip(String playerId) {
        if (playerId == null) {
            return ship;
        }
        PlayerRuntime runtime = playerRuntimes.get(playerId);
        return runtime != null ? runtime.ship : null;
    }

    @Override
    public int getShipX(String playerId) {
        ShipEntity target = getShip(playerId);
        return target != null ? target.getX() : 370;
    }

    @Override
    public int getShipY(String playerId) {
        ShipEntity target = getShip(playerId);
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

    @Override
    public void addCoins(String playerId, int amount) {
        if (amount == 0) {
            return;
        }
        if (playerId == null) {
            playerId = primaryPlayerId;
        }
        if (playerId == null) {
            return;
        }
        gameStateManager.addCoins(playerId, amount);
    }

    /**
     * 플레이어 스킨 설정
     */
    public void setPlayerSkin(String playerId, String skinPath) {
        if (playerId != null && skinPath != null) {
            playerSkins.put(playerId, skinPath);
            System.out.println("서버: 플레이어 " + playerId + " 스킨 설정: " + skinPath);
            
            // 기존 플레이어의 스킨을 즉시 업데이트
            updatePlayerSkin(playerId, skinPath);
        }
    }
    
    /**
     * 기존 플레이어의 스킨을 업데이트
     */
    private void updatePlayerSkin(String playerId, String skinPath) {
        System.out.println("서버 updatePlayerSkin 호출: playerId=" + playerId + ", skinPath=" + skinPath);
        PlayerRuntime runtime = playerRuntimes.get(playerId);
        if (runtime != null && runtime.ship != null) {
            System.out.println("  - PlayerRuntime 발견, 스킨 변경 시도...");
            runtime.ship.changeSkin(skinPath);
            System.out.println("서버: 플레이어 " + playerId + " 스킨 즉시 업데이트: " + skinPath);
        } else {
            System.out.println("  - PlayerRuntime 또는 ship이 null: runtime=" + (runtime != null ? "존재" : "null") + 
                             ", ship=" + (runtime != null && runtime.ship != null ? "존재" : "null"));
        }
    }
    
    /**
     * 플레이어 스킨 가져오기
     */
    public String getPlayerSkin(String playerId) {
        return playerSkins.getOrDefault(playerId, "sprites/ship.gif");
    }
    
    @Override
    public ShipEntity createPlayerShip(String playerId) {
        String skinPath = getPlayerSkin(playerId);
        return new ShipEntity(this, skinPath, 370, 550);
    }

    @Override
    public Entity createNearEntity(int round, int index) {
        int posX = 150 + (index * 100);
        return new NearEntity(this, posX, 120, round, index);
    }

    @Override
    public Entity createBossEntity(int bossRound) {
        return new BossEntity(this, 400, 160, bossRound);
    }

    @Override
    public void onRoundBackgroundChanged(int round) {
        // Server has no rendering surface; background changes are client-side concerns.
    }

    @Override
    public void showCoinEarned(String playerId, int x, int y, int coinAmount) {
        if (coinAmount <= 0) {
            return;
        }
        if (playerId == null || playerId.isEmpty()) {
            playerId = primaryPlayerId;
        }
        if (playerId == null || playerId.isEmpty()) {
            return;
        }
        String payload = x + "|" + y + "|" + coinAmount;
        pendingEvents.add(new GameEvent(GameEvent.Type.COIN_POPUP,
                playerId,
                payload,
                System.currentTimeMillis()));
    }

    @Override
    public void onNearMonsterDestroyed(NearEntity nearEntity, String killerPlayerId, double killX, double killY) {
        boolean cleared = SharedMultiplayerRoundCoordinator.handleNearMonsterDestroyed(
                this, nearEntity, killerPlayerId, killX, killY);
        alienCount = gameStateManager.getAlienCount();

        if (cleared) {
            notifyWin();
        }
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

    private RoundTransition.Type mapTransitionKind(SharedMultiplayerRoundCoordinator.TransitionKind kind) {
        switch (kind) {
            case WAVE_CLEARED:
                return RoundTransition.Type.WAVE_CLEARED;
            case BOSS_DEFEATED:
                return RoundTransition.Type.BOSS_DEFEATED;
            case GAME_COMPLETED:
            default:
                return RoundTransition.Type.GAME_COMPLETED;
        }
    }

    private void spawnBoss(boolean announce) {
        int round = gameStateManager.getCurrentRound();
        SharedMultiplayerRoundCoordinator.spawnBoss(this, round);
        alienCount = gameStateManager.getAlienCount();
        if (announce) {
            gameStateManager.setMessage("BOSS APPEARED!");
            gameStateManager.setWaitingForKeyPress(true);
        }
    }

    private void revivePlayersForNextRound() {
        for (String playerId : new ArrayList<>(playerRuntimes.keySet())) {
            if (playerId == null) {
                continue;
            }
            PlayerState state = gameStateManager.ensurePlayer(playerId);
            state.restoreFullHealth();
            PlayerRuntime runtime = playerRuntimes.get(playerId);
            if (runtime != null) {
                runtime.spectating = false;
                runtime.ship = null;
                runtime.leftPressed = false;
                runtime.rightPressed = false;
                runtime.firePressed = false;
                runtime.lastFire = 0;
            }
        }
    }

    private boolean isEveryoneEliminated() {
        if (playerRuntimes.isEmpty()) {
            return false;
        }
        for (String id : playerRuntimes.keySet()) {
            PlayerState state = gameStateManager.getPlayerState(id);
            if (state != null && state.isAlive()) {
                return false;
            }
        }
        return true;
    }
}
