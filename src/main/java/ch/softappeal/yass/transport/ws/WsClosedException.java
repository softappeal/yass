package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.util.Nullable;

import javax.websocket.CloseReason;

public final class WsClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public final @Nullable CloseReason closeReason;

    public WsClosedException(final @Nullable CloseReason closeReason) {
        super((closeReason == null) ? "<no-closeReason>" : closeReason.toString());
        this.closeReason = closeReason;
    }

}
