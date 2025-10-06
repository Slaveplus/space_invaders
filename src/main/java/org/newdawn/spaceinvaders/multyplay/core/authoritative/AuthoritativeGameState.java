package org.newdawn.spaceinvaders.multyplay.core.authoritative;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import org.newdawn.spaceinvaders.multyplay.net.msg.GameSnapshotMsg;
import org.newdawn.spaceinvaders.multyplay.net.msg.PlayerInputMsg;

/**
 * Lightweight cooperative stage simulation inspired by the single-player loop.
 */
public class AuthoritativeGameState implements AuthoritativeWorld {
    private final List<AuthoritativeEntity> entities = new CopyOnWriteArrayList<>();
    private final Map<String, Queue<PlayerInputMsg>> pendingInputs = new ConcurrentHashMap<>();
    private final Map<String, PlayerState> players = new ConcurrentHashMap<>();

    private final int stageWidth;
    private final int stageHeight;
    private long tick;

    public AuthoritativeGameState(int stageWidth, int stageHeight) {
        this.stageWidth = stageWidth;
        this.stageHeight = stageHeight;
    }

    public void registerPlayer(String playerId) {
        players.putIfAbsent(playerId, new PlayerState(playerId));
        pendingInputs.putIfAbsent(playerId, new ConcurrentLinkedQueue<>());
        if (!hasShip(playerId)) {
            spawnEntity(new AuthoritativeShipEntity(playerId, stageWidth / 2.0, stageHeight - 80));
        }
    }

    public void queueInput(PlayerInputMsg msg) {
        pendingInputs.computeIfAbsent(msg.getPlayerId(), k -> new ConcurrentLinkedQueue<>()).add(msg);
        PlayerState state = players.get(msg.getPlayerId());
        if (state != null) {
            state.lastInputSequence = Math.max(state.lastInputSequence, msg.getSequence());
        }
    }

