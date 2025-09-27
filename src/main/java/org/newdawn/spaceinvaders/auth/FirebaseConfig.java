package org.newdawn.spaceinvaders.auth;

public final class FirebaseConfig {
    public static final String WEB_API_KEY = "AIzaSyC-F2lzG_xfAiYHxA2-N26Yg6D6isGEKe8";

    // (선택) 추후 DB/Functions 등을 쓸 계획이라면 프로젝트 ID도 보관 가능
    public static final String PROJECT_ID = "spaceinvader-9436d";
    
    // Firebase Realtime Database URL
    public static final String DATABASE_URL = "https://spaceinvader-9436d-default-rtdb.firebaseio.com/";

    private FirebaseConfig() {}
}