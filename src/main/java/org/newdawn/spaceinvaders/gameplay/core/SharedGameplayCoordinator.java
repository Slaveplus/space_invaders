package org.newdawn.spaceinvaders.gameplay.core;

import java.util.List;
import java.util.Objects;
import org.newdawn.spaceinvaders.gameplay.GameStateManager;
import org.newdawn.spaceinvaders.gameplay.SkillManager;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.entity.alien.AlienEntity;
import org.newdawn.spaceinvaders.common.entity.boss.BossEntity;
import org.newdawn.spaceinvaders.common.entity.near.NearEntity;

/**
 * Shared gameplay coordinator consolidating round progression and reward logic.
 */
public class SharedGameplayCoordinator {

    private final GameplayContext context;

    public SharedGameplayCoordinator(GameplayContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public GameStateManager getGameStateManager() {
        return context.getGameStateManager();
    }

    public void startNewGame(String playerId) {
        GameStateManager gsm = getGameStateManager();
        gsm.startNewGame();
        initializeRound(playerId);
        context.onRoundBackgroundChanged(gsm.getCurrentRound());
    }

    public void initializeRound(String playerId) {
        GameStateManager gsm = getGameStateManager();

        ShipEntity ship = context.getShip(playerId);
        if (ship == null) {
            ship = context.createPlayerShip(playerId);
        }
        if (!context.getActiveEntities().contains(ship)) {
            context.addEntity(ship);
        }

        int currentRound = gsm.getCurrentRound();
        if (isNearRound(currentRound)) {
            spawnNearMonsters(currentRound);
        } else {
            spawnBoss(currentRound);
        }

        gsm.setWaitingForKeyPress(false);
    }

    public void handleAlienKilled(String playerId) {
        GameStateManager gsm = getGameStateManager();
        String targetId = playerId != null ? playerId : gsm.getLocalPlayerId();
        SkillManager skillManager = context.getSkillManager(targetId);

        gsm.addSkillPoints(skillManager.getRandomSkillPoints(gsm.getCurrentRound()));
        double dropChance = skillManager.getSkillDropChance(gsm.getCurrentRound());
        if (Math.random() < dropChance) {
            skillManager.dropSkill(gsm.getCurrentRound());
        }

        int remainingHostiles = 0;
        boolean bossAlive = false;
        List<Entity> entities = context.getActiveEntities();
        List<Entity> removeQueue = context.getPendingRemovals();

        for (Entity entity : entities) {
            if (removeQueue.contains(entity)) {
                continue;
            }
            if (entity instanceof AlienEntity) {
                remainingHostiles++;
            } else if (entity instanceof NearEntity) {
                remainingHostiles++;
            } else if (entity instanceof BossEntity) {
                bossAlive = true;
            }
        }

        if (remainingHostiles == 0 && !bossAlive && !isNearRound(gsm.getCurrentRound())) {
            handleRoundClear(targetId);
        }
    }

    public void handleNearMonstersCleared(String playerId) {
        GameStateManager gsm = getGameStateManager();
        List<Entity> entities = context.getActiveEntities();
        List<Entity> removeQueue = context.getPendingRemovals();

        int nearCount = 0;
        for (Entity entity : entities) {
            if (removeQueue.contains(entity)) {
                continue;
            }
            if (entity instanceof NearEntity) {
                nearCount++;
            }
        }

        if (nearCount == 0) {
            int reward = gsm.getCurrentRound() * 8;
            if (reward > 0) {
                context.addEarnedCoins(playerId, reward);
            }

            boolean advanced = gsm.advanceRound();
            if (advanced) {
                resetEntities();
                initializeRound(playerId);
                gsm.setMessage("🎯 ALL NEAR MONSTERS DEFEATED! 🎯");
                gsm.setWaitingForKeyPress(false);
            } else {
                notifyGameCompleted(gsm);
            }
        }
    }

    public void handleRoundClear(String playerId) {
        GameStateManager gsm = getGameStateManager();
        String targetId = playerId != null ? playerId : gsm.getLocalPlayerId();
        int completedRound = gsm.getCurrentRound();

        int bossReward = Math.max(0, completedRound * 15);
        if (bossReward > 0) {
            context.addEarnedCoins(targetId, bossReward);
        }

        boolean advanced = gsm.advanceRound();

        if (advanced) {
            context.onRoundBackgroundChanged(gsm.getCurrentRound());
            resetEntities();
            initializeRound(targetId);
            gsm.setMessage("");
            gsm.setWaitingForKeyPress(false);
        } else {
            notifyGameCompleted(gsm);
        }
    }

    public boolean isNearRound(int round) {
        return round % 2 == 1;
    }

    public int getBossRoundFromNearRound(int round) {
        switch (round) {
            case 1:
                return 1;
            case 3:
                return 2;
            case 5:
                return 3;
            case 7:
                return 4;
            default:
                return 1;
        }
    }

    private int mapBossRound(int currentRound) {
        if (isNearRound(currentRound)) {
            return getBossRoundFromNearRound(currentRound);
        }
        if (currentRound == 2) {
            return 1;
        }
        if (currentRound == 4) {
            return 2;
        }
        if (currentRound == 6) {
            return 3;
        }
        return 4;
    }

    private void spawnNearMonsters(int round) {
        GameStateManager gsm = getGameStateManager();
        for (int i = 0; i < 6; i++) {
            Entity near = context.createNearEntity(round, i);
            context.addEntity(near);
        }
        gsm.setAlienCount(6);
        gsm.setMessage("🎯 NEAR MONSTERS APPEARED! 🎯");
        gsm.setWaitingForKeyPress(false);
    }

    private void spawnBoss(int round) {
        GameStateManager gsm = getGameStateManager();
        Entity boss = context.createBossEntity(mapBossRound(round));
        context.addEntity(boss);
        gsm.setAlienCount(1);
        gsm.setMessage("⚠️ BOSS APPEARED! ⚠️");
        gsm.setWaitingForKeyPress(false);
    }

    private void resetEntities() {
        context.getActiveEntities().clear();
        context.getPendingRemovals().clear();
    }

    private void notifyGameCompleted(GameStateManager gsm) {
        String message = String.format("경축%n걸린시간 : %s%n획득코인 : %d개",
                gsm.getPlayTime(), gsm.getEarnedCoins());
        context.onGameCompleted(message);
    }
}
