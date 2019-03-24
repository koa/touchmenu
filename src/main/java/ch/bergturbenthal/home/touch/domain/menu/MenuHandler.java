package ch.bergturbenthal.home.touch.domain.menu;

import ch.bergturbenthal.home.touch.domain.mqtt.MqttClient;
import ch.bergturbenthal.home.touch.domain.renderer.DisplayRenderer;
import ch.bergturbenthal.home.touch.domain.settings.DisplaySettings;
import ch.bergturbenthal.home.touch.domain.menu.settings.MenuEntry;
import ch.bergturbenthal.home.touch.domain.menu.settings.View;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Slf4j
@Service
public class MenuHandler extends AbstractDisplayHandler {
  public MenuHandler(final MqttClient mqttClient, final ObjectMapper mapper) {
    super(mqttClient, mapper);
  }

  Mono<String> showMenu(String topic, View view, DisplaySettings settings) {
    final DisplayRenderer displayRenderer = new DisplayRenderer(settings);
    Queue<Disposable> disposables = new ConcurrentLinkedDeque<>();
    return Mono.create(
            (MonoSink<String> sink) -> {
              final ArrayList<Map.Entry<String, MenuEntry>> menu =
                  new ArrayList<>(view.getMenu().entrySet());
              final int touchRowCount = settings.getTouchRowCount();
              int rowCountWithoutScroll = touchRowCount - 1;
              int rowCountWithScroll = touchRowCount - 2;
              boolean enableScroll = menu.size() > rowCountWithoutScroll;
              int visibleMenuEntryCount = enableScroll ? rowCountWithScroll : rowCountWithoutScroll;
              AtomicInteger startEntry = new AtomicInteger(0);
              int maxStartEnry = Math.max(menu.size() - visibleMenuEntryCount, 0);
              List<DisplayEntry> displayList = new ArrayList<>();
              AtomicReference<Runnable> refresh = new AtomicReference<>(() -> {});
              if (enableScroll) {
                displayList.add(
                    createUpButton(
                        displayRenderer,
                        () -> {
                          startEntry.updateAndGet(i -> Math.max(i - 1, 0));
                          refresh.get().run();
                        },
                        true));
                displayList.add(
                    createDownButton(
                        displayRenderer,
                        touchRowCount - 1,
                        () -> {
                          startEntry.updateAndGet(i -> Math.min(i + 1, maxStartEnry));
                          refresh.get().run();
                        },
                        true));
              }
              displayList.add(
                  createBackButton(
                      displayRenderer, touchRowCount - 1, sink::success, enableScroll));
              int menuStartRow = enableScroll ? 1 : 0;
              for (int i = 0; i < visibleMenuEntryCount; i++) {
                final int finalI = i;
                Supplier<Optional<Map.Entry<String, MenuEntry>>> currentEntry =
                    () -> {
                      final int currentIndex = finalI + startEntry.get();
                      if (currentIndex < menu.size()) return Optional.of(menu.get(currentIndex));
                      return Optional.empty();
                    };
                final DisplayRenderer.ZoneAddress position =
                    DisplayRenderer.ZoneAddress.builder()
                        .row(menuStartRow + i)
                        .column(0)
                        .colSpan(settings.getTouchColumnCount())
                        .build();
                displayList.add(
                    new DisplayEntry() {
                      @Override
                      public void draw() {
                        currentEntry
                            .get()
                            .map(Map.Entry::getValue)
                            .map(MenuEntry::getLabel)
                            .ifPresent(
                                menuEntry ->
                                    displayRenderer.drawText(
                                        menuEntry,
                                        position,
                                        DisplayRenderer.VerticalAlignment.MIDDLE,
                                        DisplayRenderer.HorizontalAlignment.LEFT));
                      }

                      @Override
                      public TouchResult handleTouch(final Point2D p) {
                        return currentEntry
                            .get()
                            .filter(e -> displayRenderer.getTouchZone(position, 0).contains(p))
                            .map(Map.Entry::getKey)
                            .map(
                                e -> {
                                  sink.success(e);
                                  return TouchResult.NOOP;
                                })
                            .orElse(TouchResult.IGNORED);
                      }
                    });
              }
              refresh.set(() -> paint(displayRenderer, topic + "/image", displayList));
              disposables.add(
                  startLoop(topic, displayRenderer, refresh.get(), displayList, () -> {}));
            })
        .doFinally(
            signal -> {
              while (true) {
                final Disposable poll = disposables.poll();
                if (poll == null) break;
                poll.dispose();
              }
            });
  }
}
