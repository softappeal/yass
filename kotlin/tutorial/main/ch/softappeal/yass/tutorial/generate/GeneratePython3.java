package ch.softappeal.yass.tutorial.generate;

import ch.softappeal.yass.generate.py.ExternalDesc;
import ch.softappeal.yass.generate.py.PythonGenerator;
import ch.softappeal.yass.tutorial.contract.Config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class GeneratePython3 {

    public static void main(final String... args) throws Exception {
        final Map<String, Path> module2includeFile = new HashMap<>();
        module2includeFile.put("", Paths.get("../../py3/tutorial/contract_include_root_module.txt"));
        final Map<Class<?>, ExternalDesc> externalTypes = new HashMap<>();
        externalTypes.put(Integer.class, new ExternalDesc("Integer", "Integer.TYPE_DESC"));
        new PythonGenerator(
            Config.class.getPackage().getName(),
            Config.PY_CONTRACT_SERIALIZER,
            Config.INITIATOR,
            Config.PY_ACCEPTOR,
            true,
            Paths.get("../../py3/tutorial/contract_include_each_module.txt"),
            module2includeFile,
            externalTypes,
            Paths.get("build/generated/py3")
        );
    }

}
