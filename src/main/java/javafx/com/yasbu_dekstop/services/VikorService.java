package javafx.com.yasbu_dekstop.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.com.yasbu_dekstop.models.ProductJson;
import javafx.com.yasbu_dekstop.models.VikorJson;
import javafx.com.yasbu_dekstop.models.VikorResponse;
import javafx.com.yasbu_dekstop.utils.ApiConnection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

public class VikorService {

    private final ApiConnection apiConnection = new ApiConnection();
    private final ObjectMapper objectMapper;

    public VikorService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Tambahkan konfigurasi untuk handling null values
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<VikorJson> calculateVikor(List<ProductJson> products) throws Exception {
        String apiUrl = apiConnection.getApiUrl() + "/vikor/calculate";
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (java.io.OutputStream outputStream = connection.getOutputStream()) {
            objectMapper.writeValue(outputStream, products);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed to calculate VIKOR: HTTP " + responseCode);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

            String rawJson = reader.lines().collect(Collectors.joining());
            System.out.println("CALCULATE VIKOR RESPONSE: " + rawJson);

            // Parse sebagai wrapper response
            VikorResponse response = objectMapper.readValue(rawJson, VikorResponse.class);

            List<VikorJson> vikorResults = response.getData();

            if (vikorResults != null) {
                // Assign ranks berdasarkan Q value (ascending - lower is better)
                vikorResults.sort(Comparator.comparingDouble(VikorJson::getQ));

                for (int i = 0; i < vikorResults.size(); i++) {
                    vikorResults.get(i).setRank(i + 1);
                }

                // Debug: Print parsed data
                System.out.println("Parsed VIKOR Results:");
                for (VikorJson vikor : vikorResults) {
                    System.out.println("Rank " + vikor.getRank() + " - Product: " + vikor.getName() +
                            ", Q: " + vikor.getQ() +
                            ", S: " + vikor.getS() +
                            ", R: " + vikor.getR());
                }
            }

            return vikorResults;
        }
    }

    public List<VikorJson> getVikorRankings() throws Exception {
        // Untuk sementara, gunakan response dari calculateVikor saja
        // Atau buat logic khusus untuk handle format yang berbeda

        String apiUrl = apiConnection.getApiUrl() + "/vikor/rankings";
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed to get VIKOR rankings: HTTP error code " + responseCode);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

            String rawJson = reader.lines().collect(Collectors.joining());
            System.out.println("RANKINGS RESPONSE: " + rawJson);

            VikorJson[] rankings = objectMapper.readValue(rawJson, VikorJson[].class);

            // Debug: Print parsed rankings
            System.out.println("Parsed Rankings:");
            for (VikorJson vikor : rankings) {
                vikor.printDebugInfo(); // Gunakan method debug yang baru
                System.out.println("Final values - Q: " + vikor.getQ() +
                        ", S: " + vikor.getS() +
                        ", R: " + vikor.getR());
                System.out.println("---");
            }

            return Arrays.asList(rankings);
        }
    }

    // Method alternatif: hanya gunakan calculateVikor dan simpan hasilnya
    public List<VikorJson> calculateAndGetRankings(List<ProductJson> products) throws Exception {
        // Hanya panggil calculateVikor yang memberikan data lengkap
        return calculateVikor(products);
    }
}