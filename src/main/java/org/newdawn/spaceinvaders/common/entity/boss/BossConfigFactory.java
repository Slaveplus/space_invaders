package org.newdawn.spaceinvaders.common.entity.boss;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;
import java.util.HashMap;
import java.util.Map;

/**
 * 보스 설정을 생성하는 팩토리 클래스
 * 라운드별 설정을 중앙에서 관리
 */
public class BossConfigFactory {
    private static final Map<Integer, BossConfig> CONFIGS = createConfigs();
    
    private static Map<Integer, BossConfig> createConfigs() {
        Map<Integer, BossConfig> configs = new HashMap<>();
        
        // 라운드 1: HP 200, 이동 속도 60
        configs.put(1, new BossConfig.Builder()
            .round(1)
            .maxHP(200)
            .spritePath(SpriteConstants.BOSS_1_BOSS_PNG)
            .bossSize(280)
            .moveSpeed(60)
            .scoreValue(1000)
            .skillPointsReward(5)
            .build());
        
        // 라운드 2: HP 280, 이동 속도 70
        configs.put(2, new BossConfig.Builder()
            .round(2)
            .maxHP(280)
            .spritePath(SpriteConstants.BOSS_2_BOSS_PNG)
            .bossSize(280)
            .moveSpeed(70)
            .scoreValue(2000)
            .skillPointsReward(10)
            .build());
        
        // 라운드 3: HP 400, 이동 속도 80
        configs.put(3, new BossConfig.Builder()
            .round(3)
            .maxHP(400)
            .spritePath(SpriteConstants.BOSS_3_BOSS_PNG)
            .bossSize(280)
            .moveSpeed(80)
            .scoreValue(3000)
            .skillPointsReward(15)
            .build());
        
        // 라운드 4: HP 600, 이동 속도 90
        configs.put(4, new BossConfig.Builder()
            .round(4)
            .maxHP(600)
            .spritePath(SpriteConstants.BOSS_4_BOSS_PNG)
            .bossSize(280)
            .moveSpeed(90)
            .scoreValue(4000)
            .skillPointsReward(20)
            .build());
        
        // 기본값 (라운드 5 이상)
        configs.put(5, new BossConfig.Builder()
            .round(5)
            .maxHP(200)
            .spritePath(SpriteConstants.BOSS_5_BOSS_PNG)
            .bossSize(300)
            .moveSpeed(50)
            .scoreValue(1000)
            .skillPointsReward(5)
            .build());
        
        return configs;
    }
    
    /**
     * 라운드에 해당하는 보스 설정 반환
     * 
     * @param round 라운드 번호
     * @return 보스 설정
     */
    public static BossConfig getConfig(int round) {
        BossConfig config = CONFIGS.get(round);
        if (config == null) {
            // 기본값 반환
            return CONFIGS.get(5);
        }
        return config;
    }
}

