package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.TestUtils;
import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ThreadSetupAction;
import io.undertow.servlet.core.CompositeThreadSetupAction;
import io.undertow.servlet.util.DefaultClassIntrospector;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.junit.Test;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UndertowPerformanceTest extends WsPerformanceTest { // $todo: review

  @Test public void test() throws Exception {
    REQUEST_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("requestExecutor", TestUtils.TERMINATE));

    final CountDownLatch latch = new CountDownLatch(1);
    createSetup(latch);

    final Xnio xnio = Xnio.getInstance();
    final XnioWorker xnioWorker = xnio.createWorker(OptionMap.builder().getMap());
    final WebSocketDeploymentInfo webSockets = new WebSocketDeploymentInfo()
      .addEndpoint(
        ServerEndpointConfig.Builder.create(ServerEndpoint.class, PATH).build()
      )
      .setWorker(xnioWorker);
    final DeploymentManager deployment = Servlets.defaultContainer()
      .addDeployment(Servlets.deployment()
        .setClassLoader(UndertowTransportTest.class.getClassLoader())
        .setContextPath("/")
        .setDeploymentName(UndertowTransportTest.class.getName())
        .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, webSockets));
    deployment.deploy();
    Undertow.builder().
      addHttpListener(PORT, "localhost")
      .setHandler(deployment.start())
      .build()
      .start();

    final XnioWorker worker = Xnio.getInstance().createWorker(OptionMap.create(Options.THREAD_DAEMON, true));
    final Pool<ByteBuffer> buffers = new ByteBufferSlicePool(1024, 10240);
    final ServerWebSocketContainer container = new ServerWebSocketContainer(
      DefaultClassIntrospector.INSTANCE, worker, buffers, new CompositeThreadSetupAction(Collections.<ThreadSetupAction>emptyList()), true
    );

    final ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();

    container.connectToServer(new ClientEndpoint(), config, THE_URI);

    latch.await();
    TimeUnit.MILLISECONDS.sleep(400L);
    REQUEST_EXECUTOR.shutdown();
    REQUEST_EXECUTOR.awaitTermination(1, TimeUnit.DAYS);
  }

}
