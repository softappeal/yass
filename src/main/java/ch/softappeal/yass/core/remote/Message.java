package ch.softappeal.yass.core.remote;

import java.io.Serializable;

/**
 * Common base class for remote requests and replies.
 */
public abstract class Message implements Serializable {

  private static final long serialVersionUID = 1L;

  Message() {
    // empty
  }

}
