package ch.bergturbenthal.home.touch.domain.settings;

import lombok.Data;

@Data
public class Screen {
  private String name;
  private String displaySettings;
  private String topic;
  private View defaultView;
  private View rootMenu;
  private String startEntry = "";
}
