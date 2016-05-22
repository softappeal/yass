package ch.softappeal.yass.transport.socket;

import java.net.ServerSocket;

@FunctionalInterface public interface SocketBinder {

    ServerSocket bind() throws Exception;

}
