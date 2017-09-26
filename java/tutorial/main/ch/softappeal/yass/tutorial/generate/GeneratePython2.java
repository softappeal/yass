package ch.softappeal.yass.tutorial.generate;

import ch.softappeal.yass.generate.PythonGenerator;
import ch.softappeal.yass.tutorial.contract.Config;

import java.util.Map;

public final class GeneratePython2 {

    public static void main(final String... args) throws Exception {
        new PythonGenerator(
            Config.class.getPackage().getName(),
            Config.PY_CONTRACT_SERIALIZER,
            Config.INITIATOR,
            Config.PY_ACCEPTOR,
            false,
            "../../py2/tutorial/contract_include_each_module.txt",
            Map.of( // module2includeFile
                "", "../../py2/tutorial/contract_include_root_module.txt"
            ),
            Map.of( // shows how to use a contract external base type
                Integer.class, new PythonGenerator.ExternalDesc("Integer", "Integer.TYPE_DESC")
            ),
            "build/generated/py2"
        );
    }

}
