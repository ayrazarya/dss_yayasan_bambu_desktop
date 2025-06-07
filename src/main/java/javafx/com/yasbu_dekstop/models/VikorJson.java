package javafx.com.yasbu_dekstop.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VikorJson {

    @JsonProperty("product_id")
    private int productId;

    @JsonProperty("name")
    private String name;

    // Field untuk response dari /vikor/calculate
    @JsonProperty("Q")
    private Double Q;

    @JsonProperty("S")
    private Double S;

    @JsonProperty("R")
    private Double R;

    // Field untuk response dari /vikor/rankings
    @JsonProperty("score")
    private Double score;  // Ini sebenarnya Q value

    @JsonProperty("rank")
    private int rank;

    @JsonProperty("evaluated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime evaluatedAt;

    // Getter methods dengan logic untuk handle kedua format
    public double getQ() {
        // Jika Q ada, gunakan Q. Jika tidak, gunakan score
        if (Q != null) {
            return Q;
        } else if (score != null) {
            return score;
        }
        return 0.0;
    }

    public double getS() {
        return S != null ? S : 0.0;
    }

    public double getR() {
        return R != null ? R : 0.0;
    }

    public String getFormattedQ() {
        double qValue = getQ();
        return String.format("%.6f", qValue);
    }

    public String getFormattedS() {
        return S != null ? String.format("%.6f", S) : "N/A";
    }

    public String getFormattedR() {
        return R != null ? String.format("%.6f", R) : "N/A";
    }

    public String getDisplayName() {
        return name != null && !name.trim().isEmpty() ? name : "Produk " + productId;
    }

    // Method untuk debugging
    public void printDebugInfo() {
        System.out.println("VikorJson Debug:");
        System.out.println("  Product ID: " + productId);
        System.out.println("  Name: " + name);
        System.out.println("  Q: " + Q);
        System.out.println("  S: " + S);
        System.out.println("  R: " + R);
        System.out.println("  Score: " + score);
        System.out.println("  Rank: " + rank);
        System.out.println("  Final Q Value: " + getQ());
    }
}