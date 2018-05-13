package ch.softappeal.yass.tutorial.generate;

import ch.softappeal.yass.generate.PythonExternalDesc;
import ch.softappeal.yass.generate.PythonGenerator;
import ch.softappeal.yass.tutorial.contract.Config;

import java.util.HashMap;
import java.util.Map;

public final class GeneratePython2 {

    public static void main(final String... args) throws Exception {
        final Map<String, String> module2includeFile = new HashMap<>();
        module2includeFile.put("", "../../py2/tutorial/contract_include_root_module.txt");
        final Map<Class<?>, PythonExternalDesc> externalTypes = new HashMap<>();
        externalTypes.put(Integer.class, new PythonExternalDesc("Integer", "Integer.TYPE_DESC")); // shows how to use a contract external base type
        new PythonGenerator(
            Config.class.getPackage().getName(),
            Config.PY_CONTRACT_SERIALIZER,
            Config.INITIATOR,
            Config.PY_ACCEPTOR,
            false,
            "../../py2/tutorial/contract_include_each_module.txt",
            module2includeFile,
            externalTypes,
            "build/generated/py2"
        );
    }

}
