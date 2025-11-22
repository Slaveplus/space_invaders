package org.newdawn.spaceinvaders.common.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Logger 인스턴스를 생성하고 관리하는 팩토리 클래스
 */
public class LoggerFactory {
    private static final Map<Class<?>, Logger> loggers = new HashMap<>();
    private static boolean debugEnabled = false;
    
    /**
     * 특정 클래스에 대한 Logger 인스턴스를 반환
     * 
     * @param clazz Logger를 가져올 클래스
     * @return Logger 인스턴스
     */
    public static Logger getLogger(Class<?> clazz) {
        return loggers.computeIfAbsent(clazz, k -> new ConsoleLogger(k, debugEnabled));
    }
    
    /**
     * 디버그 모드 설정
     * 
     * @param enabled 디버그 모드 활성화 여부
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        // 기존 로거들도 업데이트
        loggers.clear();
    }
    
    /**
     * 모든 로거 초기화
     */
    public static void clear() {
        loggers.clear();
    }
}

