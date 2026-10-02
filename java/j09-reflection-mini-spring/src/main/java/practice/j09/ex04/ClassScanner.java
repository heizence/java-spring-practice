package practice.j09.ex04;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Finds the classes of a package and its sub-packages on the class path, the first step of component scanning.
 *
 * <p><strong>Given code: you don't need to change it</strong>, but it's worth reading. Spring's scanner does the
 * same job differently: it reads the {@code .class} files with a bytecode library and checks their annotations
 * <em>without loading the classes</em>, which keeps startup fast for big applications.
 */
public final class ClassScanner {

    private ClassScanner() {
    }

    /**
     * Returns every class in {@code basePackage} and its sub-packages, sorted by name, nested classes included. The
     * classes are loaded but not initialized: their static initializers don't run yet. A package that doesn't exist
     * gives an empty list.
     *
     * @throws IllegalArgumentException if {@code basePackage} is blank
     */
    public static List<Class<?>> findClasses(String basePackage) {
        Objects.requireNonNull(basePackage, "basePackage");
        if (basePackage.isBlank()) {
            throw new IllegalArgumentException("basePackage must not be blank");
        }
        String path = basePackage.replace('.', '/');
        ClassLoader loader = Objects.requireNonNullElse(
                Thread.currentThread().getContextClassLoader(), ClassScanner.class.getClassLoader());
        // A sorted set: the same package can appear in several class path entries (main and test classes, for
        // example), and a stable order makes the container's behavior repeatable.
        SortedSet<String> classNames = new TreeSet<>();
        try {
            Enumeration<URL> roots = loader.getResources(path);
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                switch (root.getProtocol()) {
                    case "file" -> collectFromDirectory(Path.of(root.toURI()), basePackage, classNames);
                    case "jar" -> collectFromJar(root, path, classNames);
                    default -> {
                        // Other kinds of class path entries are rare; this scanner skips them.
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return classNames.stream().<Class<?>>map(name -> load(name, loader)).toList();
    }

    private static void collectFromDirectory(Path directory, String basePackage, SortedSet<String> classNames)
            throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            files.filter(file -> file.toString().endsWith(".class"))
                    .map(file -> directory.relativize(file).toString().replace(File.separatorChar, '/'))
                    .map(relative -> basePackage + "." + toClassName(relative))
                    .filter(ClassScanner::isRegularClass)
                    .forEach(classNames::add);
        }
    }

    private static void collectFromJar(URL root, String path, SortedSet<String> classNames) throws IOException {
        var connection = (JarURLConnection) root.openConnection();
        connection.setUseCaches(false); // so closing the JarFile below doesn't close a shared, cached one
        try (JarFile jar = connection.getJarFile()) {
            jar.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.startsWith(path + "/") && name.endsWith(".class"))
                    .map(ClassScanner::toClassName)
                    .filter(ClassScanner::isRegularClass)
                    .forEach(classNames::add);
        }
    }

    /** {@code "practice/j09/Foo.class"} → {@code "practice.j09.Foo"}. */
    private static String toClassName(String classFile) {
        return classFile.substring(0, classFile.length() - ".class".length()).replace('/', '.');
    }

    private static boolean isRegularClass(String className) {
        return !className.endsWith("package-info") && !className.endsWith("module-info");
    }

    private static Class<?> load(String className, ClassLoader loader) {
        try {
            return Class.forName(className, false, loader); // false: don't run static initializers yet
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Found " + className + " on the class path, but can't load it", e);
        }
    }
}
