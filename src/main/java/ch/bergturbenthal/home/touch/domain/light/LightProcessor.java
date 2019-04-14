package ch.bergturbenthal.home.touch.domain.light;

import ch.bergturbenthal.home.touch.domain.mqtt.MqttClient;
import ch.bergturbenthal.home.touch.domain.util.DisposableConsumer;
import ch.bergturbenthal.home.touch.domain.util.MqttMessageUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import javax.annotation.PostConstruct;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Service
public class LightProcessor {
  private final MqttClient mqttClient;
  private final ScheduledExecutorService scheduledExecutorService;

  public LightProcessor(
      final MqttClient mqttClient, final ScheduledExecutorService scheduledExecutorService) {
    this.mqttClient = mqttClient;
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @PostConstruct
  public void initLightConfiguration() {

    final Consumer<Disposable> disposableConsumer = new DisposableConsumer();
    mqttClient.registerTopic("light/dimmer", receivedMqttMessage -> {}, disposableConsumer);
    registerDimmer("Licht/Demo/Demo", "DMXBricklet/EJy/channel/1", "DMXBricklet/EJy/channel/0");
  }

  public Disposable registerDimmer(String topicBase, String warmOutput, String coldOutput) {
    AtomicReference<Double> currentWarmStart = new AtomicReference<>(0d);
    AtomicReference<Double> currentBrightBalance = new AtomicReference<>(0d);
    AtomicReference<Double> currentDimmValue = new AtomicReference<>(0d);
    Supplier<LightTemperatureSettings> settingsSupplier =
        () -> new LightTemperatureSettings(currentWarmStart.get(), currentBrightBalance.get());
    Consumer<Double> dimmerConsumer =
        value -> {
          final LightTemperatureSettings lightTemperatureSettings = settingsSupplier.get();
          final WarmColdValues warmColdValues =
              LightTemperatureCalculator.calculateLight(value, lightTemperatureSettings);
          mqttClient.send(
              warmOutput,
              MqttMessageUtil.createMessage(Double.toString(warmColdValues.getWarmPower()), true));
          mqttClient.send(
              coldOutput,
              MqttMessageUtil.createMessage(Double.toString(warmColdValues.getColdPower()), true));
        };
    Runnable refresh =
        () -> {
          dimmerConsumer.accept(currentDimmValue.get());
        };
    final DisposableConsumer warmStartDisposable = new DisposableConsumer();
    Consumer<ScheduledFuture<?>> pendingFutureConsumer =
        new Consumer<ScheduledFuture<?>>() {
          private AtomicReference<ScheduledFuture<?>> pendingFuture = new AtomicReference<>();

          @Override
          public void accept(final ScheduledFuture<?> scheduledFuture) {
            final ScheduledFuture<?> lastFuture = pendingFuture.getAndSet(scheduledFuture);
            if (lastFuture != null && !lastFuture.isDone()) lastFuture.cancel(false);
          }
        };
    mqttClient.registerTopic(
        topicBase + "/warmStart",
        receivedMqttMessage -> {
          final MqttMessage message = receivedMqttMessage.getMessage();
          currentWarmStart.set(parseDoubleValue(message, 0d));
          if (message.isRetained()) refresh.run();
          else {
            dimmerConsumer.accept(
                LightTemperatureCalculator.calculateWarmstartBrightness(settingsSupplier.get()));
            pendingFutureConsumer.accept(
                scheduledExecutorService.schedule(refresh, 2, TimeUnit.SECONDS));
          }
        },
        warmStartDisposable);
    final DisposableConsumer brightBalanceDisposableConsumer = new DisposableConsumer();
    mqttClient.registerTopic(
        topicBase + "/brightBalance",
        receivedMqttMessage -> {
          final MqttMessage message = receivedMqttMessage.getMessage();
          currentBrightBalance.set(parseDoubleValue(message, -1d));
          if (message.isRetained()) refresh.run();
          else {
            dimmerConsumer.accept(1d);
            pendingFutureConsumer.accept(
                scheduledExecutorService.schedule(refresh, 2, TimeUnit.SECONDS));
          }
        },
        brightBalanceDisposableConsumer);
    final DisposableConsumer valueDisposableConsumer = new DisposableConsumer();
    mqttClient.registerTopic(
        topicBase + "/value",
        receivedMqttMessage -> {
          final MqttMessage message = receivedMqttMessage.getMessage();
          currentDimmValue.set(parseDoubleValue(message, 0d));
          refresh.run();
        },
        valueDisposableConsumer);
    return () -> {
      warmStartDisposable.accept(null);
      brightBalanceDisposableConsumer.accept(null);
      valueDisposableConsumer.accept(null);
      pendingFutureConsumer.accept(null);
    };
  }

  protected double parseDoubleValue(final MqttMessage message, final double lowerBound) {
    return Math.max(
        lowerBound, Math.min(1.0, Double.parseDouble(new String(message.getPayload()))));
  }
}
