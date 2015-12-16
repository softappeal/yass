package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.serialize.Serializer;

import javax.websocket.RemoteEndpoint;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

/**
 * Sends messages asynchronously.
 * Closes session if timeout reached.
 */
public final class AsyncWsConnection extends WsConnection {

    /**
     * @see RemoteEndpoint.Async#setSendTimeout(long)
     */
    public static Factory factory(final long sendTimeoutMilliSeconds) {
        if (sendTimeoutMilliSeconds < 0) {
            throw new IllegalArgumentException("sendTimeoutMilliSeconds < 0");
        }
        return new Factory() {
            @Override public WsConnection create(final Serializer packetSerializer, final Session session) throws Exception {
                return new AsyncWsConnection(packetSerializer, session, sendTimeoutMilliSeconds);
            }
        };
    }

    private AsyncWsConnection(final Serializer packetSerializer, final Session session, final long sendTimeoutMilliSeconds) {
        super(packetSerializer, session);
        remoteEndpoint = session.getAsyncRemote();
        remoteEndpoint.setSendTimeout(sendTimeoutMilliSeconds);
    }

    private final RemoteEndpoint.Async remoteEndpoint;

    @Override public void write(final Packet packet) throws Exception {
        remoteEndpoint.sendBinary(writeToBuffer(packet), new SendHandler() {
            @Override public void onResult(final SendResult result) {
                if (result == null) {
                    AsyncWsConnection.this.onError(null);
                } else if (!result.isOK()) {
                    AsyncWsConnection.this.onError(result.getException());
                }
            }
        });
    }

}
