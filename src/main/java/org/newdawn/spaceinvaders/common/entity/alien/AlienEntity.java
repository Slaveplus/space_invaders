package org.newdawn.spaceinvaders.common.entity.alien;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;

import java.util.function.IntUnaryOperator;

/**
 * 싱글/멀티 모든 모드에서 사용하는 표준 Alien 엔티티.
 */
public class AlienEntity extends BaseAlienEntity {

    public AlienEntity(AlienEnvironment environment, int x, int y, IntUnaryOperator hpResolver) {
        super(environment, spriteForRound(environment.getCurrentRound()), x, y, hpResolver);
    }

    public AlienEntity(AlienEnvironment environment,
                       String spriteRef,
                       int x,
                       int y,
                       IntUnaryOperator hpResolver) {
        super(environment, spriteRef, x, y, hpResolver);
    }

    private static String spriteForRound(int round) {
        switch (round) {
            case 1:
                return SpriteConstants.BOSS_1_NEAR_PNG;
            case 2:
                return SpriteConstants.BOSS_2_NEAR_PNG;
            case 3:
                return SpriteConstants.BOSS_3_NEAR_PNG;
            case 4:
                return SpriteConstants.BOSS_4_NEAR_PNG;
            case 5:
                return SpriteConstants.BOSS_5_NEAR_PNG;
            default:
                return SpriteConstants.BOSS_5_NEAR_PNG;
        }
    }
}
