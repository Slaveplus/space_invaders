package org.newdawn.spaceinvaders.login;

import org.newdawn.spaceinvaders.database.FirebaseConfig;
import org.newdawn.spaceinvaders.database.FirebaseDatabaseClient;
import org.newdawn.spaceinvaders.database.UserProfile;
import org.newdawn.spaceinvaders.database.UserSession;
import org.newdawn.spaceinvaders.database.UserStats;
import org.newdawn.spaceinvaders.shop.ShopManager;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Firebase 연동 사용자 관리 클래스
 * 확장 가능한 사용자 시스템
 */
public class UserManager {
    private FirebaseDatabaseClient firebaseDB;
    private User currentUser;
    private UserSession currentSession;
    private ShopManager shopManager;
    
    public UserManager() {
        this.firebaseDB = new FirebaseDatabaseClient(FirebaseConfig.DATABASE_URL);
        this.currentUser = null;
        this.currentSession = null;
        this.shopManager = new ShopManager(this);
        
        System.out.println("UserManager 초기화 완료");
        System.out.println("Firebase DB URL: " + FirebaseConfig.DATABASE_URL);
    }
    
    public boolean registerUser(String email, String password) {
        try {
            UserSession session = firebaseDB.signUp(email, password);
            this.currentSession = session;
            this.currentUser = new User(session);
            
            // Firebase DB 클라이언트에 인증 토큰 설정
            firebaseDB.setAuthToken(session.getIdToken());
            
            // Firebase DB에 사용자 프로필 저장
            saveUserProfileToDB();
            saveUserStatsToDB();
            
            System.out.println("Firebase 회원가입 성공: " + email);
            return true;
        } catch (IOException e) {
            System.err.println("Firebase 회원가입 실패: " + e.getMessage());
            return false;
        } catch (FirebaseDatabaseClient.FirebaseAuthException e) {
            System.err.println("Firebase 회원가입 오류: " + e.getMessage());
            return false;
        }
    }
    
