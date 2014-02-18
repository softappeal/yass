package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.TestUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.junit.Test;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JettyPerformanceTest extends WsPerformanceTest { // $todo: review

  @Test public void test() throws Exception {
    REQUEST_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("requestExecutor", TestUtils.TERMINATE));


    final CountDownLatch latch = new CountDownLatch(1);
    createSetup(latch);

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

    latch.await();

    TimeUnit.MILLISECONDS.sleep(400L);

    container.stop();
    server.stop();
    REQUEST_EXECUTOR.shutdown();
    REQUEST_EXECUTOR.awaitTermination(1, TimeUnit.DAYS);
  }


}
