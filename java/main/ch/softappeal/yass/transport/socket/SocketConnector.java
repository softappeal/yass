package ch.softappeal.yass.transport.socket;

import java.net.Socket;

@FunctionalInterface public interface SocketConnector {

    Socket connect() throws Exception;

}
