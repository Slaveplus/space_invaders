package org.newdawn.spaceinvaders.multyplay.core;

import java.util.Iterator;
import java.util.Random;
import org.newdawn.spaceinvaders.common.entity.alien.AlienEntity;
import org.newdawn.spaceinvaders.common.entity.boss.BossEntity;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.near.NearEntity;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerSkillManager;
import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;

/**
 * Shared helper consolidating round layout logic for multiplayer client and server.
 */
public final class SharedMultiplayerRoundCoordinator {

    private static final int NEAR_MONSTER_COUNT = 6;
    private static final Random RNG = new Random();

    private SharedMultiplayerRoundCoordinator() {
    }

    public static boolean isNearRound(int round) {
        return round % 2 == 1;
    }

    public static int mapBossRound(int round) {
        if (isNearRound(round)) {
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
        if (round <= 0) {
            return 1;
        }
        return Math.max(1, round / 2);
    }

    public static void setupRound(MultiplayerGameContext context, int round) {
        MultiplayerGameStateManager gsm = context.getGameStateManager();
        boolean hasExistingHostiles = gsm.getEntities().stream().anyMatch(entity ->
                (entity instanceof AlienEntity) ||
                        (entity instanceof NearEntity) ||
                        (entity instanceof BossEntity));
        if (hasExistingHostiles) {
            return;
        }
        clearHostiles(gsm);
        context.onRoundBackgroundChanged(round);

        if (isNearRound(round)) {
            spawnNearMonsters(context, round);
            gsm.setMessage("🎯 NEAR MONSTERS APPEARED! 🎯");
        } else {
            spawnBoss(context, round);
            gsm.setMessage("⚠️ BOSS APPEARED! ⚠️");
        }
        gsm.setWaitingForKeyPress(false);
    }

    public static void spawnNearMonsters(MultiplayerGameContext context, int round) {
        MultiplayerGameStateManager gsm = context.getGameStateManager();
        clearHostiles(gsm);
        for (int index = 0; index < NEAR_MONSTER_COUNT; index++) {
            Entity near = context.createNearEntity(round, index);
            if (near != null) {
                context.addEntity(near);
            }
        }
        gsm.setAlienCount(NEAR_MONSTER_COUNT);
        gsm.setMessage("🎯 NEAR MONSTERS APPEARED! 🎯");
        gsm.setWaitingForKeyPress(false);
        context.onRoundBackgroundChanged(round);
    }

    public static void spawnBoss(MultiplayerGameContext context, int round) {
        MultiplayerGameStateManager gsm = context.getGameStateManager();
        clearHostiles(gsm);
        Entity boss = context.createBossEntity(mapBossRound(round));
        if (boss != null) {
            context.addEntity(boss);
            gsm.setAlienCount(1);
        } else {
            gsm.setAlienCount(0);
        }
        gsm.setMessage("⚠️ BOSS APPEARED! ⚠️");
        gsm.setWaitingForKeyPress(false);
        context.onRoundBackgroundChanged(round);
    }

    public static boolean handleNearMonsterDestroyed(MultiplayerGameContext context,
                                                     NearEntity nearEntity,
                                                     String killerPlayerId,
                                                     double killX,
                                                     double killY) {
        MultiplayerGameStateManager gsm = context.getGameStateManager();
        if (killerPlayerId == null) {
            killerPlayerId = gsm.getLocalPlayerId();
        }

        if (killerPlayerId != null) {
            context.addSkillPoints(killerPlayerId, 2 + mapBossRound(nearEntity.getRound()));
            MultiplayerSkillManager manager = context.getSkillManager(killerPlayerId);
            if (manager != null) {
                double dropChance = manager.getSkillDropChance(gsm.getCurrentRound());
                if (RNG.nextDouble() < dropChance) {
                    manager.dropSkill(gsm.getCurrentRound(), killX, killY);
                }
            }
            int coinReward = 5 + mapBossRound(nearEntity.getRound());
            context.addCoins(killerPlayerId, coinReward);
            int coinX = Double.isNaN(killX) ? 400 : (int) killX;
            int coinY = Double.isNaN(killY) ? 200 : (int) killY;
            context.showCoinEarned(killerPlayerId, coinX, coinY, coinReward);
        }

        int remaining = Math.max(0, gsm.getAlienCount() - 1);
        gsm.setAlienCount(remaining);
        if (remaining == 0) {
            int waveClearReward = Math.max(0, gsm.getCurrentRound() * 8);
            if (waveClearReward > 0) {
                context.addCoins(killerPlayerId, waveClearReward);
                int rewardX = Double.isNaN(killX) ? 400 : (int) killX;
                int rewardY = Double.isNaN(killY) ? 200 : (int) killY;
                context.showCoinEarned(killerPlayerId, rewardX, rewardY, waveClearReward);
            }
            return true;
        }
        return false;
    }

    public static void handleBossDefeated(MultiplayerGameContext context, String killerPlayerId) {
        MultiplayerGameStateManager gsm = context.getGameStateManager();
        if (killerPlayerId == null) {
            killerPlayerId = gsm.getLocalPlayerId();
        }
        if (killerPlayerId != null) {
            context.addSkillPoints(killerPlayerId, Math.max(5, gsm.getCurrentRound() * 2));
            int coinReward = gsm.getCurrentRound() * 15;
            context.addCoins(killerPlayerId, coinReward);
            context.showCoinEarned(killerPlayerId, 400, 200, coinReward);
        }
        gsm.setAlienCount(0);
    }

    public static TransitionDescriptor waveClearedTransition(int completedRound, int nextRound) {
        boolean bossNext = !isNearRound(nextRound);
        String message = String.format("라운드 %d 클리어! 모든 플레이어 준비 후 다음 라운드가 시작됩니다.", completedRound);
        return new TransitionDescriptor(TransitionKind.WAVE_CLEARED, message, bossNext);
    }

    public static TransitionDescriptor bossDefeatedTransition(int completedRound, int nextRound) {
        boolean bossNext = isNearRound(nextRound);
        String message = "보스를 처치했습니다! 다음 라운드를 준비하세요.";
        return new TransitionDescriptor(TransitionKind.BOSS_DEFEATED, message, bossNext);
    }

    public static TransitionDescriptor gameCompletedTransition(int completedRound) {
        String message = "모든 보스를 물리쳤습니다!";
        return new TransitionDescriptor(TransitionKind.GAME_COMPLETED, message, false);
    }

    public enum TransitionKind {
        WAVE_CLEARED,
        BOSS_DEFEATED,
        GAME_COMPLETED
    }

    public static final class TransitionDescriptor {
        public final TransitionKind kind;
        public final String message;
        public final boolean bossNext;

        public TransitionDescriptor(TransitionKind kind, String message, boolean bossNext) {
            this.kind = kind;
            this.message = message;
            this.bossNext = bossNext;
        }
    }

    private static void clearHostiles(MultiplayerGameStateManager gsm) {
        Iterator<Entity> iterator = gsm.getEntities().iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (entity instanceof AlienEntity || entity instanceof NearEntity || entity instanceof BossEntity) {
                iterator.remove();
            }
        }
    }
}
