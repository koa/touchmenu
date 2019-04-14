package ch.bergturbenthal.home.touch.domain.motiondetection;

import ch.bergturbenthal.home.touch.domain.mqtt.MqttClient;
import ch.bergturbenthal.home.touch.domain.util.DisposableConsumer;
import ch.bergturbenthal.home.touch.domain.util.MqttMessageUtil;
import ch.bergturbenthal.home.touch.domain.util.ScheduledFutureConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
@Service
public class MotionDetectionProcessor {
  private static final long SECONDS_TO_NANOS = TimeUnit.SECONDS.toNanos(1);
  private final MqttClient mqttClient;
  private final ScheduledExecutorService scheduledExecutorService;

  public MotionDetectionProcessor(
      final MqttClient mqttClient, final ScheduledExecutorService scheduledExecutorService) {
    this.mqttClient = mqttClient;
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @PostConstruct
  public void initListeners() {
    registerMotionDetector(
        "demo/motionDetector", "BrickletMotionDetectorV2/DVn/motion", "Licht/Demo/Demo/value");
  }

  public Disposable registerMotionDetector(
      String topicBase, String inputTopic, String outputTopic) {
    final AtomicReference<String> onValueReference = new AtomicReference<>("1");
    final AtomicReference<String> offValueReference = new AtomicReference<>("0");
    final AtomicReference<Duration> keepOnDuration = new AtomicReference<>(Duration.ofSeconds(5));
    final AtomicReference<Duration> blindDuration = new AtomicReference<>(Duration.ofMillis(100));
    final AtomicReference<MotionDetectorState> currentStateReference =
        new AtomicReference<>(MotionDetectorState.OFF);
    final Runnable refresh =
        () -> {
          final String currentStateValue;
          MotionDetectorState motionDetectorState = currentStateReference.get();
          if (motionDetectorState == MotionDetectorState.OFF
              || motionDetectorState == MotionDetectorState.BLIND) {
            currentStateValue = offValueReference.get();
          } else if (motionDetectorState == MotionDetectorState.ON) {
            currentStateValue = onValueReference.get();
          } else {
            return;
          }
          mqttClient.send(outputTopic, MqttMessageUtil.createMessage(currentStateValue, true));
        };
    final Consumer<ScheduledFuture<?>> pendingFutureConsumer = new ScheduledFutureConsumer();
    AtomicReference<byte[]> lastInputMessage = new AtomicReference<>(new byte[0]);
    final DisposableConsumer inputDisposable = new DisposableConsumer();
    mqttClient.registerTopic(
        inputTopic,
        receivedMqttMessage -> {
          final byte[] payload = receivedMqttMessage.getMessage().getPayload();
          final byte[] lastMessage = lastInputMessage.getAndSet(payload);
          if (Arrays.equals(payload, lastMessage)) return;
          if (currentStateReference.get() == MotionDetectorState.BLIND) return;
          currentStateReference.set(MotionDetectorState.ON);
          pendingFutureConsumer.accept(
              scheduledExecutorService.schedule(
                  () -> {
                    currentStateReference.set(MotionDetectorState.BLIND);
                    pendingFutureConsumer.accept(
                        scheduledExecutorService.schedule(
                            () -> {
                              currentStateReference.set(MotionDetectorState.OFF);
                              refresh.run();
                            },
                            blindDuration.get().toMillis(),
                            TimeUnit.MILLISECONDS));
                    refresh.run();
                  },
                  keepOnDuration.get().toMillis(),
                  TimeUnit.MILLISECONDS));
          refresh.run();
        },
        inputDisposable);
    final Consumer<Disposable> onValueDisposableConsumer = new DisposableConsumer();
    mqttClient.registerTopic(
        topicBase + "/onValue",
        receivedMqttMessage -> {
          onValueReference.set(new String(receivedMqttMessage.getMessage().getPayload()));
          refresh.run();
        },
        onValueDisposableConsumer);
    final Consumer<Disposable> offValueDisposableConsumer = new DisposableConsumer();
    mqttClient.registerTopic(
        topicBase + "/offValue",
        receivedMqttMessage -> {
          offValueReference.set(new String(receivedMqttMessage.getMessage().getPayload()));
          refresh.run();
        },
        offValueDisposableConsumer);
    final Consumer<Disposable> onTimeDisposableConsumer = new DisposableConsumer();
    mqttClient.registerTopic(
        topicBase + "/onTime",
        receivedMqttMessage ->
            keepOnDuration.set(
                secondsTuDuration(new String(receivedMqttMessage.getMessage().getPayload()))),
        onTimeDisposableConsumer);
    final Consumer<Disposable> offTimeDisposableConsumer = new DisposableConsumer();
    mqttClient.registerTopic(
        topicBase + "/offTime",
        receivedMqttMessage ->
            blindDuration.set(
                secondsTuDuration(new String(receivedMqttMessage.getMessage().getPayload()))),
        offTimeDisposableConsumer);
    return () -> {
      inputDisposable.accept(null);
      onValueDisposableConsumer.accept(null);
      offValueDisposableConsumer.accept(null);
      onTimeDisposableConsumer.accept(null);
      offTimeDisposableConsumer.accept(null);
      pendingFutureConsumer.accept(null);
    };
  }

  protected Duration secondsTuDuration(final String durationString) {
    final double durationTime = Double.parseDouble(durationString);
    return Duration.ofNanos((long) (durationTime * SECONDS_TO_NANOS));
  }

  enum MotionDetectorState {
    OFF,
    ON,
    BLIND
  }
}
