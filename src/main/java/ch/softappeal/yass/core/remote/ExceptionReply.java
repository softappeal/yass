package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

/**
 * A remote exception reply.
 */
public final class ExceptionReply extends Reply {

  private static final long serialVersionUID = 1L;

  public final Throwable throwable;

  public ExceptionReply(@Nullable final Object context, final Throwable throwable) {
    super(context);
    this.throwable = Check.notNull(throwable);
  }

  @Override Object process() throws Throwable {
    throw throwable;
  }

}
