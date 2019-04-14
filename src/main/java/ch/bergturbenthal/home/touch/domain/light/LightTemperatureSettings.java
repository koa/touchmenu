package ch.bergturbenthal.home.touch.domain.light;

import lombok.Value;

@Value
public class LightTemperatureSettings {
  private double warmStart;
  private double brightBalance;
}
