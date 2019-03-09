package ch.bergturbenthal.home.touch.domain.menu;

import ch.bergturbenthal.home.touch.domain.mqtt.MqttClient;
import ch.bergturbenthal.home.touch.domain.renderer.DisplayRenderer;
import ch.bergturbenthal.home.touch.domain.settings.DisplaySettings;
import ch.bergturbenthal.home.touch.domain.settings.MenuEntry;
import ch.bergturbenthal.home.touch.domain.settings.Screen;
import ch.bergturbenthal.home.touch.domain.settings.View;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
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

  Mono<String> showMenu(View view, Screen screen, DisplaySettings settings) {
    final DisplayRenderer displayRenderer = new DisplayRenderer(settings);
    Queue<Disposable> disposables = new ConcurrentLinkedDeque<>();
    return Mono.create(
            (MonoSink<String> sink) -> {
              final List<MenuEntry> menu = view.getMenu();
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
                Supplier<MenuEntry> currentEntry =
                    () -> {
                      final int currentIndex = finalI + startEntry.get();
                      if (currentIndex < menu.size()) return menu.get(currentIndex);
                      return null;
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
                        final MenuEntry menuEntry = currentEntry.get();
                        if (menuEntry == null) return;
                        displayRenderer.drawText(
                            menuEntry.getLabel(),
                            position,
                            DisplayRenderer.VerticalAlignment.MIDDLE,
                            DisplayRenderer.HorizontalAlignment.LEFT);
                      }

                      @Override
                      public boolean handleTouch(final Point2D p) {
                        final MenuEntry menuEntry = currentEntry.get();
                        if (menuEntry != null
                            && displayRenderer.getTouchZone(position, 0).contains(p)) {
                          sink.success(menuEntry.getId());
                          return true;
                        }
                        return false;
                      }
                    });
              }
              disposables.add(startLoop(screen, displayRenderer, refresh, displayList, () -> {}));
              refresh.set(() -> paint(displayRenderer, screen.getTopic() + "/image", displayList));
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
