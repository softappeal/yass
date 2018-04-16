package ch.softappeal.yass;

/**
 * Wraps a value.
 * @param <T> the value type
 */
public final class Reference<T> {

    private @Nullable T value;

    private Reference(final @Nullable T value) {
        this.value = value;
    }

    public @Nullable T get() {
        return value;
    }

    public boolean isNull() {
        return value == null;
    }

    public void set(final @Nullable T value) {
        this.value = value;
    }

    public void setNull() {
        value = null;
    }

    @Override public String toString() {
        return String.valueOf(value);
    }

    public static <T> Reference<T> create(final @Nullable T value) {
        return new Reference<>(value);
    }

    /**
     * Creates a null reference.
     */
    public static <T> Reference<T> create() {
        return new Reference<>(null);
    }

}
