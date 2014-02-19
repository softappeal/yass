package ch.softappeal.yass.transport.ws.test;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class UndertowTransportTest extends UndertowTest {

  private static void run(
    final boolean serverInvoke, final boolean serverCreateException, final boolean serverOpenedException, final boolean serverInvokeBeforeOpened,
    final boolean clientInvoke, final boolean clientCreateException, final boolean clientOpenedException, final boolean clientInvokeBeforeOpened
  ) throws Exception {
    setTransportSetup(
      serverInvoke, serverCreateException, serverOpenedException, serverInvokeBeforeOpened,
      clientInvoke, clientCreateException, clientOpenedException, clientInvokeBeforeOpened
    );
    run(new CountDownLatch(0));
  }

  @Test public void createException() throws Exception {
    run(
      false, false, false, false,
      false, true, false, false
    );
  }

  @Test public void openedException() throws Exception {
    run(
      false, false, true, false,
      false, false, true, false
    );
  }

  @Test public void invokeBeforeOpened() throws Exception {
    run(
      false, false, false, false,
      false, false, false, true
    );
  }

  @Test public void clientInvoke() throws Exception {
    run(
      false, false, false, false,
      true, false, false, false
    );
  }

  @Test public void serverInvoke() throws Exception {
    run(
      true, false, false, false,
      false, false, false, false
    );
  }

}
