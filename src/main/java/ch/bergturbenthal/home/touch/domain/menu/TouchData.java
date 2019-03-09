package ch.bergturbenthal.home.touch.domain.menu;

import lombok.Value;

@Value
public class TouchData {
  private int pressure;
  private int x;
  private int y;
  private int age;
}
