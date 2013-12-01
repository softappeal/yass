package ch.softappeal.yass.util;

import java.util.concurrent.TimeUnit;

public abstract class PerformanceTask {

  protected abstract void run(int count) throws Throwable;

  private String runOne(final int count, final TimeUnit timeUnit) throws Throwable {
    final Stopwatch stopwatch = new Stopwatch();
    run(count);
    stopwatch.stop();
    switch (timeUnit) {
      case NANOSECONDS:
        return (stopwatch.nanoSeconds() / count) + "ns";
      case MICROSECONDS:
        return (stopwatch.microSeconds() / count) + "us";
      case MILLISECONDS:
        return (stopwatch.milliSeconds() / count) + "ms";
      case SECONDS:
        return (stopwatch.seconds() / count) + "s";
      default:
        throw new RuntimeException("unsupported TimeUnit");
    }
  }

  public final void run(final int count, final TimeUnit timeUnit) {
    try {
      System.out.println(count + " iterations, one took - cold: " + runOne(count, timeUnit) + ", hot: " + runOne(count, timeUnit));
    } catch (final Throwable t) {
      throw new Error(t);
    }
  }

}
