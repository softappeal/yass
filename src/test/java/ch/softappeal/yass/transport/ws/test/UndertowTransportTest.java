package ch.softappeal.yass.transport.ws.test;

import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class UndertowTransportTest extends UndertowTest {

  private static void run(
    final boolean serverInvoke, final boolean serverCreateException,
    final boolean clientInvoke, final boolean clientCreateException
  ) throws Exception {
    setTransportSetup(
      serverInvoke, serverCreateException,
      clientInvoke, clientCreateException
    );
    run(new CountDownLatch(0));
  }

  @Ignore // $todo Undertow does not yet catch this exception
  @Test public void createException() throws Exception {
    run(
      false, false,
      false, true
    );
  }

  @Test public void clientInvoke() throws Exception {
    run(
      false, false,
      true, false
    );
  }

  @Test public void serverInvoke() throws Exception {
    run(
      true, false,
      false, false
    );
  }

}
