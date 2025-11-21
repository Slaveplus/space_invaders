package org.newdawn.spaceinvaders.multyplay.entity;

import java.util.function.IntUnaryOperator;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.alien.AlienEnvironment;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;

/**
 * 멀티플레이 모드 Alien 엔티티용 환경 어댑터.
 */
public class MultiplayerAlienEnvironment implements AlienEnvironment {
    private final MultiplayerGameContext game;

    public MultiplayerAlienEnvironment(MultiplayerGameContext game) {
        this.game = game;
    }

    public static IntUnaryOperator hpResolver() {
        return round -> {
            switch (round) {
                case 1:
                    return 1;
                case 2:
                    return 4;
                case 3:
                    return 10;
                case 4:
                    return 18;
                case 5:
                    return 30;
                default:
                    return 30 + Math.max(0, (round - 5) * 15);
            }
        };
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
        // 멀티플레이 로직에서는 별도 헨들러에서 처리한다.
    }

    @Override
    public int getPlayerShipX() {
        String localId = localPlayerId();
        return localId != null ? game.getShipX(localId) : 400;
    }

    @Override
    public int getPlayerShipY() {
        String localId = localPlayerId();
        return localId != null ? game.getShipY(localId) : 300;
    }

    private String localPlayerId() {
        MultiplayerGameStateManager gsm = game.getGameStateManager();
        return (gsm != null) ? gsm.getLocalPlayerId() : null;
    }
}
