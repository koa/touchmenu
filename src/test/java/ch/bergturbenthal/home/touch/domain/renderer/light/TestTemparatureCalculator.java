package ch.bergturbenthal.home.touch.domain.renderer.light;

import ch.bergturbenthal.home.touch.domain.light.LightTemperatureCalculator;
import ch.bergturbenthal.home.touch.domain.light.LightTemperatureSettings;
import ch.bergturbenthal.home.touch.domain.light.WarmColdValues;
import org.junit.Test;

public class TestTemparatureCalculator {
  @Test
  public void testWp50() {
    final LightTemperatureSettings temperatureSettings = new LightTemperatureSettings(0.5, 0);
    for (int i = 0; i <= 10; i++) {
      final double targetBrightness = i / 10.0;
      final WarmColdValues values =
          LightTemperatureCalculator.calculateLight(targetBrightness, temperatureSettings);
      System.out.println(targetBrightness + ": " + values);
    }
  }

  @Test
  public void testWp50Wm80() {
    final LightTemperatureSettings temperatureSettings = new LightTemperatureSettings(0.5, 0.2);
    for (int i = 0; i <= 10; i++) {
      final double targetBrightness = i / 10.0;
      final WarmColdValues values =
          LightTemperatureCalculator.calculateLight(targetBrightness, temperatureSettings);
      System.out.println(targetBrightness + ": " + values);
    }
  }

  @Test
  public void testWp50Wc80() {
    final LightTemperatureSettings temperatureSettings = new LightTemperatureSettings(0.5, -0.2);
    for (int i = 0; i <= 10; i++) {
      final double targetBrightness = i / 10.0;
      final WarmColdValues values =
          LightTemperatureCalculator.calculateLight(targetBrightness, temperatureSettings);
      System.out.println(targetBrightness + ": " + values);
    }
  }

  @Test
  public void testWp0() {
    final LightTemperatureSettings temperatureSettings = new LightTemperatureSettings(0, 0);
    for (int i = 0; i <= 10; i++) {
      final double targetBrightness = i / 10.0;
      final WarmColdValues values =
          LightTemperatureCalculator.calculateLight(targetBrightness, temperatureSettings);
      System.out.println(targetBrightness + ": " + values);
    }
  }

  @Test
  public void testNeg() {
    final LightTemperatureSettings temperatureSettings = new LightTemperatureSettings(1, -0.9);
    for (int i = 0; i <= 10; i++) {
      final double targetBrightness = i / 10.0;
      final WarmColdValues values =
          LightTemperatureCalculator.calculateLight(targetBrightness, temperatureSettings);
      System.out.println(targetBrightness + ": " + values);
    }
  }
}
