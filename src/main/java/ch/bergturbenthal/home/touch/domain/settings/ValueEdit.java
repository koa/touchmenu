package ch.bergturbenthal.home.touch.domain.settings;

import lombok.Data;

@Data
public class ValueEdit {
  private String topic;
  private double increment;
  private String unit;
}
