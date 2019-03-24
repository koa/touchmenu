package ch.bergturbenthal.home.touch.domain.menu;

import ch.bergturbenthal.home.touch.domain.menu.settings.MenuEntry;
import ch.bergturbenthal.home.touch.domain.menu.settings.Screen;
import ch.bergturbenthal.home.touch.domain.menu.settings.View;
import ch.bergturbenthal.home.touch.domain.settings.DisplaySettings;
import ch.bergturbenthal.home.touch.domain.settings.MenuProperties;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class MenuProcessor {
  private final MenuProperties menuProperties;
  private final ValueListHandler valueListHandler;
  private final MenuHandler menuHandler;
  private ValueEditHandler valueEditHandler;

  public MenuProcessor(
      final MenuProperties menuProperties,
      final ValueListHandler valueListHandler,
      final MenuHandler menuHandler,
      final ValueEditHandler valueEditHandler) {
    this.menuProperties = menuProperties;
    this.valueListHandler = valueListHandler;
    this.menuHandler = menuHandler;
    this.valueEditHandler = valueEditHandler;
  }

  public Disposable startTopic(final String topic, final Screen screen) {
    final Map<String, DisplaySettings> displaySettings = menuProperties.getDisplaySettings();
    final DisplaySettings settings = displaySettings.get(screen.getDisplaySettings());
    final DisplayState currentState = new DisplayState(false, null);
    AtomicReference<Disposable> currentDisposables = new AtomicReference<>();
    Consumer<Disposable> disposableConsumer =
        disposable -> {
          final Disposable previousDispable = currentDisposables.getAndSet(disposable);
          if (previousDispable != null) previousDispable.dispose();
        };
    enterState(topic, screen, settings, currentState, Collections.emptyList(), disposableConsumer);
    return () -> {
      disposableConsumer.accept(null);
    };
  }

  protected void enterState(
      final String topic,
      final Screen screen,
      final DisplaySettings settings,
      final DisplayState currentState,
      final List<DisplayState> backList,
      Consumer<Disposable> disposableConsumer) {
    AtomicBoolean done = new AtomicBoolean(false);
    disposableConsumer.accept(null);
    final Mono<MenuResult> finalResult = showState(topic, screen, settings, currentState);
    disposableConsumer.accept(
        finalResult.subscribe(
            menuResult -> {
              final DisplayState newState;
              final List<DisplayState> newBackList;
              if (menuResult instanceof GoBackResult && backList.size() > 0) {
                newState = backList.get(backList.size() - 1);
                newBackList = backList.subList(0, backList.size() - 1);
              } else if (menuResult instanceof DisplayState) {
                newBackList =
                    Stream.concat(backList.stream(), Stream.of(currentState))
                        .collect(Collectors.toList());
                newState = (DisplayState) menuResult;
              } else {
                newState = new DisplayState(false, null);
                newBackList = Collections.emptyList();
              }
              done.set(true);
              enterState(topic, screen, settings, newState, newBackList, disposableConsumer);
            },
            ex -> {
              log.warn("Exception while showing menu", ex);
              enterState(
                  topic,
                  screen,
                  settings,
                  new DisplayState(false, null),
                  Collections.emptyList(),
                  disposableConsumer);
            },
            () -> {
              if (!done.get())
                enterState(
                    topic,
                    screen,
                    settings,
                    new DisplayState(false, null),
                    Collections.emptyList(),
                    disposableConsumer);
              // log.warn("Finished state " + currentState);
            }));
  }

  private Mono<MenuResult> showState(
      final String topic,
      final Screen screen,
      final DisplaySettings settings,
      final DisplayState currentState) {
    // log.info("Show state " + currentState);

    final View selectedView =
        currentState.getView() == null ? screen.getDefaultView() : currentState.getView();

    final Mono<MenuResult> resultMono =
        showView(topic, selectedView, currentState, screen, settings)
        // .doFinally(signal -> log.info("Hide state by " + signal + ": " + currentState))
        ;
    if (!currentState.isEnabledBackgroundLight()) {
      return resultMono;
    }
    return Mono.first(
        resultMono, Mono.delay(screen.getScreenTimeout()).map(l -> new TimeoutExceededResult()));
  }

  private Mono<MenuResult> showView(
      String topic,
      final View selectedView,
      final DisplayState currentState,
      final Screen screen,
      final DisplaySettings settings) {
    if (selectedView.getMenu() != null && !selectedView.getMenu().isEmpty()) {
      return menuHandler
          .showMenu(topic, selectedView, settings)
          .map(e -> selectedView.getMenu().get(e))
          .map(MenuEntry::getContent)
          .map(e -> new DisplayState(true, e))
          .cast(MenuResult.class)
          .defaultIfEmpty(new GoBackResult());
    }
    if (selectedView.getValueEdit() != null) {
      return valueEditHandler
          .showValueEditor(topic, selectedView.getValueEdit(), settings)
          .cast(MenuResult.class)
          .defaultIfEmpty(new GoBackResult());
    }

    final Mono<ValueListHandler.ExitReason> exitReasonMono =
        valueListHandler.handleView(
            topic, selectedView, settings, false, currentState.isEnabledBackgroundLight());
    return exitReasonMono.map(
        r -> {
          if (currentState.getView() == null && r == ValueListHandler.ExitReason.TOUCH)
            if (!currentState.isEnabledBackgroundLight()) {
              return new DisplayState(true, null);
            } else {
              return new DisplayState(true, screen.getRootMenu());
            }
          else return new GoBackResult();
        });
  }

  private interface MenuResult {}

  private static class GoBackResult implements MenuResult {}

  private static class TimeoutExceededResult implements MenuResult {}

  @Value
  private static class DisplayState implements MenuResult {
    private boolean enabledBackgroundLight;
    private View view;
  }
}
