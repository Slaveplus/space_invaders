package org.newdawn.spaceinvaders.multyplay.entity;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.projectile.BaseBossShotEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.BossShotEnvironment;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;

/**
 * 멀티플레이용 BossShot 구현.
 */
public class BossShotEntity extends BaseBossShotEntity {
    public BossShotEntity(MultiplayerGameContext game,
                          int x,
                          int y,
                          double directionX,
                          double directionY,
                          double speed) {
        this(game, x, y, directionX, directionY, speed, 8, false, 0, 0);
    }

    public BossShotEntity(MultiplayerGameContext game,
                          int x,
                          int y,
                          double directionX,
                          double directionY,
                          double speed,
                          int radius,
                          boolean canSplit,
                          double splitY,
                          int splitCount) {
        super(new MultiplayerBossShotEnvironment(game),
                x,
                y,
                directionX,
                directionY,
                speed,
                radius,
                canSplit,
                splitY,
                splitCount);
    }

    @Override
    protected BaseBossShotEntity createChild(int x, int y, double directionX, double directionY, double speed, int radius) {
        MultiplayerGameContext game = ((MultiplayerBossShotEnvironment) environment).game;
        return new BossShotEntity(game, x, y, directionX, directionY, speed, radius, false, 0, 0);
    }

    @Override
    protected String snapshotMetadata() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        map.put("dirX", Double.toString(directionX));
        map.put("dirY", Double.toString(directionY));
        map.put("speed", Double.toString(speed));
        map.put("radius", Integer.toString(radius));
        map.put("split", canSplit ? "1" : "0");
        map.put("splitY", Double.toString(splitY));
        map.put("splitCount", Integer.toString(splitCount));
        return org.newdawn.spaceinvaders.multyplay.net.protocol.MetadataCodec.encode(map);
    }

    @Override
    protected void applySnapshotMetadata(String metadata) {
        java.util.Map<String, String> map = org.newdawn.spaceinvaders.multyplay.net.protocol.MetadataCodec.decode(metadata);
        if (map.isEmpty()) {
            return;
        }
        try { directionX = Double.parseDouble(map.getOrDefault("dirX", Double.toString(directionX))); } catch (NumberFormatException ignore) {}
        try { directionY = Double.parseDouble(map.getOrDefault("dirY", Double.toString(directionY))); } catch (NumberFormatException ignore) {}
        try { speed = Double.parseDouble(map.getOrDefault("speed", Double.toString(speed))); } catch (NumberFormatException ignore) {}
        try { radius = Integer.parseInt(map.getOrDefault("radius", Integer.toString(radius))); } catch (NumberFormatException ignore) {}
        canSplit = "1".equals(map.get("split"));
        try { splitY = Double.parseDouble(map.getOrDefault("splitY", Double.toString(splitY))); } catch (NumberFormatException ignore) {}
        try { splitCount = Integer.parseInt(map.getOrDefault("splitCount", Integer.toString(splitCount))); } catch (NumberFormatException ignore) {}
    }

    private static final class MultiplayerBossShotEnvironment implements BossShotEnvironment {
        private final MultiplayerGameContext game;

        private MultiplayerBossShotEnvironment(MultiplayerGameContext game) {
            this.game = game;
        }

        @Override
        public void addEntity(Entity entity) {
            game.addEntity(entity);
        }

        @Override
        public void removeEntity(Entity entity) {
            game.removeEntity(entity);
        }

        @Override
        public void notifyPlayerHit(String ownerId) {
            game.notifyDeath(ownerId);
        }
    }
}
