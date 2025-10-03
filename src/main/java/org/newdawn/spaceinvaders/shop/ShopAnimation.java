package org.newdawn.spaceinvaders.shop;

/**
 * 상점 진입 애니메이션 클래스
 * 양쪽 박스가 밀려오는 효과를 구현
 */
public class ShopAnimation {
    private boolean isAnimating = false;
    private long animationStartTime;
    private long animationDuration = 800; // 0.8초 애니메이션
    private float animationProgress = 0.0f;
    
    // 애니메이션 타입
    public enum AnimationType {
        SLIDE_IN,    // 양쪽에서 밀려오는 효과
        SLIDE_OUT,   // 양쪽으로 밀려나가는 효과
        FADE_IN,     // 페이드 인 효과
        FADE_OUT,    // 페이드 아웃 효과
        SCALE_IN     // 크기 변화 효과
    }
    
    private AnimationType currentType = AnimationType.SLIDE_IN;
    
    /**
     * 애니메이션 시작
     */
    public void startAnimation(AnimationType type) {
        this.isAnimating = true;
        this.animationStartTime = System.currentTimeMillis();
        this.animationProgress = 0.0f;
        this.currentType = type;
    }
    
    /**
     * 애니메이션 업데이트
     */
    public void update() {
        if (!isAnimating) return;
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - animationStartTime;
        
        if (elapsed >= animationDuration) {
            // 애니메이션 완료
            isAnimating = false;
            animationProgress = 1.0f;
        } else {
            // 애니메이션 진행률 계산 (0.0 ~ 1.0)
            animationProgress = (float) elapsed / animationDuration;
            
            // 이징 함수 적용 (부드러운 애니메이션)
            animationProgress = easeOutCubic(animationProgress);
        }
    }
    
    /**
     * 이징 함수 - 부드러운 애니메이션을 위한
     */
    private float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3.0f);
    }
    
    /**
     * 애니메이션 중인지 확인
     */
    public boolean isAnimating() {
        return isAnimating;
    }
    
    /**
     * 애니메이션 진행률 반환 (0.0 ~ 1.0)
     */
    public float getProgress() {
        return animationProgress;
    }
    
    /**
     * 애니메이션 타입 반환
     */
    public AnimationType getCurrentType() {
        return currentType;
    }
    
    /**
     * 애니메이션 중지
     */
    public void stopAnimation() {
        isAnimating = false;
        animationProgress = 1.0f;
    }
    
    /**
     * 애니메이션 리셋
     */
    public void reset() {
        isAnimating = false;
        animationProgress = 0.0f;
        animationStartTime = 0;
    }
}
