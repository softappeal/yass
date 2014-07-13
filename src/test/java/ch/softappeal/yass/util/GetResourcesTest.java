package ch.softappeal.yass.util;

import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;

public class GetResourcesTest {

  private static void print(final Class<?> rootClass) throws Exception {
    System.out.println("# " + rootClass);
    final ClassLoader classLoader = rootClass.getClassLoader();
    final Enumeration<URL> resources = classLoader.getResources(rootClass.getPackage().getName().replace('.', '/'));
    while (resources.hasMoreElements()) {
      final File file = new File(resources.nextElement().getFile());
      System.out.println(file + ", isDirectory: " + file.isDirectory());
    }
    System.out.println();
  }

  public static void main(final String... args) throws Exception {
    print(GetResourcesTest.class); // works because the class is in the file system
    print(Test.class);             // doesn't work because the class is in a jar
  }

}
