package ch.bergturbenthal.home.touch.domain.settings;

import lombok.Data;

@Data
public class DisplayValue {
  private String label;
  private String topic;
  private String format;
  private Type type;

  public enum Type {
    INTEGER,
    STRING,
    FLOAT
  }
}
