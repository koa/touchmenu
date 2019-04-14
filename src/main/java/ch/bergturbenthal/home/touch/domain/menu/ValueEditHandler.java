package ch.bergturbenthal.home.touch.domain.menu;

import ch.bergturbenthal.home.touch.domain.menu.settings.Type;
import ch.bergturbenthal.home.touch.domain.menu.settings.ValueEdit;
import ch.bergturbenthal.home.touch.domain.mqtt.MqttClient;
import ch.bergturbenthal.home.touch.domain.renderer.DisplayRenderer;
import ch.bergturbenthal.home.touch.domain.settings.DisplaySettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Service
public class ValueEditHandler extends AbstractDisplayHandler {
  private final MqttClient mqttClient;

  public ValueEditHandler(final MqttClient mqttClient, final ObjectMapper mapper) {
    super(mqttClient, mapper);
    this.mqttClient = mqttClient;
  }

  public Mono<Void> showValueEditor(
      String screenTopic, ValueEdit valueEdit, DisplaySettings displaySettings) {
    return Mono.create(
        sink -> {
          final DisplayRenderer displayRenderer = new DisplayRenderer(displaySettings);
          final String valueTopic = valueEdit.getTopic();
          final List<DisplayEntry> displayList = Collections.synchronizedList(new ArrayList<>());
          List<Disposable> cleanupActions = new ArrayList<>();

          final AtomicReference<Runnable> refresh = new AtomicReference<>(() -> {});
          AtomicReference<String> lastTakenValue =
              new AtomicReference<>(
                  valueEdit.getType() == Type.INTEGER
                      ? Integer.toString((int) valueEdit.getDefaultValue())
                      : Double.toString(valueEdit.getDefaultValue()));
          mqttClient.registerTopic(
              valueTopic,
              message -> {
                final String value = new String(message.getMessage().getPayload());
                lastTakenValue.set(value);
                refresh.get().run();
              },
              cleanupActions::add);

          final Consumer<Integer> valueChangeConsumer =
              increment -> {
                final String valueBefore = lastTakenValue.get();
                // log.info("Value before: " + valueBefore);
                int currentValue =
                    (int) (Double.parseDouble(valueBefore) / valueEdit.getIncrement());
                // log.info("Current value: " + currentValue);

                final double newValueNumber = (currentValue + increment) * valueEdit.getIncrement();
                final Double minBounded =
                    valueEdit
                        .getMin()
                        .map(min -> Math.max(min, newValueNumber))
                        .orElse(newValueNumber);
                final double boundedValue =
                    valueEdit.getMax().map(max -> Math.min(max, minBounded)).orElse(minBounded);
                String newValue =
                    valueEdit.getType() == Type.INTEGER
                        ? Integer.toString((int) boundedValue)
                        : Double.toString(boundedValue);
                // log.info("Value after: " + newValue);
                final MqttMessage message = new MqttMessage();
                message.setQos(1);
                message.setRetained(true);
                message.setPayload(newValue.getBytes());
                mqttClient
                    .publish(valueTopic, message)
                    .subscribe(ret -> {}, ex -> log.warn("Cannot send updated value to mqtt"));
              };
          final Runnable upAction = () -> valueChangeConsumer.accept(1);
          final Runnable downAction = () -> valueChangeConsumer.accept(-1);
          final Runnable exitAction = sink::success;
          int rowIndex = 1;
          final String label = valueEdit.getLabel();
          if (label != null) {
            final int labelRow = rowIndex++;
            displayList.add(
                new DisplayEntry() {
                  @Override
                  public void draw() {
                    displayRenderer.drawText(
                        label,
                        DisplayRenderer.ZoneAddress.builder()
                            .row(labelRow)
                            .column(0)
                            .colSpan(displaySettings.getTouchColumnCount())
                            .build(),
                        DisplayRenderer.VerticalAlignment.MIDDLE,
                        DisplayRenderer.HorizontalAlignment.LEFT);
                  }

                  @Override
                  public TouchResult handleTouch(final Point2D position) {
                    return TouchResult.IGNORED;
                  }
                });
          }
          final int valueRow = rowIndex++;
          final Function<String, String> displayFormatter =
              createDisplayFormatter(valueEdit.getType(), valueEdit.getFormat());
          displayList.add(
              new DisplayEntry() {
                @Override
                public void draw() {
                  final String valueString = displayFormatter.apply(lastTakenValue.get());
                  displayRenderer.drawText(
                      valueString,
                      DisplayRenderer.ZoneAddress.builder()
                          .row(valueRow)
                          .column(0)
                          .colSpan(displaySettings.getTouchColumnCount())
                          .build(),
                      DisplayRenderer.VerticalAlignment.MIDDLE,
                      DisplayRenderer.HorizontalAlignment.LEFT);
                }

                @Override
                public TouchResult handleTouch(final Point2D position) {
                  return TouchResult.IGNORED;
                }
              });

          displayList.add(createUpButton(displayRenderer, upAction, false));
          displayList.add(
              createDownButton(
                  displayRenderer, displaySettings.getTouchRowCount() - 2, downAction, false));
          displayList.add(
              createBackButton(
                  displayRenderer, displaySettings.getTouchRowCount() - 1, exitAction, false));
          refresh.set(() -> paint(displayRenderer, screenTopic + "/image", displayList));
          cleanupActions.add(
              startLoop(
                  screenTopic, displayRenderer, refresh.get(), displayList, () -> sink.success()));
          sink.onDispose(() -> cleanupActions.forEach(Disposable::dispose));
        });
  }
}
