package ch.bergturbenthal.home.touch.domain.light;

public class LightTemperatureCalculator {
  public static WarmColdValues calculateLight(
      double targetBrightness, LightTemperatureSettings settings) {
    final double coldMax;
    final double warmMax;
    final double brightBalance = settings.getBrightBalance();
    if (brightBalance < 0) {
      coldMax = 1;
      warmMax = 1 + brightBalance;
    } else {
      coldMax = 1 - brightBalance;
      warmMax = 1;
    }
    final double warmStart = settings.getWarmStart();
    double maxLight = coldMax + warmMax;
    double targetLight = targetBrightness * maxLight;
    if (warmStart > targetLight) return new WarmColdValues(targetLight, 0);
    double coldValue = (targetLight - warmStart) / ((maxLight - warmStart) / coldMax);
    double warmValue = targetLight - coldValue;
    return new WarmColdValues(warmValue, coldValue);
  }

  public static double calculateWarmstartBrightness(LightTemperatureSettings settings) {
    final double coldMax;
    final double warmMax;
    final double brightBalance = settings.getBrightBalance();
    if (brightBalance < 0) {
      coldMax = 1;
      warmMax = 1 + brightBalance;
    } else {
      coldMax = 1 - brightBalance;
      warmMax = 1;
    }
    final double warmStart = settings.getWarmStart();
    double maxLight = coldMax + warmMax;
    return  warmStart/maxLight;
  }
}
