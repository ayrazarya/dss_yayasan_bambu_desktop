package javafx.com.yasbu_dekstop.utils;

import java.io.IOException;
import java.net.HttpURLConnection;

public class ApiConnection {
    public enum Environment {
        PRODUCTION,
        DEVELOPMENT
    }
    private static String authToken;
    private static final String PRODUCTION_URL = "https://api.production.com";
    private static final String DEVELOPMENT_URL = "http://localhost:8000";

    private Environment env = Environment.DEVELOPMENT;

    public String getApiUrl() {
        switch (env) {
            case PRODUCTION:
                return PRODUCTION_URL;
            case DEVELOPMENT:
            default:
                return DEVELOPMENT_URL;
        }
    }

    public void setEnvironment(Environment env) {
        this.env = env;
    }

    public static void setAuthToken(String token) {
        authToken = token;
    }

    public static String getAuthToken() {
        return authToken;
    }

    // Gunakan ini untuk request yang butuh auth
    public HttpURLConnection createAuthenticatedConnection(String endpoint) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new java.net.URL(getApiUrl() + endpoint).openConnection();
        con.setRequestProperty("Authorization", "Bearer " + authToken);
        return con;
    }
}

