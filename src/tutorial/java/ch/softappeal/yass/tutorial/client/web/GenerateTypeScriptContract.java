package ch.softappeal.yass.tutorial.client.web;

import ch.softappeal.yass.ts.ContractGenerator;
import ch.softappeal.yass.tutorial.contract.Config;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public final class GenerateTypeScriptContract {

    public static void main(final String... args) throws Exception {
        final Map<Class<?>, String> externalJavaBaseType2tsBaseType = new HashMap<>();
        externalJavaBaseType2tsBaseType.put(Double.class, "instrument.stock.Double");
        new ContractGenerator(
            Config.class.getPackage(), Config.CONTRACT_SERIALIZER, Config.METHOD_MAPPER_FACTORY,
            "baseTypes", "contract", externalJavaBaseType2tsBaseType, "src/tutorial/ts/contract.ts"
        );
        Config.CONTRACT_SERIALIZER.print(new PrintWriter(System.out, true));
    }

}
