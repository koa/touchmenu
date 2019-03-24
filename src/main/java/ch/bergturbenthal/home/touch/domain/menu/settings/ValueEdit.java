package ch.bergturbenthal.home.touch.domain.menu.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ValueEdit.ValueEditBuilder.class)
public class ValueEdit {
  private String topic;
  private String label;
  private double increment;
  private double defaultValue;
  private String format;
  private Type type;

  @JsonPOJOBuilder(withPrefix = "")
  public static class ValueEditBuilder {
    {
      defaultValue = 0;
      type = Type.FLOAT;
      increment = 0.1;
    }
  }
}
