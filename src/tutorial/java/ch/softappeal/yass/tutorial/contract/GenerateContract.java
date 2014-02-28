package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.js.ModelGenerator;

public final class GenerateContract {

  public static void main(final String... args) throws Exception {
    new ModelGenerator(
      Config.class.getPackage(), Config.CONTRACT_SERIALIZER, Config.METHOD_MAPPER_FACTORY,
      "yass", "contract", "src/tutorial/js/contract.js"
    );
  }

}
