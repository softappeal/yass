package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

import java.io.Serializable;

/**
 * Common base class for remote requests and replies.
 */
public abstract class Message implements Serializable {

  private static final long serialVersionUID = 1L;

  @Nullable public final Object context;

  Message(@Nullable final Object context) {
    this.context = context;
  }

}
