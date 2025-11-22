package org.newdawn.spaceinvaders.database;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Firebase Realtime Database REST API 클라이언트
 */
public class FirebaseDatabaseClient {
    private static final Logger LOGGER = Logger.getLogger(FirebaseDatabaseClient.class.getName());
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();

    private enum HttpMethod {
        GET, POST, PUT, DELETE
    }

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
        return writeData(path, data, HttpMethod.PUT, "PUT");
    }
    
    /**
     * 데이터 업데이트 (PUT)
     */
    public boolean updateData(String path, Object data) {
        return writeData(path, data, HttpMethod.PUT, "UPDATE");
    }
    
    /**
     * 데이터 조회 (GET)
     */
    public <T> T getData(String path, Class<T> responseType) {
        return readData(path, responseType);
    }
    
    /**
     * 맵 형태 데이터 조회
     */
    public Map<String, Object> getDataAsMap(String path) {
        return readData(path, MAP_TYPE);
    }
    
    /**
     * 데이터 삭제 (DELETE)
     */
    public boolean deleteData(String path) {
        try {
            DatabaseResponse response = executeRequest(path, HttpMethod.DELETE, null);
            if (!response.isSuccessful()) {
                logHttpFailure("DELETE", path, response);
            }
            return response.isSuccessful();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Firebase DB DELETE 오류");
            return false;
        }
    }
    
    /**
     * 새 데이터 추가 (POST)
     */
    public String postData(String path, Object data) {
        try {
            DatabaseResponse response = executeRequest(path, HttpMethod.POST, data);
            if (response.isSuccessful() && response.hasBody()) {
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> result = gson.fromJson(response.body, type);
                return result.get("name");
            }
            logHttpFailure("POST", path, response);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Firebase DB POST 오류");
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

    private boolean writeData(String path, Object data, HttpMethod method, String operation) {
        Objects.requireNonNull(path, "path must not be null");
        try {
            DatabaseResponse response = executeRequest(path, method, data);
            if (!response.isSuccessful()) {
                logHttpFailure(operation, path, response);
            }
            return response.isSuccessful();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> String.format("Firebase DB %s 오류", operation));
            return false;
        }
    }

    private <T> T readData(String path, Type responseType) {
        Objects.requireNonNull(path, "path must not be null");
        try {
            DatabaseResponse response = executeRequest(path, HttpMethod.GET, null);
            if (!response.isSuccessful() || !response.hasBody()) {
                logHttpFailure("GET", path, response);
                return null;
            }
            return gson.fromJson(response.body, responseType);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Firebase DB GET 오류");
            return null;
        }
    }

    private DatabaseResponse executeRequest(String path, HttpMethod method, Object payload) throws IOException {
        String url = composeUrl(path);
        LOGGER.log(Level.INFO, () -> String.format("Firebase DB %s 요청: %s", method.name(), url));
        HttpURLConnection connection = createConnection(url, method.name());
        if (payload != null) {
            connection.setDoOutput(true);
            String jsonData = gson.toJson(payload);
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(jsonData);
            }
        }

        int responseCode = connection.getResponseCode();
        InputStream responseStream = responseCode >= 200 && responseCode < 300
            ? connection.getInputStream()
            : connection.getErrorStream();

        String body = responseStream != null ? readStream(responseStream) : "";
        LOGGER.log(Level.FINE, () -> String.format("Firebase DB %s 응답 [%d]: %s", method.name(), responseCode, body));
        return new DatabaseResponse(responseCode, body);
    }

    private String composeUrl(String path) {
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return databaseUrl + normalizedPath + ".json";
    }

    private String readStream(InputStream stream) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private void logHttpFailure(String operation, String path, DatabaseResponse response) {
        if (response == null) {
            LOGGER.log(Level.WARNING, () -> String.format("Firebase DB %s 요청 실패: path=%s, 응답 없음", operation, path));
            return;
        }
        LOGGER.log(Level.WARNING, () -> String.format(
            "Firebase DB %s 요청 실패: path=%s, code=%d, body=%s",
            operation, path, response.code, response.body));
    }

    private static final class DatabaseResponse {
        private final int code;
        private final String body;

        private DatabaseResponse(int code, String body) {
            this.code = code;
            this.body = body;
        }

        private boolean isSuccessful() {
            return code >= 200 && code < 300;
        }

        private boolean hasBody() {
            return body != null && !body.isEmpty() && !"null".equals(body);
        }
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
