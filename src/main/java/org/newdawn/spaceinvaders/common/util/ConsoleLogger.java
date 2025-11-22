package org.newdawn.spaceinvaders.common.util;

/**
 * 콘솔 출력을 위한 Logger 구현체
 * System.out.println과 System.err.println을 사용
 */
public class ConsoleLogger implements Logger {
    private final String className;
    private final boolean debugEnabled;
    
    public ConsoleLogger(Class<?> clazz) {
        this(clazz, false);
    }
    
    public ConsoleLogger(Class<?> clazz, boolean debugEnabled) {
        this.className = clazz.getSimpleName();
        this.debugEnabled = debugEnabled;
    }
    
    @Override
    public void debug(String message) {
        if (debugEnabled) {
            System.out.println("[DEBUG][" + className + "] " + message);
        }
    }
    
    @Override
    public void info(String message) {
        System.out.println("[INFO][" + className + "] " + message);
    }
    
    @Override
    public void warn(String message) {
        System.out.println("[WARN][" + className + "] " + message);
    }
    
    @Override
    public void error(String message) {
        System.err.println("[ERROR][" + className + "] " + message);
    }
    
    @Override
    public void error(String message, Throwable throwable) {
        System.err.println("[ERROR][" + className + "] " + message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}

