package ch.softappeal.yass.core.remote.session;

/**
 * Is thrown if an outgoing request has been interrupted.
 * <p/>
 * An outgoing request timeout can be implemented with an interceptor interrupting the outgoing request.
 */
public final class RequestInterruptedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

}
