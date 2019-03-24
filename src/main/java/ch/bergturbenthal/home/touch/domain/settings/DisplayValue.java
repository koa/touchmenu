package ch.bergturbenthal.home.touch.domain.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DisplayValue {
  private String label;
  private String topic;
  private String format;
  private Type type;

  public enum Type {
    @JsonProperty("integer")
    INTEGER,
    @JsonProperty("string")
    STRING,
    @JsonProperty("float")
    FLOAT
  }
}
