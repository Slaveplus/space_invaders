package org.newdawn.spaceinvaders.database;

/**
 * Firebase 설정 클래스
 */
public class FirebaseConfig {
    // Firebase 프로젝트 설정
    public static final String WEB_API_KEY = "AIzaSyC-F2lzG_xfAiYHxA2-N26Yg6D6isGEKe8";
    public static final String PROJECT_ID = "spaceinvader-9436d";
    public static final String DATABASE_URL = "https://spaceinvader-9436d-default-rtdb.firebaseio.com/";
    
    // 인증 관련 URL
    public static final String AUTH_SIGN_UP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + WEB_API_KEY;
    public static final String AUTH_SIGN_IN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + WEB_API_KEY;
    public static final String AUTH_REFRESH_URL = "https://securetoken.googleapis.com/v1/token?key=" + WEB_API_KEY;
}