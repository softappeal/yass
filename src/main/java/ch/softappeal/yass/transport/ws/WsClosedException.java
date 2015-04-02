package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.util.Nullable;

import javax.websocket.CloseReason;

public final class WsClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @Nullable public final CloseReason closeReason;

    public WsClosedException(@Nullable final CloseReason closeReason) {
        super((closeReason == null) ? "<no-closeReason>" : closeReason.toString());
        this.closeReason = closeReason;
    }

}
