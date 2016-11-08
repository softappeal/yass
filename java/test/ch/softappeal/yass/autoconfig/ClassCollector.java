package ch.softappeal.yass.autoconfig;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Collects all classes in the same root package recursively.
 */
public final class ClassCollector {

    private static final char DOT = '.';
    private static final char SLASH = '/';
    private static final String CLASS_SUFFIX = ".class";

    private final ClassLoader classLoader;
    private final String rootPackage;

    public final List<Class<?>> classes = new ArrayList<>();

    public final List<Class<?>> enums = new ArrayList<>();
    public final List<Class<?>> interfaces = new ArrayList<>();
    public final List<Class<?>> abstractClasses = new ArrayList<>();
    public final List<Class<?>> concreteClasses = new ArrayList<>();

    public ClassCollector(final Class<?> classInRootPackage) throws Exception {
        this(classInRootPackage.getPackage().getName(), classInRootPackage.getClassLoader());
    }

    public ClassCollector(final String aPackage) throws Exception {
        this(aPackage, Thread.currentThread().getContextClassLoader());
    }

    public ClassCollector(final String aPackage, final ClassLoader aClassLoader) throws Exception {
        classLoader = aClassLoader;
        rootPackage = aPackage;
        inspectPackage();
    }

    private void inspectPackage() throws Exception  {
        for (final Enumeration<URL> resources = classLoader.getResources(rootPackage.replace(DOT, SLASH)); resources.hasMoreElements(); ) {
            final URL url = resources.nextElement();
            final String protocol = url.getProtocol().toLowerCase();
            if ("jar".equals(protocol)) {
                collectJarFile(((JarURLConnection) url.openConnection()).getJarFile().getName(), rootPackage);
            } else if ("file".equals(protocol)) {
                collectFileSystem(new File(url.getFile()), rootPackage);
            } else {
                throw new RuntimeException("unexpected protocol '" + protocol + "'");
            }
        }
    }

    private void collectFileSystem(final File file, final String packageName) throws ClassNotFoundException {
        if (file.isDirectory()) {
            for (final File f : file.listFiles()) {
                collectFileSystem(f, packageName + (f.isDirectory() ? DOT + f.getName() : ""));
            }
        } else if (isClass(file.getName())) {
            loadClass(getClassName(packageName + DOT + file.getName()));
        }
    }

    private void collectJarFile(final String jarName, final String packageName) throws Exception {
        try (JarFile jarFile = new JarFile(jarName)) {
            for (final Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                final String name = entries.nextElement().getName();
                if (isClass(name)) {
                    final String className = getClassName(name.replace(SLASH, DOT));
                    if (className.startsWith(packageName)) {
                        loadClass(className);
                    }
                }
            }
        }
    }

    private void loadClass(final String className) throws ClassNotFoundException {
        specifyClass(Class.forName(className, true, classLoader));
    }

    private void specifyClass(final Class<?> clazz) {
        if (clazz.isEnum()) {
            enums.add(clazz);
            classes.add(clazz);
        } else if (clazz.isInterface()) {
            interfaces.add(clazz);
        } else if (Modifier.isAbstract(clazz.getModifiers())) {
            abstractClasses.add(clazz);
        } else {
            concreteClasses.add(clazz);
            classes.add(clazz);
        }
    }

    private static boolean isClass(final String resource) {
        return resource.endsWith(CLASS_SUFFIX);
    }

    private static String getClassName(final String resource) {
        return resource.substring(0, resource.length() - CLASS_SUFFIX.length());
    }

    public void print() {
        System.out.println("================================================================================");
        System.out.println("  Collected types from RootPackage: [" + rootPackage + "]");
        System.out.println("================================================================================");
        System.out.println("Enum (included in contract) ----------------------------------------------------");
        this.enums.forEach(enumeration -> System.out.println("  + " + enumeration.getCanonicalName()));
        System.out.println("Class (included in contract) ---------------------------------------------------");
        this.concreteClasses.forEach(clazz -> System.out.println("  + " + clazz.getCanonicalName()));
        System.out.println("Interface (to be implemented by backend and frontend) --------------------------");
        this.interfaces.forEach(i -> System.out.println("  + " + i.getCanonicalName()));
        System.out.println("Abstract Class (fyi) -----------------------------------------------------------");
        this.abstractClasses.forEach(ac -> System.out.println("  + " + ac.getCanonicalName()));
        System.out.println("================================================================================");
    }

}
