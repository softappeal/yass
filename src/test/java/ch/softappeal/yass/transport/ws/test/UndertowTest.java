package ch.softappeal.yass.transport.ws.test;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ThreadSetupAction;
import io.undertow.servlet.core.CompositeThreadSetupAction;
import io.undertow.servlet.util.DefaultClassIntrospector;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import javax.websocket.server.ServerEndpointConfig;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public abstract class UndertowTest extends WsTest {

  protected static void run(final CountDownLatch latch) throws Exception {
    final Xnio xnio = Xnio.getInstance();
    final XnioWorker xnioWorker = xnio.createWorker(OptionMap.builder().getMap());
    final WebSocketDeploymentInfo webSockets = new WebSocketDeploymentInfo()
      .addEndpoint(ServerEndpointConfig.Builder.create(ServerEndpoint.class, PATH).build())
      .setWorker(xnioWorker);
    final DeploymentManager deployment = Servlets.defaultContainer()
      .addDeployment(Servlets.deployment()
        .setClassLoader(UndertowTest.class.getClassLoader())
        .setContextPath("/")
        .setDeploymentName(UndertowTest.class.getName())
        .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, webSockets)
      );
    deployment.deploy();
    final Undertow server = Undertow.builder()
      .addHttpListener(PORT, "localhost")
      .setHandler(deployment.start())
      .build();
    server.start();
    final XnioWorker worker = Xnio.getInstance().createWorker(OptionMap.create(Options.THREAD_DAEMON, true));
    final Pool<ByteBuffer> buffers = new ByteBufferSlicePool(1024, 10240);
    final ServerWebSocketContainer container = new ServerWebSocketContainer(
      DefaultClassIntrospector.INSTANCE, worker, buffers, new CompositeThreadSetupAction(Collections.<ThreadSetupAction>emptyList()), true
    );
    connect(container, latch);
    server.stop();
  }

}
