package ch.softappeal.yass.transport.socket;

import java.net.Socket;

public interface SocketConnector {

    Socket connect() throws Exception;

}
