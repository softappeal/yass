package ch.softappeal.yass.autoconfig;

import ch.softappeal.yass.core.Interceptor;

import java.lang.reflect.Modifier;
import java.util.function.Predicate;

public final class ClassCollectorTest {

    private static void print(final ClassCollector classCollector, final Predicate<? super Class<?>> filter) {
        classCollector.classes.stream().filter(filter).forEach(c -> System.out.println(c.getName()));
    }

    public static void main(final String... args) throws Exception {
        System.out.println("--- from file system ---");
        final ClassCollector classCollector = new ClassCollector(Interceptor.class.getPackage().getName(), Interceptor.class.getClassLoader());
        System.out.println("* interfaces");
        print(classCollector, Class::isInterface);
        System.out.println("* concreteClasses");
        print(classCollector, c -> c.isEnum() || !Modifier.isAbstract(c.getModifiers()));

        System.out.println("--- from jar ---");
        print(new ClassCollector("org.junit", Interceptor.class.getClassLoader()), c -> true);
    }

}
