package javafx.com.yasbu_dekstop.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.com.yasbu_dekstop.models.UserJson;
import javafx.com.yasbu_dekstop.utils.ApiConnection;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class UserService {
    private final ApiConnection apiConnection = new ApiConnection();
    private final ObjectMapper objectMapper;

    public UserService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    public UserJson login(String email, String password) throws IOException {
        String apiUrl = apiConnection.getApiUrl() + "/users/login";
        String jsonInputString = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);

        System.out.println("Sending login request to: " + apiUrl); // Debug log
        System.out.println("Request body: " + jsonInputString);    // Debug log

        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        // Tambahkan header CORS jika diperlukan
        con.setRequestProperty("Origin", "http://localhost");

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = con.getResponseCode();
        String responseBody;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        code == 200 ? con.getInputStream() : con.getErrorStream(),
                        StandardCharsets.UTF_8))) {
            responseBody = br.lines().collect(Collectors.joining());
        }

        System.out.println("API Response (" + code + "): " + responseBody); // Debug log

        if (code == 200) {
            return objectMapper.readValue(responseBody, UserJson.class);
        } else {
            throw new IOException("Login failed (" + code + "): " + responseBody);
        }
    }

    public UserJson getUser(int userId) throws IOException {
        String apiUrl = apiConnection.getApiUrl() + "/users/" + userId;

        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");

        int code = con.getResponseCode();
        if (code == 200) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                return objectMapper.readValue(response.toString(), UserJson.class);
            }
        } else {
            throw new IOException("Get User failed with HTTP code: " + code);
        }
    }
}
