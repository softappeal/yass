package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.transport.socket.test.SocketPerformanceTest;

import java.util.concurrent.CountDownLatch;

public abstract class WsPerformanceTest extends WsTestBase {

  protected static void createSetup(final CountDownLatch latch) {
    PACKET_SERIALIZER = SocketPerformanceTest.PACKET_SERIALIZER;
    SESSION_SETUP_SERVER = PerformanceTest.createSetup(REQUEST_EXECUTOR, null, SocketPerformanceTest.COUNTER);
    SESSION_SETUP_CLIENT = PerformanceTest.createSetup(REQUEST_EXECUTOR, latch, SocketPerformanceTest.COUNTER);
  }

}
