package org.newdawn.spaceinvaders.multyplay.entity;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.alien.AlienEnvironment;
import org.newdawn.spaceinvaders.common.entity.alien.BaseAlienEntity;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;

/**
 * 멀티플레이 Alien 구현 (공통 베이스 사용).
 */
public class AlienEntity extends BaseAlienEntity {
    private final MultiplayerGameContext game;

    public AlienEntity(MultiplayerGameContext game, int x, int y) {
        super(new MultiplayerAlienEnvironment(game), spriteForRound(game.getCurrentRound()), x, y);
        this.game = game;
    }

    private static String spriteForRound(int round) {
        switch (round) {
            case 1:
                return "sprites/Boss/1near.png";
            case 2:
                return "sprites/Boss/2near.png";
            case 3:
                return "sprites/Boss/3near.png";
            case 4:
                return "sprites/Boss/4near.png";
            case 5:
                return "sprites/Boss/5near.png";
            default:
                return "sprites/Boss/5near.png";
        }
    }

    @Override
    protected int resolveMaxHp(int round) {
        switch (round) {
            case 1: return 1;
            case 2: return 4;
            case 3: return 10;
            case 4: return 18;
            case 5: return 30;
            default: return 30 + ((round - 5) * 15);
        }
    }

    @Override
    protected void onAlienKilled() {
        // Multiplayer runtime handles kill notifications elsewhere.
    }

    private static final class MultiplayerAlienEnvironment implements AlienEnvironment {
        private final MultiplayerGameContext game;

        private MultiplayerAlienEnvironment(MultiplayerGameContext game) {
            this.game = game;
        }

        @Override
        public int getCurrentRound() {
            return game.getCurrentRound();
        }

        @Override
        public void addAimedAlienShot(int x, int y, int alienX) {
            game.addAimedAlienShot(x, y, alienX);
        }

        @Override
        public void removeEntity(Entity entity) {
            game.removeEntity(entity);
        }

        @Override
        public void notifyAlienKilled() {
            // Multiplayer mode handles kill notifications elsewhere.
        }

        @Override
        public int getPlayerShipX() {
            String localId = localPlayerId();
            return (localId != null) ? game.getShipX(localId) : 400;
        }

        @Override
        public int getPlayerShipY() {
            String localId = localPlayerId();
            return (localId != null) ? game.getShipY(localId) : 300;
        }

        private String localPlayerId() {
            MultiplayerGameStateManager gsm = game.getGameStateManager();
            return gsm != null ? gsm.getLocalPlayerId() : null;
        }
    }
}
