package javafx.com.yasbu_dekstop.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.com.yasbu_dekstop.models.ProductJson;
import javafx.com.yasbu_dekstop.utils.ApiConnection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class ProductService {

    private final ApiConnection apiConnection = new ApiConnection();
    private final ObjectMapper objectMapper;

    public ProductService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public List<ProductJson> getAllProducts() throws Exception {
        String apiUrl = apiConnection.getApiUrl() + "/products/";
        URL url = new URL(apiUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");

        int responseCode = con.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String json = reader.lines().collect(java.util.stream.Collectors.joining());
                ProductJson[] products = objectMapper.readValue(json, ProductJson[].class);
                return Arrays.asList(products);
            }
        } else {
            throw new RuntimeException("Failed to get products: HTTP error code " + responseCode);
        }
    }

    public ProductJson getProductById(int productId) throws Exception {
        String apiUrl = apiConnection.getApiUrl() + "/products/" + productId;
        URL url = new URL(apiUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");

        int responseCode = con.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String json = reader.lines().collect(java.util.stream.Collectors.joining());
                return objectMapper.readValue(json, ProductJson.class);
            }
        } else {
            throw new RuntimeException("Failed to get product by ID: HTTP error code " + responseCode);
        }
    }
}
