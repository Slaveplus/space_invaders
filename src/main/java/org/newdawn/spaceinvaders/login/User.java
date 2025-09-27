package org.newdawn.spaceinvaders.login;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.newdawn.spaceinvaders.auth.UserSession;

/**
 * 사용자 클래스
 * 확장 가능한 사용자 정보 관리
 */
public class User {
    private String username;
    private String password;
    private String email;
    /** Firebase Authentication UID */
    private String uid;
    private int highScore;
    private int level;
    private int totalGamesPlayed;
    private int totalWins;
    private LocalDateTime lastLogin;
    private LocalDateTime registrationDate;
    private int coins;
    private int gems;
    
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.highScore = 0;
        this.level = 1;
        this.totalGamesPlayed = 0;
        this.totalWins = 0;
        this.lastLogin = LocalDateTime.now();
        this.registrationDate = LocalDateTime.now();
        this.coins = 1000; // 시작 코인
        this.gems = 50; // 시작 젬
    }

    /**
     * Firebase 세션으로부터 사용자 객체 생성
     */
    public User(UserSession session) {
        this.username = (session.getEmail() != null && session.getEmail().contains("@"))
                ? session.getEmail().substring(0, session.getEmail().indexOf('@'))
                : "player";
        this.password = null; // Firebase Auth 사용 시 로컬 비밀번호는 보관하지 않음
        this.email = session.getEmail();
        this.uid = session.getLocalId();
        this.highScore = 0;
        this.level = 1;
        this.totalGamesPlayed = 0;
        this.totalWins = 0;
        this.lastLogin = LocalDateTime.now();
        this.registrationDate = LocalDateTime.now();
        this.coins = 1000;
        this.gems = 50;
    }

    /**
     * Firebase 세션 정보를 현재 유저 객체에 반영 (로그인 직후 호출)
     */
    public void applyFirebaseSession(UserSession session) {
        if (session == null) return;
        this.email = session.getEmail() != null ? session.getEmail() : this.email;
        this.uid = session.getLocalId();
        this.lastLogin = LocalDateTime.now();
    }
    
    public void updateStats(int score, int level) {
        if (score > this.highScore) {
            this.highScore = score;
        }
        if (level > this.level) {
            this.level = level;
        }
        this.totalGamesPlayed++;
        this.lastLogin = LocalDateTime.now();
    }
    
    public void addWin() {
        this.totalWins++;
    }
    
    public void addCoins(int amount) {
        this.coins += amount;
    }
    
    public boolean spendCoins(int amount) {
        if (this.coins >= amount) {
            this.coins -= amount;
            return true;
        }
        return false;
    }
    
    public void addGems(int amount) {
        this.gems += amount;
    }
    
    public boolean spendGems(int amount) {
        if (this.gems >= amount) {
            this.gems -= amount;
            return true;
        }
        return false;
    }
    
    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public int getHighScore() { return highScore; }
    public void setHighScore(int highScore) { this.highScore = highScore; }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public void setTotalGamesPlayed(int totalGamesPlayed) { this.totalGamesPlayed = totalGamesPlayed; }
    
    public int getTotalWins() { return totalWins; }
    public void setTotalWins(int totalWins) { this.totalWins = totalWins; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
    
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    
    public int getGems() { return gems; }
    public void setGems(int gems) { this.gems = gems; }
    
    public double getWinRate() {
        if (totalGamesPlayed == 0) return 0.0;
        return (double) totalWins / totalGamesPlayed * 100.0;
    }
    
    public String getFormattedLastLogin() {
        return lastLogin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
    
    public String getFormattedRegistrationDate() {
        return registrationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    
    @Override
    public String toString() {
        return String.format("User{username='%s', level=%d, highScore=%d, games=%d, wins=%d, winRate=%.1f%%}", 
                username, level, highScore, totalGamesPlayed, totalWins, getWinRate());
    }
}
