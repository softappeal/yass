package ch.softappeal.yass.tutorial.client.web;

import ch.softappeal.yass.ts.ContractGenerator;
import ch.softappeal.yass.tutorial.contract.Config;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public final class GenerateTypeScriptContract {

    public static void main(final String... args) throws Exception {
        final Map<Class<?>, String> contractExternalJavaBaseType2contractInternalTsBaseType = new HashMap<>();
        contractExternalJavaBaseType2contractInternalTsBaseType.put(Double.class, "instrument.stock.Double");
        new ContractGenerator(
            Config.class.getPackage(), Config.CONTRACT_SERIALIZER, Config.METHOD_MAPPER_FACTORY,
            "baseTypes", "contract", contractExternalJavaBaseType2contractInternalTsBaseType, "src/tutorial/ts/contract.ts"
        );
        Config.CONTRACT_SERIALIZER.print(new PrintWriter(System.out, true));
    }

}
