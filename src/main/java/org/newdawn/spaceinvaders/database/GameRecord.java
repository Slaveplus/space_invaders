package org.newdawn.spaceinvaders.database;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 게임 플레이 기록을 저장하는 클래스
 * 싱글플레이 리더보드용 데이터 구조
 */
public class GameRecord {
    private String recordId;
    private String userId;
    private String username;
    private long playTimeMs; // 플레이 시간 (밀리초)
    private String playTime; // 플레이 시간 (분:초 형식)
    private int earnedCoins; // 획득한 코인
    private boolean completed; // 게임 클리어 여부
    private LocalDateTime playDate; // 플레이 날짜
    private int finalRound; // 도달한 라운드
    
    /**
     * 기본 생성자
     */
    public GameRecord() {
        this.playDate = LocalDateTime.now();
    }
    
    /**
     * 생성자
     * 
     * @param userId 사용자 ID
     * @param username 사용자명
     * @param playTimeMs 플레이 시간 (밀리초)
     * @param earnedCoins 획득한 코인
     * @param completed 게임 클리어 여부
     * @param finalRound 도달한 라운드
     */
    public GameRecord(String userId, String username, long playTimeMs, int earnedCoins, boolean completed, int finalRound) {
        this.userId = userId;
        this.username = username;
        this.playTimeMs = playTimeMs;
        this.playTime = formatPlayTime(playTimeMs);
        this.earnedCoins = earnedCoins;
        this.completed = completed;
        this.finalRound = finalRound;
        this.playDate = LocalDateTime.now();
        this.recordId = generateRecordId();
    }
    
    /**
     * 플레이 시간을 분:초 형식으로 변환
     * 
     * @param playTimeMs 플레이 시간 (밀리초)
     * @return 분:초 형식의 문자열
     */
    private String formatPlayTime(long playTimeMs) {
        long totalSeconds = playTimeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * 고유한 기록 ID 생성
     * 
     * @return 기록 ID
     */
    private String generateRecordId() {
        return userId + "_" + playDate.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
    
    // Getters and Setters
    public String getRecordId() {
        return recordId;
    }
    
    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public long getPlayTimeMs() {
        return playTimeMs;
    }
    
    public void setPlayTimeMs(long playTimeMs) {
        this.playTimeMs = playTimeMs;
        this.playTime = formatPlayTime(playTimeMs);
    }
    
    public String getPlayTime() {
        return playTime;
    }
    
    public void setPlayTime(String playTime) {
        this.playTime = playTime;
    }
    
    public int getEarnedCoins() {
        return earnedCoins;
    }
    
    public void setEarnedCoins(int earnedCoins) {
        this.earnedCoins = earnedCoins;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public LocalDateTime getPlayDate() {
        return playDate;
    }
    
    public void setPlayDate(LocalDateTime playDate) {
        this.playDate = playDate;
    }
    
    public int getFinalRound() {
        return finalRound;
    }
    
    public void setFinalRound(int finalRound) {
        this.finalRound = finalRound;
    }
    
    /**
     * 플레이 날짜를 문자열로 반환
     * 
     * @return 날짜 문자열 (yyyy-MM-dd HH:mm:ss)
     */
    public String getPlayDateString() {
        return playDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * 리더보드 표시용 문자열 생성
     * 
     * @return 리더보드 표시용 문자열
     */
    public String toLeaderboardString() {
        String status = completed ? "클리어" : "실패";
        return String.format("%s - %s (%s) - %d코인", 
                           username, playTime, status, earnedCoins);
    }
    
    @Override
    public String toString() {
        return "GameRecord{" +
                "recordId='" + recordId + '\'' +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", playTime='" + playTime + '\'' +
                ", earnedCoins=" + earnedCoins +
                ", completed=" + completed +
                ", finalRound=" + finalRound +
                ", playDate=" + getPlayDateString() +
                '}';
    }
}

