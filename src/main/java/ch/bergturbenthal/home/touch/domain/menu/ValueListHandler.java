package ch.bergturbenthal.home.touch.domain.menu;

import ch.bergturbenthal.home.touch.domain.menu.settings.DisplayValue;
import ch.bergturbenthal.home.touch.domain.menu.settings.View;
import ch.bergturbenthal.home.touch.domain.mqtt.MqttClient;
import ch.bergturbenthal.home.touch.domain.renderer.DisplayRenderer;
import ch.bergturbenthal.home.touch.domain.settings.DisplaySettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@Slf4j
public class ValueListHandler extends AbstractDisplayHandler {
  private MqttClient mqttClient;

  public ValueListHandler(final MqttClient mqttClient, ObjectMapper mapper) {
    super(mqttClient, mapper);
    this.mqttClient = mqttClient;
  }

  public Mono<ExitReason> handleView(
      String topic,
      View view,
      DisplaySettings settings,
      boolean showBackButton,
      final boolean enabledBackgroundLight) {
    return Mono.create(
        sink -> {
          final DisplayRenderer displayRenderer = new DisplayRenderer(settings);
          final List<DisplayValue> displayValue = view.getDisplayValue();
          final int touchRowCount = settings.getTouchRowCount();
          final int touchColumnCount = settings.getTouchColumnCount();
          final Queue<Disposable> cleanupQueue = new ConcurrentLinkedDeque<>();
          final AtomicReference<Runnable> refresh = new AtomicReference<>(() -> {});
          final boolean hasCloseButton = showBackButton;
          setBackgroundLightEnabled(topic + "/enableBacklight", enabledBackgroundLight);

          List<Supplier<String>> menuEntries = new ArrayList<>();
          for (DisplayValue dv : displayValue) {
            AtomicReference<String> lastValue = new AtomicReference<>("");
            Function<String, String> valueFormatter =
                createDisplayFormatter(dv.getType(), dv.getFormat());
            Function<String, String> formatter =
                c -> {
                  if (c == null || c.isEmpty()) return "-";

                  return valueFormatter.apply(c);
                };

            final String label = dv.getLabel();
            if (label != null) menuEntries.add(() -> label);
            if (dv.getTopic() != null) {
              // log.info("Register for " + dv.getTopic());
              mqttClient.registerTopic(
                  dv.getTopic(),
                  message -> {
                    final String stringMessage = new String(message.getMessage().getPayload());
                    final String lastMessage = lastValue.getAndSet(stringMessage);
                    if (!Objects.equals(lastMessage, stringMessage)) refresh.get().run();
                  },
                  e -> {
                    cleanupQueue.add(
                        () -> {
                          // log.info("Disposing " + dv.getTopic());
                          e.dispose();
                        });
                  });
              menuEntries.add(() -> formatter.apply(lastValue.get()));
            }
          }
          final int menuRowCount = menuEntries.size();
          final int visibleRowsWithoutScroll = hasCloseButton ? touchRowCount - 1 : touchRowCount;
          final int visibleRowsWithScroll = touchRowCount - 2;
          boolean enableScroll = menuEntries.size() > visibleRowsWithoutScroll;
          int visibleRows = enableScroll ? visibleRowsWithScroll : visibleRowsWithoutScroll;

          final AtomicInteger firstRow = new AtomicInteger(0);
          int maxFirstRow = Math.max(menuRowCount - visibleRows, 0);
          final int lastRow = touchRowCount - 1;
          final List<DisplayEntry> displayList = new ArrayList<>();

          final Runnable upAction =
              () -> {
                firstRow.updateAndGet(r -> Math.max(r - 1, 0));
                refresh.get().run();
              };
          final Runnable downAction =
              () -> {
                firstRow.updateAndGet(r -> Math.min(r + 1, maxFirstRow));
                refresh.get().run();
              };
          if (enableScroll) {
            displayList.add(createUpButton(displayRenderer, upAction, hasCloseButton));
            displayList.add(createDownButton(displayRenderer, lastRow, downAction, hasCloseButton));
          }
          if (hasCloseButton)
            displayList.add(
                createBackButton(
                    displayRenderer, lastRow, () -> sink.success(ExitReason.BACK), enableScroll));
          int menuStartRow = enableScroll ? 1 : 0;
          for (int i = 0; i < visibleRows; i++) {
            final int finalI = i;
            displayList.add(
                new DisplayEntry() {
                  @Override
                  public void draw() {
                    int index = firstRow.get() + finalI;
                    try {
                      if (index >= menuRowCount) return;
                      final Supplier<String> menuEntry = menuEntries.get(index);
                      displayRenderer.drawText(
                          menuEntry.get(),
                          DisplayRenderer.ZoneAddress.builder()
                              .column(0)
                              .row(finalI + menuStartRow)
                              .colSpan(touchColumnCount)
                              .build(),
                          DisplayRenderer.VerticalAlignment.MIDDLE,
                          DisplayRenderer.HorizontalAlignment.LEFT);
                    } catch (Exception ex) {
                      log.warn("Cannot display entry " + index, ex);
                    }
                  }

                  @Override
                  public TouchResult handleTouch(final Point2D position) {
                    return TouchResult.IGNORED;
                  }
                });
          }
          refresh.set(() -> paint(displayRenderer, topic + "/image", displayList));
          cleanupQueue.add(
              startLoop(
                  topic,
                  displayRenderer,
                  refresh.get(),
                  displayList,
                  () -> sink.success(ExitReason.TOUCH)));
          sink.onDispose(
              new Disposable() {
                AtomicBoolean closeInvoked = new AtomicBoolean(false);

                @Override
                public boolean isDisposed() {
                  return closeInvoked.get();
                }

                @Override
                public void dispose() {
                  if (!closeInvoked.getAndSet(true)) {
                    while (true) {
                      final Disposable disposable = cleanupQueue.poll();
                      if (disposable == null) break;
                      disposable.dispose();
                    }
                  }
                }
              });
        });
  }

  public enum ExitReason {
    BACK,
    TOUCH
  }
}
