package org.newdawn.spaceinvaders.server.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.newdawn.spaceinvaders.common.entity.alien.AlienEntity;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.entity.MultiplayerAlienEnvironment;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;
import org.newdawn.spaceinvaders.multyplay.state.PlayerState;

public class ServerMultiplayerGameTest {

    private ServerMultiplayerGame createGameWithPlayers(String... playerIds) {
        ServerMultiplayerGame game = new ServerMultiplayerGame(0L);
        if (playerIds.length > 0) {
            game.registerPlayers(Arrays.asList(playerIds));
            game.setPrimaryPlayerId(playerIds[0]);
        }
        game.startGame();
        return game;
    }

    @Test
    public void shouldSpawnOneShipPerPlayerOnStart() {
        ServerMultiplayerGame game = createGameWithPlayers("host", "guest");

        List<ShipEntity> ships = game.getGameStateManager().getEntities().stream()
                .filter(ShipEntity.class::isInstance)
                .map(ShipEntity.class::cast)
                .collect(Collectors.toList());

        assertEquals("Incorrect number of ships spawned", 2, ships.size());
        assertEquals("Ship owners should be unique", 2,
                ships.stream().map(ShipEntity::getOwnerId).distinct().count());
        assertTrue("All ships must have an owner id",
                ships.stream().allMatch(ship -> ship.getOwnerId() != null && !ship.getOwnerId().isEmpty()));
    }

    @Test
    public void shouldRespawnShipsAfterRoundAdvance() {
        ServerMultiplayerGame game = createGameWithPlayers("host", "guest");
        MultiplayerGameStateManager state = game.getGameStateManager();

        state.getEntities().removeIf(AlienEntity.class::isInstance);
        game.notifyAlienKilled("host", 0, 0);

        assertTrue("Round start should be pending after clearing aliens", game.hasPendingRoundStart());
        game.startPendingRound();

        List<ShipEntity> ships = state.getEntities().stream()
                .filter(ShipEntity.class::isInstance)
                .map(ShipEntity.class::cast)
                .collect(Collectors.toList());

        assertEquals("Ships should respawn for each player", 2, ships.size());
        assertEquals("Ship owners should remain unique after respawn", 2,
                ships.stream().map(ShipEntity::getOwnerId).distinct().count());
    }

    @Test
    public void shouldReduceHpOnlyForTargetPlayer() {
        ServerMultiplayerGame game = createGameWithPlayers("host", "guest");
        MultiplayerGameStateManager state = game.getGameStateManager();
        PlayerState hostState = state.getPlayerState("host");
        PlayerState guestState = state.getPlayerState("guest");

        // PlayerState 초기 HP는 10
        assertEquals("Initial host HP should be 10", 10, hostState.getCurrentHP());
        assertEquals("Initial guest HP should be 10", 10, guestState.getCurrentHP());

        game.notifyDeath("guest");

        assertEquals("Host HP should remain unchanged", 10, hostState.getCurrentHP());
        assertEquals("Guest HP should decrease by one", 9, guestState.getCurrentHP());
    }

    @Test
    public void shouldAlignAlienBoundsWithScaledSprite() {
        ServerMultiplayerGame game = createGameWithPlayers("host");
        TestAlienEntity alien = new TestAlienEntity(game, 240, 180);

        // 실제 구현에 맞는 스케일 사용 (라운드 1: 0.18, 그 외: 0.75)
        double scale = (game.getCurrentRound() == 1) ? 0.18 : 0.75;
        int scaledWidth = (int) (alien.getSpriteWidth() * scale);
        int scaledHeight = (int) (alien.getSpriteHeight() * scale);
        
        // 실제 getBounds()는 스케일된 크기의 5% (너비)와 3% (높이)를 hitbox로 사용
        int expectedHitboxWidth = (int) Math.max(4, scaledWidth * 0.05);
        int expectedHitboxHeight = (int) Math.max(4, scaledHeight * 0.03);
        
        int drawX = (int) Math.round(alien.getPreciseCenterX()) - scaledWidth / 2;
        int drawY = (int) Math.round(alien.getPreciseCenterY()) - scaledHeight / 2;
        int expectedHitboxX = drawX + (scaledWidth - expectedHitboxWidth) / 2;
        int expectedHitboxY = drawY + (scaledHeight - expectedHitboxHeight) / 2;
        
        java.awt.Rectangle bounds = alien.getBounds();

        assertEquals("Alien bounds width should match hitbox calculation", expectedHitboxWidth, bounds.width);
        assertEquals("Alien bounds height should match hitbox calculation", expectedHitboxHeight, bounds.height);
        assertEquals("Alien bounds X offset should match hitbox position", expectedHitboxX, bounds.x);
        assertEquals("Alien bounds Y offset should match hitbox position", expectedHitboxY, bounds.y);
    }

    private static class TestAlienEntity extends AlienEntity {
        TestAlienEntity(MultiplayerGameContext game, int x, int y) {
            super(new MultiplayerAlienEnvironment(game), x, y, MultiplayerAlienEnvironment.hpResolver());
        }

        int getSpriteWidth() {
            return sprite.getWidth();
        }

        int getSpriteHeight() {
            return sprite.getHeight();
        }

        double getPreciseCenterX() {
            return x;
        }

        double getPreciseCenterY() {
            return y;
        }
    }
}
