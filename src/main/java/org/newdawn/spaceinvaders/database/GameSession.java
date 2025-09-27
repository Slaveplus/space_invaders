package org.newdawn.spaceinvaders.database;

/**
 * 게임 세션 데이터 클래스
 */
public class GameSession {
    private String uid;
    private String startTime;
    private String endTime;
    private int duration;
    private int score;
    private int level;
    private String result;
    private int aliensKilled;
    private int shotsFired;
    private double accuracy;
    private int coinsEarned;
    private int gemsEarned;
    
    public GameSession() {}
    
    public GameSession(String uid, String startTime, String endTime, int duration,
                      int score, int level, String result, int aliensKilled,
                      int shotsFired, int coinsEarned, int gemsEarned) {
        this.uid = uid;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.score = score;
        this.level = level;
        this.result = result;
        this.aliensKilled = aliensKilled;
        this.shotsFired = shotsFired;
        this.accuracy = shotsFired > 0 ? (double) aliensKilled / shotsFired * 100.0 : 0.0;
        this.coinsEarned = coinsEarned;
        this.gemsEarned = gemsEarned;
    }
    
    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    
    public int getAliensKilled() { return aliensKilled; }
    public void setAliensKilled(int aliensKilled) { this.aliensKilled = aliensKilled; }
    
    public int getShotsFired() { return shotsFired; }
    public void setShotsFired(int shotsFired) { this.shotsFired = shotsFired; }
    
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    
    public int getCoinsEarned() { return coinsEarned; }
    public void setCoinsEarned(int coinsEarned) { this.coinsEarned = coinsEarned; }
    
    public int getGemsEarned() { return gemsEarned; }
    public void setGemsEarned(int gemsEarned) { this.gemsEarned = gemsEarned; }
}
