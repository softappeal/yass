package ch.softappeal.yass.tutorial.shared.socket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class SocketSetup {

    private SocketSetup() {
        // disable
    }

    public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

}
