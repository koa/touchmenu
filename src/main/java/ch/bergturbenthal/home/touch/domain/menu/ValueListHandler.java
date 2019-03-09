package ch.bergturbenthal.home.touch.domain.menu;

import ch.bergturbenthal.home.touch.domain.mqtt.MqttClient;
import ch.bergturbenthal.home.touch.domain.renderer.DisplayRenderer;
import ch.bergturbenthal.home.touch.domain.settings.DisplaySettings;
import ch.bergturbenthal.home.touch.domain.settings.DisplayValue;
import ch.bergturbenthal.home.touch.domain.settings.Screen;
import ch.bergturbenthal.home.touch.domain.settings.View;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.MessageFormat;
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

  public Disposable handleView(
      View view,
      Screen screen,
      DisplaySettings settings,
      Runnable closeHandler,
      boolean showBackButton,
      final boolean enabledBackgroundLight) {
    final DisplayRenderer displayRenderer = new DisplayRenderer(settings);
    final String contentTopic = screen.getTopic() + "/image";
    final List<DisplayValue> displayValue = view.getDisplayValue();
    final int touchRowCount = settings.getTouchRowCount();
    final int touchColumnCount = settings.getTouchColumnCount();
    Queue<Disposable> cleanupQueue = new ConcurrentLinkedDeque<>();
    final AtomicReference<Runnable> refresh = new AtomicReference<>(() -> {});
    final boolean hasCloseButton = closeHandler != null && showBackButton;
    setBackgroundLight(screen.getTopic() + "/backlight", enabledBackgroundLight ? 100 : 0);

    List<Supplier<String>> menuEntries = new ArrayList<>();
    for (DisplayValue dv : displayValue) {
      AtomicReference<String> lastValue = new AtomicReference<>("");
      Function<String, String> valueFormatter;
      if (dv.getType() != null && dv.getFormat() != null) {
        switch (dv.getType()) {
          case INTEGER:
            {
              final DecimalFormat format = new DecimalFormat(dv.getFormat());
              valueFormatter = c -> format.format(Integer.parseInt(c));
            }
            break;
          case STRING:
          default:
            {
              final MessageFormat messageFormat = new MessageFormat(dv.getFormat());
              valueFormatter = messageFormat::format;
            }
            break;
          case FLOAT:
            {
              final DecimalFormat format = new DecimalFormat(dv.getFormat());
              valueFormatter = c -> format.format(Double.parseDouble(c));
            }
            break;
        }
      } else valueFormatter = Function.identity();
      Function<String, String> formatter =
          c -> {
            if (c == null || c.isEmpty()) return "-";

            return valueFormatter.apply(c);
          };

      final String label = dv.getLabel();
      if (label != null) menuEntries.add(() -> label);
      if (dv.getTopic() != null) {
        mqttClient.registerTopic(
            dv.getTopic(),
            message -> {
              final String stringMessage = new String(message.getPayload());
              final String lastMessage = lastValue.getAndSet(stringMessage);
              if (!Objects.equals(lastMessage, stringMessage)) refresh.get().run();
            },
            cleanupQueue::add);
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
    List<DisplayEntry> displayList = new ArrayList<>();

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
    final Runnable exitAction =
        () -> {
          while (true) {
            final Disposable disposable = cleanupQueue.poll();
            if (disposable == null) break;
            disposable.dispose();
          }
          if (closeHandler != null) closeHandler.run();
        };
    if (enableScroll) {
      displayList.add(createUpButton(displayRenderer, upAction, hasCloseButton));
      displayList.add(createDownButton(displayRenderer, lastRow, downAction, hasCloseButton));
    }
    if (hasCloseButton)
      displayList.add(createBackButton(displayRenderer, lastRow, exitAction, enableScroll));
    int menuStartRow = enableScroll ? 1 : 0;
    for (int i = 0; i < visibleRows; i++) {
      final int finalI = i;
      displayList.add(
          new DisplayEntry() {
            @Override
            public void draw() {
              int index = firstRow.get() + finalI;
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
            }

            @Override
            public boolean handleTouch(final Point2D position) {
              return false;
            }
          });
    }
    cleanupQueue.add(
        startLoop(
            screen, displayRenderer, refresh, displayList, showBackButton ? () -> {} : exitAction));
    refresh.set(() -> paint(displayRenderer, contentTopic, displayList));
    return new Disposable() {
      AtomicBoolean closeInvoked = new AtomicBoolean(false);
      @Override
      public boolean isDisposed() {
        return closeInvoked.get();
      }

      @Override
      public void dispose() {
        if (!closeInvoked.getAndSet(true)) exitAction.run();
      }
    };
  }
}
