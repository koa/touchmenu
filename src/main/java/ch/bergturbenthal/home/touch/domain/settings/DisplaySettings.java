package ch.bergturbenthal.home.touch.domain.settings;

import lombok.Data;

import java.time.Duration;

@Data
public class DisplaySettings {
  private String name;
  private int width = 64;
  private int height = 128;
  private DisplayOrientation orientation = DisplayOrientation.ROTATE_LEFT;
  private int touchColumnCount = 2;
  private int touchRowCount = 5;
  private Duration screenOffTime = Duration.ofMinutes(1);
  private int bigFontSize=20;

  public enum DisplayOrientation {
    DEFAULT,
    ROTATE_RIGHT,
    ROTATE_LEFT,
    UPSIDE_DOWN
  }
}
