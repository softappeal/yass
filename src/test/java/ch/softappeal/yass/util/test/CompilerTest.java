package ch.softappeal.yass.util.test;

import ch.softappeal.yass.util.Compiler;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CompilerTest {

  public static final String TEXT = "Hello";

  @Test public void test() throws Exception {
    final String source = CompilerTestSource.class.getName();
    final Map<String, CharSequence> map = new HashMap<>();
    map.put(
      source,
      Compiler.readFile(
        new File("src/test/java", source.replace('.', '/') + Compiler.JAVA_EXT),
        StandardCharsets.US_ASCII
      )
    );
    final ClassLoader classLoader = Compiler.compile(
      CompilerTest.class.getClassLoader(),
      map,
      "-Xlint"
    );
    classLoader.loadClass(source).getMethod("x").invoke(null);
    CompilerTestSource.x();
  }

}