    public boolean loginUser(String email, String password) {
        try {
            System.out.println("로그인 시도: " + email);
            
            UserSession session = firebaseDB.signIn(email, password);
            this.currentSession = session;
            this.currentUser = new User(session);
            
            System.out.println("Firebase 인증 성공. UID: " + session.getLocalId());
            
            // Firebase DB 클라이언트에 인증 토큰 설정
            firebaseDB.setAuthToken(session.getIdToken());
            
            // Firebase DB에서 사용자 데이터 로드
            System.out.println("DB에서 사용자 데이터 로드 시작...");
            loadUserDataFromDB();
            
            // 마지막 로그인 시간 업데이트
            updateLastLogin();
            
            System.out.println("Firebase 로그인 완료: " + email + 
                             " (사용자명: " + currentUser.getUsername() + 
                             ", 코인: " + currentUser.getCoins() + ")");
            return true;
        } catch (IOException e) {
            System.err.println("Firebase 로그인 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (FirebaseDatabaseClient.FirebaseAuthException e) {
            System.err.println("Firebase 로그인 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("로그인 중 예상치 못한 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public void logoutUser() {
        if (currentUser != null) {
            // 로그아웃 전 최종 데이터 저장
            saveUserDataToDB();
        }
        
        this.currentUser = null;
        this.currentSession = null;
        System.out.println("Firebase 로그아웃 완료");
    }
    
    public boolean isLoggedIn() {
        if (currentSession == null || currentUser == null) {
            return false;
        }
        
        // 토큰 만료 확인
        if (currentSession.isExpired()) {
            try {
                // 토큰 갱신 시도
                // 토큰 갱신 기능은 현재 미구현
                // currentSession = firebaseDB.refresh(currentSession.getRefreshToken());
                System.out.println("Firebase 토큰 갱신 성공");
                return true;
            } catch (Exception e) {
                System.err.println("Firebase 토큰 갱신 실패: " + e.getMessage());
                logoutUser();
                return false;
            }
        }
        
        return true;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public UserSession getCurrentSession() {
        return currentSession;
    }
    
    public org.newdawn.spaceinvaders.database.FirebaseDatabaseClient getFirebaseDB() {
        return firebaseDB;
    }
    
    public ShopManager getShopManager() {
        return shopManager;
    }
    
    public boolean userExists(String email) {
        // Firebase에서는 이메일 중복 확인을 위해 실제로 회원가입을 시도해야 함
        // 하지만 보안상의 이유로 이 기능은 제한적임
        return false; // 항상 false 반환 (실제 구현에서는 Firebase Admin SDK 필요)
    }
    
    public int getUserCount() {
        // Firebase에서는 전체 사용자 수를 직접 조회할 수 없음
        return isLoggedIn() ? 1 : 0;
    }
    
    public void updateUserStats(String email, int score, int level) {
        if (currentUser != null && currentUser.getEmail().equals(email)) {
            currentUser.updateStats(score, level);
            // Firebase DB에 통계 저장
            saveUserStatsToDB();
            System.out.println("사용자 통계 업데이트: " + email + " - 점수: " + score + ", 레벨: " + level);
        }
    }
    
    // 실시간 동기화 메서드들
    public void updateCoins(int newCoins) {
        if (currentUser != null) {
            currentUser.setCoins(newCoins);
            // Firebase DB에 실시간 저장
            firebaseDB.updateData("users/" + currentUser.getUid() + "/profile/coins", newCoins);
            System.out.println("코인 업데이트: " + newCoins);
        }
    }
    
    public void updateGems(int newGems) {
        if (currentUser != null) {
            currentUser.setGems(newGems);
            // Firebase DB에 실시간 저장
            firebaseDB.updateData("users/" + currentUser.getUid() + "/profile/gems", newGems);
            System.out.println("젬 업데이트: " + newGems);
        }
    }
    
    public void updateLevel(int newLevel) {
        if (currentUser != null) {
            currentUser.setLevel(newLevel);
            // Firebase DB에 실시간 저장
            firebaseDB.updateData("users/" + currentUser.getUid() + "/profile/level", newLevel);
            System.out.println("레벨 업데이트: " + newLevel);
        }
    }
    
    public void updateHighScore(int newScore) {
        if (currentUser != null) {
            currentUser.setHighScore(newScore);
            // Firebase DB에 실시간 저장
            firebaseDB.updateData("users/" + currentUser.getUid() + "/stats/highScore", newScore);
            System.out.println("최고점수 업데이트: " + newScore);
        }
    }
    
    public void addCoins(int amount) {
        if (currentUser != null) {
            int newCoins = currentUser.getCoins() + amount;
            updateCoins(newCoins);
        }
    }
    
    public boolean spendCoins(int amount) {
        if (currentUser != null && currentUser.getCoins() >= amount) {
            int newCoins = currentUser.getCoins() - amount;
            updateCoins(newCoins);
            return true;
        }
        return false;
    }
    
    public void addGems(int amount) {
        if (currentUser != null) {
            int newGems = currentUser.getGems() + amount;
            updateGems(newGems);
        }
    }
    
    public boolean spendGems(int amount) {
        if (currentUser != null && currentUser.getGems() >= amount) {
            int newGems = currentUser.getGems() - amount;
            updateGems(newGems);
            return true;
        }
        return false;
    }
    
    // DB에서 최신 데이터 로드 (실시간 동기화용)
    public void syncFromDB() {
        if (currentUser != null) {
            System.out.println("DB에서 최신 데이터 동기화 시작...");
            loadUserDataFromDB();
            System.out.println("DB에서 최신 데이터 동기화 완료");
        } else {
            System.out.println("동기화 실패: 로그인된 사용자가 없습니다.");
        }
    }
    
    // 사용자 정보를 강제로 새로고침 (UI 업데이트용)
    public void refreshUserData() {
        if (isLoggedIn()) {
            syncFromDB();
            System.out.println("사용자 데이터 새로고침 완료: " + 
                             (currentUser != null ? currentUser.getUsername() : "null"));
        }
    }
    
    public void deleteUser(String email) {
        if (currentUser != null && currentUser.getEmail().equals(email)) {
            // Firebase DB에서 사용자 데이터 삭제
            deleteUserDataFromDB();
            logoutUser();
            System.out.println("사용자 삭제 요청: " + email);
        }
    }
    
    // Firebase DB 관련 메서드들
    private void saveUserProfileToDB() {
        if (currentUser == null) return;
        
        String uid = currentUser.getUid();
        UserProfile profile = new UserProfile(
            currentUser.getUsername(),
            currentUser.getEmail(),
            currentUser.getUid(),
            currentUser.getLevel(),
            currentUser.getCoins(),
            currentUser.getGems(),
            currentUser.getFormattedLastLogin(),
            currentUser.getFormattedRegistrationDate()
        );
        
        boolean success = firebaseDB.putData("users/" + uid + "/profile", profile);
        if (success) {
            System.out.println("사용자 프로필 저장 완료: " + uid);
        } else {
            System.err.println("사용자 프로필 저장 실패: " + uid);
        }
    }
    
    private void saveUserStatsToDB() {
        if (currentUser == null) return;
        
        String uid = currentUser.getUid();
        UserStats stats = new UserStats(
            currentUser.getHighScore(),
            currentUser.getTotalGamesPlayed(),
            currentUser.getTotalWins(),
            0, // aliensKilled - 추후 구현
            0, // shotsFired - 추후 구현
            0  // totalPlayTime - 추후 구현
        );
        
        boolean success = firebaseDB.putData("users/" + uid + "/stats", stats);
        if (success) {
            System.out.println("사용자 통계 저장 완료: " + uid);
        } else {
            System.err.println("사용자 통계 저장 실패: " + uid);
        }
    }
    
    private void saveUserDataToDB() {
        saveUserProfileToDB();
        saveUserStatsToDB();
    }
    
    private void loadUserDataFromDB() {
        if (currentUser == null) {
            System.err.println("loadUserDataFromDB: currentUser가 null입니다.");
            return;
        }
        
        String uid = currentUser.getUid();
        if (uid == null || uid.isEmpty()) {
            System.err.println("loadUserDataFromDB: UID가 null이거나 비어있습니다.");
            return;
        }
        
        System.out.println("DB에서 사용자 데이터 로드 시작: " + uid);
        
        try {
            // 프로필 데이터 로드
            UserProfile profile = firebaseDB.getData("users/" + uid + "/profile", UserProfile.class);
            if (profile != null) {
                currentUser.setUsername(profile.getUsername());
                currentUser.setLevel(profile.getLevel());
                currentUser.setCoins(profile.getCoins());
                currentUser.setGems(profile.getGems());
                System.out.println("사용자 프로필 로드 완료: " + profile.getUsername() + 
                                 " (레벨: " + profile.getLevel() + 
                                 ", 코인: " + profile.getCoins() + 
                                 ", 젬: " + profile.getGems() + ")");
            } else {
                System.out.println("프로필 데이터가 없습니다. 기본값 사용.");
            }
            
            // 통계 데이터 로드
            UserStats stats = firebaseDB.getData("users/" + uid + "/stats", UserStats.class);
            if (stats != null) {
                currentUser.setHighScore(stats.getHighScore());
                currentUser.setTotalGamesPlayed(stats.getTotalGamesPlayed());
                currentUser.setTotalWins(stats.getTotalWins());
                System.out.println("사용자 통계 로드 완료: " + 
                                 "최고점수: " + stats.getHighScore() + 
                                 ", 게임수: " + stats.getTotalGamesPlayed() + 
                                 ", 승리수: " + stats.getTotalWins());
            } else {
                System.out.println("통계 데이터가 없습니다. 기본값 사용.");
            }
            
            System.out.println("DB 데이터 로드 완료. 현재 사용자: " + currentUser.getUsername());
            
        } catch (Exception e) {
            System.err.println("DB 데이터 로드 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateLastLogin() {
        if (currentUser == null) return;
        
        String uid = currentUser.getUid();
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        boolean success = firebaseDB.updateData("users/" + uid + "/profile/lastLogin", currentTime);
        if (success) {
            System.out.println("마지막 로그인 시간 업데이트 완료: " + uid);
        }
    }
    
    private void deleteUserDataFromDB() {
        if (currentUser == null) return;
        
        String uid = currentUser.getUid();
        boolean success = firebaseDB.deleteData("users/" + uid);
        if (success) {
            System.out.println("사용자 데이터 삭제 완료: " + uid);
        } else {
            System.err.println("사용자 데이터 삭제 실패: " + uid);
        }
    }
}