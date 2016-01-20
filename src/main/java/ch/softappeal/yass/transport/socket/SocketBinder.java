package ch.softappeal.yass.transport.socket;

import java.net.ServerSocket;

public interface SocketBinder {

    ServerSocket bind() throws Exception;

}
