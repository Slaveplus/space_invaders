package org.newdawn.spaceinvaders.multyplay.entity;

/**
 * Placeholder for skill drop entity metadata.
 */
public class Skill {
    public enum Type { INVINCIBLE, PIERCING, TRIPLE_SHOT }

    private final Type type;
    private final int value;

    public Skill(Type type, int value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public int getValue() {
        return value;
    }
}
