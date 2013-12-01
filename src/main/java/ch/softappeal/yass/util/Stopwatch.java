package ch.softappeal.yass.util;

/**
 * An "one-shot" stopwatch.
 */
public final class Stopwatch {

  private boolean stopped = false;
  private final long startNanos;
  @SuppressWarnings("InstanceVariableMayNotBeInitialized") private long endNanos;

  public Stopwatch() {
    startNanos = System.nanoTime();
  }

  public void stop() {
    if (stopped) {
      throw new IllegalStateException("stopwatch is already stopped");
    }
    endNanos = System.nanoTime();
    stopped = true;
  }

  public long nanoSeconds() {
    if (!stopped) {
      throw new IllegalStateException("stopwatch is not yet stopped");
    }
    return endNanos - startNanos;
  }

  public long microSeconds() {
    return nanoSeconds() / 1_000L;
  }

  public long milliSeconds() {
    return nanoSeconds() / 1_000_000L;
  }

  public long seconds() {
    return nanoSeconds() / 1_000_000_000L;
  }

}
