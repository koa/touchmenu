package ch.bergturbenthal.home.touch.domain.menu.settings;

import lombok.Data;

@Data
public class DisplayValue {
  private String label;
  private String topic;
  private String format;
  private Type type;

}
