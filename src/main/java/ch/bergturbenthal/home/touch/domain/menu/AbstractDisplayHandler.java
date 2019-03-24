package ch.bergturbenthal.home.touch.domain.menu;

import ch.bergturbenthal.home.touch.domain.menu.settings.Type;
import ch.bergturbenthal.home.touch.domain.mqtt.MqttClient;
import ch.bergturbenthal.home.touch.domain.renderer.DisplayRenderer;
import ch.bergturbenthal.home.touch.domain.renderer.Shapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class AbstractDisplayHandler {
  private static final Base64.Encoder encoder = Base64.getEncoder();
  private final MqttClient mqttClient;
  private final ObjectReader touchDataReader;

  public AbstractDisplayHandler(final MqttClient mqttClient, final ObjectMapper mapper) {
    this.mqttClient = mqttClient;
    touchDataReader = mapper.readerFor(TouchData.class);
  }

  protected ShapeButtonDisplayEntry createBackButton(
      final DisplayRenderer displayRenderer,
      final int lastRow,
      final Runnable exitAction,
      final boolean enableScroll) {
    return new ShapeButtonDisplayEntry(
        Shapes.LEFT_ARROW,
        displayRenderer,
        DisplayRenderer.ZoneAddress.builder()
            .column(0)
            .row(lastRow)
            .colSpan(enableScroll ? 1 : 2)
            .build(),
        exitAction);
  }

  protected ShapeButtonDisplayEntry createDownButton(
      final DisplayRenderer displayRenderer,
      final int lastRow,
      final Runnable downAction,
      final boolean hasCloseButton) {
    return new ShapeButtonDisplayEntry(
        Shapes.DOWN_ARROW,
        displayRenderer,
        DisplayRenderer.ZoneAddress.builder()
            .column(hasCloseButton ? 1 : 0)
            .row(lastRow)
            .colSpan(hasCloseButton ? 1 : 2)
            .build(),
        downAction);
  }

  protected ShapeButtonDisplayEntry createUpButton(
      final DisplayRenderer displayRenderer,
      final Runnable upAction,
      final boolean hasCloseButton) {
    return new ShapeButtonDisplayEntry(
        Shapes.UP_ARROW,
        displayRenderer,
        DisplayRenderer.ZoneAddress.builder()
            .column(hasCloseButton ? 1 : 0)
            .row(0)
            .colSpan(hasCloseButton ? 1 : 2)
            .build(),
        upAction);
  }

  protected Disposable startLoop(
      String topic,
      final DisplayRenderer displayRenderer,
      final Runnable refresh,
      final List<DisplayEntry> displayList,
      Runnable defaultAction) {
    refresh.run();
    return mqttClient
        .listenTopic(topic + "/touchPosition")
        .map(MqttClient.ReceivedMqttMessage::getMessage)
        .map(MqttMessage::getPayload)
        .map(
            d -> {
              try {
                return touchDataReader.<TouchData>readValue(d);
              } catch (IOException e) {
                throw new RuntimeException("Cannot decode message", e);
              }
            })
        .map(d -> displayRenderer.calcTouchPosition(d.getX(), d.getY()))
        .onErrorResume(
            ex -> {
              log.warn("Error processing touch message");
              return Mono.empty();
            })
        .subscribe(
            p -> {
              Iterator<DisplayEntry> iterator = displayList.iterator();
              DisplayEntry.TouchResult touchResult = DisplayEntry.TouchResult.IGNORED;
              while (touchResult == DisplayEntry.TouchResult.IGNORED && iterator.hasNext())
                touchResult = iterator.next().handleTouch(p);
              //log.info("Touch result: "+touchResult);
              switch (touchResult) {
                case IGNORED:
                  defaultAction.run();
                  break;
                case DIRTY:
                  refresh.run();
                  break;
                case NOOP:
                  break;
              }
            },
            ex -> log.warn("Cannot update ui", ex));
  }

  protected void setBackgroundLight(String topic, int brightness) {
    final MqttMessage backgroundLightMessage = new MqttMessage();
    backgroundLightMessage.setRetained(true);
    backgroundLightMessage.setQos(1);
    backgroundLightMessage.setPayload(Integer.toString(brightness).getBytes());
    mqttClient
        .publish(topic, backgroundLightMessage)
        .subscribe(response -> {}, ex -> log.warn("Cannot set background light", ex));
  }

  protected void paint(
      final DisplayRenderer displayRenderer,
      final String contentTopic,
      final List<DisplayEntry> displayList) {
    try {
      displayRenderer.clear();
      for (DisplayEntry entry : displayList) {
        entry.draw();
      }
      final ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
      displayRenderer.render(encoder.wrap(arrayOutputStream));
      final MqttMessage message = new MqttMessage();
      message.setRetained(true);
      message.setQos(1);
      message.setPayload(arrayOutputStream.toByteArray());
      mqttClient
          .publish(contentTopic, message)
          .subscribe(response -> {}, ex -> log.warn("Cannot update image", ex));
    } catch (IOException ex) {
      log.warn("Cannot generate image", ex);
    }
  }

  protected Function<String, String> createDisplayFormatter(final Type type, final String dvFormat) {
    final Function<String, String> valueFormatter;
    if (type != null && dvFormat != null) {
      switch (type) {
        case INTEGER:
          {
            final DecimalFormat format = new DecimalFormat(dvFormat);
            valueFormatter = c -> format.format(Integer.parseInt(c));
          }
          break;
        case STRING:
        default:
          {
            final MessageFormat messageFormat = new MessageFormat(dvFormat);
            valueFormatter = messageFormat::format;
          }
          break;
        case FLOAT:
          {
            final DecimalFormat format = new DecimalFormat(dvFormat);
            valueFormatter = c -> format.format(Double.parseDouble(c));
          }
          break;
      }
    } else valueFormatter = Function.identity();
    return valueFormatter;
  }
}
