package ch.softappeal.yass.tutorial.generate;

import ch.softappeal.yass.generate.ts.*;
import ch.softappeal.yass.tutorial.contract.*;

import java.nio.file.*;
import java.util.*;

public final class GenerateTypeScript {

    public static void main(final String... args) {
        final Map<Class<?>, ExternalDesc> externalTypes = new HashMap<>();
        externalTypes.put(Integer.class, new ExternalDesc("Integer", "IntegerHandler"));
        new TypeScriptGenerator(
            Config.class.getPackage().getName(),
            Config.CONTRACT_SERIALIZER,
            Config.INITIATOR,
            Config.ACCEPTOR,
            Paths.get("../../ts/tutorial/contract-include.txt"),
            externalTypes,
            Paths.get("build/generated/ts/contract.ts")
            // , "protected readonly __TYPE_KEY__!: never;"
        );
    }

}
