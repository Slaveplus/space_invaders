package org.newdawn.spaceinvaders.multyplay.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps entity type identifiers to sprite resources shared with the single-player client.
 */
public final class EntitySpriteResolver {
    private static final Map<String, String> SPRITES = new ConcurrentHashMap<>();

    static {
        SPRITES.put("ship", "sprites/ship.gif");
        SPRITES.put("alien", "sprites/alien.gif");
        SPRITES.put("boss", "sprites/Boss/2round_Boss.png");
        SPRITES.put("shot", "sprites/shot.gif");
        SPRITES.put("missile", "sprites/weapons/missile.png");
        SPRITES.put("boss_shot", "sprites/shot.gif");
        SPRITES.put("explosion", "sprites/Skill/Explosion.png");
        SPRITES.put("heat_effect", "sprites/Skill/Heat.gif");
        SPRITES.put("skill_drop", "sprites/Skill/1.png");
    }

    private EntitySpriteResolver() {
    }

    public static void register(String type, String spritePath) {
        if (type == null || spritePath == null) {
            return;
        }
        SPRITES.put(type, spritePath);
    }

    public static String resolve(String type) {
        return SPRITES.get(type);
    }
}
