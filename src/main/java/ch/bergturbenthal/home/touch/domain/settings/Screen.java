package ch.bergturbenthal.home.touch.domain.settings;

import lombok.Data;

import java.util.List;

@Data
public class Screen {
  private String name;
  private String displaySettings;
  private String touchTopic;
  private String contentTopic;
  private View defaultView;
  private List<MenuEntry> rootMenu;
  private String startEntry;
}
