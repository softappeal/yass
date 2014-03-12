package ch.softappeal.yass.util;

/**
 * An "one-shot" stopwatch.
 */
public final class Stopwatch {

  private boolean stopped;
  private final long startNanos;
  private long endNanos;

  public Stopwatch() {
    startNanos = System.nanoTime();
  }

  public void stop() {
    final long endNanos = System.nanoTime();
    if (stopped) {
      throw new IllegalStateException("stopwatch is already stopped");
    }
    this.endNanos = endNanos;
    stopped = true;
  }

  public long nanoSeconds() {
    if (!stopped) {
      throw new IllegalStateException("stopwatch is not yet stopped");
    }
    return (endNanos - startNanos);
  }

  public long microSeconds() {
    return (nanoSeconds() / 1_000L);
  }

  public long milliSeconds() {
    return (nanoSeconds() / 1_000_000L);
  }

  public long seconds() {
    return (nanoSeconds() / 1_000_000_000L);
  }

}
