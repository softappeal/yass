package ch.softappeal.yass.tutorial.server;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import javax.websocket.server.ServerEndpointConfig;
import java.io.File;

public final class UndertowServer {

  public static void main(final String... args) throws Exception {
    final Xnio xnio = Xnio.getInstance();
    final XnioWorker xnioWorker = xnio.createWorker(OptionMap.builder().getMap());
    final WebSocketDeploymentInfo webSockets = new WebSocketDeploymentInfo()
      .addEndpoint(ServerEndpointConfig.Builder.create(JettyServer.Endpoint.class, JettyServer.PATH).build())
      .setWorker(xnioWorker);
    final DeploymentManager deployment = Servlets.defaultContainer()
      .addDeployment(Servlets.deployment()
        .setClassLoader(UndertowServer.class.getClassLoader())
        .setContextPath("/")
        .setDeploymentName(UndertowServer.class.getName())
        .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, webSockets)
      );
    deployment.deploy();
    final HttpHandler webSocketHandler = deployment.start();
    final HttpHandler fileHandler = Handlers.resource(new FileResourceManager(new File("."), 100));
    Undertow.builder()
      .addHttpListener(JettyServer.PORT, JettyServer.HOST)
      .setHandler(new HttpHandler() {
        @Override public void handleRequest(final HttpServerExchange exchange) throws Exception {
          if (JettyServer.PATH.equals(exchange.getRequestPath())) {
            webSocketHandler.handleRequest(exchange);
          } else {
            fileHandler.handleRequest(exchange);
          }
        }
       })
      .build()
      .start();
    System.out.println("started");
  }

}
