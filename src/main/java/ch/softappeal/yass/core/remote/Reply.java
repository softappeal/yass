package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

/**
 * A remote reply.
 */
public abstract class Reply extends Message {

  private static final long serialVersionUID = 1L;

  Reply(@Nullable final Object context) {
    super(context);
  }

  @Nullable abstract Object process() throws Throwable;

}
