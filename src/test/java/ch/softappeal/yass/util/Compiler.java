package ch.softappeal.yass.util;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class Compiler {


  private Compiler() {
    // disable
  }

  private static final String JAVA_EXT = ".java";
  private static final String CLASS_EXT = ".class";

  private static URI toURI(final String name) {
    try {
      return new URI(name);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static URI uri(final JavaFileManager.Location location, final String packageName, final String relativeName) {
    return toURI(location.getName() + '/' + packageName + '/' + relativeName);
  }


  private static final class JavaFileObjectImpl extends SimpleJavaFileObject {

    private ByteArrayOutputStream byteCode; // If kind == CLASS, this stores byte code from openOutputStream
    private final CharSequence source; // if kind == SOURCE, this contains the source text

    JavaFileObjectImpl(final String baseName, final CharSequence source) {
      super(toURI(baseName + JAVA_EXT), Kind.SOURCE);
      this.source = source;
    }

    JavaFileObjectImpl(final String name, final Kind kind) {
      super(toURI(name), kind);
      source = null;
    }

    @Override public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
      return source;
    }

    @Override public InputStream openInputStream() {
      return new ByteArrayInputStream(getByteCode());
    }

    @Override public OutputStream openOutputStream() {
      byteCode = new ByteArrayOutputStream();
      return byteCode;
    }

    byte[] getByteCode() {
      return byteCode.toByteArray();
    }

  }


  private static final class ClassLoaderImpl extends ClassLoader {

    final Map<String, JavaFileObject> classes = new HashMap<>();

    ClassLoaderImpl(final ClassLoader parentClassLoader) {
      super(parentClassLoader);
    }

    @Override protected Class<?> findClass(final String qualifiedClassName) throws ClassNotFoundException {
      final JavaFileObject file = classes.get(qualifiedClassName);
      if (file != null) {
        final byte[] bytes = ((JavaFileObjectImpl)file).getByteCode();
        return defineClass(qualifiedClassName, bytes, 0, bytes.length);
      }
      return super.findClass(qualifiedClassName);
    }

    @Override public InputStream getResourceAsStream(final String name) {
      if (name.endsWith(CLASS_EXT)) {
        final JavaFileObjectImpl file = (JavaFileObjectImpl)classes.get(name.substring(0, name.length() - CLASS_EXT.length()).replace('/', '.'));
        if (file != null) {
          return new ByteArrayInputStream(file.getByteCode());
        }
      }
      return super.getResourceAsStream(name);
    }

  }


  private static final class FileManagerImpl extends ForwardingJavaFileManager<JavaFileManager> {

    private final ClassLoaderImpl classLoader;
    private final Map<URI, JavaFileObject> fileObjects = new HashMap<>();

    FileManagerImpl(final JavaFileManager fileManager, final ClassLoaderImpl classLoader) {
      super(fileManager);
      this.classLoader = classLoader;
    }

    void putFileForInput(final StandardLocation location, final String packageName, final String relativeName, final JavaFileObject file) {
      fileObjects.put(uri(location, packageName, relativeName), file);
    }

    @Override public FileObject getFileForInput(final Location location, final String packageName, final String relativeName) throws IOException {
      final FileObject fileObject = fileObjects.get(uri(location, packageName, relativeName));
      if (fileObject != null) {
        return fileObject;
      }
      return super.getFileForInput(location, packageName, relativeName);
    }

    @Override public JavaFileObject getJavaFileForOutput(final Location location, final String qualifiedName, final JavaFileObject.Kind kind, final FileObject outputFile) {
      final JavaFileObject file = new JavaFileObjectImpl(qualifiedName, kind);
      classLoader.classes.put(qualifiedName, file);
      return file;
    }

    @Override public ClassLoader getClassLoader(final Location location) {
      return classLoader;
    }

    @Override public String inferBinaryName(final Location loc, final JavaFileObject file) {
      return (file instanceof JavaFileObjectImpl) ? file.getName() : super.inferBinaryName(loc, file);
    }

    @Override public Iterable<JavaFileObject> list(final Location location, final String packageName, final Set<JavaFileObject.Kind> kinds, final boolean recurse) throws IOException {
      final Iterable<JavaFileObject> result = super.list(location, packageName, kinds, recurse);
      final Collection<JavaFileObject> files = new ArrayList<>();
      if ((location == StandardLocation.CLASS_PATH) && kinds.contains(JavaFileObject.Kind.CLASS)) {
        for (final JavaFileObject file : fileObjects.values()) {
          if ((file.getKind() == JavaFileObject.Kind.CLASS) && file.getName().startsWith(packageName)) {
            files.add(file);
          }
        }
        files.addAll(classLoader.classes.values());
      } else if ((location == StandardLocation.SOURCE_PATH) && kinds.contains(JavaFileObject.Kind.SOURCE)) {
        for (final JavaFileObject file : fileObjects.values()) {
          if ((file.getKind() == JavaFileObject.Kind.SOURCE) && file.getName().startsWith(packageName)) {
            files.add(file);
          }
        }
      }
      for (final JavaFileObject file : result) {
        files.add(file);
      }
      return files;
    }

  }


  private static String toString(final String reason, final DiagnosticCollector<JavaFileObject> diagnostics) {
    final StringWriter writer = new StringWriter();
    final PrintWriter printer = new PrintWriter(writer);
    printer.println(reason);
    for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
      printer.println(diagnostic);
    }
    return writer.toString();
  }

  public static ClassLoader compile(final ClassLoader loader, final Map<String, CharSequence> classes, final String... options) {
    final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    final ClassLoaderImpl classLoader = new ClassLoaderImpl(loader);
    final FileManagerImpl javaFileManager = new FileManagerImpl(compiler.getStandardFileManager(diagnostics, null, null), classLoader);
    final List<JavaFileObject> sources = new ArrayList<>();
    for (final Entry<String, CharSequence> entry : classes.entrySet()) {
      final String qualifiedClassName = entry.getKey();
      final int dotPos = qualifiedClassName.lastIndexOf('.');
      final String className = (dotPos == -1) ? qualifiedClassName : qualifiedClassName.substring(dotPos + 1);
      final JavaFileObjectImpl source = new JavaFileObjectImpl(className, entry.getValue());
      sources.add(source);
      javaFileManager.putFileForInput(StandardLocation.SOURCE_PATH, (dotPos == -1) ? "" : qualifiedClassName.substring(0, dotPos), className + JAVA_EXT, source);
    }
    if (!compiler.getTask(null, javaFileManager, diagnostics, Arrays.asList(options), null, sources).call()) {
      throw new RuntimeException(toString("Compilation failed.", diagnostics));
    } else if (!diagnostics.getDiagnostics().isEmpty()) {
      throw new RuntimeException(toString("Compilation warnings.", diagnostics));
    }
    return classLoader;
  }


}
