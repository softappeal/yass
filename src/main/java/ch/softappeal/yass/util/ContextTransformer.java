package ch.softappeal.yass.util;

/**
 * A {@link ContextLocator} that transform a context from another {@link ContextLocator}.
 * @param <F> type of from context
 * @param <T> type of to context
 */
public abstract class ContextTransformer<F, T> implements ContextLocator<T> {

  private final ContextLocator<F> fromLocator;

  protected ContextTransformer(final ContextLocator<F> fromLocator) {
    this.fromLocator = Check.notNull(fromLocator);
  }

  /**
   * @return the transformed context
   */
  protected abstract T transform(F fromContext);

  @Override public final T context() {
    return transform(fromLocator.context());
  }

}
