package org.newdawn.spaceinvaders.database;

/**
 * 사용자 프로필 데이터 클래스
 */
public class UserProfile {
    private String username;
    private String email;
    private String uid;
    private int level;
    private int coins;
    private int gems;
    private String lastLogin;
    private String registrationDate;
    
    public UserProfile() {}
    
    public UserProfile(String username, String email, String uid, int level, 
                      int coins, int gems, String lastLogin, String registrationDate) {
        this.username = username;
        this.email = email;
        this.uid = uid;
        this.level = level;
        this.coins = coins;
        this.gems = gems;
        this.lastLogin = lastLogin;
        this.registrationDate = registrationDate;
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    
    public int getGems() { return gems; }
    public void setGems(int gems) { this.gems = gems; }
    
    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
    
    public String getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(String registrationDate) { this.registrationDate = registrationDate; }
}
