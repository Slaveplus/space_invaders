package org.newdawn.spaceinvaders.database;

/**
 * 사용자 통계 데이터 클래스
 */
public class UserStats {
    private int highScore;
    private int totalGamesPlayed;
    private int totalWins;
    private double winRate;
    private int aliensKilled;
    private int shotsFired;
    private int totalPlayTime;
    private int currentStreak;
    private int longestStreak;
    
    public UserStats() {}
    
    public UserStats(int highScore, int totalGamesPlayed, int totalWins, 
                    int aliensKilled, int shotsFired, int totalPlayTime) {
        this.highScore = highScore;
        this.totalGamesPlayed = totalGamesPlayed;
        this.totalWins = totalWins;
        this.aliensKilled = aliensKilled;
        this.shotsFired = shotsFired;
        this.totalPlayTime = totalPlayTime;
        this.winRate = totalGamesPlayed > 0 ? (double) totalWins / totalGamesPlayed * 100.0 : 0.0;
        this.currentStreak = 0;
        this.longestStreak = 0;
    }
    
    // Getters and Setters
    public int getHighScore() { return highScore; }
    public void setHighScore(int highScore) { this.highScore = highScore; }
    
    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public void setTotalGamesPlayed(int totalGamesPlayed) { 
        this.totalGamesPlayed = totalGamesPlayed;
        updateWinRate();
    }
    
    public int getTotalWins() { return totalWins; }
    public void setTotalWins(int totalWins) { 
        this.totalWins = totalWins;
        updateWinRate();
    }
    
    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }
    
    public int getAliensKilled() { return aliensKilled; }
    public void setAliensKilled(int aliensKilled) { this.aliensKilled = aliensKilled; }
    
    public int getShotsFired() { return shotsFired; }
    public void setShotsFired(int shotsFired) { this.shotsFired = shotsFired; }
    
    public int getTotalPlayTime() { return totalPlayTime; }
    public void setTotalPlayTime(int totalPlayTime) { this.totalPlayTime = totalPlayTime; }
    
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    
    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
    
    private void updateWinRate() {
        this.winRate = totalGamesPlayed > 0 ? (double) totalWins / totalGamesPlayed * 100.0 : 0.0;
    }
}
