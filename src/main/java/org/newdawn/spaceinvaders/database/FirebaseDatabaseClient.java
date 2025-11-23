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
    
    // HTTP 관련 상수
    private static final String HTTP_METHOD_POST = "POST";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String AUTH_PARAM = "auth";
    
    // JSON 키 상수
    private static final String JSON_KEY_EMAIL = "email";
    private static final String JSON_KEY_PASSWORD = "password";
    private static final String JSON_KEY_RETURN_SECURE_TOKEN = "returnSecureToken";
    private static final String JSON_KEY_ERROR = "error";
    private static final String JSON_KEY_MESSAGE = "message";
    private static final String JSON_KEY_ID_TOKEN = "idToken";
    private static final String JSON_KEY_REFRESH_TOKEN = "refreshToken";
    private static final String JSON_KEY_LOCAL_ID = "localId";
    private static final String JSON_KEY_EXPIRES_IN = "expiresIn";
    private static final String JSON_KEY_NAME = "name";
    
    // 기타 상수
    private static final String JSON_EXTENSION = ".json";
    private static final String NULL_STRING = "null";
    private static final String UNKNOWN_ERROR = "Unknown error";
    private static final String FIREBASE_DB_PREFIX = "Firebase DB";
    private static final String ERROR_SUFFIX = " 오류";
    private static final String OPERATION_PUT = "PUT";
    private static final String OPERATION_UPDATE = "UPDATE";
    private static final String OPERATION_GET = "GET";
    private static final String OPERATION_POST = "POST";
    private static final String OPERATION_DELETE = "DELETE";

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
        return writeData(path, data, HttpMethod.PUT, OPERATION_PUT);
    }
    
    /**
     * 데이터 업데이트 (PUT)
     */
    public boolean updateData(String path, Object data) {
        return writeData(path, data, HttpMethod.PUT, OPERATION_UPDATE);
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
                logHttpFailure(OPERATION_DELETE, path, response);
            }
            return response.isSuccessful();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> FIREBASE_DB_PREFIX + " " + OPERATION_DELETE + ERROR_SUFFIX);
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
                return result.get(JSON_KEY_NAME);
            }
            logHttpFailure(OPERATION_POST, path, response);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> FIREBASE_DB_PREFIX + " " + OPERATION_POST + ERROR_SUFFIX);
        }
        return null;
    }
    
    private HttpURLConnection createConnection(String url, String method) throws IOException {
        // 인증 토큰이 있으면 URL에 추가
        if (authToken != null && !authToken.isEmpty()) {
            url += (url.contains("?") ? "&" : "?") + AUTH_PARAM + "=" + authToken;
        }
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
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
            LOGGER.log(Level.SEVERE, e, () -> String.format(FIREBASE_DB_PREFIX + " %s" + ERROR_SUFFIX, operation));
            return false;
        }
    }

    private <T> T readData(String path, Type responseType) {
        Objects.requireNonNull(path, "path must not be null");
        try {
            DatabaseResponse response = executeRequest(path, HttpMethod.GET, null);
            if (!response.isSuccessful() || !response.hasBody()) {
                logHttpFailure(OPERATION_GET, path, response);
                return null;
            }
            return gson.fromJson(response.body, responseType);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> FIREBASE_DB_PREFIX + " " + OPERATION_GET + ERROR_SUFFIX);
            return null;
        }
    }

    private DatabaseResponse executeRequest(String path, HttpMethod method, Object payload) throws IOException {
        String url = composeUrl(path);
        LOGGER.log(Level.INFO, () -> String.format(FIREBASE_DB_PREFIX + " %s 요청: %s", method.name(), url));
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
        LOGGER.log(Level.FINE, () -> String.format(FIREBASE_DB_PREFIX + " %s 응답 [%d]: %s", method.name(), responseCode, body));
        return new DatabaseResponse(responseCode, body);
    }

    private String composeUrl(String path) {
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return databaseUrl + normalizedPath + JSON_EXTENSION;
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
            LOGGER.log(Level.WARNING, () -> String.format(FIREBASE_DB_PREFIX + " %s 요청 실패: path=%s, 응답 없음", operation, path));
            return;
        }
        LOGGER.log(Level.WARNING, () -> String.format(
            FIREBASE_DB_PREFIX + " %s 요청 실패: path=%s, code=%d, body=%s",
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
            return body != null && !body.isEmpty() && !NULL_STRING.equals(body);
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
        payload.addProperty(JSON_KEY_EMAIL, email);
        payload.addProperty(JSON_KEY_PASSWORD, password);
        payload.addProperty(JSON_KEY_RETURN_SECURE_TOKEN, true);

        String response = postAuthJson(FirebaseConfig.AUTH_SIGN_UP_URL, payload.toString());
        return parseAuthResponse(response);
    }

    /**
     * 사용자 로그인
     */
    public UserSession signIn(String email, String password) throws IOException, FirebaseAuthException {
        JsonObject payload = new JsonObject();
        payload.addProperty(JSON_KEY_EMAIL, email);
        payload.addProperty(JSON_KEY_PASSWORD, password);
        payload.addProperty(JSON_KEY_RETURN_SECURE_TOKEN, true);

        String response = postAuthJson(FirebaseConfig.AUTH_SIGN_IN_URL, payload.toString());
        return parseAuthResponse(response);
    }
    
    private String postAuthJson(String url, String jsonData) throws IOException, FirebaseAuthException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(HTTP_METHOD_POST);
        connection.setRequestProperty(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
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
            if (errorObj != null && errorObj.has(JSON_KEY_ERROR)) {
                JsonObject error = errorObj.getAsJsonObject(JSON_KEY_ERROR);
                String message = error.has(JSON_KEY_MESSAGE) ? error.get(JSON_KEY_MESSAGE).getAsString() : UNKNOWN_ERROR;
                throw new FirebaseAuthException(message);
            }
        }
        
        return response.toString();
    }
    
    private UserSession parseAuthResponse(String json) throws FirebaseAuthException {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        if (obj != null && obj.has(JSON_KEY_ERROR)) {
            JsonObject error = obj.getAsJsonObject(JSON_KEY_ERROR);
            String message = error.has(JSON_KEY_MESSAGE) ? error.get(JSON_KEY_MESSAGE).getAsString() : UNKNOWN_ERROR;
            throw new FirebaseAuthException(message);
        }
        
        String idToken = getAsString(obj, JSON_KEY_ID_TOKEN);
        String refreshToken = getAsString(obj, JSON_KEY_REFRESH_TOKEN);
        String localId = getAsString(obj, JSON_KEY_LOCAL_ID);
        String email = getAsString(obj, JSON_KEY_EMAIL);
        long expiresIn = getAsLong(obj, JSON_KEY_EXPIRES_IN);

        return new UserSession(idToken, refreshToken, localId, email, expiresIn);
    }
    
    private String getAsString(JsonObject obj, String key) {
        return obj != null && obj.has(key) ? obj.get(key).getAsString() : null;
    }
    
    private long getAsLong(JsonObject obj, String key) {
        return obj != null && obj.has(key) ? obj.get(key).getAsLong() : 0L;
    }
}
