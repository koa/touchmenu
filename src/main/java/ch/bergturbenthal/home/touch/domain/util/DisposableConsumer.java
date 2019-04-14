package ch.bergturbenthal.home.touch.domain.util;

import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class DisposableConsumer implements Consumer<Disposable> {
  private AtomicReference<Disposable> currentRunningDisposable = new AtomicReference<>();

  @Override
  public void accept(final Disposable disposable) {
    final Disposable oldDisposable = currentRunningDisposable.getAndSet(disposable);
    if (oldDisposable != null) oldDisposable.dispose();
  }
}
