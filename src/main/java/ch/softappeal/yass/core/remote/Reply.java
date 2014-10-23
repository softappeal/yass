package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

public abstract class Reply extends Message {

  private static final long serialVersionUID = 1L;

  Reply() {
    // empty
  }

  @Nullable abstract Object process() throws Throwable;

}
