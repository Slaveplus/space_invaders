package org.newdawn.spaceinvaders.gameplay.core;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared helper that encapsulates the random skill drop distribution used by
 * both single-player and multiplayer runtimes.
 */
public final class SkillDropTable {

    private SkillDropTable() {
    }

    private static final SkillDrop INVINCIBLE_DROP = new SkillDrop(0, 5); // seconds
    private static final SkillDrop TRIPLE_SHOT_DROP = new SkillDrop(2, 8); // seconds
    private static final SkillDrop MISSILE_DROP = new SkillDrop(3, 1); // missile count

    /**
     * Roll a random skill drop for the supplied round.
     *
     * The current table mirrors the single-player drop behaviour:
     * <ul>
     *     <li>Invincible (5s)</li>
     *     <li>Triple Shot (8s)</li>
     *     <li>Missile x1</li>
     * </ul>
     *
     * @param round current round number (reserved for future tuning)
     * @return the selected skill drop
     */
    public static SkillDrop rollDrop(int round) {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.4) {
            return INVINCIBLE_DROP;
        }
        if (roll < 0.8) {
            return TRIPLE_SHOT_DROP;
        }
        return MISSILE_DROP;
    }

    /**
     * Simple value holder describing a generated skill drop.
     */
    public static final class SkillDrop {
        public final int skillType;
        public final int skillValue;

        public SkillDrop(int skillType, int skillValue) {
            this.skillType = skillType;
            this.skillValue = skillValue;
        }
    }
}
