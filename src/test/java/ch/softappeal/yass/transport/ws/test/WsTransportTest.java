package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.core.remote.session.test.LocalConnectionTest;
import ch.softappeal.yass.transport.test.PacketSerializerTest;

public abstract class WsTransportTest extends WsTestBase {

  protected static void createSetup(
    final boolean serverInvoke, final boolean serverCreateException, final boolean serverOpenedException, final boolean serverInvokeBeforeOpened,
    final boolean clientInvoke, final boolean clientCreateException, final boolean clientOpenedException, final boolean clientInvokeBeforeOpened
  ) {
    PACKET_SERIALIZER = PacketSerializerTest.SERIALIZER;
    SESSION_SETUP_SERVER = LocalConnectionTest.createSetup(serverInvoke, "server", REQUEST_EXECUTOR, serverCreateException, serverOpenedException, serverInvokeBeforeOpened);
    SESSION_SETUP_CLIENT = LocalConnectionTest.createSetup(clientInvoke, "client", REQUEST_EXECUTOR, clientCreateException, clientOpenedException, clientInvokeBeforeOpened);
  }

}
