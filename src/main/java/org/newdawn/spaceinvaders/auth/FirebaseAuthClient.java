package org.newdawn.spaceinvaders.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FirebaseAuthClient {

    private static final String SIGN_UP_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=%s";
    private static final String SIGN_IN_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=%s";
    private static final String TOKEN_REFRESH_URL =
            "https://securetoken.googleapis.com/v1/token?key=%s";

    private final String webApiKey;
    private final Gson gson = new Gson();

    public FirebaseAuthClient(String webApiKey) {
        if (webApiKey == null || webApiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Firebase Web API Key must not be empty.");
        }
        this.webApiKey = webApiKey;
    }

    public UserSession signUp(String email, String password) throws IOException, FirebaseAuthException {
        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);
        payload.addProperty("returnSecureToken", true);

        String endpoint = String.format(SIGN_UP_URL, webApiKey);
        String response = postJson(endpoint, payload.toString());
        return parseAuthResponse(response);
    }

    public UserSession signIn(String email, String password) throws IOException, FirebaseAuthException {
        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);
        payload.addProperty("returnSecureToken", true);

        String endpoint = String.format(SIGN_IN_URL, webApiKey);
        String response = postJson(endpoint, payload.toString());
        return parseAuthResponse(response);
    }

    public UserSession refresh(String refreshToken) throws IOException, FirebaseAuthException {
        String endpoint = String.format(TOKEN_REFRESH_URL, webApiKey);
        String body = "grant_type=refresh_token&refresh_token=" + urlEncode(refreshToken);
        String response = postForm(endpoint, body);
        return parseRefreshResponse(response);
    }

    private UserSession parseAuthResponse(String json) throws FirebaseAuthException {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        if (obj != null && obj.has("error")) {
            throw errorFrom(obj.getAsJsonObject("error"));
        }
        String idToken = getAsString(obj, "idToken");
        String refreshToken = getAsString(obj, "refreshToken");
        String localId = getAsString(obj, "localId");
        String email = getAsString(obj, "email");
        long expiresIn = getAsLong(obj, "expiresIn");

        return new UserSession(idToken, refreshToken, localId, email, expiresIn);
    }

    private UserSession parseRefreshResponse(String json) throws FirebaseAuthException {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        if (obj != null && obj.has("error")) {
            throw errorFrom(obj.getAsJsonObject("error"));
        }
        String idToken = getAsString(obj, "id_token");
        String refreshToken = getAsString(obj, "refresh_token");
        String userId = getAsString(obj, "user_id");
        long expiresIn = getAsLong(obj, "expires_in");
        // refresh 응답엔 email이 없음
        return new UserSession(idToken, refreshToken, userId, null, expiresIn);
    }

    private String postJson(String endpoint, String json) throws IOException, FirebaseAuthException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection con = (HttpURLConnection) new URL(endpoint).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setDoOutput(true);
        con.setConnectTimeout(10000);
        con.setReadTimeout(15000);

        try (DataOutputStream out = new DataOutputStream(con.getOutputStream())) {
            out.write(bytes);
        }
        int code = con.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
        String resp = readFully(is);
        if (code < 200 || code >= 300) {
            JsonObject obj = gson.fromJson(resp, JsonObject.class);
            if (obj != null && obj.has("error")) {
                throw errorFrom(obj.getAsJsonObject("error"));
            }
            throw new IOException("HTTP " + code + " " + con.getResponseMessage());
        }
        return resp;
    }

    private String postForm(String endpoint, String formBody) throws IOException, FirebaseAuthException {
        byte[] bytes = formBody.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection con = (HttpURLConnection) new URL(endpoint).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        con.setDoOutput(true);
        con.setConnectTimeout(10000);
        con.setReadTimeout(15000);

        try (DataOutputStream out = new DataOutputStream(con.getOutputStream())) {
            out.write(bytes);
        }
        int code = con.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
        String resp = readFully(is);
        if (code < 200 || code >= 300) {
            JsonObject obj = gson.fromJson(resp, JsonObject.class);
            if (obj != null && obj.has("error")) {
                throw errorFrom(obj.getAsJsonObject("error"));
            }
            throw new IOException("HTTP " + code + " " + con.getResponseMessage());
        }
        return resp;
    }

    private static String readFully(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static FirebaseAuthException errorFrom(JsonObject errorObj) {
        String message = "Unknown error";
        int code = 400;
        try {
            if (errorObj.has("message")) {
                message = errorObj.get("message").getAsString();
            } else if (errorObj.has("errors")) {
                message = errorObj.get("errors").toString();
            }
            if (errorObj.has("code")) code = errorObj.get("code").getAsInt();
        } catch (Exception ignored) {}
        return new FirebaseAuthException(code, message);
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private static String getAsString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private static long getAsLong(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return 0L;
        String v = obj.get(key).getAsString();
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            try {
                return obj.get(key).getAsLong();
            } catch (Exception ex) {
                return 0L;
            }
        }
    }

    // Firebase Auth 오류를 전달하기 위한 간단한 checked 예외
    public static class FirebaseAuthException extends Exception {
        private final int httpCode;

        public FirebaseAuthException(int httpCode, String message) {
            super(message);
            this.httpCode = httpCode;
        }

        public int getHttpCode() {
            return httpCode;
        }
    }
}