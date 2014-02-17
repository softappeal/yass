package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.TestUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.junit.Ignore;
import org.junit.Test;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Ignore
public class JettyTransportTest extends WsTransportTest { // $todo: review

  private static void run(
    final boolean serverInvoke, final boolean serverCreateException, final boolean serverOpenedException, final boolean serverInvokeBeforeOpened,
    final boolean clientInvoke, final boolean clientCreateException, final boolean clientOpenedException, final boolean clientInvokeBeforeOpened
  ) throws Exception {
    REQUEST_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("requestExecutor", TestUtils.TERMINATE));

    createSetup(
      serverInvoke, serverCreateException, serverOpenedException, serverInvokeBeforeOpened,
      clientInvoke, clientCreateException, clientOpenedException, clientInvokeBeforeOpened
    );

    final Server server = new Server(PORT);
    server.addConnector(new ServerConnector(server));
    final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    WebSocketServerContainerInitializer.configureContext(context).addEndpoint(
      ServerEndpointConfig.Builder.create(ServerEndpoint.class, PATH).build()
    );
    server.start();

    final ClientContainer container = new ClientContainer();
    container.start();
    final ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();

    container.connectToServer(new ClientEndpoint(), config, THE_URI);
    TimeUnit.MILLISECONDS.sleep(400L);

    container.stop();
    TimeUnit.MILLISECONDS.sleep(400L);
    server.stop();
    TimeUnit.MILLISECONDS.sleep(400L);

    REQUEST_EXECUTOR.awaitTermination(1, TimeUnit.DAYS);
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
