package org.newdawn.spaceinvaders.server.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.entity.AlienEntity;
import org.newdawn.spaceinvaders.multyplay.entity.ShipEntity;
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

        game.notifyDeath("guest");

        assertEquals("Host HP should remain unchanged", 3, hostState.getCurrentHP());
        assertEquals("Guest HP should decrease by one", 2, guestState.getCurrentHP());
    }

    @Test
    public void shouldAlignAlienBoundsWithScaledSprite() {
        ServerMultiplayerGame game = createGameWithPlayers("host");
        TestAlienEntity alien = new TestAlienEntity(game, 240, 180);

        double scale = (game.getCurrentRound() == 1) ? 0.45 : 0.75;
        int expectedWidth = (int) Math.round(alien.getSpriteWidth() * scale);
        int expectedHeight = (int) Math.round(alien.getSpriteHeight() * scale);
        java.awt.Rectangle bounds = alien.getBounds();

        assertEquals("Alien bounds width should match scaled sprite", expectedWidth, bounds.width);
        assertEquals("Alien bounds height should match scaled sprite", expectedHeight, bounds.height);

        double offsetX = (alien.getSpriteWidth() - expectedWidth) / 2.0;
        double offsetY = (alien.getSpriteHeight() - expectedHeight) / 2.0;

        assertEquals("Alien bounds X offset should match draw position",
                Math.round(alien.getX() + offsetX), bounds.x);
        assertEquals("Alien bounds Y offset should match draw position",
                Math.round(alien.getY() + offsetY), bounds.y);
    }

    private static class TestAlienEntity extends AlienEntity {
        TestAlienEntity(MultiplayerGameContext game, int x, int y) {
            super(game, x, y);
        }

        int getSpriteWidth() {
            return sprite.getWidth();
        }

        int getSpriteHeight() {
            return sprite.getHeight();
        }
    }
}
