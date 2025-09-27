package org.newdawn.spaceinvaders.database;

/**
 * Firebase 사용자 세션 정보
 */
public class UserSession {
    private final String idToken;
    private final String refreshToken;
    private final String localId;
    private final String email;
    private final long expiresIn;
    private final long createdAt;
    
    public UserSession(String idToken, String refreshToken, String localId, String email, long expiresIn) {
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.localId = localId;
        this.email = email;
        this.expiresIn = expiresIn;
        this.createdAt = System.currentTimeMillis() / 1000; // 초 단위
    }
    
    public String getIdToken() { return idToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getLocalId() { return localId; }
    public String getEmail() { return email; }
    public long getExpiresIn() { return expiresIn; }
    
    public boolean isExpired() {
        long currentTime = System.currentTimeMillis() / 1000;
        return (createdAt + expiresIn) <= currentTime;
    }
}
