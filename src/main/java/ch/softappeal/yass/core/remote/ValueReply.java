package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

/**
 * A remote value reply.
 */
public final class ValueReply extends Reply {

  private static final long serialVersionUID = 1L;

  @Nullable public final Object value;

  public ValueReply(@Nullable final Object context, @Nullable final Object value) {
    super(context);
    this.value = value;
  }

  @Override @Nullable Object process() {
    return value;
  }

}
