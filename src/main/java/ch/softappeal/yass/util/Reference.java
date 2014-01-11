package ch.softappeal.yass.util;

/**
 * Wraps a value.
 * @param <T> the value type
 */
public final class Reference<T> {

  @Nullable private T value;

  private Reference(@Nullable final T value) {
    this.value = value;
  }

  @Nullable public T get() {
    return value;
  }

  public boolean isNull() {
    return value == null;
  }

  public void set(@Nullable final T value) {
    this.value = value;
  }

  public void setNull() {
    value = null;
  }

  @Override public String toString() {
    return String.valueOf(value);
  }

  public static <T> Reference<T> create(@Nullable final T value) {
    return new Reference<>(value);
  }

  /**
   * Creates a null reference.
   */
  public static <T> Reference<T> create() {
    return new Reference<>(null);
  }

}
