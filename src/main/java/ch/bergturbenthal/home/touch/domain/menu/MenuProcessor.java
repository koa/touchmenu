package ch.bergturbenthal.home.touch.domain.menu;

import ch.bergturbenthal.home.touch.domain.settings.*;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import javax.annotation.PostConstruct;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Service
public class MenuProcessor {
  private final MenuProperties menuProperties;
  private final ValueListHandler valueListHandler;
  private final MenuHandler menuHandler;
  private final ScheduledExecutorService scheduledExecutorService;

  public MenuProcessor(
      final MenuProperties menuProperties,
      final ValueListHandler valueListHandler,
      final MenuHandler menuHandler,
      ScheduledExecutorService scheduledExecutorService) {
    this.menuProperties = menuProperties;
    this.valueListHandler = valueListHandler;
    this.menuHandler = menuHandler;
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @PostConstruct
  public void startMenu() {
    final Map<String, DisplaySettings> displaySettings = menuProperties.getDisplaySettings();
    for (Screen screen : menuProperties.getScreens()) {
      Deque<DisplayState> backList = new ConcurrentLinkedDeque<>();
      final DisplaySettings settings = displaySettings.get(screen.getDisplaySettings());
      final DisplayState currentState = new DisplayState(false, null);
      AtomicReference<Disposable> openDisposable = new AtomicReference<>(null);
      Consumer<Disposable> disposableConsumer =
          disp -> {
            final Disposable oldDisposable = openDisposable.getAndSet(disp);
            if (oldDisposable != null) oldDisposable.dispose();
          };
      AtomicReference<ScheduledFuture<?>> runningSchedule = new AtomicReference<>(null);
      Consumer<ScheduledFuture<?>> pendingTimerConsumer =
          scheduledFuture -> {
            final ScheduledFuture<?> oldSchedule = runningSchedule.getAndSet(scheduledFuture);
            if (oldSchedule != null && !oldSchedule.isDone()) oldSchedule.cancel(false);
          };
      enterState(
          screen, settings, currentState, disposableConsumer, pendingTimerConsumer, backList);
    }
  }

  protected void enterState(
      final Screen screen,
      final DisplaySettings settings,
      final DisplayState currentState,
      final Consumer<Disposable> disposableConsumer,
      final Consumer<ScheduledFuture<?>> pendingTimerConsumer,
      final Deque<DisplayState> backList) {
    log.info("Show state " + currentState);
    AtomicBoolean closed = new AtomicBoolean(false);
    Consumer<DisplayState> enterStateConsumer =
        nextState ->
            enterState(
                screen, settings, nextState, disposableConsumer, pendingTimerConsumer, backList);
    final String menuEntry = currentState.getMenuEntry();
    Consumer<DisplayState> nextStateConsumer =
        nextState -> {
          if (!closed.getAndSet(true)) {
            if (menuEntry != null && !Objects.equals(nextState, currentState))
              backList.push(currentState);
            enterStateConsumer.accept(nextState);
          }
        };
    Runnable goBackRunnable =
        () -> {
          if (!closed.getAndSet(true)) {
            final DisplayState backState = backList.pollFirst();
            if (backState != null) enterStateConsumer.accept(backState);
            else enterStateConsumer.accept(new DisplayState(true, null));
          }
        };

    final Runnable closeMenuRunnable =
        () -> {
          if (!closed.getAndSet(true)) {
            backList.clear();
            enterStateConsumer.accept(new DisplayState(false, null));
          }
        };
    if (menuEntry != null || currentState.isEnabledBackgroundLight()) {
      pendingTimerConsumer.accept(
          scheduledExecutorService.schedule(closeMenuRunnable, 10, TimeUnit.MINUTES));
    }

    final View selectedView;
    String currentViewId;
    if (menuEntry == null) {
      selectedView = screen.getDefaultView();
      currentViewId = null;
    } else if (menuEntry.isEmpty()) {
      selectedView = screen.getRootMenu();
      currentViewId = "";
    } else {
      final View rootMenu = screen.getRootMenu();
      View currentMenu = rootMenu;
      final String[] split = menuEntry.split("\\.");
      for (int i = 0; i < split.length && currentMenu != null; i++) {
        final String menuPart = split[i];
        final List<MenuEntry> menu = currentMenu.getMenu();
        if (menu != null) {
          currentMenu =
              menu.stream()
                  .filter(m -> m.getId().equals(menuPart))
                  .findFirst()
                  .map(MenuEntry::getContent)
                  .orElse(null);
        } else currentMenu = null;
      }
      if (currentMenu == null) {
        log.warn("Cannot find menu entry " + menuEntry);
        selectedView = rootMenu;
        currentViewId = "";
      } else {
        selectedView = currentMenu;
        currentViewId = menuEntry;
      }
    }
    disposableConsumer.accept(
        showView(
            selectedView,
            currentState,
            nextStateConsumer,
            screen,
            settings,
            goBackRunnable,
            currentViewId));
  }

  private Disposable showView(
      final View selectedView,
      final DisplayState currentState,
      final Consumer<DisplayState> nextStateConsumer,
      final Screen screen,
      final DisplaySettings settings,
      final Runnable goBackRunnable,
      final String currentViewId) {
    if (selectedView.getMenu() != null && !selectedView.getMenu().isEmpty()) {
      final Function<String, String> menuEntryFunction;
      if (currentViewId.isEmpty()) menuEntryFunction = Function.identity();
      else menuEntryFunction = s -> currentViewId + "." + s;
      return menuHandler
          .showMenu(selectedView, screen, settings)
          .map(menuEntryFunction)
          .map(e -> new DisplayState(true, e))
          .subscribe(
              nextStateConsumer,
              ex -> {
                log.warn("Error processing menu", ex);
                nextStateConsumer.accept(new DisplayState(true, null));
              },
              goBackRunnable);
    }

    return valueListHandler.handleView(
        selectedView,
        screen,
        settings,
        () ->
            nextStateConsumer.accept(
                !currentState.isEnabledBackgroundLight()
                    ? new DisplayState(true, null)
                    : new DisplayState(true, screen.getStartEntry())),
        false,
        currentState.isEnabledBackgroundLight());
  }

  @Value
  private static class DisplayState {
    boolean enabledBackgroundLight;
    String menuEntry;
  }
}
