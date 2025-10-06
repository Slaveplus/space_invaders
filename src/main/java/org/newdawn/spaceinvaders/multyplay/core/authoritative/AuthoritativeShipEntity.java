package org.newdawn.spaceinvaders.multyplay.core.authoritative;

import java.util.Collection;

import org.newdawn.spaceinvaders.multyplay.net.msg.PlayerInputMsg;

/**
 * Authoritative representation of a player's ship, handling movement constraints and firing cadence.
 */
public class AuthoritativeShipEntity extends AuthoritativeEntity {
    private static final double MOVE_SPEED = 300.0;
    private static final long FIRE_INTERVAL_MS = 250L;

    private final String playerId;
    private long lastFireTimestamp;

    public AuthoritativeShipEntity(String playerId, double x, double y) {
        super(playerId + "_ship", "ship", x, y);
        this.playerId = playerId;
        this.maxHp = 3;
        this.hp = maxHp;
        this.collisionRadius = 20.0;
    }

    public String getPlayerId() {
        return playerId;
    }

    @Override
    public void tick(long deltaMillis, AuthoritativeWorld world) {
        Collection<PlayerInputMsg> inputs = world.consumePendingInputs(playerId);
        boolean moveLeft = false;
        boolean moveRight = false;
        boolean firing = false;
        if (inputs != null) {
            for (PlayerInputMsg input : inputs) {
                if (input.getPressedKeys().contains(java.awt.event.KeyEvent.VK_LEFT) ||
                        input.getPressedKeys().contains(java.awt.event.KeyEvent.VK_A)) {
                    moveLeft = true;
                }
                if (input.getPressedKeys().contains(java.awt.event.KeyEvent.VK_RIGHT) ||
                        input.getPressedKeys().contains(java.awt.event.KeyEvent.VK_D)) {
                    moveRight = true;
                }
                if (input.isFiring()) {
                    firing = true;
                }
            }
        }

        velocityX = 0;
        if (moveLeft && !moveRight) {
            velocityX = -MOVE_SPEED;
        } else if (moveRight && !moveLeft) {
            velocityX = MOVE_SPEED;
        }
        velocityY = 0;

        super.tick(deltaMillis, world);

        // Clamp to playfield
        int stageWidth = world.getStageWidth();
        if (x < 10) {
            x = 10;
        }
        if (x > stageWidth - 50) {
            x = stageWidth - 50;
        }

        if (firing) {
            long now = System.currentTimeMillis();
            if (now - lastFireTimestamp >= FIRE_INTERVAL_MS) {
                world.spawnEntity(new AuthoritativeShotEntity(playerId, x + 16, y - 20));
                lastFireTimestamp = now;
            }
        }
    }
}
