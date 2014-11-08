package ch.softappeal.yass.util.test;

import ch.softappeal.yass.util.Compiler;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CompilerTest {

    private static String add(final Map<String, CharSequence> map, final String name) throws IOException {
        final String source = "ch.softappeal.yass.util.test." + name;
        map.put(
            source,
            Compiler.readFile(
                new File("src/test/java", source.replace('.', '/') + ".txt"),
                StandardCharsets.US_ASCII
            )
        );
        return source;
    }

    @Test public void test() throws Exception {
        final Map<String, CharSequence> map = new HashMap<>();
        final String source = add(map, "CompilerTestSource");
        add(map, "CompilerTestSourceText");
        final ClassLoader classLoader = Compiler.compile(
            CompilerTest.class.getClassLoader(),
            map,
            "-Xlint"
        );
        classLoader.loadClass(source).getMethod("x").invoke(null);
    }

}
