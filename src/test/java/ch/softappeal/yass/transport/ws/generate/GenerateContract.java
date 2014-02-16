package ch.softappeal.yass.transport.ws.generate;

import ch.softappeal.yass.transport.ws.JettyServer;
import ch.softappeal.yass.tutorial.contract.Config;

public final class GenerateContract {

  public static void main(final String... args) throws Exception {
    new ModelGenerator(
      Config.class.getPackage(), JettyServer.CONTRACT_SERIALIZER, "yass", "contract", "src/test/java/ch/softappeal/yass/transport/ws/generated/contract.js"
    );
  }

}
