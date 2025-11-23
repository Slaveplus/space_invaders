package org.newdawn.spaceinvaders.common.util;

/**
 * 폰트 관련 상수 클래스
 * 애플리케이션 전역에서 사용하는 폰트 이름을 중앙에서 관리합니다.
 */
public final class FontConstants {
    /** 기본 폰트 이름 (Arial) */
    public static final String DEFAULT_FONT_NAME = "Arial";
    
    // 인스턴스화 방지
    private FontConstants() {
        throw new AssertionError("FontConstants는 인스턴스화할 수 없습니다.");
    }
}

