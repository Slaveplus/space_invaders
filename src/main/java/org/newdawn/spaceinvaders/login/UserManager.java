package org.newdawn.spaceinvaders.login;

import org.newdawn.spaceinvaders.auth.FirebaseAuthClient;
import org.newdawn.spaceinvaders.auth.FirebaseConfig;
import org.newdawn.spaceinvaders.auth.UserSession;
import java.io.IOException;

/**
 * Firebase 연동 사용자 관리 클래스
 * 확장 가능한 사용자 시스템
 */
public class UserManager {
    private FirebaseAuthClient firebaseAuth;
    private User currentUser;
    private UserSession currentSession;
    
    public UserManager() {
        this.firebaseAuth = new FirebaseAuthClient(FirebaseConfig.WEB_API_KEY);
        this.currentUser = null;
        this.currentSession = null;
    }
    
    public boolean registerUser(String email, String password) {
        try {
            UserSession session = firebaseAuth.signUp(email, password);
            this.currentSession = session;
            this.currentUser = new User(session);
            System.out.println("Firebase 회원가입 성공: " + email);
            return true;
        } catch (IOException e) {
            System.err.println("Firebase 회원가입 실패: " + e.getMessage());
            return false;
        } catch (FirebaseAuthClient.FirebaseAuthException e) {
            System.err.println("Firebase 회원가입 오류: " + e.getMessage());
            return false;
        }
    }
    
    public boolean loginUser(String email, String password) {
        try {
            UserSession session = firebaseAuth.signIn(email, password);
            this.currentSession = session;
            this.currentUser = new User(session);
            System.out.println("Firebase 로그인 성공: " + email);
            return true;
        } catch (IOException e) {
            System.err.println("Firebase 로그인 실패: " + e.getMessage());
            return false;
        } catch (FirebaseAuthClient.FirebaseAuthException e) {
            System.err.println("Firebase 로그인 오류: " + e.getMessage());
            return false;
        }
    }
    
    public void logoutUser() {
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
                currentSession = firebaseAuth.refresh(currentSession.getRefreshToken());
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
            // Firebase Firestore에 저장하는 로직 추가 가능
            System.out.println("사용자 통계 업데이트: " + email + " - 점수: " + score + ", 레벨: " + level);
        }
    }
    
    public void deleteUser(String email) {
        // Firebase에서는 사용자 삭제를 위해 Admin SDK가 필요
        // 현재 구현에서는 로그아웃만 수행
        if (currentUser != null && currentUser.getEmail().equals(email)) {
            logoutUser();
            System.out.println("사용자 삭제 요청: " + email);
        }
    }
}