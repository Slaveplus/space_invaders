package org.newdawn.spaceinvaders.common.util;

/**
 * 로깅을 위한 추상화 인터페이스
 * 로깅 정책을 외부에서 제어할 수 있도록 함
 */
public interface Logger {
    /**
     * 디버그 레벨 로그 출력
     * 
     * @param message 로그 메시지
     */
    void debug(String message);
    
    /**
     * 정보 레벨 로그 출력
     * 
     * @param message 로그 메시지
     */
    void info(String message);
    
    /**
     * 경고 레벨 로그 출력
     * 
     * @param message 로그 메시지
     */
    void warn(String message);
    
    /**
     * 에러 레벨 로그 출력
     * 
     * @param message 로그 메시지
     */
    void error(String message);
    
    /**
     * 에러 레벨 로그 출력 (예외 포함)
     * 
     * @param message 로그 메시지
     * @param throwable 예외 객체
     */
    void error(String message, Throwable throwable);
}

