package ch.softappeal.yass.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CompilerTest {

  public static final String TEXT = "hello";

  private static final String CODE =
    "package x;" +
    "public class T {" +
    "public static void x() {" +
    "System.out.println(ch.softappeal.yass.util.CompilerTest.TEXT + \", world\");" +
    "" +
    "}" +
    "}";

  @Test public void test() throws Exception {
    final Map<String, CharSequence> map = new HashMap<>();
    map.put("x.T", CODE);
    final ClassLoader classLoader = Compiler.compile(Thread.currentThread().getContextClassLoader(), map, "-Xlint");
    classLoader.loadClass("x.T").getMethod("x").invoke(null);
  }

}
