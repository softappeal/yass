package ch.softappeal.yass.tutorial.initiator.web;

import ch.softappeal.yass.ts.ContractGenerator;
import ch.softappeal.yass.tutorial.contract.Config;

import java.util.HashMap;
import java.util.Map;

public final class GenerateTypeScriptContract {

    public static void main(final String... args) throws Exception {
        final Map<Class<?>, ContractGenerator.TypeDesc> externalTypes = new HashMap<>();
        externalTypes.put(Integer.class, new ContractGenerator.TypeDesc("Integer", "IntegerHandler")); // shows how to use a contract external base type
        new ContractGenerator(
            Config.class.getPackage().getName(), Config.CONTRACT_SERIALIZER, Config.INITIATOR, Config.ACCEPTOR,
            "baseTypes", "contract", externalTypes, "src/tutorial/ts/contract.ts"
        );
    }

}
