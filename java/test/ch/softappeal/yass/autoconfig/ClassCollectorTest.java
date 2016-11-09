package ch.softappeal.yass.autoconfig;

import ch.softappeal.yass.core.Interceptor;

public final class ClassCollectorTest {

    public static void main(final String... args) throws Exception {
        new ClassCollector(Interceptor.class.getPackage().getName(), Interceptor.class.getClassLoader()).classes.forEach(c -> System.out.println(c.getName()));
        System.out.println();
        new ClassCollector("org.junit", Interceptor.class.getClassLoader()).classes.forEach(c -> System.out.println(c.getName()));
    }

}
