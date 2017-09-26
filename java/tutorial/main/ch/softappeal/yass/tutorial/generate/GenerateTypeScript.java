package ch.softappeal.yass.tutorial.generate;

import ch.softappeal.yass.generate.TypeScriptGenerator;
import ch.softappeal.yass.tutorial.contract.Config;

import java.util.Map;

public final class GenerateTypeScript {

    public static void main(final String... args) throws Exception {
        new TypeScriptGenerator(
            Config.class.getPackage().getName(),
            Config.CONTRACT_SERIALIZER,
            Config.INITIATOR,
            Config.ACCEPTOR,
            "../../ts/tutorial/contract-include.txt",
            Map.of( // shows how to use a contract external base type
                Integer.class, new TypeScriptGenerator.ExternalDesc("Integer", "IntegerHandler")
            ),
            "build/generated/ts/contract.ts"
        );
    }

}
