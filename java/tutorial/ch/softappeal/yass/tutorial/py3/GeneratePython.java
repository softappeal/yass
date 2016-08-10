package ch.softappeal.yass.tutorial.py3;

import ch.softappeal.yass.generate.Python3Generator;
import ch.softappeal.yass.tutorial.contract.Config;

import java.util.HashMap;
import java.util.Map;

public final class GeneratePython {

    public static void main(final String... args) throws Exception {
        final Map<String, String> module2includeFile = new HashMap<>();
        module2includeFile.put("", "py3/tutorial/contract_include_root_module.txt");
        final Map<Class<?>, Python3Generator.ExternalDesc> externalTypes = new HashMap<>();
        externalTypes.put(Integer.class, new Python3Generator.ExternalDesc("Integer", "Integer.TYPE_DESC")); // shows how to use a contract external base type
        new Python3Generator(
            Config.class.getPackage().getName(), Config.PY3_CONTRACT_SERIALIZER, Config.INITIATOR, Config.ACCEPTOR,
            "py3/tutorial/contract_include_each_module.txt", module2includeFile, externalTypes, "py3/tutorial/generated"
        );
    }

}
