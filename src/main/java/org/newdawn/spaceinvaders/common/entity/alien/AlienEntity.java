package org.newdawn.spaceinvaders.common.entity.alien;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * 싱글/멀티 모든 모드에서 사용하는 표준 Alien 엔티티.
 */
public class AlienEntity extends BaseAlienEntity {
    private final IntUnaryOperator hpResolver;

    public AlienEntity(AlienEnvironment environment, int x, int y, IntUnaryOperator hpResolver) {
        this(environment, spriteForRound(environment.getCurrentRound()), x, y, hpResolver);
    }

    public AlienEntity(AlienEnvironment environment,
                       String spriteRef,
                       int x,
                       int y,
                       IntUnaryOperator hpResolver) {
        super(environment, spriteRef, x, y);
        this.hpResolver = Objects.requireNonNull(hpResolver, "hpResolver");
    }

    @Override
    protected int resolveMaxHp(int round) {
        return hpResolver.applyAsInt(round);
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
}
