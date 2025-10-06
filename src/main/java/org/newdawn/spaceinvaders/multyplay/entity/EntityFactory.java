package org.newdawn.spaceinvaders.multyplay.entity;

/**
 * Factory for instantiating multiplayer entity representations based on snapshot type hints.
 */
public final class EntityFactory {
    private EntityFactory() {
    }

    public static Entity create(String entityId, String type) {
        if (type == null) {
            return new Entity(entityId, "unknown");
        }
        switch (type) {
            case "ship":
                return new ShipEntity(entityId);
            case "alien":
                return new AlienEntity(entityId);
            case "boss":
                return new BossEntity(entityId);
            case "boss_shot":
                return new BossShotEntity(entityId);
            case "shot":
                return new ShotEntity(entityId);
            case "missile":
                return new MissileEntity(entityId);
            case "explosion":
                return new ExplosionEntity(entityId);
            case "heat_effect":
                return new HeatEffectEntity(entityId);
            case "skill_drop":
                return new SkillEntity(entityId);
            default:
                return new Entity(entityId, type);
        }
    }
}
