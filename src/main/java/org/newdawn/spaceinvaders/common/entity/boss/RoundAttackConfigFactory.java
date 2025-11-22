package org.newdawn.spaceinvaders.common.entity.boss;

import java.util.HashMap;
import java.util.Map;

/**
 * 라운드별 공격 설정을 생성하는 팩토리 클래스
 */
public class RoundAttackConfigFactory {
    private static final Map<Integer, RoundAttackConfig> CONFIGS = createConfigs();
    
    private static Map<Integer, RoundAttackConfig> createConfigs() {
        Map<Integer, RoundAttackConfig> configs = new HashMap<>();
        
        // 라운드 1 설정
        configs.put(1, new RoundAttackConfig.Builder()
            .mainAttackTimer(3000)
            .iceAttackTimer(4000)
            .iceBallAttackTimer(4000)
            .magneticFieldTimer(15000)
            .build());
        
        // 라운드 2 설정
        configs.put(2, new RoundAttackConfig.Builder()
            .mainAttackTimer(4000)
            .build());
        
        // 라운드 3 설정
        configs.put(3, new RoundAttackConfig.Builder()
            .mainAttackTimer(4000)
            .build());
        
        // 라운드 4 설정
        configs.put(4, new RoundAttackConfig.Builder()
            .healAttackTimer(60000)
            .greenSphereAttackTimer(2000)
            .playerLineAttackTimer(5000)
            .timeLimit(600000)
            .build());
        
        return configs;
    }
    
    /**
     * 라운드에 해당하는 공격 설정 반환
     * 
     * @param round 라운드 번호
     * @return 공격 설정
     */
    public static RoundAttackConfig getConfig(int round) {
        RoundAttackConfig config = CONFIGS.get(round);
        if (config == null) {
            // 기본값 반환 (라운드 1 설정)
            return CONFIGS.get(1);
        }
        return config;
    }
}

