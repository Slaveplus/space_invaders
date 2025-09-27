package org.newdawn.spaceinvaders.auth;

public class UserSession {
    private final String idToken;
    private final String refreshToken;
    private final String localId;
    private final String email;
    private final long expiresAtEpochMs;

    public UserSession(String idToken, String refreshToken, String localId, String email, long expiresInSeconds) {
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.localId = localId;
        this.email = email;

        long now = System.currentTimeMillis();
        // 만료 여유 10초를 빼서 경계상황 방지
        this.expiresAtEpochMs = now + Math.max(0, (expiresInSeconds - 10)) * 1000L;
    }

    public String getIdToken() { return idToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getLocalId() { return localId; }
    public String getEmail() { return email; }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAtEpochMs;
    }
}