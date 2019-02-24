package ch.bergturbenthal.home.touch.domain.settings;

import lombok.Data;

@Data
public class MenuEntry {
  private String id;
  private String label;
  private String icon;
  private View content;
}
