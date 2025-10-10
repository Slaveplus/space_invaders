package org.newdawn.spaceinvaders.multyplay.core;

/**
 * 해상도 스케일링을 관리하는 클래스
 * 게임의 기본 해상도(800x600)를 기준으로 균등 스케일링을 적용합니다.
 */
public class ResolutionManager {
    // 기본 게임 해상도 (게임 로직은 이 해상도 기준)
    public static final int BASE_WIDTH = 800;
    public static final int BASE_HEIGHT = 600;
    
    // 현재 화면 해상도
    private int currentWidth = BASE_WIDTH;
    private int currentHeight = BASE_HEIGHT;
    
    // 균등 스케일 팩터 (가로/세로 중 작은 값 사용)
    private double uniformScale = 1.0;
    
    /**
     * 해상도를 설정하고 균등 스케일 팩터를 계산합니다.
     * 
     * @param width 새로운 화면 너비
     * @param height 새로운 화면 높이
     */
    public void setResolution(int width, int height) {
        this.currentWidth = width;
        this.currentHeight = height;
        
        // 균등 스케일링 (가로/세로 중 작은 값 사용하여 비율 유지)
        double scaleX = (double) width / BASE_WIDTH;
        double scaleY = (double) height / BASE_HEIGHT;
        this.uniformScale = Math.min(scaleX, scaleY);
    }
    
    // Getter 메서드들
    public double getScaleX() { 
        return uniformScale; 
    }
    
    public double getScaleY() { 
        return uniformScale; 
    }
    
    public double getUniformScale() { 
        return uniformScale; 
    }
    
    public int getCurrentWidth() { 
        return currentWidth; 
    }
    
    public int getCurrentHeight() { 
        return currentHeight; 
    }
    
    // 좌표 변환 메서드들
    /**
     * X 좌표를 현재 해상도에 맞게 스케일링합니다.
     */
    public int scaleX(int x) { 
        return (int) (x * getScaleX()); 
    }
    
    /**
     * Y 좌표를 현재 해상도에 맞게 스케일링합니다.
     */
    public int scaleY(int y) { 
        return (int) (y * getScaleY()); 
    }
    
    /**
     * 값을 균등 스케일링합니다.
     */
    public int uniformScale(int value) { 
        return (int) (value * uniformScale); 
    }
    
    /**
     * X 좌표를 기본 해상도로 역변환합니다. (마우스 좌표 등에 사용)
     */
    public int unscaleX(int x) { 
        return (int) (x / getScaleX()); 
    }
    
    /**
     * Y 좌표를 기본 해상도로 역변환합니다. (마우스 좌표 등에 사용)
     */
    public int unscaleY(int y) { 
        return (int) (y / getScaleY()); 
    }
    
    /**
     * 폰트 크기를 현재 해상도에 맞게 조정합니다.
     */
    public float scaleFontSize(float originalSize) {
        return (float) (originalSize * uniformScale);
    }
    
    /**
     * 이미지 크기를 현재 해상도에 맞게 조정합니다.
     */
    public int scaleImageSize(int originalSize) {
        return (int) (originalSize * uniformScale);
    }
    
    /**
     * 현재 해상도 정보를 문자열로 반환합니다.
     */
    @Override
    public String toString() {
        return String.format("ResolutionManager[%dx%d, scale=%.2f]", 
                           currentWidth, currentHeight, uniformScale);
    }
}
