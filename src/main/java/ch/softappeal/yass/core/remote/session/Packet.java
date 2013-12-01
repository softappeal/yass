package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.io.Serializable;

/**
 * A {@link Message} with a request number.
 */
public final class Packet implements Serializable {

  private static final long serialVersionUID = 1L;

  private final int requestNumber;
  @Nullable private final Message message;

  public boolean isEnd() {
    return requestNumber == END_REQUEST_NUMBER;
  }

  private void checkNotEnd() {
    if (isEnd()) {
      throw new IllegalStateException("not allowed if isEnd");
    }
  }

  /**
   * Must not be called if {@link #isEnd()}.
   */
  public int requestNumber() {
    checkNotEnd();
    return requestNumber;
  }

  /**
   * Must not be called if {@link #isEnd()}.
   */
  public Message message() {
    checkNotEnd();
    return message;
  }

  /**
   * @param requestNumber must not be {@link #END_REQUEST_NUMBER}; use {@link #END} instead
   */
  public Packet(final int requestNumber, final Message message) {
    if (requestNumber == END_REQUEST_NUMBER) {
      throw new IllegalArgumentException("use END");
    }
    this.requestNumber = requestNumber;
    this.message = Check.notNull(message);
  }

  private Packet() {
    requestNumber = END_REQUEST_NUMBER;
    message = null;
  }

  public static boolean isEnd(final int requestNumber) {
    return requestNumber == END_REQUEST_NUMBER;
  }

  public static final Packet END = new Packet();

  public static final int END_REQUEST_NUMBER = 0;

}
