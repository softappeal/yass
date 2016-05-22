package ch.softappeal.yass.transport.ws.test;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class JettyPerformanceTest extends JettyTest {

    @Test public void test() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        setPerformanceSetup(latch);
        run(latch);
    }

}
