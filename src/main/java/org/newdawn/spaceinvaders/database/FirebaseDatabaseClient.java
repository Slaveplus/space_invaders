package org.newdawn.spaceinvaders.database;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Firebase Realtime Database REST API 클라이언트
 */
public class FirebaseDatabaseClient {
    private final String databaseUrl;
    private final Gson gson;
    private String authToken;
    
    public FirebaseDatabaseClient(String databaseUrl) {
        this.databaseUrl = databaseUrl.endsWith("/") ? databaseUrl : databaseUrl + "/";
        this.gson = new Gson();
        this.authToken = null;
    }
    
    public void setAuthToken(String token) {
        this.authToken = token;
    }
    
    /**
     * 데이터 저장 (PUT)
     */
    public boolean putData(String path, Object data) {
        try {
            String url = databaseUrl + path + ".json";
            String jsonData = gson.toJson(data);
            
            System.out.println("Firebase DB PUT 요청:");
            System.out.println("URL: " + url);
            System.out.println("Data: " + jsonData);
            
            HttpURLConnection connection = createConnection(url, "PUT");
            connection.setDoOutput(true);
            
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                writer.write(jsonData);
                writer.flush();
            }
            
            int responseCode = connection.getResponseCode();
            System.out.println("Firebase DB PUT 응답 코드: " + responseCode);
            
            if (responseCode != 200) {
                // 에러 응답 읽기
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.err.println("Firebase DB PUT 에러 응답: " + errorResponse.toString());
                }
            }
            
            return responseCode == 200;
            
        } catch (IOException e) {
            System.err.println("Firebase DB PUT 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 데이터 업데이트 (PUT)
     */
    public boolean updateData(String path, Object data) {
        try {
            String url = databaseUrl + path + ".json";
            String jsonData = gson.toJson(data);
            
            System.out.println("Firebase DB UPDATE 요청:");
            System.out.println("URL: " + url);
            System.out.println("Data: " + jsonData);
            
            HttpURLConnection connection = createConnection(url, "PUT");
            connection.setDoOutput(true);
            
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                writer.write(jsonData);
                writer.flush();
            }
            
            int responseCode = connection.getResponseCode();
            System.out.println("Firebase DB UPDATE 응답 코드: " + responseCode);
            
            if (responseCode != 200) {
                // 에러 응답 읽기
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.err.println("Firebase DB UPDATE 에러 응답: " + errorResponse.toString());
                }
            }
            
            return responseCode == 200;
            
        } catch (IOException e) {
            System.err.println("Firebase DB UPDATE 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 데이터 조회 (GET)
     */
    public <T> T getData(String path, Class<T> responseType) {
        try {
            String url = databaseUrl + path + ".json";
            HttpURLConnection connection = createConnection(url, "GET");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    String responseBody = response.toString();
                    if ("null".equals(responseBody)) {
                        return null;
                    }
                    
                    return gson.fromJson(responseBody, responseType);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Firebase DB GET 오류: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 맵 형태 데이터 조회
     */
    public Map<String, Object> getDataAsMap(String path) {
        try {
            String url = databaseUrl + path + ".json";
            HttpURLConnection connection = createConnection(url, "GET");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    String responseBody = response.toString();
                    if ("null".equals(responseBody)) {
                        return null;
                    }
                    
                    Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                    return gson.fromJson(responseBody, mapType);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Firebase DB GET MAP 오류: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 데이터 삭제 (DELETE)
     */
    public boolean deleteData(String path) {
        try {
            String url = databaseUrl + path + ".json";
            HttpURLConnection connection = createConnection(url, "DELETE");
            
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
            
        } catch (IOException e) {
            System.err.println("Firebase DB DELETE 오류: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 새 데이터 추가 (POST)
     */
    public String postData(String path, Object data) {
        try {
            String url = databaseUrl + path + ".json";
            String jsonData = gson.toJson(data);
            
            HttpURLConnection connection = createConnection(url, "POST");
            connection.setDoOutput(true);
            
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                writer.write(jsonData);
                writer.flush();
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    // Firebase는 POST 시 생성된 키를 반환 {"name": "generated_key"}
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    Map<String, String> result = gson.fromJson(response.toString(), type);
                    return result.get("name");
                }
            }
            
        } catch (IOException e) {
            System.err.println("Firebase DB POST 오류: " + e.getMessage());
        }
        return null;
    }
    
    private HttpURLConnection createConnection(String url, String method) throws IOException {
        // 인증 토큰이 있으면 URL에 추가
        if (authToken != null && !authToken.isEmpty()) {
            url += (url.contains("?") ? "&" : "?") + "auth=" + authToken;
        }
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        return connection;
    }
    
    // ========== 인증 기능 ==========
    
    /**
     * Firebase 인증 예외
     */
    public static class FirebaseAuthException extends Exception {
        public FirebaseAuthException(String message) {
            super(message);
        }
    }
    
    /**
     * 사용자 회원가입
     */
    public UserSession signUp(String email, String password) throws IOException, FirebaseAuthException {
        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);
        payload.addProperty("returnSecureToken", true);

        String response = postAuthJson(FirebaseConfig.AUTH_SIGN_UP_URL, payload.toString());
        return parseAuthResponse(response);
    }

    /**
     * 사용자 로그인
     */
    public UserSession signIn(String email, String password) throws IOException, FirebaseAuthException {
        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);
        payload.addProperty("returnSecureToken", true);

        String response = postAuthJson(FirebaseConfig.AUTH_SIGN_IN_URL, payload.toString());
        return parseAuthResponse(response);
    }
    
    private String postAuthJson(String url, String jsonData) throws IOException, FirebaseAuthException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        
        try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
            out.write(jsonData.getBytes(StandardCharsets.UTF_8));
        }
        
        int responseCode = connection.getResponseCode();
        InputStream inputStream = (responseCode >= 200 && responseCode < 300) 
            ? connection.getInputStream() 
            : connection.getErrorStream();
            
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        if (responseCode >= 400) {
            JsonObject errorObj = gson.fromJson(response.toString(), JsonObject.class);
            if (errorObj != null && errorObj.has("error")) {
                JsonObject error = errorObj.getAsJsonObject("error");
                String message = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                throw new FirebaseAuthException(message);
            }
        }
        
        return response.toString();
    }
    
    private UserSession parseAuthResponse(String json) throws FirebaseAuthException {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        if (obj != null && obj.has("error")) {
            JsonObject error = obj.getAsJsonObject("error");
            String message = error.has("message") ? error.get("message").getAsString() : "Unknown error";
            throw new FirebaseAuthException(message);
        }
        
        String idToken = getAsString(obj, "idToken");
        String refreshToken = getAsString(obj, "refreshToken");
        String localId = getAsString(obj, "localId");
        String email = getAsString(obj, "email");
        long expiresIn = getAsLong(obj, "expiresIn");

        return new UserSession(idToken, refreshToken, localId, email, expiresIn);
    }
    
    private String getAsString(JsonObject obj, String key) {
        return obj != null && obj.has(key) ? obj.get(key).getAsString() : null;
    }
    
    private long getAsLong(JsonObject obj, String key) {
        return obj != null && obj.has(key) ? obj.get(key).getAsLong() : 0L;
    }
}
