package ch.bergturbenthal.home.touch.domain.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ValueEdit.ValueEditBuilder.class)
public class ValueEdit {
  private String topic;
  private double increment;
  private String unit;

  @JsonPOJOBuilder(withPrefix = "")
  public static class ValueEditBuilder {}
}
