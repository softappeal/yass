package ch.softappeal.yass.transport.ws;

import javax.websocket.CloseReason;

public final class WsClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public final CloseReason closeReason;

    public WsClosedException(final CloseReason closeReason) {
        super(closeReason.toString());
        this.closeReason = closeReason;
    }

}
