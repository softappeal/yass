package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.js.ModelGenerator;

public final class GenerateJsContract { // $todo: review

  public static void main(final String... args) throws Exception {
    new ModelGenerator(
      Config.class.getPackage(), Config.CONTRACT_SERIALIZER, "yass", "contract", "src/tutorial/js/contract.js"
    );
  }

}
