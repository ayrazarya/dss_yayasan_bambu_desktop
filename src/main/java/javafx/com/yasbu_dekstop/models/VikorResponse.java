package javafx.com.yasbu_dekstop.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class VikorResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private List<VikorJson> data;
}
