package org.newdawn.spaceinvaders.common.entity.boss;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 공격 패턴 시퀀스를 관리하는 클래스
 * 여러 공격 패턴을 순차적으로 또는 랜덤하게 실행
 */
public class AttackPatternSequence {
    private final List<AttackPattern> patterns = new ArrayList<>();
    private final Random random = new Random();
    
    /**
     * 공격 패턴 추가
     */
    public void addPattern(AttackPattern pattern) {
        patterns.add(pattern);
    }
    
    /**
     * 사용 가능한 패턴 중 랜덤하게 하나 선택하여 실행
     * 모든 패턴이 사용되면 리셋
     * 
     * @return 실행된 패턴, 없으면 null
     */
    public AttackPattern selectAndExecuteRandom() {
        // 모든 패턴이 사용되었으면 리셋
        if (allPatternsUsed()) {
            resetAll();
        }
        
        // 사용 가능한 패턴 목록
        List<AttackPattern> available = getAvailablePatterns();
        if (available.isEmpty()) {
            return null;
        }
        
        // 랜덤 선택
        int index = random.nextInt(available.size());
        AttackPattern selected = available.get(index);
        selected.setUsed(true);
        selected.execute();
        
        return selected;
    }
    
    /**
     * 사용 가능한 패턴 목록 반환
     */
    private List<AttackPattern> getAvailablePatterns() {
        List<AttackPattern> available = new ArrayList<>();
        for (AttackPattern pattern : patterns) {
            if (!pattern.isUsed()) {
                available.add(pattern);
            }
        }
        return available;
    }
    
    /**
     * 모든 패턴이 사용되었는지 확인
     */
    private boolean allPatternsUsed() {
        for (AttackPattern pattern : patterns) {
            if (!pattern.isUsed()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 모든 패턴 사용 여부 리셋
     */
    public void resetAll() {
        for (AttackPattern pattern : patterns) {
            pattern.setUsed(false);
        }
    }
    
    /**
     * 패턴 목록 반환
     */
    public List<AttackPattern> getPatterns() {
        return new ArrayList<>(patterns);
    }
}

