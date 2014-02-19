package ch.softappeal.yass.transport.ws.echo;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import javax.websocket.server.ServerEndpointConfig;

public final class EchoUndertowServer {

  public static void main(final String... args) throws Exception {
    final Xnio xnio = Xnio.getInstance();
    final XnioWorker xnioWorker = xnio.createWorker(OptionMap.builder().getMap());
    final WebSocketDeploymentInfo webSockets = new WebSocketDeploymentInfo()
      .addEndpoint(ServerEndpointConfig.Builder.create(EchoServerEndpoint.class, EchoJettyServer.PATH).build())
      .setWorker(xnioWorker);
    final DeploymentManager deployment = Servlets.defaultContainer()
      .addDeployment(Servlets.deployment()
        .setClassLoader(EchoUndertowServer.class.getClassLoader())
        .setContextPath("/")
        .setDeploymentName(EchoUndertowServer.class.getName())
        .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, webSockets)
      );
    deployment.deploy();
    Undertow.builder()
      .addHttpListener(EchoJettyServer.PORT, "localhost")
      .setHandler(deployment.start())
      .build()
      .start();
  }

}