    public void spawnInitialWave(int rows, int columns) {
        double margin = 50;
        double usableWidth = stageWidth - (2 * margin);
        double spacingX = usableWidth / Math.max(1, columns - 1);
        double spacingY = 60;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                double x = margin + col * spacingX;
                double y = 80 + row * spacingY;
                spawnEntity(new AuthoritativeAlienEntity(x, y));
            }
        }
    }

    public void tick(long deltaMillis) {
        tick++;
        for (AuthoritativeEntity entity : entities) {
            entity.tick(deltaMillis, this);
        }
        processCollisions();
        entities.removeIf(entity -> entity.getHp() <= 0 && !(entity instanceof AuthoritativeShotEntity));
        ensureActiveWaves();
    }

    public GameSnapshotMsg buildSnapshot(String roomId) {
        List<GameSnapshotMsg.EntityState> entityStates = new ArrayList<>(entities.size());
        for (AuthoritativeEntity entity : entities) {
            entityStates.add(entity.toSnapshot());
        }
        Map<String, GameSnapshotMsg.PlayerState> playerStates = new ConcurrentHashMap<>();
        for (PlayerState state : players.values()) {
            playerStates.put(state.playerId, state.toSnapshot());
        }
        return new GameSnapshotMsg(
                roomId,
                tick,
                System.currentTimeMillis(),
                1,
                entityStates,
                playerStates,
                stateMaxSequence(),
                ""
        );
    }

    private int stateMaxSequence() {
        int max = 0;
        for (PlayerState state : players.values()) {
            max = Math.max(max, state.lastInputSequence);
        }
        return max;
    }

    @Override
    public void spawnEntity(AuthoritativeEntity entity) {
        if (entity == null) return;
        entities.add(entity);
    }

    @Override
    public void removeEntity(AuthoritativeEntity entity) {
        entities.remove(entity);
    }

    @Override
    public Collection<PlayerInputMsg> consumePendingInputs(String playerId) {
        Queue<PlayerInputMsg> queue = pendingInputs.get(playerId);
        if (queue == null || queue.isEmpty()) {
            return Collections.emptyList();
        }
        List<PlayerInputMsg> drained = new ArrayList<>();
        PlayerInputMsg msg;
        while ((msg = queue.poll()) != null) {
            drained.add(msg);
        }
        return drained;
    }

    private boolean hasShip(String playerId) {
        return findShipForPlayer(playerId) != null;
    }

    private AuthoritativeShipEntity findShipForPlayer(String playerId) {
        for (AuthoritativeEntity entity : entities) {
            if (entity instanceof AuthoritativeShipEntity) {
                AuthoritativeShipEntity ship = (AuthoritativeShipEntity) entity;
                if (ship.getPlayerId().equals(playerId)) {
                    return ship;
                }
            }
        }
        return null;
    }

    @Override
    public int getStageWidth() {
        return stageWidth;
    }

    @Override
    public int getStageHeight() {
        return stageHeight;
    }

    @Override
    public void onPlayerDamaged(String playerId, int amount) {
        PlayerState state = players.get(playerId);
        if (state != null) {
            state.hp = Math.max(0, state.hp - amount);
            AuthoritativeShipEntity ship = findShipForPlayer(playerId);
            if (ship != null) {
                ship.setHp(state.hp);
                if (state.hp <= 0) {
                    removeEntity(ship);
                }
            }
        }
    }

    @Override
    public void onAlienKilled() {
        // TODO: award score/skill points
    }

    public void reset() {
        entities.clear();
        tick = 0;
    }

    private void processCollisions() {
        Set<AuthoritativeEntity> toRemove = new HashSet<>();
        List<AuthoritativeEntity> snapshot = new ArrayList<>(entities);

        for (int i = 0; i < snapshot.size(); i++) {
            AuthoritativeEntity first = snapshot.get(i);
            if (toRemove.contains(first)) {
                continue;
            }
            for (int j = i + 1; j < snapshot.size(); j++) {
                AuthoritativeEntity second = snapshot.get(j);
                if (toRemove.contains(second)) {
                    continue;
                }
                if (!first.collides(second)) {
                    continue;
                }

                if (first instanceof AuthoritativeShotEntity) {
                    AuthoritativeShotEntity playerShot = (AuthoritativeShotEntity) first;
                    if (second instanceof AuthoritativeAlienEntity) {
                        handlePlayerShotHitsAlien(playerShot, second, toRemove);
                        continue;
                    }
                }
                if (second instanceof AuthoritativeShotEntity) {
                    AuthoritativeShotEntity playerShot = (AuthoritativeShotEntity) second;
                    if (first instanceof AuthoritativeAlienEntity) {
                        handlePlayerShotHitsAlien(playerShot, first, toRemove);
                        continue;
                    }
                }

                if (first instanceof AuthoritativeAlienShotEntity && second instanceof AuthoritativeShipEntity) {
                    handleAlienShotHitsShip((AuthoritativeAlienShotEntity) first, (AuthoritativeShipEntity) second, toRemove);
                    continue;
                }
                if (second instanceof AuthoritativeAlienShotEntity && first instanceof AuthoritativeShipEntity) {
                    handleAlienShotHitsShip((AuthoritativeAlienShotEntity) second, (AuthoritativeShipEntity) first, toRemove);
                    continue;
                }

                if (first instanceof AuthoritativeAlienEntity && second instanceof AuthoritativeShipEntity) {
                    handleAlienHitsShip((AuthoritativeAlienEntity) first, (AuthoritativeShipEntity) second, toRemove);
                    continue;
                }
                if (second instanceof AuthoritativeAlienEntity && first instanceof AuthoritativeShipEntity) {
                    handleAlienHitsShip((AuthoritativeAlienEntity) second, (AuthoritativeShipEntity) first, toRemove);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            entities.removeAll(toRemove);
        }
    }

    private void handlePlayerShotHitsAlien(AuthoritativeShotEntity shot,
                                           AuthoritativeEntity alien,
                                           Set<AuthoritativeEntity> toRemove) {
        toRemove.add(shot);
        toRemove.add(alien);
        onAlienKilled();
    }

    private void handleAlienShotHitsShip(AuthoritativeAlienShotEntity shot,
                                         AuthoritativeShipEntity ship,
                                         Set<AuthoritativeEntity> toRemove) {
        toRemove.add(shot);
        onPlayerDamaged(ship.getPlayerId(), 1);
        PlayerState state = players.get(ship.getPlayerId());
        if (state != null && state.hp <= 0) {
            toRemove.add(ship);
        }
    }

    private void handleAlienHitsShip(AuthoritativeAlienEntity alien,
                                     AuthoritativeShipEntity ship,
                                     Set<AuthoritativeEntity> toRemove) {
        toRemove.add(alien);
        onPlayerDamaged(ship.getPlayerId(), 1);
        PlayerState state = players.get(ship.getPlayerId());
        if (state != null && state.hp <= 0) {
            toRemove.add(ship);
        }
    }

    private void ensureActiveWaves() {
        if (players.isEmpty()) {
            return;
        }
        boolean anyAlien = false;
        for (AuthoritativeEntity entity : entities) {
            if (entity instanceof AuthoritativeAlienEntity) {
                anyAlien = true;
                break;
            }
        }
        if (!anyAlien) {
            spawnInitialWave(3, 6);
        }
    }

    private static class PlayerState {
        private final String playerId;
        private int hp = 3;
        private int maxHp = 3;
        private int attackPower = 1;
        private double attackSpeed = 1.0;
        private int skillPoints = 0;
        private int lastInputSequence = 0;

        private PlayerState(String playerId) {
            this.playerId = playerId;
        }

        private GameSnapshotMsg.PlayerState toSnapshot() {
            return new GameSnapshotMsg.PlayerState(playerId, hp, maxHp, attackPower, attackSpeed, skillPoints);
        }
    }
}
