package ch.bergturbenthal.home.touch.domain.util;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ScheduledFutureConsumer<T> implements Consumer<ScheduledFuture<T>> {
  private AtomicReference<ScheduledFuture<T>> pendingFuture = new AtomicReference<>();

  @Override
  public void accept(final ScheduledFuture<T> scheduledFuture) {
    final ScheduledFuture<?> lastFuture = pendingFuture.getAndSet(scheduledFuture);
    if (lastFuture != null && !lastFuture.isDone()) lastFuture.cancel(false);
  }
}
