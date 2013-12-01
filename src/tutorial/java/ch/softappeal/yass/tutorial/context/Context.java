package ch.softappeal.yass.tutorial.context;

import ch.softappeal.yass.core.Invocation;

import java.io.Serializable;

/**
 * Shows how to use {@link Invocation#context}.
 */
public final class Context implements Serializable {

  private static final long serialVersionUID = 1L;

  public final String user;
  public final int someOtherContext;

  public Context(final String user, final int someOtherContext) {
    this.user = user;
    this.someOtherContext = someOtherContext;
  }

  @Override public String toString() {
    return '(' + user + ", " + someOtherContext + ')';
  }

}
