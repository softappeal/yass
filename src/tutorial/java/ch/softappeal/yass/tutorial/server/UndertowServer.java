package ch.softappeal.yass.tutorial.server;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.xnio.OptionMap;
import org.xnio.Xnio;

public final class UndertowServer extends WsServerSetup {

  public static void main(final String... args) throws Exception {
    final DeploymentManager deployment = Servlets.defaultContainer()
      .addDeployment(
        Servlets.deployment()
          .setClassLoader(UndertowServer.class.getClassLoader())
          .setContextPath("/")
          .setDeploymentName(UndertowServer.class.getName())
          .addServletContextAttribute(
            WebSocketDeploymentInfo.ATTRIBUTE_NAME,
            new WebSocketDeploymentInfo()
              .addEndpoint(ENDPOINT_CONFIG)
              .setWorker(Xnio.getInstance().createWorker(OptionMap.builder().getMap()))
          )
      );
    deployment.deploy();
    Undertow.builder()
      .addHttpListener(PORT, HOST)
      .setHandler(deployment.start())
      .build()
      .start();
    System.out.println("started");
  }

}
