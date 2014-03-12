package ch.softappeal.yass.tutorial.client;

import ch.softappeal.yass.ts.ContractGenerator;
import ch.softappeal.yass.tutorial.contract.Config;

public final class GenerateContract {

  public static void main(final String... args) throws Exception {
    new ContractGenerator(
      Config.class.getPackage(), Config.CONTRACT_SERIALIZER, Config.METHOD_MAPPER_FACTORY,
      "../../main/ts/yass", "src/tutorial/ts/contract.ts"
    );
  }

}
