package ch.softappeal.yass.js;

import ch.softappeal.yass.tutorial.contract.Config;

public final class GenerateContract {

  public static void main(final String... args) throws Exception {
    new ModelGenerator(
      Config.class.getPackage(), Config.CONTRACT_SERIALIZER, "yass", "contract", "src/tutorial/js/contract.js"
    );
  }

}
