package ch.softappeal.yass.autoconfig;

import java.io.File;
import java.net.JarURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.jar.JarFile;

/**
 * Collects all classes in the same root package recursively.
 */
public final class ClassCollector {

    private static final char DOT = '.';
    private static final char SLASH = '/';
    private static final String CLASS_SUFFIX = ".class";

    private final ClassLoader classLoader;
    public final Collection<Class<?>> classes = new ArrayList<>();

    public ClassCollector(final String rootPackage, final ClassLoader classLoader) throws Exception {
        this.classLoader = Objects.requireNonNull(classLoader);
        for (final var resources = classLoader.getResources(rootPackage.replace(DOT, SLASH)); resources.hasMoreElements(); ) {
            final var url = resources.nextElement();
            final var protocol = url.getProtocol().toLowerCase();
            if ("jar".equals(protocol)) {
                collectJarFile(((JarURLConnection)url.openConnection()).getJarFile().getName(), rootPackage);
            } else if ("file".equals(protocol)) {
                collectFileSystem(new File(url.getFile()), rootPackage);
            } else {
                throw new RuntimeException("unexpected protocol '" + protocol + "'");
            }
        }
    }

    private void collectFileSystem(final File file, final String packageName) throws ClassNotFoundException {
        if (file.isDirectory()) {
            for (final var f : file.listFiles()) {
                collectFileSystem(f, packageName + (f.isDirectory() ? DOT + f.getName() : ""));
            }
        } else if (isClass(file.getName())) {
            loadClass(getClassName(packageName + DOT + file.getName()));
        }
    }

    private void collectJarFile(final String jarName, final String packageName) throws Exception {
        try (var jarFile = new JarFile(jarName)) {
            for (final var entries = jarFile.entries(); entries.hasMoreElements(); ) {
                final var name = entries.nextElement().getName();
                if (isClass(name)) {
                    final var className = getClassName(name.replace(SLASH, DOT));
                    if (className.startsWith(packageName)) {
                        loadClass(className);
                    }
                }
            }
        }
    }

    private void loadClass(final String className) throws ClassNotFoundException {
        classes.add(Class.forName(className, true, classLoader));
    }

    private static boolean isClass(final String resource) {
        return resource.endsWith(CLASS_SUFFIX);
    }

    private static String getClassName(final String resource) {
        return resource.substring(0, resource.length() - CLASS_SUFFIX.length());
    }

}
